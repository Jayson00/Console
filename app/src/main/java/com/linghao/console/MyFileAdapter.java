package com.linghao.console;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by linghao on 2018/12/4.
 */

class MyFileAdapter extends BaseAdapter{

    Context mContext;
    ArrayList<MusicFileDTO> mMusicFileDTOS;


    public MyFileAdapter(Context context, ArrayList<MusicFileDTO> musicFileDTOS) {
        mContext = context;
        mMusicFileDTOS = musicFileDTOS;
    }

    @Override
    public int getCount() {
        return mMusicFileDTOS.size();
    }

    @Override
    public Object getItem(int i) {
        return mMusicFileDTOS.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder = null;
        if (view == null){
            view = LayoutInflater.from(mContext).inflate(R.layout.item_file,viewGroup,false);
            viewHolder = new ViewHolder();

            viewHolder.mNameTv = view.findViewById(R.id.tv_name);
            viewHolder.mSingerTv = view.findViewById(R.id.tv_singer);
            viewHolder.mPathTv = view.findViewById(R.id.tv_path);
            view.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) view.getTag();
        }

        viewHolder.mNameTv.setText(mMusicFileDTOS.get(i).getName());
        viewHolder.mSingerTv.setText(mMusicFileDTOS.get(i).getSingername());
        viewHolder.mPathTv.setText(mMusicFileDTOS.get(i).getData());


        return view;
    }

    private class ViewHolder{
        TextView mNameTv;
        TextView mSingerTv;
        TextView mPathTv;
    }
}
