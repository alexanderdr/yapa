package com.example.yapa;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

public class ImageViewActivity extends Activity {

    private final String TAG = "image-view-activity";

    public static final String IMAGE_PATH_EXTRA = "imagePath";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_view);

        Intent intent = getIntent();
        String imagePath = intent.getStringExtra(IMAGE_PATH_EXTRA);
        Log.d(TAG, "imagePath is "+imagePath);
        if(imagePath != null && imagePath.length() > 0) {

            DropboxManager dbm = new DropboxManager(this, getApplicationContext());
            byte[] imageData = dbm.getFileAsBytes(imagePath);
            Log.d(TAG, "Have " +imageData.length+" bytes for image");
            if(imageData != null && imageData.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

                ImageView image = (ImageView) findViewById(R.id.imageView_image);
                image.setImageBitmap(bitmap);
                Log.d(TAG, "Successfully set image to bitmap");
            }
        }

    }
}