package com.baituo.www.audiorecoderdemo;

import android.Manifest;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

public class AudioRecordActivity extends AppCompatActivity {

    private Button startRecordBtn,pauseRecordBtn,resumeRecordBtn,endRecordBtn ;
    private TextView dbText,recordTimeText ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startRecordBtn =findViewById(R.id.startRecordBtn);
        endRecordBtn = findViewById(R.id.endRecordBtn);
        pauseRecordBtn = findViewById(R.id.pauseRecordBtn);
        resumeRecordBtn = findViewById(R.id.resumeRecordBtn);
        dbText = findViewById(R.id.dbText);
        recordTimeText = findViewById(R.id.recordTimeText);

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},0x11); //权限必须要的，demo对权限没有仔细检查
        startRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //开始录制
                AudioRecordHelper.getAudioHelper()
                        .startRecord(getExternalCacheDir().getAbsolutePath() + File.separator + System.currentTimeMillis(), new AudioRecordHelper.OnAudioRecordlListener() {
                            @Override
                            public void dbResult(final double db) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dbText.setText("分贝值:"+db);
                                    }
                                });
                            }

                            @Override
                            public void recordTime(final double recordTime) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        recordTimeText.setText("当前录制时间:"+recordTime);
                                    }
                                });
                            }
                        });
            }
        });

        endRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //停止录制
                AudioRecordHelper.getAudioHelper().releaseRecord();
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
