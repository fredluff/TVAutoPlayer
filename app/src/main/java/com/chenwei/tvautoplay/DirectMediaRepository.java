package com.chenwei.tvautoplay;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Strict TVAutoPlay folder scanner.
 *
 * On each accessible storage volume, only these directories are enumerated:
 *   Movies/TVAutoPlay/**
 *   Pictures/TVAutoPlay/**
 * No MediaStore query and no storage-root or parent-directory traversal here.
 * Runs on the caller's thread: invoke from a background thread.
 */
public final class DirectMediaRepository {
    private static final String TAG = "EUI_FILE";
    private static final String[] ALLOWED_RELATIVE_PATHS = {
            "Movies/TVAutoPlay",
            "Pictures/TVAutoPlay"
    };
    private static final int MAX_DEPTH = 8;
    private static final int MAX_VISITS = 30000;

    private static final class PendingDir {
        final File directory;
        final String allowedCanonicalRoot;
        final String label;
        final int depth;

        PendingDir(File directory, String allowedCanonicalRoot,
                   String label, int depth) {
            this.directory = directory;
            this.allowedCanonicalRoot = allowedCanonicalRoot;
            this.label = label;
            this.depth = depth;
        }
    }

    public List<PlaylistItem> queryMedia(Context context) {
        ArrayList<PlaylistItem> found = new ArrayList<>();
        Set<String> visitedDirs = new HashSet<>();
        Set<String> visitedFiles = new HashSet<>();
        ArrayDeque<PendingDir> queue = new ArrayDeque<>();

        // Only generate two allowed target paths per volume. Do NOT list the
        // volume root or any parent directory (Movies or Pictures).
        for (File volumeRoot : collectVolumeRoots(context)) {
            String canonicalVolumeRoot = canonicalPath(volumeRoot);
            if (canonicalVolumeRoot == null) continue;
            for (String relativePath : ALLOWED_RELATIVE_PATHS) {
                File target = new File(volumeRoot, relativePath);
                String canonicalTarget = canonicalPath(target);
                String expectedTarget = new File(
                        canonicalVolumeRoot, relativePath).getPath();

                // Refuse redirected/symlinked target folders, so even a
                // symlink cannot make the scanner enumerate outside scope.
                if (canonicalTarget == null || !canonicalTarget.equals(expectedTarget)) {
                    Log.w(TAG, "TARGET_REDIRECT_BLOCKED path=" + target
                            + " resolved=" + canonicalTarget);
                    continue;
                }

                Log.i(TAG, "TARGET_CHECK path=" + target.getAbsolutePath()
                        + " exists=" + target.exists()
                        + " directory=" + target.isDirectory()
                        + " readable=" + target.canRead());
                if (!target.isDirectory()) {
                    continue;
                }
                queue.addLast(new PendingDir(
                        target, canonicalTarget, relativePath, 0));
            }
        }

        int visitedEntries = 0;
        int skippedDirs = 0;
        int skippedSize = 0;
        int skippedType = 0;
        int skippedUnreadable = 0;
        int blockedOutside = 0;
        int skippedDepth = 0;

        while (!queue.isEmpty() && visitedEntries < MAX_VISITS) {
            PendingDir current = queue.removeFirst();
            String dirPath = canonicalPath(current.directory);
            if (!insideAllowedTarget(dirPath, current.allowedCanonicalRoot)) {
                blockedOutside++;
//                Log.w(TAG, "OUTSIDE_SCOPE_BLOCKED dir=" + current.directory);
                continue;
            }
            if (!visitedDirs.add(dirPath)) {
                continue;
            }

            File[] children;
            try {
                children = current.directory.listFiles();
            } catch (SecurityException exception) {
                skippedDirs++;
//                Log.w(TAG, "TARGET_DENIED dir=" + current.directory, exception);
                continue;
            }
            if (children == null) {
                skippedDirs++;
                Log.w(TAG, "TARGET_NULL dir=" + current.directory
                        + " exists=" + current.directory.exists()
                        + " readable=" + current.directory.canRead());
                continue;
            }
            Log.i(TAG, "TARGET_SCAN dir=" + current.directory
                    + " children=" + children.length);

            for (File child : children) {
                if (++visitedEntries > MAX_VISITS) break;
                String childPath = canonicalPath(child);
                if (!insideAllowedTarget(childPath, current.allowedCanonicalRoot)) {
                    blockedOutside++;
//                    Log.w(TAG, "OUTSIDE_SCOPE_BLOCKED item=" + child);
                    continue;
                }

                if (child.isDirectory()) {
                    if (current.depth >= MAX_DEPTH) {
                        skippedDepth++;
//                        Log.w(TAG, "DEPTH_LIMIT dir=" + child);
                    } else {
                        queue.addLast(new PendingDir(child,
                                current.allowedCanonicalRoot,
                                current.label, current.depth + 1));
                    }
                    continue;
                }
                if (!child.isFile()) continue;

                String fileName = child.getName();
                MediaKind kind = MediaTypeDetector.detect(null, fileName);
                if (kind == null) {
                    skippedType++;
                    continue;
                }
                if (!child.canRead()) {
                    skippedUnreadable++;
//                    Log.w(TAG, "UNREADABLE file=" + child);
                    continue;
                }
                long size = child.length();
                if (size <= 0) {
                    skippedSize++;
//                    Log.w(TAG, "BAD_SIZE=" + size + " file=" + child);
                    continue;
                }
                if (visitedFiles.add(childPath)) {
                    found.add(new PlaylistItem(
                            Uri.fromFile(child).toString(), fileName, kind));
                    Log.i(TAG, "FOUND file=" + child.getAbsolutePath());
                }
            }
        }

        Log.i(TAG, "STRICT_DONE files=" + found.size()
                + " visited=" + visitedEntries
                + " skippedDirs=" + skippedDirs
                + " skippedType=" + skippedType
                + " skippedSize=" + skippedSize
                + " skippedUnreadable=" + skippedUnreadable
                + " blockedOutside=" + blockedOutside
                + " skippedDepth=" + skippedDepth
                + " hitLimit=" + (visitedEntries >= MAX_VISITS));
        return MediaCatalog.normalize(found);
    }

    private static boolean insideAllowedTarget(
            String canonicalPath, String allowedCanonicalRoot) {
        return canonicalPath != null && allowedCanonicalRoot != null
                && (canonicalPath.equals(allowedCanonicalRoot)
                || canonicalPath.startsWith(allowedCanonicalRoot + File.separator));
    }

    private static Set<File> collectVolumeRoots(Context context) {
        LinkedHashSet<File> roots = new LinkedHashSet<>();
        roots.add(Environment.getExternalStorageDirectory());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            StorageManager manager = context.getSystemService(StorageManager.class);
            if (manager != null) {
                try {
                    for (StorageVolume volume : manager.getStorageVolumes()) {
                        String state = volume.getState();
                        File root = volume.getDirectory();
                        Log.i(TAG, "VOLUME state=" + state
                                + " removable=" + volume.isRemovable()
                                + " root=" + (root == null
                                ? "NULL" : root.getAbsolutePath()));
                        if (root != null && (Environment.MEDIA_MOUNTED.equals(state)
                                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))) {
                            roots.add(root);
                        }
                    }
                } catch (RuntimeException exception) {
                    Log.w(TAG, "VOLUME_ENUM_FAILED", exception);
                }
            }
        }
        return roots;
    }

    private static String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException | SecurityException exception) {
            // Fail closed: do not scan paths that cannot be resolved safely.
//            Log.w(TAG, "CANONICAL_FAILED path=" + file, exception);
            return null;
        }
    }
}
