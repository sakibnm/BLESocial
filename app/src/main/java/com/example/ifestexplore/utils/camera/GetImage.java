package com.example.ifestexplore.utils.camera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.ifestexplore.R;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GetImage extends AppCompatActivity {

    private MaterialButton button_cancel, button_ok;
    private ImageView imageView;
    String fileDir = "";
    String lensface = "";
    private int FLIP_X = 1;
    private int FLIP_Y = 2;
    private int FLIP_NONE = 3;
    private ConstraintLayout progressContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_image);

        imageView = findViewById(R.id.preview_image);

        fileDir = getIntent().getStringExtra("filedir");
        lensface = getIntent().getStringExtra("lensface");

        Bitmap bitmap = BitmapFactory.decodeFile(fileDir);

//        Working with Exif
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(fileDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        float rotation = getRotation(orientation);

        if (lensface.equals("FRONT"))bitmap = rotateAndScaleBitmap(bitmap, FLIP_X , rotation);
        else bitmap = rotateAndScaleBitmap(bitmap, FLIP_NONE , rotation);
//        if (purpose!=null && this.purpose.equals("PROFILE"))bitmap = demirrorBitmap(bitmap);

        String filepath = replaceImageFile(bitmap);

        imageView.setImageBitmap(bitmap);

        button_cancel = findViewById(R.id.materialButton_retry);
        button_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        button_ok = findViewById(R.id.materialButton_ok);
        button_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("filepath", filepath);
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        });
    }

    private float getRotation(int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                Log.d("demo", "Rotation 90: "+ExifInterface.ORIENTATION_ROTATE_90);
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                Log.d("demo", "Rotation 180: "+ExifInterface.ORIENTATION_ROTATE_180);
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                Log.d("demo", "Rotation 270: "+ExifInterface.ORIENTATION_ROTATE_270);
                return 270;
            default:
                return 0;
        }
    }

    public Bitmap rotateAndScaleBitmap(Bitmap source, int flip, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.preRotate(angle);
        switch(flip){
            case 1:
                matrix.postScale(-1.0f,1.0f);
                break;
            case 2:
                matrix.postScale(1.0f,-1.0f);
                break;
            default:
                break;
        }

        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
    }

//    private Bitmap demirrorBitmap(Bitmap source) {
//        Matrix matrix = new Matrix();
//        matrix.preScale(-1.0f, 1.0f);
////        matrix.postScale(-1.0f,1.0f);
//        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
//    }

    private String replaceImageFile(Bitmap bitmap) {
        // Create an image file name
        String currentPhotoPath = fileDir;
        File file;
        FileOutputStream out = null;
        try {
            file = new File(currentPhotoPath);
            if (file.exists()){
                file.delete();
                file.createNewFile();
            } else{
                file.createNewFile();
            }
            out = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return currentPhotoPath;
    }
}
