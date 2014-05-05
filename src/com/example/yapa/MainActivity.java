package com.example.yapa;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;


public class MainActivity extends Activity {

    private final String TAG = "yapam-main";

    DropboxManager dropboxManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        dropboxManager = new DropboxManager(this, getApplicationContext());
        dropboxManager.asyncSync(new CallbackWrapper() {
            public void call() {
                //updateFileList();
            }
        }); //need to listen for changes as well, but this should at least handle things if we restart

        updateSyncButton();

        //really really unhappy to do this since the callback thread isn't permitted to update the UI
        while(!dropboxManager.isSynced());

        updateFileList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "paused");
        dropboxManager.onPause();
        //need to unlisten for dropbox changes...
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "resumed");
        dropboxManager.onResume();
        if(dropboxManager.isUpdateNeeded()) {
            updateFileList();
            dropboxManager.setUpdateConsumed(true);
        }
    }

    public void updateFileList() {

        ListView fileList = (ListView) findViewById(R.id.filename_list);
        String[] files = dropboxManager.getFileListAsStrings();
        fileList.setAdapter(new ArrayAdapter<String>(getApplicationContext(), R.layout.file_item , files) {
            //adapted from https://github.com/thecodepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the data item for this position
                final String filename = getItem(position);
                // Check if an existing view is being reused, otherwise inflate the view
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.file_item, parent, false);
                }
                // Lookup view for data population
                TextView filenameView = (TextView) convertView.findViewById(R.id.listitem_filename);
                // Populate the data into the template view using the data object
                filenameView.setText(filename);

                convertView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        viewImage(filename);
                    }
                });

                // Return the completed view to render on screen
                return convertView;
            }
        });
    }

    public void onClickToggleLinkToDropbox(View view) {
        dropboxManager.toggleLinkage();

        updateSyncButton();
    }

    public void viewImage(String s) {
        Intent intent = new Intent(this, ImageViewActivity.class);
        //intent.putExtra("imageData", imageData); //this causes a java.lang.SecurityException because apparently intents have a limited size for extras
        intent.putExtra("imagePath", s);
        startActivity(intent);
    }

    public void onClickPhotoMode(View view) {
        if(!dropboxManager.linkReady()) {
            //should print some kind of error message, or make the button appear disabled
            //"can't take pictures without a linked dropbox account"
            return;
        }

        Intent intent = new Intent(this, PhotoActivity.class);
        startActivity(intent);
    }

    //We could use a ToggleButton here but we would need to update on app load anyway, and update based on whether or not
    //we successfully link, so it wouldn't save us much, and just provides a different display, a switch may be worth considering
    private void updateSyncButton() {
        if(dropboxManager.linkReady()) {
            Button myButton = (Button) findViewById(R.id.sync_button);
            myButton.setText(R.string.unlink_account);

        } else {
            Button myButton = (Button) findViewById(R.id.sync_button);
            myButton.setText(R.string.link_account);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DropboxManager.REQUEST_LINK_TO_DBX) {
            if (resultCode == Activity.RESULT_OK) {
                // ... Start using Dropbox files.
            } else {
                // ... Link failed or was cancelled by the user.
                Log.d(TAG, "Failed to link to dropbox account");
            }

            updateSyncButton();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
