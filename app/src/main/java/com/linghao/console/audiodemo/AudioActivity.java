package com.linghao.console.audiodemo;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.linghao.console.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AudioActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mStartBtn,mStopBtn,mCheckBtn;
    private TextView mPathTv;

    /**
     * 录音数队列
     */
    private ConcurrentLinkedQueue<byte[]> audioQueue = new ConcurrentLinkedQueue<byte[]>();
    private ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    private AudioTrack mAudioTrack;
    private AudioRecord mAudioRecord;
    private int mRecorderBufferSize;
    private byte[] mAudioData;

    /*默认数据*/
    private int mSampleRateInHZ = 8000; //采样率
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;  //位数
    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;   //声道


    private boolean isRecording = false;
    private String mTmpFileAbs = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        mStartBtn  = findViewById(R.id.btn_start_voice);
        mStopBtn = findViewById(R.id.btn_stop_voice);
        mCheckBtn = findViewById(R.id.btn_check);
        mPathTv = findViewById(R.id.tv_show_path);

        mStartBtn.setOnLongClickListener(new ButtonLongClick());
        mStartBtn.setOnTouchListener(new ButtonLongClick());
        mStopBtn.setOnClickListener(this);
        mCheckBtn.setOnClickListener(this);

        if (checkPermission()) {
            showToast("无异常");
        } else {
            showToast("发现异常");
        }

        if (mAudioRecord != null) {
            mAudioRecord.release();
        }
        mSampleRateInHZ = 8000;
        mAudioFormat = 2;
        initData();
    }


    private void initData() {
        mRecorderBufferSize = AudioRecord.getMinBufferSize(mSampleRateInHZ, mChannelConfig, mAudioFormat);
        mAudioData = new byte[320];
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, mSampleRateInHZ, mChannelConfig, mAudioFormat, mRecorderBufferSize);
//        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRateInHZ, mChannelConfig, mAudioFormat, mRecorderBufferSize * 2
//                , AudioTrack.MODE_STREAM);
    }


    private class ButtonLongClick  implements View.OnLongClickListener,View.OnTouchListener{

        @Override
        public boolean onLongClick(View view) {
//            System.out.println("hkacxkjanskcnc");

            return true;
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {

            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                System.out.println("长按....");
                if (!isRecording) {

                    showToast("已开始");

                    String tmpName = System.currentTimeMillis() + "_" + mSampleRateInHZ + "";
                    final File tmpFile = createFile(tmpName + ".pcm");
                    final File tmpOutFile = createFile(tmpName + ".wav");
                    mTmpFileAbs = tmpOutFile.getAbsolutePath();
                    System.out.println("路径名："+mTmpFileAbs);
                    mPathTv.setText(mTmpFileAbs);

                    isRecording = true;
                    mAudioRecord.startRecording();
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                FileOutputStream outputStream = new FileOutputStream(tmpFile.getAbsoluteFile());


                                while (isRecording) {
                                    int readSize = mAudioRecord.read(mAudioData, 0, mAudioData.length);
                                    Log.i("录音", "run: ------>" + readSize);
                                    outputStream.write(mAudioData);
                                }

                                outputStream.close();
                                pcmToWave(tmpFile.getAbsolutePath(), tmpOutFile.getAbsolutePath());
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                System.out.println("已松开.....");
                if (isRecording) {
                    showToast("已结束");
                    isRecording = false;
                    mAudioRecord.stop();
                }
            }
            return true;
        }
    }




    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_start_voice:

                break;
            case R.id.btn_stop_voice:

                break;
            case R.id.btn_check:

                break;
            default:
                break;
        }
    }


    private boolean checkPermission() {
        if (!PermissionUtil.isHasSDCardWritePermission(this)) {
            PermissionUtil.requestSDCardWrite(this);
            return false;
        }
        if (!PermissionUtil.isHasRecordPermission(this)) {
            PermissionUtil.requestRecordPermission(this);
            return false;
        }

        return true;
    }

    private void showToast(String content) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
    }

    private void pcmToWave(String inFileName, String outFileName) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long longSampleRate = mSampleRateInHZ;
        long totalDataLen = totalAudioLen + 36;
        int channels = 1;//你录制是单声道就是1 双声道就是2（如果错了声音可能会急促等）
        long byteRate = 16 * longSampleRate * channels / 8;

        byte[] data = new byte[mRecorderBufferSize];
        try {
            in = new FileInputStream(inFileName);
            out = new FileOutputStream(outFileName);

            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            writeWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private File createFile(String name) {

        String dirPath = Environment.getExternalStorageDirectory().getPath() + "/AudioRecord/";
        File file = new File(dirPath);

        if (!file.exists()) {
            file.mkdirs();
        }

        String filePath = dirPath + name;
        File objFile = new File(filePath);
        if (!objFile.exists()) {
            try {
                objFile.createNewFile();
                return objFile;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;


    }

    /*
    任何一种文件在头部添加相应的头文件才能够确定的表示这种文件的格式，wave是RIFF文件结构，每一部分为一个chunk，其中有RIFF WAVE chunk，
    FMT Chunk，Fact chunk,Data chunk,其中Fact chunk是可以选择的，
     */
    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate,
                                     int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (1 * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
