package com.linghao.console;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by linghao on 2018/11/30.
 */

public class MyChatAdapter extends BaseAdapter {

    private SimpleDateFormat formatter;
    private Context mContext;
    private List<ChatDTO> mList;


    public MyChatAdapter(Context context, List<ChatDTO> list) {
        mContext = context;
        mList = list;
        formatter  = new SimpleDateFormat("MM月dd日 HH:mm:ss ");
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int i) {
        return mList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder = null;
        if (view == null){
            view = LayoutInflater.from(mContext).inflate(R.layout.item_chat,viewGroup,false);
            viewHolder = new ViewHolder();

            viewHolder.mLeftRl = view.findViewById(R.id.rl_left);
            viewHolder.mRightRl = view.findViewById(R.id.rl_right);
            viewHolder.left_content = view.findViewById(R.id.tv_chat_left_content);
            viewHolder.right_content = view.findViewById(R.id.tv_chat_right_content);
            viewHolder.mTimeTv = view.findViewById(R.id.tv_show_time);

            viewHolder.mLeftVoice = view.findViewById(R.id.iv_left_chat_voice);
            viewHolder.mRightVoice = view.findViewById(R.id.iv_right_chat_voice);

            viewHolder.mLeftFile = view.findViewById(R.id.lv_left_file);
            viewHolder.mLeftFilename = view.findViewById(R.id.tv_chat_left_file_name);
            viewHolder.mRightFile = view.findViewById(R.id.lv_right_file);
            viewHolder.mRightFilename = view.findViewById(R.id.tv_chat_right_file_name);


            viewHolder.mLeftVideoView = view.findViewById(R.id.vv_left_chat_video_start);
            //viewHolder.mImageView = view.findViewById(R.id.asd);
            viewHolder.mRightVideoView = view.findViewById(R.id.vv_right_chat_video_start);

            if (mList.get(i).getStatus() == 0){
                viewHolder.mLeftRl.setVisibility(View.GONE);
                viewHolder.mRightRl.setVisibility(View.VISIBLE);
            }else if(mList.get(i).getStatus() == 1){
                viewHolder.mLeftRl.setVisibility(View.VISIBLE);
                viewHolder.mRightRl.setVisibility(View.GONE);
            }


            view.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) view.getTag();
        }


        if (mList.get(i).getStatus() == 0){
            if (mList.get(i).getType().equals("txt")){
                viewHolder.right_content.setText(mList.get(i).getContent());
                viewHolder.mRightVoice.setVisibility(View.GONE);
                viewHolder.mRightFile.setVisibility(View.GONE);
                viewHolder.mRightVideoView.setVisibility(View.GONE);

            }else if (mList.get(i).getType().equals("voice")){
                viewHolder.right_content.setVisibility(View.GONE);
                viewHolder.mRightVoice.setVisibility(View.VISIBLE);
                viewHolder.mRightFile.setVisibility(View.GONE);
                viewHolder.mRightVideoView.setVisibility(View.GONE);

            }else if (mList.get(i).getType().equals("file")){
                viewHolder.right_content.setVisibility(View.GONE);
                viewHolder.mRightVoice.setVisibility(View.GONE);
                viewHolder.mRightFile.setVisibility(View.VISIBLE);
                viewHolder.mRightVideoView.setVisibility(View.GONE);
                viewHolder.mRightFilename.setText(mList.get(i).getContent());

            }else if (mList.get(i).getType().equals("video")){
                viewHolder.right_content.setVisibility(View.GONE);
                viewHolder.mRightVoice.setVisibility(View.GONE);
                viewHolder.mRightFile.setVisibility(View.GONE);
                viewHolder.mRightVideoView.setVisibility(View.VISIBLE);
            }
        }

        else if(mList.get(i).getStatus() == 1){
            if (mList.get(i).getType().equals("txt")){
                viewHolder.left_content.setText(mList.get(i).getContent());
                viewHolder.mLeftVoice.setVisibility(View.GONE);
                viewHolder.mLeftFile.setVisibility(View.GONE);
                viewHolder.mLeftVideoView.setVisibility(View.GONE);

            }else if (mList.get(i).getType().equals("voice")){
                viewHolder.left_content.setVisibility(View.GONE);
                viewHolder.mLeftVoice.setVisibility(View.VISIBLE);
                viewHolder.mLeftFile.setVisibility(View.GONE);
                viewHolder.mLeftVideoView.setVisibility(View.GONE);

            }else if (mList.get(i).getType().equals("file")){
                viewHolder.left_content.setVisibility(View.GONE);
                viewHolder.mLeftVoice.setVisibility(View.GONE);
                viewHolder.mLeftFile.setVisibility(View.VISIBLE);
                viewHolder.mLeftVideoView.setVisibility(View.GONE);

            }else if (mList.get(i).getType().equals("video")){
                viewHolder.left_content.setVisibility(View.GONE);
                viewHolder.mLeftVoice.setVisibility(View.GONE);
                viewHolder.mLeftFile.setVisibility(View.GONE);
                viewHolder.mLeftVideoView.setVisibility(View.VISIBLE);
            }
        }


        viewHolder.mTimeTv.setText(formatter.format(new Date(mList.get(i).getTime())));

        return view;
    }

    class ViewHolder{
        TextView left_content;
        TextView right_content;
        RelativeLayout mLeftRl;
        RelativeLayout mRightRl;
        TextView mTimeTv;

        ImageView mLeftVoice,mRightVoice;

        LinearLayout mLeftFile,mRightFile;
        TextView mLeftFilename,mRightFilename;

        VideoView mLeftVideoView,mRightVideoView;
        ImageView mImageView;

    }
}
