package bcr6.uow.comp548.application;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Brendan on 17/02/17.
 *
 */

public class ImageHelper {

    static void setUpDirectory() throws IOException {
        String storageDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/Friends";
        File f = new File(storageDirPath);
        if (!f.exists())
            if (!f.mkdirs())
                throw new IOException("Unable to create home directory");
    }

    public static Bitmap bitmapSmaller(String filePath, int reqWidth, int reqHeight) {
        return decodeSampledBitmapFromString(filePath, reqWidth, reqHeight);
    }

// --Commented out by Inspection START (5/4/17 11:53 AM):
//    public static Bitmap bitmapSmaller(Resources resources, int resID, int reqWidth, int reqHeight) {
//        return decodeSampledBitmapFromResource(resources, resID, reqWidth, reqHeight);
//    }
// --Commented out by Inspection STOP (5/4/17 11:53 AM)

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private static Bitmap decodeSampledBitmapFromString(String photo,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photo, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(photo, options);
    }

    public static String createImageFile(Activity a) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

        String imagePath = a.getFilesDir().getAbsolutePath() + "/JPEG_" + timeStamp + ".jpg";
        try {
            if (!new File(imagePath).createNewFile())
                throw new IOException("Unable to create new image file");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("IMAGE", e.getMessage());
        }
        return imagePath;
    }

}
