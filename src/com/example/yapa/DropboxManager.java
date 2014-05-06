package com.example.yapa;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.dropbox.sync.android.*;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class DropboxManager {

    private final String APP_KEY = "akqeqxu5gzn2nd1";
    private final String APP_SECRET = "ik4rwf8bczkdsur"; //I somehow feel like this shouldn't be stored in the app... but it's needed

    private final String TAG = "dropbox-wrapper";

    private ArrayList<Runnable> updateListeners = new ArrayList<Runnable>();
    private ArrayList<Runnable> linkageListeners = new ArrayList<Runnable>();

    private DbxFileSystem.PathListener listener;

    private Activity owningActivity;
    private Fragment listenerFragment;

    private DbxAccountManager dropboxAccountManager;
    public static final int REQUEST_LINK_TO_DBX = 582937465; //arbitrary number-- not really needed now because of private fragment

    public DropboxManager(Activity owner, Context ctx) {
        dropboxAccountManager = DbxAccountManager.getInstance(ctx, APP_KEY, APP_SECRET);
        owningActivity = owner;

        listener = new DbxFileSystem.PathListener() {
            @Override
            public void onPathChange(DbxFileSystem fs, DbxPath registeredPath, Mode registeredMode) {
                updateListenersOnMainThread(updateListeners);
            }
        };

        listenerFragment = new Fragment() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                if (resultCode == Activity.RESULT_OK) {
                    addPathListener();
                } else {
                    Log.d(TAG, "Failed to link to dropbox account");
                }

                updateListenersOnMainThread(linkageListeners);
            }
        };

        FragmentManager fragmentManager = owner.getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(listenerFragment, "dropboxManagerFragment");
        fragmentTransaction.commit();

        addPathListener();
    }

    public void onPause() {
    }

    public void onResume() {
    }

    private void updateListenersOnMainThread(ArrayList<Runnable> listeners) {
        Handler h = new Handler(Looper.getMainLooper());
        for(Runnable r: listeners) {
            if (r != null) {
                h.post(r);
            }
        }
    }

    //listeners are always called from the main thread
    public void registerUpdateListener(Runnable r) {
        updateListeners.add(r);
    }

    //listeners are always called from the main thread
    public void registerLinkageListener(Runnable r) {
        linkageListeners.add(r);
    }

    private boolean synced = false;

    public boolean isSynced() {
        return synced;
    }

    public void asyncSync() {
        Thread t = new Thread() {
            public void run() {
                try {
                    if(!linkReady()) {
                        return;
                    }

                    DbxFileSystem dbxFs = DbxFileSystem.forAccount(dropboxAccountManager.getLinkedAccount());
                    dbxFs.syncNowAndWait();

                    updateListenersOnMainThread(updateListeners);

                } catch (Exception e) {
                    Log.d(TAG, "error syncing", e);
                }

                synced = true;
            }
        };
        t.start();
    }

    public String[] getFileListAsStrings() {
        if(dropboxAccountManager.hasLinkedAccount()) {
            try {
                DbxFileSystem dbxFs = DbxFileSystem.forAccount(dropboxAccountManager.getLinkedAccount());
                List<DbxFileInfo> files = dbxFs.listFolder(new DbxPath("/"));
                String[] result = new String[files.size()];
                int i = 0;
                for (DbxFileInfo info : files) {
                    result[i] = info.path.getName(); //toString();
                    i++;
                }
                return result;
            } catch (Exception e) {
                Log.d(TAG, "error listing files", e);
            }
        }
        return new String[0];
    }

    public void toggleLinkage() {
        if(dropboxAccountManager.hasLinkedAccount()) {
            //return; //don't try and link twice or we'll get an exception
            try {
                DbxFileSystem dbxFs = DbxFileSystem.forAccount(dropboxAccountManager.getLinkedAccount());
                dbxFs.removePathListener(listener, new DbxPath("/"), DbxFileSystem.PathListener.Mode.PATH_OR_CHILD);

                dropboxAccountManager.unlink();

                updateListenersOnMainThread(linkageListeners);
            } catch (Exception e) {
                Log.d(TAG, "Exception attempting to dropboxAccountManager.unlink", e);
            }
        } else {
            try {
                dropboxAccountManager.startLink(listenerFragment, REQUEST_LINK_TO_DBX);
            } catch (Exception e) {
                Log.d(TAG, "Exception attempting to dropboxAccountManager.startLink", e);
            }
        }
    }

    public void addPathListener() {
        if(!linkReady()) return;
        try {
            DbxFileSystem dbxFs = DbxFileSystem.forAccount(dropboxAccountManager.getLinkedAccount());
            dbxFs.addPathListener(listener, new DbxPath("/"), DbxFileSystem.PathListener.Mode.PATH_OR_CHILD);
        } catch (Exception e) {
            Log.d(TAG, "Exception attempting to add path listener", e);
        }
    }

    public boolean linkReady() {
        if(dropboxAccountManager == null) { //should be impossible
            return false;
        }
        DbxAccount account = dropboxAccountManager.getLinkedAccount();
        if(account == null) {
            return false;
        }
        if(!account.isLinked()) {
            return false;
        }
        return true;
    }

    public byte[] getFileAsBytes(String s) {
        try {
            DbxFileSystem dbxFs = DbxFileSystem.forAccount(dropboxAccountManager.getLinkedAccount());

            DbxFile f = dbxFs.open(new DbxPath(s));
            DbxFileInfo info = f.getInfo();
            byte[] result = new byte[(int)info.size];
            f.getReadStream().read(result);

            return result;
        } catch (Exception e) {
            Log.d(TAG, "Problem getting file", e);
        }

        return new byte[0];
    }

    public void saveImageData(byte[] data) {

        DbxFile dbxFile = null;

        int nextNumber = 0;
        String filename = "image.jpg";

        //kind of ugly, but until we start using a local db and listening for sync updates this is probably the best we can do
        String[] fileList = getFileListAsStrings();
        for(String s: fileList) {
            if(s == null) {
                continue;
            }
            StringTokenizer stz = new StringTokenizer(s, ".");
            if(stz.countTokens() > 1) {
                String name = stz.nextToken();
                String ending = stz.nextToken();
                if(!ending.equals("jpg") && !ending.equals("png")) {
                    continue;
                }

                try {
                    int i = Integer.parseInt(name);
                    nextNumber = Math.max(nextNumber, i+1);
                } catch (NumberFormatException nfe) {

                }
            }
        }

        String numString = nextNumber+"";
        int numToPad = 5 - numString.length();
        for(int i = 0; i < numToPad; i++) {
            numString = "0"+numString;
        }
        filename = numString+".jpg";

        try {
            Log.d(TAG, "Initializing dbx FS");
            DbxFileSystem dbxFs = DbxFileSystem.forAccount(dropboxAccountManager.getLinkedAccount());

            Log.d(TAG, "Creating dbxFS file "+filename);
            dbxFile = dbxFs.create(new DbxPath(filename));

            Log.d(TAG, "Getting write stream for dbxFS file");
            OutputStream os = dbxFile.getWriteStream();
            os.write(data);
            Log.d(TAG, "Bytes written to dbxFS file");
            os.close();

            Log.d(TAG, "file stream closed");

        } catch(Exception e){
            Log.d(TAG, "Problem creating image file", e);
        } finally {
            if(dbxFile != null) {
                dbxFile.close();
            }
        }
    }

}
