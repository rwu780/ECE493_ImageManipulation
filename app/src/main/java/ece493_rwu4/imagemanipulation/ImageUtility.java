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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by James on 2018-01-30.
 */

public class ImageUtility {

    private List<int[]> bitmapList;
    private final int maxNumber = 10; // Maximum number of undos is 10
    private int numberOfUndos = 3;

    public ImageUtility(){
        bitmapList = new ArrayList<int[]>();
    }

    public void addBitMapToCache(int[] pixels){
        bitmapList.add(pixels);

        if(cacheSize() > numberOfUndos){
            bitmapList.remove(0);
        }
    }

    public int cacheSize(){
        return bitmapList.size();
    }

    public int[] getLastBitmap(){
        int lastIndex = cacheSize() - 1;
        int[] bm = bitmapList.get(lastIndex);
        bitmapList.remove(lastIndex);
        return bm;
    }

    public void setNumberOfUndos(int x){
        if(x > maxNumber){
            numberOfUndos = maxNumber;
        }
        else{
            numberOfUndos = x;
        }
    }

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
