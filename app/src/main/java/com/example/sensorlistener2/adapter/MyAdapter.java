package com.example.sensorlistener2.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.example.sensorlistener2.R;
import com.example.sensorlistener2.bean.CheckBean;

import java.util.ArrayList;
import java.util.List;
public class MyAdapter extends BaseAdapter {

    Context mContext;
    List<CheckBean> lists;
    LayoutInflater inflater;
    CheckBox ck;
    private int testBool;

    public MyAdapter(Context context, List<CheckBean> lists, CheckBox ck) {
        this.mContext = context;
        this.lists = lists;
        this.ck = ck;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return lists.size();
    }

    @Override
    public Object getItem(int position) {
        return lists.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.music_item, parent, false);
            holder = new ViewHolder();
            holder.tv = convertView.findViewById(R.id.music_title);
            holder.ck = convertView.findViewById(R.id.ck);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        // 初始化
        holder.tv.setText(lists.get(position).getTitle());
        // 防止滑动时选中混乱//
        if (holder.ck.isChecked()) { // 选中和未选中状态
            holder.ck.setButtonTintList(ColorStateList.valueOf(mContext.getResources().getColor(R.color.green)));
        } else {
            holder.ck.setButtonTintList(ColorStateList.valueOf(mContext.getResources().getColor(R.color.grey_dark)));
        }

        holder.ck.setId(position);
        holder.ck.setChecked(lists.get(position).isChecked());
        ///

        final ViewHolder finalHolder = holder;
        holder.ck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CheckBean dat = lists.get(buttonView.getId());
                dat.setChecked(isChecked);
                lists.set(buttonView.getId(), dat);

                // 这里做的是监听每个item是否全是true或者有false出现
                testBool = 0;
                for (int i = 0; i < lists.size(); i++) {
                    if (lists.get(i).isChecked()) {
                        testBool++;
                    }
                }
                if (testBool == lists.size()) {
                    ck.setChecked(true);
                } else {
                    ck.setChecked(false);
                }
                //
                if (isChecked) {
                    finalHolder.ck
                            .setButtonTintList(ColorStateList.valueOf(mContext.getResources().getColor(R.color.green)));
                } else {
                    finalHolder.ck.setButtonTintList(
                            ColorStateList.valueOf(mContext.getResources().getColor(R.color.grey_dark)));
                }
                if (finalHolder.ck.isChecked()) {
                    finalHolder.ck.setChecked(true);
                    dat.setChecked(true);
                } else {
                    finalHolder.ck.setChecked(false);
                    dat.setChecked(false);
                }
            }
        });
        return convertView;
    }

    public class ViewHolder {
        TextView tv;
        CheckBox ck;
    }

    public void ckAllVertical(boolean isCheck) {
        for (int i = 0; i < lists.size(); i++) {
            lists.get(i).setChecked(isCheck);
        }
        this.notifyDataSetChanged();
    }
}