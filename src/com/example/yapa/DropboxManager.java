package com.example.yapa;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.dropbox.sync.android.*;

import java.io.OutputStream;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by Dar on 5/4/2014.
 */
public class DropboxManager {

    private DbxAccountManager dropboxAccountManager;
    private DbxFile rootDirectory;

    private boolean shouldUpdate = false;

    private final String APP_KEY = "akqeqxu5gzn2nd1";
    private final String APP_SECRET = "ik4rwf8bczkdsur"; //I somehow feel like this shouldn't be stored in the app... but it's needed

    private final String TAG = "DropboxWrapper";

    Activity owningActivity;

    public static final int REQUEST_LINK_TO_DBX = 582937465;

    public DropboxManager(Activity owner, Context ctx) {
        dropboxAccountManager = DbxAccountManager.getInstance(ctx, APP_KEY, APP_SECRET);
        owningActivity = owner;

        try {
            DbxFileSystem dbxFs = DbxFileSystem.forAccount(dropboxAccountManager.getLinkedAccount());
            dbxFs.addPathListener(new DbxFileSystem.PathListener() {
                @Override
                public void onPathChange(DbxFileSystem fs, DbxPath registeredPath, Mode registeredMode) {
                    Log.d(TAG, "We should now try and update...");
                    shouldUpdate = true;
                }
            }, new DbxPath("/"), DbxFileSystem.PathListener.Mode.PATH_OR_CHILD);
        } catch (Exception e) {
            Log.d(TAG, "Problem listening to root", e);
        }
    }

    public void asyncSync() {
        asyncSync(null);
    }

    private boolean synced = false;

    public boolean isSynced() {
        return synced;
    }

    public void asyncSync(final CallbackWrapper callback) {
        Thread t = new Thread() {
            public void run() {
                try {
                    DbxFileSystem dbxFs = DbxFileSystem.forAccount(dropboxAccountManager.getLinkedAccount());
                    dbxFs.syncNowAndWait();

                } catch (Exception e) {
                    Log.d(TAG, "error syncing", e);
                }

                synced = true;

                if(callback != null) {
                    callback.call();
                }
            }
        };
        t.start();
    }

    public void setUpdateConsumed(boolean b) {
        shouldUpdate = b;
    }

    public boolean isUpdateNeeded() {
        return shouldUpdate;
    }

    public String[] getListIfUpdateNeeded() {
        if(shouldUpdate) {
            return getFileListAsStrings();
        }
        shouldUpdate = false;
        return new String[0];
    }

    public String[] getFileListAsStrings() {
        try {
            DbxFileSystem dbxFs = DbxFileSystem.forAccount(dropboxAccountManager.getLinkedAccount());
            List<DbxFileInfo> files = dbxFs.listFolder(new DbxPath("/"));
            String[] result = new String[files.size()];
            int i = 0;
            for(DbxFileInfo info : files) {
                result[i] = info.path.getName(); //toString();
                i++;
            }
            return result;
        } catch (Exception e) {
            Log.d(TAG, "error listing files", e);
        }
        return new String[0];
    }

    public void toggleLinkage() {
        if(dropboxAccountManager.hasLinkedAccount()) {
            //return; //don't try and link twice or we'll get an exception
            try {
                dropboxAccountManager.unlink(); //startLink((Activity) this, REQUEST_LINK_TO_DBX);
            } catch (Exception e) {
                Log.d(TAG, "Exception attempting to dropboxAccountManager.startLink", e);
            }
        } else {
            try {
                dropboxAccountManager.startLink(owningActivity, REQUEST_LINK_TO_DBX);
            } catch (Exception e) {
                Log.d(TAG, "Exception attempting to dropboxAccountManager.startLink", e);
            }
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

    public void onPause() {
    }

    public void onResume() {
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

        DbxFile testFile = null;

        int nextNumber = 0;
        String filename = "image.jpg";

        //kind of ugly, but until we start using a local db and listening for sync updates this is probably the best we can do
        String[] fileList = getFileListAsStrings();
        for(String s: fileList) {
            //Log.d(TAG, "considering file "+s);
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
            testFile = dbxFs.create(new DbxPath(filename));

            Log.d(TAG, "Getting write stream for dbxFS file");
            OutputStream os = testFile.getWriteStream();
            os.write(data);
            Log.d(TAG, "Bytes written to dbxFS file");
            os.close();

            Log.d(TAG, "file stream closed");

        } catch(Exception e){
            Log.d(TAG, "Problem creating image file", e);
        } finally {
            if(testFile != null) {
                Log.d(TAG, "closing test file");
                testFile.close();
                Log.d(TAG, "test file closed");
            }
        }
    }

}
