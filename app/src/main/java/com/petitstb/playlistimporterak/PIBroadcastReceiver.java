package com.petitstb.playlistimporterak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;


import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class PIBroadcastReceiver extends BroadcastReceiver {
    final private static String TAG = "PIBroadcastReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {

        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            List<String> externalStorageList = removableStoragePathList();

            //内部ストレージをスキャン
            String path = Environment.getExternalStorageDirectory().getPath() + "/ToPlaylists";
            PlaylistUtils.scanDir(context, path);

            //外部ストレージをスキャン
            for (int i=0; i<externalStorageList.size(); i++){
                path = externalStorageList.get(i);
                PlaylistUtils.scanDir(context, path + "/ToPlaylists");
            }
        }

        if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            List<String> externalStorageList = removableStoragePathList();

            //内部ストレージをスキャン
            String path = Environment.getExternalStorageDirectory().getPath() + "/ToPlaylists";
            PlaylistUtils.scanDir(context, path);

            //外部ストレージをスキャン
            for (int i=0; i<externalStorageList.size(); i++){
                path = externalStorageList.get(i);
                PlaylistUtils.scanDir(context, path + "/ToPlaylists");
            }
        }

    }

    private static List<String> removableStoragePathList(){
        List<String> pathList = new ArrayList<String>();
        File fileList[] = new File("/storage/").listFiles();
        for (File file : fileList)
        {     if(!file.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()) && file.isDirectory() && file.canRead())
            pathList.add(file.getAbsolutePath());
        }
        return pathList;
    }


}


