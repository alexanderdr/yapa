package com.example.yapa;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;

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
                updateFileList();
            }
        });

        updateSyncness();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "paused");
        dropboxManager.onPause();
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
        ArrayList<String> filteredList = new ArrayList<String>();

        //somewhat slow for longer file lists
        for(String s : files) {
            if(!s.matches(".*\\.jpg")&&!s.matches(".*\\.png")) { //filter non-pngs and non-jpgs from the list
                continue;
            }
            filteredList.add(s);
        }
        files = new String[filteredList.size()];
        filteredList.toArray(files);
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

                //attach onClick to view image for each list item
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

        updateSyncness();
    }

    public void viewImage(String s) {
        Intent intent = new Intent(this, ImageViewActivity.class);
        //intent.putExtra("imageData", imageData); //this causes a java.lang.SecurityException because apparently intents have a limited size for extras
        intent.putExtra(ImageViewActivity.IMAGE_PATH_EXTRA, s);
        startActivity(intent);
    }

    public void onClickPhotoMode(View view) {
        if(!dropboxManager.linkReady()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.need_linked_account_message)
                    .setTitle(R.string.need_linked_account_title)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //don't care, just want a button
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();

            return;
        }

        Intent intent = new Intent(this, PhotoActivity.class);
        startActivity(intent);
    }

    //We could use a ToggleButton here but we would need to update on app load anyway, and update based on whether or not
    //we successfully link, so it wouldn't save us much, and just provides a different display, a switch may be worth considering
    private void updateSyncness() {
        if(dropboxManager.linkReady()) {
            Button myButton = (Button) findViewById(R.id.sync_button);
            myButton.setText(R.string.unlink_account);

            updateFileList();
        } else {
            Button myButton = (Button) findViewById(R.id.sync_button);
            myButton.setText(R.string.link_account);

            updateFileList();
            //empty out file list
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DropboxManager.REQUEST_LINK_TO_DBX) {
            if (resultCode == Activity.RESULT_OK) {
                dropboxManager.addPathListener(); //would really like to have this somewhere in the DropboxManager
            } else {
                Log.d(TAG, "Failed to link to dropbox account");
            }

            updateSyncness();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
