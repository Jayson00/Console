package com.linghao.console;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


import com.linghao.console.audiodemo.PermissionUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.function.Consumer;

public class MusicFileActivity extends AppCompatActivity {

    private MusicFileDTO mMusicFileDTO;
    private ArrayList<MusicFileDTO> mMusicFileDTOS= new ArrayList<>();
    private ListView mListView;
    private MyFileAdapter mAdapter;

    private Activity mActivity;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_file);
        mListView = findViewById(R.id.list_file);
        mActivity = this;

        if (checkPermission()){

            mMusicFileDTOS = getDataFromLocal(MusicFileActivity.this);
            System.out.println("输出音乐文件的数量"+getDataFromLocal(MusicFileActivity.this).size());
            if (mMusicFileDTOS.size()>0){
                mHandler.sendEmptyMessage(10);
            }
        }
    }



    private boolean checkPermission() {
        if (!PermissionUtil.isHasSDCardWritePermission(this)) {
            PermissionUtil.requestSDCardWrite(this);
            return false;
        }

        return true;
    }


    public  ArrayList<MusicFileDTO> getDataFromLocal(Context context) {

        System.out.println("进入方法");
        ArrayList<MusicFileDTO> list = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                , null, null, null, MediaStore.Audio.AudioColumns.IS_MUSIC);

        if (cursor != null) {
            System.out.println("cursor不为空");
            while (cursor.moveToNext()) {

                mMusicFileDTO = new MusicFileDTO();
                mMusicFileDTO.setName(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)));
                mMusicFileDTO.setSingername(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));
                mMusicFileDTO.setData(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
                mMusicFileDTO.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)));
                mMusicFileDTO.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)));

                System.out.println("名称："+cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)));
                System.out.println("歌手："+cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));
                System.out.println("路径："+cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));

                list.add(mMusicFileDTO);
            }
        }
        cursor.close();
        return list;
    }





    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 10:
                    mAdapter = new MyFileAdapter(MusicFileActivity.this,mMusicFileDTOS);
                    mListView.setAdapter(mAdapter);
                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            Intent intent = new Intent();
                            intent.putExtra("data",mMusicFileDTOS.get(i).getData());
                            intent.putExtra("name",mMusicFileDTOS.get(i).getName());
                            mActivity.setResult(RESULT_OK,intent);
                            finish();
                        }
                    });
                    break;
            }
        }
    };



}
