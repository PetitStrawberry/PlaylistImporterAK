package com.petitstb.playlistimporterak;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Intent lisensesIntent = new Intent(this, OssLicensesMenuActivity.class);


        Button lisensesButton = findViewById(R.id.button);
        lisensesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(lisensesIntent);
            }
        });


        List<String> externalStorageList = removableStoragePathList();

        //内部ストレージをスキャン
        String path = Environment.getExternalStorageDirectory().getPath() + "/ToPlaylists";
        PlaylistUtils.scanDir(getApplicationContext(), path);

        //外部ストレージをスキャン
        for (int i=0; i<externalStorageList.size(); i++){
            path = externalStorageList.get(i);
            PlaylistUtils.scanDir(getApplicationContext(), path + "/ToPlaylists");
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
