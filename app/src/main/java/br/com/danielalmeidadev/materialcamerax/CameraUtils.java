package br.com.danielalmeidadev.materialcamerax;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraUtils {

    public static final int DEGREES_90 = 90;
    public static final int DEGREES_270 = 270;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File makeTempFile(@NonNull Context context, @Nullable String saveDir, String prefix, String extension) {
        if (saveDir == null)
            saveDir = context.getCacheDir().getAbsolutePath();
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final File dir = new File(saveDir);
        dir.mkdirs();
        return new File(dir, prefix + timeStamp + extension);
    }

    @SuppressWarnings("all")
    public static void saveFileOnStorage(Context context, Uri imageUri, String saveDir) throws Exception {
        File file = new File(imageUri.getPath());
        String formattedFileName = file.getName().replace(context.getString(R.string.temp_img), "");

        String sourceFilePath = imageUri.getPath();
        String destinationFilePath = getBatchDirectoryName(saveDir) + File.separatorChar + formattedFileName;

        BufferedInputStream inputStream = null;
        BufferedOutputStream outputStream = null;

        try {
            inputStream = new BufferedInputStream(new FileInputStream(sourceFilePath));
            outputStream = new BufferedOutputStream(new FileOutputStream(destinationFilePath, false));
            byte[] buf = new byte[1024];
            inputStream.read(buf);
            do {
                outputStream.write(buf);
            } while (inputStream.read(buf) != -1);
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                throw e;
            }
        }
    }

    public static String getBatchDirectoryName(String saveDir) {
        String path = Environment.getExternalStorageDirectory().toString() + File.separatorChar + saveDir;

        File dir = new File(path);

        if (!dir.exists() && !dir.mkdirs()) {
            return "";
        }

        return path;
    }

    public static void clearApplicationTempImgData(Context context, String lastSavedImageName) {
        File[] files = context.getCacheDir().listFiles();

        if (files != null && files.length > 0) {
            for (File file : files) {
                String fileName = file.getName();

                if (fileName.contains(context.getString(R.string.temp_img)) && !fileName.equals(lastSavedImageName)) {
                    deleteDir(file);
                }
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deleteDir(File file) {
        assert file != null;
        file.delete();
    }

    @Nullable
    public static Bitmap getRotatedBitmap(String inputFile, int reqWidth, int reqHeight) {
        final int rotationInDegrees = getExifDegreesFromJpeg(inputFile);

        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(inputFile, opts);
        opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight, rotationInDegrees);
        opts.inJustDecodeBounds = false;

        final Bitmap originalBitmap = BitmapFactory.decodeFile(inputFile, opts);

        if (originalBitmap == null)
            return null;

        Matrix matrix = new Matrix();
        matrix.preRotate(rotationInDegrees);

        return Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
    }

    @SuppressWarnings("all")
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight, int rotationInDegrees) {

        final int height;
        final int width;
        int inSampleSize = 1;

        if (rotationInDegrees == DEGREES_90 || rotationInDegrees == DEGREES_270) {
            width = options.outHeight;
            height = options.outWidth;
        } else {
            height = options.outHeight;
            width = options.outWidth;
        }

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            inSampleSize = Math.min(heightRatio, widthRatio);
        }

        return inSampleSize;
    }

    private static int getExifDegreesFromJpeg(String inputFile) {
        try {
            final ExifInterface exif = new ExifInterface(inputFile);

            final int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    return 90;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    return 90;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    return -90;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    return -270;
            }
        } catch (IOException e) {
            Log.e("Error", "Error when trying to get exif data from : " + inputFile, e);
        }

        return 0;
    }
}
