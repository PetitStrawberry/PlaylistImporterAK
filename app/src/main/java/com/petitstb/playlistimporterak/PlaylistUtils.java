package com.petitstb.playlistimporterak;

import android.content.ContentResolver;

import android.provider.MediaStore.Audio.PlaylistsColumns;
import android.provider.MediaStore.Audio.Playlists;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlaylistUtils {
    final static String TAG = "PlaylistUtils";
    private static ContentValues[] mContentValuesCache = null;

    //この時、読み込まれたファイルは削除される
    public static String readTextFromFile(String path){
        String str = "";
        File file = new File(path);
        if (file.exists()){
            try(FileInputStream fileInputStream =
                        new FileInputStream(file);
                InputStreamReader inputStreamReader =
                        new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
                BufferedReader reader=
                        new BufferedReader(inputStreamReader) ) {
                String lineBuffer;
                while( (lineBuffer = reader.readLine()) != null ) {
                    str = str + lineBuffer + "\n";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            file.delete();
        }
        //NFCに正規化して返す。
        return Normalizer.normalize(str,Normalizer.Form.NFC);
    }

    //指定ディレクトリ下のファイルをスキャンし、プレイリストとして読み込む
    public static void scanDir(final Context context, final String dirPath){
        File[] files;
        List<String> playlistFileList = new ArrayList<String>();
        files = new File(dirPath).listFiles();

        if (files!=null){
            for(int i = 0; i < files.length; i++){
                if(files[i].isFile() && (files[i].getName().endsWith(".m3u")||files[i].getName().endsWith(".m3u8"))){
                    playlistFileList.add(files[i].getPath());
                }
            }

            for(int i = 0; i<playlistFileList.size(); i++){
                createPlaylistFromFile(context, playlistFileList.get(i));
            }
        }
    }

    //m3u, m3u8ファイルからプレイリストを作成
    public static long createPlaylistFromFile(final Context context, final String path) {
        File file = new File(path);
        String parentPath = file.getParent();
        List<String> pathList = convertToAbsPath(parentPath, getPathListByFile(path));

        //Toast.makeText(getApplicationContext() , pathList.toString(), Toast.LENGTH_LONG).show();

        String basename = new File(path).getName();
        String playlist_name = basename.substring(0, basename.lastIndexOf('.'));
        long playlist_id = getPlaylistId(context, playlist_name);

        if (playlist_id != -1) {
            long[] ids = new long[1];
            ids[0] = playlist_id;
            removePlaylists(context, ids);
        }
        playlist_id = newPlaylist(context, playlist_name);

        try {
            if (pathList.size() > 0) {
                long[] track_ids = new long[pathList.size()];
                for (int i = 0; i < pathList.size(); ++i) {
                    long track_id = getAudioIdByPath(context, pathList.get(i));

                    track_ids[i] = track_id;

                }
                addTrackToPlaylist(context, track_ids, playlist_id);
            }

        } catch (Exception e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
        }

        return playlist_id;
    }

    //m3u,m3u8ファイルから音楽ファイルのパスを格納したリストを取得
    public static List<String> getPathListByFile(String path){
        String str;
        String replaced;
        List<String> pathList = new ArrayList<String>();

        pathList.clear();
        //pathからファイル読み込み
        str = readTextFromFile(path);

        if (str != "") {
            //コメント行を削除
            replaced = str.replaceAll("^#.*\\n","")
                    .replaceAll("\\n#.*\\n", "\n");
            pathList = Arrays.asList(replaced.split("\n"));
        }
        return pathList;
    }

    //相対パスを絶対パスに変換 絶対パスの時はそのまま
    public static List<String> convertToAbsPath(String parentPath, List<String> pathList){
        List<String> absPathList = new ArrayList<String>();

        for (int i = 0; i < pathList.size(); i++) {
            boolean isAbsPath = new File(pathList.get(i)).isAbsolute();
            String absPath = "";
            if (isAbsPath) {
                absPath = pathList.get(i);
            }else{
                String path = parentPath + "/" + pathList.get(i);
                File file = new File(path);

                try{
                    absPath = file.getCanonicalPath();
                }catch (Exception e) {}
            }

            if (absPath!="") absPathList.add(absPath);

        }
        return absPathList;
    }







    //プレイリストを作成
    public static long newPlaylist(final Context context, final String name) {
        if (name != null && name.length() > 0) {
            final ContentResolver resolver = context.getContentResolver();
            final String[] projection = new String[]{
                    PlaylistsColumns.NAME
            };
            final String selection = PlaylistsColumns.NAME + " = '" + name + "'";
            Cursor cursor = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    projection, selection, null, null);
            if (cursor.getCount() <= 0) {
                final ContentValues values = new ContentValues(1);
                values.put(PlaylistsColumns.NAME, name);
                final Uri uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                        values);
                return Long.parseLong(uri.getLastPathSegment());
            }
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
            return -1;
        }
        return -1;
    }


    public static void addTrackToPlaylist(final Context context, final long[] ids, final long playlistid) {
        final int size = ids.length;
        final ContentResolver resolver = context.getContentResolver();
        final String[] projection = new String[]{
                "max(" + Playlists.Members.PLAY_ORDER + ")",
        };
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid);
        Cursor cursor = null;
        int base = 0;

        try {
            cursor = resolver.query(uri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                base = cursor.getInt(0) + 1;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }

        int numinserted = 0;
        for (int offSet = 0; offSet < size; offSet += 1000) {
            makeInsertItems(ids, offSet, 1000, base);
            numinserted += resolver.bulkInsert(uri, mContentValuesCache);
        }
    }

    public static void makeInsertItems(final long[] ids, final int offset, int len, final int base) {
        if (offset + len > ids.length) {
            len = ids.length - offset;
        }

        if (mContentValuesCache == null || mContentValuesCache.length != len) {
            mContentValuesCache = new ContentValues[len];
        }
        for (int i = 0; i < len; i++) {
            if (mContentValuesCache[i] == null) {
                mContentValuesCache[i] = new ContentValues();
            }
            mContentValuesCache[i].put(Playlists.Members.PLAY_ORDER, base + offset + i);
            mContentValuesCache[i].put(Playlists.Members.AUDIO_ID, ids[offset + i]);
        }
    }

    //プレイリストを削除
    public static void removePlaylists(Context context, long[] ids){
        Uri uri = null;
        uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        removeItems(context, uri, ids);
    }
    protected static void removeItems(Context context, Uri uri, long[] ids){
        ContentResolver contentResolver = context.getContentResolver();
        if(uri == null || ids == null){
        }else{
            String where = "_id IN(";
            for(int i=0; i<ids.length; i++){
                where += Integer.valueOf((int)ids[i]);
                if(i < (ids.length -1)){
                    where += ", ";
                }
            }
            where += ")";
            //削除
            contentResolver.delete(uri, where, null);
        }
    }




    //プレイリスト名からplaylist_id取得
    public static int getPlaylistId(Context context, String name){
        int id;
        ContentResolver cr = context.getContentResolver();
        Cursor c =cr.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Playlists._ID }, MediaStore.Audio.Playlists.NAME + "=?",
                new String[] { name }, MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER);
        id=-1;
        if (c != null){
            c.moveToFirst();
            if (!c.isAfterLast()) {
                id = c.getInt(0);
            }
            c.close();
        }

        return id;
    }

    //ファイルパスからAudio_idを取得
    public static long getAudioIdByPath(Context context, String pathToFile) {
        long id = -1;
        String[] projection = {MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA};
        String selection = MediaStore.Audio.Media.DATA + " like ?";
        Cursor c = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection,
                new String[]{pathToFile}, null);
        c.moveToFirst();
        if (c.getCount() > 0)
            id = c.getLong(0);
        c.close();
        return id;
    }

}
