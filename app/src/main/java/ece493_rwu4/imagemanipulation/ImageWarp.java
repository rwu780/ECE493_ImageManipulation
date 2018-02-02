package ece493_rwu4.imagemanipulation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import java.io.File;

/**
 * Created by James on 2018-01-30.
 */

public class ImageWarp {

    private Bitmap bitmap;
    private int[] verts;
    private int[] original;

    public ImageWarp(Bitmap bitmap){
        this.bitmap = bitmap;
    }

    public Bitmap blur(){
        int distance = 1;
        verts = new int[bitmap.getByteCount()];
        original = new int[bitmap.getByteCount()];

        bitmap.getPixels(original, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        for(int x = 0; x < width; x ++){
            for (int y = 0; y < height; y++){
                int color = 0;
                int size = 0;
                if(x - distance >= 0 && x + distance < width && y-distance >= 0 && y+distance < height){
                    color += original[y*width+x-distance];
                    color += original[y*width+x];
                    color += original[y*width+x+distance];
                    color += original[(y-distance)*width+x-distance];
                    color += original[(y-distance)*width+x];
                    color += original[(y-distance)*width+x+distance];
                    color += original[(y+distance)*width+x-distance];
                    color += original[(y+distance)*width+x];
                    color += original[(y+distance)*width+x+distance];
                    verts[y*width + x] = color / 9;
                }
                else{
                    verts[y*width + x] = 0;
                }
            }
        }
        try {
            bitmap.setPixels(verts, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        } catch (Exception e){
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            bitmap.setPixels(verts, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        }
        verts = null;
        original = null;
        return bitmap;
    }

    public Bitmap fisheye(float cx, float cy){
        verts = new int[bitmap.getByteCount()];
        original = new int[bitmap.getByteCount()];
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        bitmap.getPixels(original, 0, width, 0, 0, width, height);

        double maxWidth = Math.pow(cx + width/2, 2);
        double maxHeight = Math.pow(cy + width/2, 2);
        double max_r = width/2;

        //for each pixel
        for(int x = 0; x<width; x++){
            for (int y = 0; y<height; y++){
                double r = Math.sqrt(Math.pow(x - cx, 2) + Math.pow(y - cy, 2));
                if(r < max_r){
                    verts[y*width + x] = original[y* width + x];
                }

                //Calculate the distance from(nx, ny) to center (cx, cy)
                //Convert (nx, ny) to polar coordinates
//                double rx = nx - cx;
//                double ry = ny - cy;
//
//                double rxx = Math.pow(rx, 2);
//                double ryy = Math.pow(ry, 2);
//                double r = Math.sqrt(rxx + ryy);
//
//                //pixels outside the sphere will remain the same
//                if(r >= 0.0 && r <= 1.0){
//                    double nr = r + (1 - Math.sqrt(r * r)) / 2;
//
//                    if (nr < 1.0){
//                        double theta = Math.atan(ry / rx);
//
//                        double newX = nr * Math.cos(theta);
//                        double newY = nr * Math.sin(theta);
//
//                        int x2 = (int) ((newX + 1)*width/2.0);
//                        int y2 = (int) ((newY + 1) * height/2.0);
//
//                        int srcpos = (int) (y2 * width + x2);
//                        if(srcpos >= 0 & srcpos < width * height){
//                            verts[y*width + x] = original[srcpos];
//                        }
//
//                    }
//                }else{
////                    verts[y*width + x] = original[y*width + x];
//                }
            }
        }

        try {
            bitmap.setPixels(verts, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        } catch (Exception e){
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            bitmap.setPixels(verts, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        }
        verts = null;
        original = null;
        return bitmap;
    }

    public Bitmap swirl(){

        verts = new int[bitmap.getByteCount()];
        original = new int[bitmap.getByteCount()];

        bitmap.getPixels(original, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        bitmap.getPixels(verts, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());


        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        double cx = width / 2;
        double cy = height / 2;
        double factor = 0.0005;

        for(int y = 0; y<height; y++){
            double relY = cy - y;

            for(int x = 0; x<width; x++){
                double relX = x - cx;

                double originalAngle;
                if(relX != 0){
                    originalAngle = Math.atan(Math.abs(relY)/Math.abs(relX));
                    if(relX > 0 && relY < 0){
                        originalAngle = 2.0 * Math.PI - originalAngle;
                    }
                    else if(relX <= 0 && relY >= 0){
                        originalAngle = Math.PI - originalAngle;
                    }
                    else if(relX <= 0 && relY < 0){
                        originalAngle += Math.PI;

                    }
                }
                else{
                    if(relY >= 0){
                        originalAngle = 0.5 * Math.PI;
                    }
                    else{
                        originalAngle = 1.5 * Math.PI;
                    }
                }

                //Calculate the distance from the center of the UV using
                double radius = Math.sqrt(relX * relX + relY * relY);
                double newAngle = originalAngle + 1/(factor*radius+(4.0/Math.PI));

                int srcX = (int) Math.floor((radius*Math.cos(newAngle) + 0.5));
                int srcY = (int) Math.floor(radius*Math.sin(newAngle) + 0.5);

                srcX += cx;
                srcY += cy;
                srcY = height - srcY;

                if(srcX < 0){
                    srcX = 0;
                }
                else if(srcX >= width){
                    srcX = width - 1;
                }

                if(srcY < 0){
                    srcY = 0;
                }else if(srcY >= height){
                    srcY = height - 1;
                }

                verts[y*width + x] = original[srcY*width + srcX];
            }
        }
        try {
            bitmap.setPixels(verts, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        } catch (Exception e){
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            bitmap.setPixels(verts, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        }
        verts = null;
        original = null;
        return bitmap;
    }
}
