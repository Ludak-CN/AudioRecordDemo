package com.baituo.www.audiorecoderdemo;

import android.Manifest;
import android.media.AudioRecord;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private Button startRecordBtn,pauseRecordBtn,resumeRecordBtn,endRecordBtn ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startRecordBtn =findViewById(R.id.startRecordBtn);
        endRecordBtn = findViewById(R.id.endRecordBtn);
        pauseRecordBtn = findViewById(R.id.pauseRecordBtn);
        resumeRecordBtn = findViewById(R.id.resumeRecordBtn);

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},0x11); //权限必须要的，demo对权限没有仔细检查
        startRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //开始录制
                AudioRecordHelper.getAudioHelper()
                        .startRecord(getExternalCacheDir().getAbsolutePath() + File.separator + System.currentTimeMillis(), new AudioRecordHelper.OnDecibelListener() {
                            @Override
                            public void dbResult(double db) {
                                System.out.println("分贝值:"+db);
                            }
                        });
            }
        });

        endRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //停止录制
                AudioRecordHelper.getAudioHelper().releaseRecord();
                System.out.println("录制的时间:"+AudioRecordHelper.getAudioHelper().getDuration());
            }
        });

        pauseRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //暂停录制
                AudioRecordHelper.getAudioHelper().pauseRecord();
            }
        });

        resumeRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //恢复录制
                AudioRecordHelper.getAudioHelper().resumeRecord();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AudioRecordHelper.getAudioHelper().releaseRecord();
    }
}
