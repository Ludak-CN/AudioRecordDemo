package com.baituo.www.audiorecoderdemo.helper.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;

/*
* author : mz
* */
public class AudioTrackHelper implements Runnable{

    public static final String DEBUG_LOG = "AudioTrackHelper";

    public static final int SAMPLE_SIZE = 44100 ; //通常的采样率
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT ; //采样位深
    public static final int AUDIO_CHANNEL = AudioFormat.CHANNEL_OUT_MONO ; //通道数 ,需要和录制音频时通道数一致,否则出错
    public static final int MIN_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_SIZE,AUDIO_CHANNEL,AUDIO_FORMAT);

    private static final AudioTrackHelper sHelper  = new AudioTrackHelper();

    private AudioTrack audioTrack ;
    private File file  ;
    private int mode ;
    private AudioTrackProgressListener listener ;

    public  static AudioTrackHelper getAudioTrackHelper(){
        return sHelper ;
    }

    public void startAudioTrack(String filePath,AudioTrackProgressListener listener){
        startAudioTrack(filePath,AudioTrack.MODE_STREAM,listener);
    }

    /*
       开始播放某一个pcm格式的音频文件
    * trackFile:  播放的pcm数据路径
    * mode : 播放模式
    * listener:播放进度监听
    * */
    public void startAudioTrack(String trackFile,int mode,AudioTrackProgressListener listener){
        if(audioTrack!=null){
            audioTrack.release();
        }
        this.listener  = listener ;
        this.mode = mode ;
        file = new File(trackFile);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            audioTrack = new AudioTrack(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    new AudioFormat.Builder()
                            .setSampleRate(SAMPLE_SIZE)
                            .setChannelMask(AUDIO_CHANNEL)
                            .setEncoding(AUDIO_FORMAT)
                            .build(),MIN_BUFFER_SIZE,mode,AudioManager.AUDIO_SESSION_ID_GENERATE);
        }else{
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,SAMPLE_SIZE,AUDIO_CHANNEL,AUDIO_FORMAT,MIN_BUFFER_SIZE,mode);
        }
        //执行播放任务
        AsyncTask.THREAD_POOL_EXECUTOR.execute(this);
    }


    @Override
    public void run() {
        if(mode==AudioTrack.MODE_STATIC){
            //延迟低，一次性写入播放数据,适合数据源小的场景
            realRunForStatic();
        }else if (mode==AudioTrack.MODE_STREAM){
            //会有延迟，播放一点写一点，慢慢播放，适合数据源大的场景
            realRunForStream();
        }
    }

    //流播放模式
    private void realRunForStream(){
        RandomAccessFile  raf = null ;
        try {
            raf = new RandomAccessFile(file,"r");
            byte[] readData = new byte[MIN_BUFFER_SIZE];
            int readLength  = 0 ;
            //需要先进行播放
            audioTrack.play();
            //开始播放
            if(listener!=null){
                listener.trackStart(getTotalDuration());
            }
            double sumLength = 0 ;
            while(true){
                if(audioTrack.getState()==AudioTrack.STATE_UNINITIALIZED){
                    //说明调用了release方法，释放了资源，需要重新初始化对象
                    Log.d(DEBUG_LOG,"播放中途，释放资源");
                    if(listener!=null){

                    }
                    break ;
                }else if(audioTrack.getPlayState()==AudioTrack.PLAYSTATE_PLAYING){
                    readLength = raf.read(readData,0,readData.length);
                    if(readLength>0){
                        sumLength+=readLength ;
                        //说明有数据,播放
                        audioTrack.write(readData,0,readLength);
                        if(listener!=null){
                            double currentTime = (sumLength/raf.length())*getTotalDuration();
                            listener.trackProgress(new BigDecimal(currentTime).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue()
                                    ,getTotalDuration(),sumLength==raf.length()?true:false);
                        }
                    }else{
                        //正常播放结束
                        Log.d(DEBUG_LOG,"正常播放结束");
                       break ;
                    }
                }
            }
        } catch (Exception e) {
            if(listener!=null){
                listener.trackFailed(e);
            }
            e.printStackTrace();
        }finally {
            audioTrack.release();
            safeClose(raf);
        }

    }

    //一次性写入播放模式
    private void realRunForStatic(){
        FileInputStream fis = null;
        BufferedInputStream bis = null ;
        ByteArrayOutputStream byteOutput = null;
        try {
            byte[] readData  = new byte[MIN_BUFFER_SIZE];
            int length = 0 ;
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            byteOutput = new ByteArrayOutputStream();
            while ((length=bis.read(readData,0,readData.length))>0){
                byteOutput.write(readData,0,length);
            }
            byte[] tmp = byteOutput.toByteArray() ;
            audioTrack.write(tmp,0,tmp.length);
            if(audioTrack.getState()==AudioTrack.STATE_NO_STATIC_DATA){
                //说明写入失败
                return ;
            }
            //写进入后播放
            audioTrack.play();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            audioTrack.release();
            safeClose(fis);
            safeClose(bis);
            safeClose(byteOutput);
        }
    }

    public void pauseTrack(){
        if(audioTrack!=null&&audioTrack.getPlayState()==AudioTrack.PLAYSTATE_PLAYING){
            audioTrack.pause();
        }
    }

    public void resumeTrack(){
        if(audioTrack!=null&&audioTrack.getPlayState()==AudioTrack.PLAYSTATE_PAUSED){
            audioTrack.play();
        }
    }

    public void releaseTrack(){
        if(audioTrack!=null){
            audioTrack.release();
        }
    }

    public double getTotalDuration(){
        double recordTime = 0l ;
        if(file!=null&&audioTrack!=null){
            double tmp = file.length() ;
            recordTime = (tmp/(audioTrack.getChannelCount()*audioTrack.getSampleRate()*AUDIO_FORMAT));
        }
        return new BigDecimal(recordTime).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private void safeClose(Closeable closeable){
        if(closeable!=null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface AudioTrackProgressListener{
        //开始播放
        void trackStart(double totalTime);

        //播放失败
        void trackFailed(Exception e);

        //播放进度
        void trackProgress(double currentTime,double totalTime,boolean isDone);
    }
}
