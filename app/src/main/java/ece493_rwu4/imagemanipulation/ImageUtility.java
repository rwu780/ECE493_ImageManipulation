package ece493_rwu4.imagemanipulation;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by James on 2018-01-30.
 */

public class ImageUtility {

    public File createFile(){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File photoFile = new File(storage + "/" + imageFileName + ".jpg");
        return photoFile;
    }

    public void savePictureToGallery(Context context, File photoFile){

        Uri imageUri = Uri.fromFile(photoFile);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri);
        context.sendBroadcast(mediaScanIntent);
    }
    public void saveBitmapToGallery(Context context, Bitmap bitmap){
        File newFile = createFile();
        try {
            FileOutputStream fos = new FileOutputStream(newFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, fos);
            fos.flush();
            fos.close();

            savePictureToGallery(context, newFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
