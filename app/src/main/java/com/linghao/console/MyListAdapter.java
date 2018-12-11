package com.linghao.console;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by linghao on 2018/12/4.
 */

class MyListAdapter  extends BaseAdapter {

    private Context mContext;
    private List<String> mList;


    public MyListAdapter(Context context, List<String> list) {
        mContext = context;
        mList = list;
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
            view = LayoutInflater.from(mContext).inflate(R.layout.item_friend,viewGroup,false);
            viewHolder = new ViewHolder();

            viewHolder.mTextView = view.findViewById(R.id.tv_username);

            view.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) view.getTag();
        }

        viewHolder.mTextView.setText(mList.get(i));


        return view;
    }

    class ViewHolder{
        TextView mTextView;
    }
}

