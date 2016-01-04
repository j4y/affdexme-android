package com.affectiva.affdexme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageHelper {

    private static final String LOG_TAG = ImageHelper.class.getSimpleName();

    // Prevent instantiation of this object
    private ImageHelper() {
    }

    public static boolean checkIfImageFileExists(@NonNull Context context, @NonNull String fileName) {

        // path to /data/data/yourapp/app_data/images
        File directory = context.getDir("images", Context.MODE_PRIVATE);

        // File location to save image
        File imagePath = new File(directory, fileName);

        return imagePath.exists();
    }

    public static boolean deleteImageFile(@NonNull Context context, @NonNull String fileName) {
        // path to /data/data/yourapp/app_data/images
        File directory = context.getDir("images", Context.MODE_PRIVATE);

        // File location to save image
        File imagePath = new File(directory, fileName);

        return imagePath.delete();
    }

    public static void resizeAndSaveResourceImageToInternalStorage(@NonNull Context context, @NonNull String fileName, @NonNull String resourceName) throws FileNotFoundException {
        final int resourceId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());

        if (resourceId == 0) {
            //unrecognised resource
            throw new FileNotFoundException("Resource not found for file named: " + resourceName);
        }
        resizeAndSaveResourceImageToInternalStorage(context, fileName, resourceId);
    }

    public static void resizeAndSaveResourceImageToInternalStorage(@NonNull Context context, @NonNull String fileName, int resourceId) {
        Resources resources = context.getResources();
        Bitmap sourceBitmap = BitmapFactory.decodeResource(resources, resourceId);
        Bitmap resizedBitmap = resizeBitmapForDeviceDensity(context, sourceBitmap);
        saveBitmapToInternalStorage(context, resizedBitmap, fileName);
        sourceBitmap.recycle();
        resizedBitmap.recycle();
    }

    public static Bitmap resizeBitmapForDeviceDensity(@NonNull Context context, @NonNull Bitmap sourceBitmap) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        int targetWidth = Math.round(sourceBitmap.getWidth() * metrics.density);
        int targetHeight = Math.round(sourceBitmap.getHeight() * metrics.density);

        return Bitmap.createScaledBitmap(sourceBitmap, targetWidth, targetHeight, false);
    }

    public static void saveBitmapToInternalStorage(@NonNull Context context, @NonNull Bitmap bitmapImage, @NonNull String fileName) {

        // path to /data/data/yourapp/app_data/images
        File directory = context.getDir("images", Context.MODE_PRIVATE);

        // File location to save image
        File imagePath = new File(directory, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imagePath);

            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "Exception while trying to save file to internal storage: " + imagePath, e);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Exception while trying to flush the output stream", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Exception wile trying to close file output stream.", e);
                }
            }
        }
    }

    public static Bitmap loadBitmapFromInternalStorage(@NonNull Context applicationContext, @NonNull String fileName) {

        // path to /data/data/yourapp/app_data/images
        File directory = applicationContext.getDir("images", Context.MODE_PRIVATE);

        // File location to save image
        File imagePath = new File(directory, fileName);

        try {
            return BitmapFactory.decodeStream(new FileInputStream(imagePath));
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "Exception wile trying to load image: " + imagePath, e);
            return null;
        }
    }

    public static void preproccessImageIfNecessary(Context context, String fileName, String resourceName) {
        // Set this to true to force the app to always load the images for debugging purposes
        final boolean DEBUG = false;

        if (ImageHelper.checkIfImageFileExists(context, fileName)) {
            // Image file already exists, no need to load the file again.

            if (DEBUG) {
                Log.d(LOG_TAG, "DEBUG: Deleting: " + fileName);
                ImageHelper.deleteImageFile(context, fileName);
            } else {
                return;
            }
        }

        try {
            ImageHelper.resizeAndSaveResourceImageToInternalStorage(context, fileName, resourceName);
            Log.d(LOG_TAG, "Resized and saved image: " + fileName);
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "Unable to process image: " + fileName, e);
            throw new RuntimeException(e);
        }
    }
}
