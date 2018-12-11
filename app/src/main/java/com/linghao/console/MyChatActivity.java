package com.linghao.console;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;


import com.hyphenate.EMCallBack;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMFileMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.chat.EMVideoMessageBody;
import com.hyphenate.chat.EMVoiceMessageBody;
import com.linghao.console.audiodemo.PermissionUtil;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class MyChatActivity extends AppCompatActivity implements View.OnClickListener,EMMessageListener {


    // 当前会话对象
    private EMConversation mConversation;
    // 消息监听器
    private EMMessageListener mMessageListener;

    private String mChatId = null;
    private TextView mNameTv;

    private ListView mListView;
    private MyChatAdapter mChatAdapter;

    private EditText mInputEt;
    private Button mCommitTextBtn;

    private LinearLayout mLinearLayout;
    private ImageView mAddIv,mVoiceIv,mVideoIv,mFileIv;

    private List<ChatDTO> mChatDTOList = new ArrayList<>();
    private ChatDTO mChatDTO;

    private Activity mActivity;
    private ClipboardManager mCM;
    private static final String TAG = "MyChatActivity";


    /**
     * 录音数队列
     */
    private ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    private AudioRecord mAudioRecord;
    private int mRecorderBufferSize;
    private byte[] mAudioData;

    /*默认数据*/
    private int mSampleRateInHZ = 8000; //采样率
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;  //位数
    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;   //声道

    private boolean isRecording = false;
    private String mTmpFileAbs = null;


    /**播放文件录音*/
    private MediaPlayer mMediaPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_chat);
        mActivity = this;
        mMessageListener = this;
        mCM = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        mNameTv = findViewById(R.id.tv_chat_name);
        mListView = findViewById(R.id.list_chat);
        mCommitTextBtn = findViewById(R.id.btn_chat_commit_text);
        mInputEt = findViewById(R.id.et_chat_input);
        mAddIv = findViewById(R.id.iv_add);
        mLinearLayout = findViewById(R.id.lv_more_options);
        mVoiceIv = findViewById(R.id.iv_voice);
        mVideoIv = findViewById(R.id.iv_video);
        mFileIv = findViewById(R.id.iv_file);
        mCommitTextBtn.setOnClickListener(this);
        mAddIv.setOnClickListener(this);
        mLinearLayout.setOnClickListener(this);
        mVoiceIv.setOnTouchListener(new ButtonLongClick());
        mVideoIv.setOnClickListener(this);
        mFileIv.setOnClickListener(this);

        mChatId = getIntent().getStringExtra("user");
        mNameTv.setText(mChatId);
        mLinearLayout.setVisibility(View.GONE);

        initConversation();

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


    /**权限请求*/
    private boolean checkPermission() {
        if (!PermissionUtil.isHasSDCardWritePermission(this)) {
            PermissionUtil.requestSDCardWrite(this);
            return false;
        }
        if (!PermissionUtil.isHasRecordPermission(this)) {
            PermissionUtil.requestRecordPermission(this);
            return false;
        }
        if (!PermissionUtil.isHasCameraPermission(this)) {
            PermissionUtil.requestCameraPermission(this);
            return false;
        }
        return true;
    }


    private void initData() {
        mRecorderBufferSize = AudioRecord.getMinBufferSize(mSampleRateInHZ, mChannelConfig, mAudioFormat);
        mAudioData = new byte[320];
        if (mAudioRecord == null){
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, mSampleRateInHZ, mChannelConfig, mAudioFormat, mRecorderBufferSize);
        }
    }


    /**
     * 初始化会话对象，并且根据需要加载更多消息
     */
    private void initConversation() {

        mConversation = EMClient.getInstance().chatManager().getConversation(mChatId, null, true);
        // 设置当前会话未读数为 0
        mConversation.markAllMessagesAsRead();

        if (mConversation.getLastMessage()!=null){
            if (mConversation.getLastMessage().getType() == EMMessage.Type.TXT){
                //文本
                mChatDTO = new ChatDTO();
                if (mChatId.equals(mConversation.getLastMessage().getFrom())){
                    mChatDTO.setStatus(1);//对方发的消息
                }else {
                    mChatDTO.setStatus(0);//我发的消息
                }
                mChatDTO.setType("txt");
                mChatDTO.setTime(mConversation.getLastMessage().getMsgTime());
                mChatDTO.setContent(((EMTextMessageBody)mConversation.getLastMessage().getBody()).getMessage());
                mChatDTOList.add(mChatDTO);
                mHandler.sendEmptyMessage(1);
            }else if (mConversation.getLastMessage().getType() == EMMessage.Type.VOICE){
                System.out.println("接收到音频");
                String url = ((EMVoiceMessageBody)mConversation.getLastMessage().getBody()).getRemoteUrl();
                System.out.println("解析出地址："+url);
                mChatDTO = new ChatDTO();
                if (mChatId.equals(mConversation.getLastMessage().getFrom())){
                    System.out.println("相同");
                    mChatDTO.setStatus(1);
                }else {
                    System.out.println("不同");
                    mChatDTO.setStatus(0);
                }
                mChatDTO.setType("voice");
                mChatDTO.setTime(mConversation.getLastMessage().getMsgTime());
                mChatDTO.setContent("");
                mChatDTO.setRemoteUrl(url);
                mChatDTOList.add(mChatDTO);
                mHandler.sendEmptyMessage(1);
            }else if (mConversation.getLastMessage().getType() == EMMessage.Type.FILE){
                String content = ((EMFileMessageBody)mConversation.getLastMessage().getBody()).getFileName();
                String url = ((EMFileMessageBody)mConversation.getLastMessage().getBody()).getRemoteUrl();
                System.out.println("解析出地址："+url+"文件名："+content);
                mChatDTO = new ChatDTO();
                if (mChatId.equals(mConversation.getLastMessage().getFrom())){
                    System.out.println("相同");
                    mChatDTO.setStatus(1);
                }else {
                    System.out.println("不同");
                    mChatDTO.setStatus(0);
                }
                mChatDTO.setType("file");
                mChatDTO.setTime(mConversation.getLastMessage().getMsgTime());
                mChatDTO.setContent(content);
                mChatDTO.setRemoteUrl(url);
                mChatDTOList.add(mChatDTO);
                mHandler.sendEmptyMessage(1);
            }else if (mConversation.getLastMessage().getType() == EMMessage.Type.VIDEO){
                System.out.println("解析视频信息："+mConversation.getLastMessage().getBody());
                String url = ((EMVideoMessageBody)mConversation.getLastMessage().getBody()).getRemoteUrl();
                System.out.println("解析出视频地址："+url);
                mChatDTO = new ChatDTO();
                if (mChatId.equals(mConversation.getLastMessage().getFrom())){
                    System.out.println("相同");
                    mChatDTO.setStatus(1);
                }else {
                    System.out.println("不同");
                    mChatDTO.setStatus(0);
                }
                mChatDTO.setType("video");
                mChatDTO.setTime(mConversation.getLastMessage().getMsgTime());
                mChatDTO.setContent("");
                mChatDTO.setRemoteUrl(url);
                mChatDTOList.add(mChatDTO);
                mHandler.sendEmptyMessage(1);
            }
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_chat_commit_text:
                if (!mInputEt.getText().toString().equals("")){

                    EMMessage message = EMMessage.createTxtSendMessage(mInputEt.getText().toString(), mChatId);
                    EMClient.getInstance().chatManager().sendMessage(message);
                    message.setMessageStatusCallback(new EMCallBack() {
                        @Override public void onSuccess() {
                            // 消息发送成功，打印下日志，正常操作应该去刷新ui
                            mChatDTO = new ChatDTO();
                            mChatDTO.setStatus(0);
                            mChatDTO.setType("txt");
                            mChatDTO.setTime(System.currentTimeMillis());
                            mChatDTO.setContent(mInputEt.getText().toString());
                            mChatDTOList.add(mChatDTO);
                            mInputEt.setText("");
                            mHandler.sendEmptyMessage(1);
                        }

                        @Override public void onError(int i, String s) {
                            // 消息发送失败，打印下失败的信息，正常操作应该去刷新ui
                        }
                        @Override public void onProgress(int i, String s) {
                            // 消息发送进度，一般只有在发送图片和文件等消息才会有回调，txt不回调
                        }
                    });
                }else {
                    Toast.makeText(mActivity,"输入内容不能为空",Toast.LENGTH_SHORT).show();
                }
            case R.id.iv_add:
                if (mLinearLayout.getVisibility() == View.VISIBLE){
                    mLinearLayout.setVisibility(View.GONE);
                }else {
                    mLinearLayout.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.iv_video:
                String videoname = System.currentTimeMillis()+".mp4";
                videoFile = createFile(videoname);
                if (videoFile.exists()){
                    Uri uri = Uri.fromFile(videoFile);
                    Intent captureImageCamera = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    //启动摄像头应用程序
                    captureImageCamera.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                    captureImageCamera.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60);

                    startActivityForResult(captureImageCamera,11);
                }
                break;
            case R.id.iv_file:
                Intent intent = new Intent(mActivity,MusicFileActivity.class);
                startActivityForResult(intent,10);
                break;
            default:
                break;
        }
    }

    private File videoFile = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {

        if (resultCode == RESULT_OK && requestCode == 10){
            EMMessage emMessage = EMMessage.createFileSendMessage(data.getStringExtra("data"),mChatId);
            EMClient.getInstance().chatManager().sendMessage(emMessage);
            emMessage.setMessageStatusCallback(new EMCallBack() {
                @Override
                public void onSuccess() {
                    System.out.println("文件发送成功");
                    // 消息发送成功，打印下日志，正常操作应该去刷新ui
                    mChatDTO = new ChatDTO();
                    mChatDTO.setStatus(0);
                    mChatDTO.setType("file");
                    mChatDTO.setTime(System.currentTimeMillis());
                    mChatDTO.setContent(data.getStringExtra("name"));
                    mChatDTOList.add(mChatDTO);
                    mHandler.sendEmptyMessage(1);
                }

                @Override
                public void onError(int code, String error) {

                }

                @Override
                public void onProgress(int progress, String status) {

                }
            });
        }else if (resultCode == RESULT_OK && requestCode == 11){
            System.out.println("录取的视频的信息："+videoFile.getAbsolutePath());
            System.out.println("录取的视频的时长："+getVideoDuration(videoFile.getAbsolutePath()));

            System.out.println("录取的视频的缩略图路径："+bitmapToStringPath(mActivity,videoFile.getAbsolutePath()));
            String video = videoFile.getAbsolutePath();
            final String thumbnail = bitmapToStringPath(mActivity,videoFile.getAbsolutePath());

            EMMessage emMessage = EMMessage.createVideoSendMessage(video,thumbnail,getVideoDuration(videoFile.getAbsolutePath()),mChatId);
            EMClient.getInstance().chatManager().sendMessage(emMessage);
            emMessage.setMessageStatusCallback(new EMCallBack() {
                @Override
                public void onSuccess() {
                    System.out.println("视频发送成功");
                    // 消息发送成功，打印下日志，正常操作应该去刷新ui
                    mChatDTO = new ChatDTO();
                    mChatDTO.setStatus(0);
                    mChatDTO.setType("video");
                    mChatDTO.setTime(System.currentTimeMillis());
                    mChatDTO.setThumbnail(thumbnail);
                    mChatDTO.setRemoteUrl(videoFile.getAbsolutePath());
                    mChatDTOList.add(mChatDTO);
                    mHandler.sendEmptyMessage(1);
                }

                @Override
                public void onError(int code, String error) {

                }
                @Override
                public void onProgress(int progress, String status) {

                }
            });
        }
        super.onActivityResult(requestCode, resultCode, data);
    }





    /**将返回的content地址转化成本地图片路径*/
    private static String bitmapToStringPath(Context context,String videoUrl){

        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(videoUrl);
        Bitmap bitmap = media.getFrameAtTime();

        String savePath = Environment.getExternalStorageDirectory().getPath() + "/AudioRecord/";
        File filePic;

        try {
            filePic = new File(savePath + System.currentTimeMillis() + ".jpg");
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
        return filePic.getAbsolutePath();
    }


    /**获取视频总时长*/
    private int getVideoDuration(String path){
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(path);
        String duration = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION); //
        return Integer.parseInt(duration)/1000;
    }



    /**收到新消息*/
    @Override public void onMessageReceived(List<EMMessage> list) {
        Log.i("接收到新消息","");
        // 循环遍历当前收到的消息
        for (EMMessage message : list) {
            Log.i("lzan13", "收到新消息:" + message);
            if (message.getFrom().equals(mChatId)) {
                // 设置消息为已读
                mConversation.markMessageAsRead(message.getMsgId());
                // 因为消息监听回调这里是非ui线程，所以要用handler去更新ui
                if (message.getType() == EMMessage.Type.TXT){
                    mChatDTO = new ChatDTO();
                    mChatDTO.setStatus(1);
                    mChatDTO.setType("txt");
                    mChatDTO.setTime(message.getMsgTime());
                    String txt = ((EMTextMessageBody)message.getBody()).getMessage();
                    mChatDTO.setContent(txt);
                    mChatDTOList.add(mChatDTO);
                    mHandler.sendEmptyMessage(1);
                }else if (message.getType() == EMMessage.Type.VOICE){
                    System.out.println("接收到音频");
                    String url = ((EMVoiceMessageBody)message.getBody()).getRemoteUrl();
                    System.out.println("解析出音频地址："+url);
                    mChatDTO = new ChatDTO();
                    mChatDTO.setStatus(1);
                    mChatDTO.setType("voice");
                    mChatDTO.setTime(message.getMsgTime());
                    mChatDTO.setRemoteUrl(url);
                    mChatDTOList.add(mChatDTO);
                    mHandler.sendEmptyMessage(1);
                }else if (message.getType() == EMMessage.Type.FILE){
                    System.out.println("接收到文件"+message.getBody());
                    String url = ((EMFileMessageBody)message.getBody()).getRemoteUrl();
                    System.out.println("解析出文件地址："+url);
                    mChatDTO = new ChatDTO();
                    mChatDTO.setStatus(1);
                    mChatDTO.setType("file");
                    mChatDTO.setTime(message.getMsgTime());
                    mChatDTO.setContent(url);
                    mChatDTOList.add(mChatDTO);
                    mHandler.sendEmptyMessage(1);
                }else if (message.getType() == EMMessage.Type.VIDEO){
                    String url = ((EMFileMessageBody)message.getBody()).getRemoteUrl();
                    System.out.println("解析出视频地址："+url);
                    mChatDTO = new ChatDTO();
                    mChatDTO.setStatus(1);
                    mChatDTO.setType("video");
                    mChatDTO.setTime(message.getMsgTime());
                    mChatDTO.setRemoteUrl(url);
                    mChatDTOList.add(mChatDTO);
                    mHandler.sendEmptyMessage(1);
                }

            } else {
                // TODO 如果消息不是当前会话的消息发送通知栏通知
            }
        }
    }

    @Override
    public void onCmdMessageReceived(List<EMMessage> messages) {

    }
    @Override
    public void onMessageRead(List<EMMessage> messages) {

    }
    @Override
    public void onMessageDelivered(List<EMMessage> messages) {

    }
    @Override
    public void onMessageRecalled(List<EMMessage> messages) {

    }
    @Override
    public void onMessageChanged(EMMessage message, Object change) {

    }


    /**
     * 自定义实现Handler，主要用于刷新UI操作
     */
    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:

                    MediaPlayer player = new MediaPlayer();
                    try {
                        player.setDataSource(mTmpFileAbs);  //为音频文件的路径
                        player.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    int duration= player.getDuration()/1000;//获取音频的时间

                    player.release();//记得释放资源

                    EMMessage message = EMMessage.createVoiceSendMessage(mTmpFileAbs,duration,mChatId);
                    EMClient.getInstance().chatManager().sendMessage(message);
                    message.setMessageStatusCallback(new EMCallBack() {
                        @Override
                        public void onSuccess() {
                            // 消息发送成功，打印下日志，正常操作应该去刷新ui
                            mChatDTO = new ChatDTO();
                            mChatDTO.setStatus(0);
                            mChatDTO.setType("voice");
                            mChatDTO.setTime(System.currentTimeMillis());
                            mChatDTO.setContent("");
                            mChatDTO.setRemoteUrl(mTmpFileAbs);
                            mChatDTOList.add(mChatDTO);
                            mHandler.sendEmptyMessage(1);

                        }
                        @Override
                        public void onError(int code, String error) {
                            System.out.println("System.out.println(\"发送失败\");");
                        }
                        @Override
                        public void onProgress(int progress, String status) {

                        }
                    });
                    break;
                case 1:
                    System.out.println("添加消息的类型："+mChatDTOList.get(mChatDTOList.size()-1).getType());
                    Log.e(TAG, "handleMessage: "+mChatDTO );
                    mChatAdapter = new MyChatAdapter(mActivity,mChatDTOList);
                    mListView.setAdapter(mChatAdapter);
                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, final View view, final int i, long l) {


                            //文本的点击事件
                            if (mChatDTOList.get(i).getType().equals("txt")){
                                if (mChatDTOList.get(i).getStatus() == 0) {
                                    int time = new Long(System.currentTimeMillis()).intValue() - new Long(mChatDTOList.get(i).getTime()).intValue();
                                    System.out.println("相差的时间：" + time);
                                    if ((time) > 120 * 1000) {
                                        Toast.makeText(mActivity, "时间大于两分钟，不可撤回", Toast.LENGTH_SHORT).show();
                                    } else {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                                        builder.setMessage("确认撤回消息");
                                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Toast.makeText(mActivity, "撤回功能暂未开通", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {

                                            }
                                        });
                                        builder.show();
                                    }
                                }else if (mChatDTOList.get(i).getStatus() == 1){
                                    TextView textView = view.findViewById(R.id.tv_chat_left_content);
                                    mCM.setPrimaryClip(ClipData.newPlainText(null,textView.getText().toString()));
                                    Toast.makeText(mActivity,"复制成功",Toast.LENGTH_SHORT).show();
                                }
                            }
                            //语音的点击事件
                            else if (mChatDTOList.get(i).getType().equals("voice")){
                                final ImageView rightimageView = view.findViewById(R.id.iv_right_chat_voice);
                                final ImageView leftimageView = view.findViewById(R.id.iv_left_chat_voice);

                                mMediaPlayer = new MediaPlayer();
                                try {
                                    mMediaPlayer.setDataSource(mChatDTOList.get(i).getRemoteUrl());
                                    mMediaPlayer.prepareAsync();
                                    mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                        @Override
                                        public void onPrepared(MediaPlayer mediaPlayer) {
                                            if (mChatDTOList.get(i).getStatus() == 0){
                                                rightimageView.setImageResource(R.mipmap.ic_chat_right_voice_start);
                                            }else if (mChatDTOList.get(i).getStatus() == 1){
                                                leftimageView.setImageResource(R.mipmap.ic_chat_left_voice_start);
                                            }
                                            mediaPlayer.start();
                                        }
                                    });
                                    mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                        @Override
                                        public void onCompletion(MediaPlayer mediaPlayer) {
                                            if (mChatDTOList.get(i).getStatus() == 0){
                                                rightimageView.setImageResource(R.mipmap.ic_chat_right_voice_stop);
                                            }else if (mChatDTOList.get(i).getStatus() == 1){
                                                leftimageView.setImageResource(R.mipmap.ic_chat_left_voice_stop);
                                            }
                                            mMediaPlayer.release();
                                            mMediaPlayer = null;
                                        }
                                    });
                                    mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                                        @Override
                                        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                                            System.out.println("i:"+i+"<---->"+"i1:"+i1);
                                            return false;
                                        }
                                    });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            //文件的点击事件
                             else if (mChatDTOList.get(i).getType().equals("file")){
                                if (mChatDTOList.get(i).getStatus() == 1){
                                    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                                    builder.setTitle("下载文件？");
                                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                        }
                                    });
                                    builder.setPositiveButton("下载", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            showToast("这里写下载代码。");
                                        }
                                    });
                                    builder.show();
                                }
                            }
                            //video的点击事件
                            else if (mChatDTOList.get(i).getType().equals("video")) {
                                showToast("触发点击事件");

                            }else {
                                showToast("进来了");
                            }
                        }

                    });
                    break;
                default:
                    break;
            }
        }
    };


    private void showToast(String content) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
    }



    private class ButtonLongClick  implements View.OnTouchListener{

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                mVoiceIv.setImageResource(R.mipmap.ic_voice_press);
                System.out.println("长按....");
                if (!isRecording) {

                    showToast("已开始");

                    String tmpName = System.currentTimeMillis() + "_" + mSampleRateInHZ + "";
                    final File tmpFile = createFile(tmpName + ".pcm");
                    final File tmpOutFile = createFile(tmpName + ".wav");
                    mTmpFileAbs = tmpOutFile.getAbsolutePath();
                    System.out.println("路径名："+mTmpFileAbs);

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
                mVoiceIv.setImageResource(R.mipmap.ic_voice);
                if (isRecording) {
                    showToast("已结束");
                    isRecording = false;
                    mAudioRecord.stop();
                }
            }
            return true;
        }
    }




    @Override protected void onResume() {
        super.onResume();
        // 添加消息监听
        Log.i("注册监听","");
        EMClient.getInstance().chatManager().addMessageListener(mMessageListener);
    }

    @Override protected void onStop() {
        super.onStop();
        // 移除消息监听
        EMClient.getInstance().chatManager().removeMessageListener(mMessageListener);
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

        mHandler.sendEmptyMessage(0);

    }





    private File createFile(String name) {

        String dirPath = Environment.getExternalStorageDirectory().getPath() + "/AudioRecord/";
        File file = new File(dirPath);

        if (!file.exists()) {
            file.mkdirs();
            System.out.println("创建文件夹");
        }

        String filePath = dirPath + name;
        File objFile = new File(filePath);
        if (!objFile.exists()) {
            try {
                objFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("新建文件");
            return objFile;

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
