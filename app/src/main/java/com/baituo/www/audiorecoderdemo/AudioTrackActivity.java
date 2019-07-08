package com.baituo.www.audiorecoderdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.folderselector.FileChooserDialog;

import java.io.File;

public class AudioTrackActivity extends AppCompatActivity{


    private final int FILE_CHOOSE_CODE = 0x89 ;
    private TextView chooseFileNameTv,fileInfoTv;
    private Button chooseFileBtn ,playBtn,pauseBtn,resumeBtn,releaseBtn;
    private String filePath ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_track_layout);
        chooseFileBtn = findViewById(R.id.chooseFileBtn);
        releaseBtn = findViewById(R.id.releaseBtn);
        playBtn = findViewById(R.id.playBtn);
        pauseBtn = findViewById(R.id.pauseBtn);
        resumeBtn = findViewById(R.id.resumeBtn);
        chooseFileNameTv = findViewById(R.id.fileNameTv);
        fileInfoTv = findViewById(R.id.fileInfoTv);

        //选择文件按钮
        chooseFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent,"请选择播放的音频文件"),FILE_CHOOSE_CODE);
            }
        });

        //开始播放
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioTrackHelper.getAudioTrackHelper()
                        .startAudioTrack(filePath, AudioTrack.MODE_STREAM, new AudioTrackHelper.AudioTrackProgressListener() {
                            @Override
                            public void trackStart(double totalTime) {

                            }

                            @Override
                            public void trackFailed(Exception e) {

                            }

                            @Override
                            public void trackProgress(double currentTime, double totalTime, boolean isDone) {
                                System.out.println("总时长:"+totalTime+"-当前播放时长:"+currentTime+"-是否完成:"+isDone);
                            }
                        });

                //文件信息
                fileInfoTv.setText("时长:"+AudioTrackHelper.getAudioTrackHelper().getTotalDuration());
            }
        });

        //暂停
        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioTrackHelper.getAudioTrackHelper()
                        .pauseTrack();
            }
        });

        resumeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioTrackHelper.getAudioTrackHelper()
                        .resumeTrack();
            }
        });

        releaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioTrackHelper.getAudioTrackHelper()
                        .releaseTrack();
            }
        });

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0x78);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==FILE_CHOOSE_CODE&&resultCode==RESULT_OK){
            filePath = UriUtils.getPath(this,data.getData());
            chooseFileNameTv.setText("文件名:"+filePath);
        }
    }
}
