package com.mobile.im.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mobile.im.R;

import java.util.List;

import im.mobile.model.Conversation;
import im.mobile.model.IMessage;


public class ConversationAdapter extends BaseAdapter {
    LayoutInflater inflater;

    List<Conversation> conversations;

    public ConversationAdapter(Context context, List<Conversation> conversations) {
        inflater = LayoutInflater.from(context);
        this.conversations = conversations;
    }

    public void updateData(List<Conversation> conversations) {
        this.conversations = conversations;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return conversations.size();
    }

    @Override
    public Conversation getItem(int i) {
        return conversations.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder = null;
        if (view == null) {
            view = inflater.inflate(R.layout.conversation_item_layout, null);
            viewHolder = new ViewHolder();
            viewHolder.msg_contet = view.findViewById(R.id.content);
            viewHolder.unreadCount = view.findViewById(R.id.unreadcount);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        Conversation conversation = getItem(i);
        IMessage msg = conversation.lastMsg;
        String content = msg.from + " : " + msg.content;
        viewHolder.msg_contet.setText(content);
        int unreadCount = conversation.getUnReadCount();

        if (unreadCount > 0) {
            viewHolder.unreadCount.setVisibility(View.VISIBLE);
            viewHolder.unreadCount.setText(String.valueOf(unreadCount));
        } else {
            viewHolder.unreadCount.setVisibility(View.GONE);
        }
        return view;
    }

    static class ViewHolder {
        TextView msg_contet;
        TextView unreadCount;
    }
}
