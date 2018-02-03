package ece493_rwu4.imagemanipulation;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import android.support.v4.content.ContextCompat;

import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.LruCache;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;

public class MainActivity extends AppCompatActivity
{
    //Status Code
    static final int REQUEST_IMAGE_CAPTURE = 188;
    static final int RESULT_LOAD_IMAGE = 288;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1001;

    //Define Gesture
    private GestureDetectorCompat mDetector;

    ImageUtility imgUtil;
    ImageView imageView;
    Bitmap bitMap = null;

    private int maxNumberOfKeys = 10;
    private int numberOfKeys = 5;
    private List<Integer> keys;
    private LruCache<Integer, Bitmap> mMemoryCache;

    File photoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check Write Permission
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }

        //Create Bitmap Cache
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<Integer, Bitmap>(cacheSize){
            protected int sizeOf(Integer key, Bitmap bitmap){
                return bitmap.getByteCount() / 1024;
            }
        };

        //Load Gestures
        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        keys = new ArrayList<Integer>();
        imgUtil = new ImageUtility();
        imageView = (ImageView) findViewById(R.id.imageView);

        //Load Photo Button
        Button loadImageButton = (Button) findViewById(R.id.loadImageButton);
        loadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mediaScanIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(mediaScanIntent, RESULT_LOAD_IMAGE);
            }
        });

        //Take Photo
        Button takePhotoButton = (Button) findViewById(R.id.takePhotoButton);
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        //Save Photo
        Button savePhotoButton = (Button) findViewById(R.id.savePhotoButton);
        savePhotoButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(bitMap != null) {
                    imgUtil.saveBitmapToGallery(getApplicationContext(), bitMap);
                    Toast.makeText(getBaseContext(), "Image Saved", Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(getBaseContext(), "No Image", Toast.LENGTH_LONG).show();
                }

            }
        });

        //Undo Photo
        Button undoPhotoButton = (Button) findViewById(R.id.undoButton);
        undoPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(keys.size() == 0){
                    Toast.makeText(getBaseContext(), "No Previous Warp", Toast.LENGTH_LONG).show();
                }else {
                    int key = keys.size() - 1;
                    bitMap = getBitmapFromMemCache(key);
                    keys.remove(key);
                    imageView.setImageBitmap(bitMap);
                    Toast.makeText(getBaseContext(), "Yes", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public boolean onTouchEvent(MotionEvent event){
        return mDetector.onTouchEvent(event);
    }

    @Override
    //Reference from: https://developer.android.com/guide/topics/ui/menus.html
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;

    }

    public boolean onOptionsItemSelected(MenuItem item){

        switch (item.getItemId()){
            case R.id.preference:
                setPreference();
                Log.d("Main", "Preference: " + numberOfKeys);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setPreference(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Set number of undos");
        builder.setMessage("Maximum number of undos is 10");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                int num = new Integer(input.getText().toString());
                if(num > maxNumberOfKeys){
                    num = maxNumberOfKeys;
                }
                numberOfKeys = num;
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();

    }

    public void addBitmapToMemoryCache(Bitmap bitmap){
        int key = keys.size();
        if(getBitmapFromMemCache(key) == null){
            mMemoryCache.put(key, bitmap);
            keys.add(key);
        }

        if(keys.size() > numberOfKeys){
            keys.remove(0);
            mMemoryCache.remove(0);
        }

    }

    public Bitmap getBitmapFromMemCache(Integer key){
        return mMemoryCache.get(key);
    }


    //Reference: https://developer.android.com/training/camera/photobasics.html
    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //Ensure that there's a camera activity to handle the intent
        if(takePictureIntent.resolveActivity(getPackageManager()) != null) {

            photoFile = imgUtil.createFile();

            if(photoFile != null){
                Uri photoURI = Uri.fromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //Capture Image and save to phone
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK){
            imgUtil.savePictureToGallery(this, photoFile);
            bitMap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
        }

        //Load Image
        //Reference: https://stackoverflow.com/questions/38352148/get-image-from-the-gallery-and-show-in-imageview
        if(requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                bitMap = BitmapFactory.decodeStream(imageStream);
            } catch (Exception e) {
                Toast.makeText(this, "Something went wrong, Unable to load image from phone", Toast.LENGTH_LONG);
            }
        }

        if(bitMap != null) {
            int width = imageView.getWidth();
            int height = imageView.getHeight();
            bitMap = Bitmap.createScaledBitmap(bitMap, width, height, false);
            imageView.setImageBitmap(bitMap);
        }
    }

    @Override
    public void onBackPressed(){
        new AlertDialog.Builder(this)
                .setTitle("Exit?")
                .setMessage("Do you want to exit")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        MainActivity.super.onBackPressed();
                    }
                }).create().show();
    }

    //Gesture Listener
    class MyGestureListener extends GestureDetector.SimpleOnGestureListener{
        private static final String DEBUT_TAG = "Gestures";
        ImageWarp imageWarp;

        public void onLongPress(MotionEvent event){
            //Implement Blur
            Log.d(DEBUT_TAG, "onLongPress:" + event.toString());
            addBitmapToMemoryCache(bitMap);
            imageWarp = new ImageWarp(bitMap);
            bitMap = imageWarp.blur();
            imageView.setImageBitmap(bitMap);
            imageWarp = null;
        }

        public boolean onDoubleTap(MotionEvent event){
            //Implement Ripple
            Log.d(DEBUT_TAG, "onDoubleTap:"+event.toString());
            imageWarp = new ImageWarp(bitMap);
            bitMap = imageWarp.ripple();
            imageView.setImageBitmap(bitMap);
            imageWarp = null;
            return true;
        }

        public boolean onSingleTapUp(MotionEvent event){
            //Implement Fish Eye
            Log.d(DEBUT_TAG, "onDoubleTap:"+event.toString());
            imageWarp = new ImageWarp(bitMap);
            bitMap = imageWarp.fisheye();
            imageView.setImageBitmap(bitMap);
            imageWarp = null;
            return true;
        }


        public boolean onFling(MotionEvent event, MotionEvent event1, float v, float v1){
            //Implement Swirl
            Log.d(DEBUT_TAG, "onFling"+event.toString());
            addBitmapToMemoryCache(bitMap);
            imageWarp = new ImageWarp(bitMap);
            bitMap = imageWarp.swirl();
            imageView.setImageBitmap(bitMap);
            imageWarp = null;
            return true;
        }
    }
}