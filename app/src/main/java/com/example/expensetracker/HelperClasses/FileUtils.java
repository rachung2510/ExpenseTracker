package com.example.expensetracker.HelperClasses;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.example.expensetracker.MainActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Calendar;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class FileUtils {

    public static final String TAG = "FileUtils";

    /**
     * Creates the specified toFile as a byte for byte copy of the fromFile.
     * If toFile already exists, then it will be replaced with a copy of fromFile.
     * The name and path of toFile will be that of toFile.
     * fromFile. If toFile already exists, then it
     * Note: fromFile and toFile will be closed by this function.
     *
     * @param fromFile FileInputStream for the file to copy from.
     * @param toFile FileInputStream for the file to copy to.
     */
    public static void copyFile(FileInputStream fromFile, FileOutputStream toFile) throws IOException {
        FileChannel fromChannel = null;
        FileChannel toChannel = null;
        try {
            fromChannel = fromFile.getChannel();
            toChannel = toFile.getChannel();
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        } finally {
            try { if (fromChannel != null) fromChannel.close(); }
            finally { if (toChannel != null) toChannel.close(); }
        }
    }

    public static File createImageFile(Context context) {
        String timeStamp = MainActivity.getDatetimeStr(Calendar.getInstance(),"yyyyMMdd_HHmmss");
        String imageFileName = "photo_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    public static ByteArrayInputStream getInputStreamFromUri(Context context, Uri uri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

        // rotate
        ExifInterface exif = new ExifInterface(context.getContentResolver().openInputStream(uri));
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        if (orientation != ExifInterface.ORIENTATION_NORMAL) {
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true); // rotating bitmap
        }

        // compression
        AssetFileDescriptor fileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri , "r");
        int size = (int) fileDescriptor.getLength();
        int compression = (int) (1.05e8 / size);
        if (compression > 100) compression = 100;

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, compression, bytes);
        byte[] bitmapData = bytes.toByteArray();
        return new ByteArrayInputStream(bitmapData);
    }

    public static RequestBody createRequestBody(final MediaType mediaType, final InputStream inputStream) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return mediaType;
            }

            @Override
            public long contentLength() {
                try {
                    return inputStream.available();
                } catch (IOException e) {
                    return 0;
                }
            }

            @Override
            public void writeTo(@NonNull BufferedSink sink) throws IOException {
                Source source = null;
                try {
                    source = Okio.source(inputStream);
                    sink.writeAll(source);
                } finally {
                    Util.closeQuietly(source);
                }
            }
        };
    }
}