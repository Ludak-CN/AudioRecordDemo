package com.baituo.www.audiorecoderdemo.helper.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

/*
* author : mz
*
* 实现录制音频-基于AudioRecord
* */
public class AudioRecordHelper implements Runnable{

    public static final String DEBUG_LOG = "AudioRecordHelper";


    public static final int SAMPLE_SIZE = 44100 ; // 采样频率,频率越高采样越“积极”，音质越好,通常为44100，可以自己设定
    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC ; //声音来源
    public static final int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO ; //通道数，单通道
    public static final int AUDIO_ENCODING_BIT = AudioFormat.ENCODING_PCM_16BIT ; //采样位深，16位
    public static final int MIN_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_SIZE,AUDIO_CHANNEL,AUDIO_ENCODING_BIT); //计算AudioRecord正常工作所需要的最小字节数组大小

    private static final AudioRecordHelper sHelper = new AudioRecordHelper();

    private AudioRecord audioRecord ;
    private File file   ;
    private OnAudioRecordlListener onDecibelListener ;

    public static AudioRecordHelper getAudioHelper(){
        return sHelper ;
    }

    //录制保存到哪一个文件路径,开始录制
    public void startRecord(String filePath){
       startRecord(filePath,null);
    }

    public void startRecord(String filePath,OnAudioRecordlListener dbListener){
        if(audioRecord!=null){
            audioRecord.release();
        }
        onDecibelListener = dbListener ;
        file = new File(filePath);
        audioRecord = new AudioRecord(AUDIO_SOURCE,SAMPLE_SIZE,AUDIO_CHANNEL,AUDIO_ENCODING_BIT,MIN_BUFFER_SIZE);
        //执行录制,简单采用AsyncTask线程池
        AsyncTask.THREAD_POOL_EXECUTOR.execute(this);
    }


    @Override
    public void run() {
        //删除存在的文件
        if(file.exists()){
            file.delete() ;
        }
        FileOutputStream fos  = null;
        BufferedOutputStream bos = null ;
        try {
            byte[] readData = new byte[MIN_BUFFER_SIZE];
            int readLength =  0 ;
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            audioRecord.startRecording();
            if(onDecibelListener!=null){
                onDecibelListener.recordStart();
            }
            while (true){
                if(audioRecord.getState()==AudioRecord.STATE_UNINITIALIZED){
                    //说明被释放资源,此AudioRecord将无效，需要重新初始化,结束录制,录制过程中点击结束录制
                    break ;
                }else if(audioRecord.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING){
                    //正在录制状态
                    readLength = audioRecord.read(readData,0,readData.length);
                    if(readLength>0){
                        bos.write(readData,0,readLength);

                        //分贝值,非UI线程调用
                        if(onDecibelListener!=null){
                            //分贝值
                            onDecibelListener.dbResult(getDecibelForPcm(readData,readLength));
                            //录制时长
                            onDecibelListener.recordTime(getDuration());
                        }
                    }
                }
            }
            Log.d(DEBUG_LOG,"正常录制结束");
        }catch (Exception e) {
            if(onDecibelListener!=null){
                onDecibelListener.recordFailed(e);
            }
            e.printStackTrace();
        }finally {
            Log.d(DEBUG_LOG,"finally录制结束");
            try {
                audioRecord.release();
                if(bos!=null){
                    bos.close();
                }
                if(fos!=null){
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkAudioRecordUnInitState(AudioRecord audioRecord){
        if(audioRecord==null){
            throw new NullPointerException("audioRecord is null");
        }
        if(audioRecord.getState()==AudioRecord.STATE_UNINITIALIZED){
            throw new IllegalStateException("audioRecord 已经被释放资源，需要重新进行初始化");
        }
    }

    private void checkPauseState(AudioRecord audioRecord){
        checkAudioRecordUnInitState(audioRecord);
        if(audioRecord.getRecordingState()==AudioRecord.RECORDSTATE_STOPPED){
            throw new IllegalStateException("已经处于pause");
        }
    }
    private void checkResumeState(AudioRecord audioRecord){
        checkAudioRecordUnInitState(audioRecord);
        if(audioRecord.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING){
            throw new IllegalStateException("已经处于recording");
        }
    }

    //暂停录制
    public void pauseRecord(){
        if(audioRecord==null){
            Log.d(DEBUG_LOG,"audioRecord is null");
            return ;
        }
        try {
            checkPauseState(audioRecord);
            audioRecord.stop(); //此时进入AudioRecord.RECORDSTATE_STOPPED状态，还是可以重用重新开始录制
        }catch (IllegalStateException e){
            Log.d(DEBUG_LOG,"pauseRecord exception");
        }
    }

    //恢复录制
    public void resumeRecord(){
        if(audioRecord==null){
            Log.d(DEBUG_LOG,"audioRecord is null");
            return ;
        }

        try {
            checkResumeState(audioRecord);
            audioRecord.startRecording();
        }catch (IllegalStateException e){
            Log.d(DEBUG_LOG,"resumeRecord exception");
        }

    }

    //停止录制
    public void releaseRecord(){
        if(audioRecord!=null){
            audioRecord.release(); //此时要无法重用对象重新进行录制，必须重新初始化对象
        }
    }


    /*
        获取录制的时间
     * 文件大小(字节) = 采样率*采样位深*采样通道数*录制时间
     * 可以通过上面的公式计算出录制时间，上面除了录制时间，其它都是以知的。
     * */
    public double getDuration(){
        double recordTime = 0l ;
        if(file!=null&&audioRecord!=null){
            double tmp = file.length() ;
            recordTime = (tmp/(audioRecord.getChannelCount()*audioRecord.getSampleRate()*AUDIO_ENCODING_BIT));
        }
        return new BigDecimal(recordTime).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    //获取录制的文件
    public File getRecordFile(){
        return file ;
    }

    /*
    * 获取某一段数据的分贝值-针对pcm的原始音频数据
    *
    *   data : 某一时间段pcm数据
        dataLenngth :有效数据大小

        这个分贝值估计值
    * */
    private double getDecibelForPcm(byte[] data,int dataLength){
        long sum = 0 ;
        long temp = 0 ;
        for(int i=0;i<data.length;i+=2){
            temp = (data[i+1]*128+data[i]); //累加求和
            temp*=temp ;
            sum+=temp ;
        }

        //平方和除以数据长度，得到音量大小
        double square = sum/(double)dataLength ; //音量大小
        double result =10* Math.log10(square*2); //分贝值
        return result ;
    }

    //分贝值监听
    public interface OnAudioRecordlListener{

        //开始录制
        void recordStart();

        //分贝值
        void dbResult(double db);

        //录制时长，保留2位小数
        void recordTime(double recordTime);

        //录制失败
        void recordFailed(Exception e);
    }
}
