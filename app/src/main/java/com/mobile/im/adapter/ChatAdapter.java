package com.mobile.im.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mobile.im.R;

import net.openmob.mobileimsdk.android.ClientCoreSDK;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.mobile.IMClientManager;
import im.mobile.model.IMessage;

/**
 * 各种显示列表Adapter实现类。
 */
public class ChatAdapter extends BaseAdapter {
    private List<IMessage> msgs;
    private LayoutInflater mInflater;


    public static final int TYPE_FROM_TX = 0;
    public static final int TYPE_FROM_IMG = 1;
    public static final int TYPE_FROM_AUDIO = 2;


    public static final int TYPE_TO_TX = 3;
    public static final int TYPE_TO_IMG = 4;
    public static final int TYPE_TO_AUDIO = 5;
    private Context context;

    public ChatAdapter(Context context, List<IMessage> msgs) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        this.msgs = msgs;
        if (this.msgs == null) {
            this.msgs = new ArrayList<>();
        }
    }

    public void addItem(IMessage msg) {
        msgs.add(msg);
        this.notifyDataSetChanged();
    }

    public void replaceData(List<IMessage> msgs) {
        this.msgs.clear();
        this.msgs.addAll(msgs);
        notifyDataSetChanged();
    }

    public void updateMsg(IMessage msg) {
        int index = msgs.indexOf(msg);
        if (index != -1) {
            msgs.remove(index);
            msgs.add(index, msg);
        }
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return msgs.size();
    }

    @Override
    public IMessage getItem(int arg0) {
        return msgs.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 6;
    }

    @Override
    public int getItemViewType(int position) {
        IMessage msg = getItem(position);
        if (msg.from.equals(IMClientManager.getInstance(context).getCurrentLoginUsername())) {
            return TYPE_TO_TX;
        } else {
            return TYPE_FROM_TX;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        /**
         * 对类型进行判断,分别inflate不同的布局.
         * */
        IMessage msg = getItem(position);
        switch (getItemViewType(position)) {
            case TYPE_FROM_TX:
                viewHolder = new ViewHolder();
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.item_msg_from_layout, null);
                    viewHolder.content = (TextView) convertView.findViewById(R.id.tv_chatcontent);
                    viewHolder.username = (TextView) convertView.findViewById(R.id.username);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                viewHolder.username.setText(msg.from);
                viewHolder.content.setText(msg.content);
                break;
            case TYPE_TO_TX:
                viewHolder = new ViewHolder();
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.item_msg_to_layout, null);
                    viewHolder.content = (TextView) convertView.findViewById(R.id.tv_chatcontent);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                viewHolder.content.setText(msg.content);
                break;
        }
        return convertView;
    }

    public void updateMsgBeReceived(String theFingerPrint) {
        for (IMessage msg : msgs) {
            if (msg.fingerPrint.equals(theFingerPrint)) {
                msg.state = IMessage.IMessageState.BERECEIVED;
                break;
            }
        }
        notifyDataSetChanged();
    }

    public final class ViewHolder {
        public TextView content;
        public TextView username;
    }
}
