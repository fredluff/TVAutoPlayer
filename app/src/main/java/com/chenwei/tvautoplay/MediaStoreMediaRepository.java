
package com.chenwei.tvautoplay;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MediaStoreMediaRepository {

    private static final String TAG = "EUI_DIAG";

    public List<PlaylistItem> queryMedia(Context context) {
        ContentResolver resolver = context.getContentResolver();
        List<PlaylistItem> discovered = new ArrayList<>();

        Log.i(TAG, "===== MediaStore scan START =====");
        Log.i(TAG, "SDK = " + Build.VERSION.SDK_INT);

        Set<Uri> collections;
        try {
            collections = mediaCollections(context);
            Log.i(TAG, "Collections = " + collections);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to enumerate volumes", e);
            collections = new LinkedHashSet<>();
        }

        for (Uri collection : collections) {
            queryCollection(resolver, collection, discovered);
        }

        // Additional diagnostic query only.
        // Does not change the actual playlist results.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri mergedUri = MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL);

            try (Cursor cursor = resolver.query(
                    mergedUri,
                    new String[]{MediaStore.MediaColumns._ID},
                    null, null, null)) {

                Log.i(TAG, "Merged video count = " +
                        (cursor == null ? "NULL" : cursor.getCount()));

            } catch (RuntimeException e) {
                Log.e(TAG, "Merged video query FAILED", e);
            }
        }

        Log.i(TAG, "Discovered before normalize = " +
                discovered.size());

        List<PlaylistItem> result =
                MediaCatalog.normalize(discovered);

        Log.i(TAG, "Final count = " + result.size());
        Log.i(TAG, "===== MediaStore scan END =====");

        return result;
    }

    private Set<Uri> mediaCollections(Context context) {
        Set<Uri> collections = new LinkedHashSet<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Set<String> volumes =
                    MediaStore.getExternalVolumeNames(context);

            Log.i(TAG, "Volumes = " + volumes);

            for (String volumeName : volumes) {
                collections.add(
                        MediaStore.Video.Media.getContentUri(volumeName));
                collections.add(
                        MediaStore.Images.Media.getContentUri(volumeName));
            }
        } else {
            collections.add(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            collections.add(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }

        return collections;
    }

    private void queryCollection(
            ContentResolver resolver,
            Uri collection,
            List<PlaylistItem> destination) {

        Log.i(TAG, "Query URI = " + collection);

        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE
        };

        int skippedSize = 0;
        int nullSize = 0;
        int skippedType = 0;
        int added = 0;

        try (Cursor cursor = resolver.query(
                collection, projection, null, null, null)) {

            if (cursor == null) {
                Log.w(TAG, "Query returned NULL cursor");
                return;
            }

            Log.i(TAG, "Raw count = " + cursor.getCount());

            int idColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.DISPLAY_NAME);
            int mimeColumn = cursor.getColumnIndex(
                    MediaStore.MediaColumns.MIME_TYPE);
            int sizeColumn = cursor.getColumnIndex(
                    MediaStore.MediaColumns.SIZE);

            while (cursor.moveToNext()) {

                if (sizeColumn >= 0) {
                    if (cursor.isNull(sizeColumn)) {
                        // EUI compatibility: missing size metadata
                        // does not necessarily mean the file is empty.
                        nullSize++;

                        Log.w(TAG,
                                "MediaStore SIZE is NULL, allowing item");
                    } else if (cursor.getLong(sizeColumn) <= 0) {
                        // Skip only when a non-positive size is confirmed.
                        skippedSize++;
                        continue;
                    }
                }

                String name = cursor.getString(nameColumn);
                String mime = mimeColumn < 0
                        ? null : cursor.getString(mimeColumn);

                MediaKind kind =
                        MediaTypeDetector.detect(mime, name);

                if (kind == null) {
                    skippedType++;
                    continue;
                }

                long id = cursor.getLong(idColumn);
                Uri mediaUri =
                        ContentUris.withAppendedId(collection, id);

                destination.add(
                        new PlaylistItem(
                                mediaUri.toString(), name, kind));
                added++;
            }

            Log.i(TAG, "URI = " + collection);
            Log.i(TAG, "Added = " + added);
            Log.i(TAG, "Skipped size = " + skippedSize);
            Log.i(TAG, "Null size = " + nullSize);
            Log.i(TAG, "Skipped type = " + skippedType);

        } catch (RuntimeException e) {
            Log.e(TAG, "Query FAILED: " + collection, e);
        }
    }
}
