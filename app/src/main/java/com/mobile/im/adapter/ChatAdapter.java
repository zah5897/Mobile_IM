package com.mobile.im.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.media.Image;
import android.media.MediaPlayer;
import android.speech.tts.Voice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mobile.im.R;
import com.mobile.im.utils.GlideApp;
import com.mobile.im.utils.VoicePlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import im.mobile.IMClientManager;
import im.mobile.model.IMessage;
import im.mobile.model.VoiceMessage;

/**
 * 各种显示列表Adapter实现类。
 */
public class ChatAdapter extends BaseAdapter {
    private List<IMessage> msgs;
    private LayoutInflater mInflater;
    private AnimationDrawable voiceAnimation;

    public static final int TYPE_FROM_TX = 0;
    public static final int TYPE_FROM_IMG = 1;
    public static final int TYPE_FROM_AUDIO = 2;


    public static final int TYPE_TO_TX = 3;
    public static final int TYPE_TO_IMG = 4;
    public static final int TYPE_TO_AUDIO = 5;
    VoicePlayer voicePlayer;

    public ChatAdapter(Context context, List<IMessage> msgs) {
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
        String from = msg.from;
        String currentUsername = IMClientManager.getInstance().getCurrentLoginUsername();
        System.out.print(from);
        System.out.print(currentUsername);
        if (from.equals(currentUsername)) {

            switch (msg.type) {
                case TXT:
                    return TYPE_TO_TX;
                case IMG:
                    return TYPE_TO_IMG;
                case AUDIO:
                    return TYPE_TO_AUDIO;
                default:
                    return TYPE_TO_TX;
            }
        } else {
            switch (msg.type) {
                case TXT:
                    return TYPE_FROM_TX;
                case IMG:
                    return TYPE_FROM_IMG;
                case AUDIO:
                    return TYPE_FROM_AUDIO;
                default:
                    return TYPE_FROM_TX;
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        /**
         * 对类型进行判断,分别inflate不同的布局.
         * */
        final IMessage msg = getItem(position);
        switch (getItemViewType(position)) {
            case TYPE_FROM_TX:
                viewHolder = new ViewHolder();
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.item_msg_txt_from_layout, null);
                    viewHolder.username = convertView.findViewById(R.id.username);
                    viewHolder.content = convertView.findViewById(R.id.tv_chatcontent);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                break;
            case TYPE_FROM_IMG:
                viewHolder = new ViewHolder();
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.item_msg_img_from_layout, null);
                    viewHolder.username = convertView.findViewById(R.id.username);
                    viewHolder.img = convertView.findViewById(R.id.image);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                break;
            case TYPE_FROM_AUDIO:
                viewHolder = new ViewHolder();
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.item_msg_audio_from_layout, null);
                    viewHolder.username = convertView.findViewById(R.id.username);
                    viewHolder.img = convertView.findViewById(R.id.iv_voice);
                    viewHolder.length = convertView.findViewById(R.id.tv_length);
                    viewHolder.bubble = convertView.findViewById(R.id.bubble);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                break;
            case TYPE_TO_TX:
                viewHolder = new ViewHolder();
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.item_msg_txt_to_layout, null);
                    viewHolder.content = convertView.findViewById(R.id.tv_chatcontent);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                break;
            case TYPE_TO_IMG:
                viewHolder = new ViewHolder();
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.item_msg_img_to_layout, null);
                    viewHolder.img = convertView.findViewById(R.id.image);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                break;
            case TYPE_TO_AUDIO:
                viewHolder = new ViewHolder();
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.item_msg_audio_to_layout, null);
                    viewHolder.img = convertView.findViewById(R.id.iv_voice);
                    viewHolder.length = convertView.findViewById(R.id.tv_length);
                    viewHolder.bubble = convertView.findViewById(R.id.bubble);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                break;
        }
        viewHolder.bindMsg(msg, getItemViewType(position));
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

    public void msgDownloadSuccess(String fingerPrint) {
        for (IMessage msg : msgs) {
            if (msg.fingerPrint.equals(fingerPrint)) {
                msg.state = IMessage.IMessageState.DOWN_SUCCESS;
                break;
            }
        }
        notifyDataSetChanged();
    }

    public final class ViewHolder {
        public TextView content;
        public TextView username;
        public TextView length;
        public ImageView img;
        public View bubble;
        public IMessage msg;

        public void bindMsg(IMessage msg, int type) {
            this.msg = msg;
            switch (type) {
                case TYPE_FROM_TX:
                    username.setText(msg.from);
                    content.setText(msg.content);
                    break;
                case TYPE_FROM_IMG:
                    username.setText(msg.from);
                    GlideApp.with(img.getContext())
                            .load(msg.localPath)
                            .centerCrop().override(160, 160)
                            .placeholder(R.drawable.chat_image_selector)
                            .into(img);
                    break;
                case TYPE_FROM_AUDIO:
                    username.setText(msg.from);
                    length.setText(((VoiceMessage) msg).voiceTimeLen + "\"");
                    voicePlayer = VoicePlayer.getInstance(img.getContext());
                    if (voicePlayer.isPlaying() && msg.fingerPrint.equals(voicePlayer.getCurrentPlayingId())) {
                        startVoicePlayAnimation();
                    }
                    bubble.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            voiceItemClick();
                        }
                    });

                    break;
                case TYPE_TO_TX:
                    content.setText(msg.content);
                    break;
                case TYPE_TO_IMG:

                    GlideApp.with(img.getContext())
                            .load(msg.localPath)
                            .centerCrop().override(160, 160)
                            .placeholder(R.drawable.chat_image_selector)
                            .into(img);
                    break;
                case TYPE_TO_AUDIO:

                    length.setText(((VoiceMessage) msg).voiceTimeLen + "\"");
                    // To avoid the item is recycled by listview and slide to this item again but the animation is stopped.
                    voicePlayer = VoicePlayer.getInstance(img.getContext());
                    if (voicePlayer.isPlaying() && msg.fingerPrint.equals(voicePlayer.getCurrentPlayingId())) {
                        startVoicePlayAnimation();
                    }
                    bubble.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            voiceItemClick();
                        }
                    });
                    break;
            }
        }

        @SuppressLint("ResourceType")
        public void startVoicePlayAnimation() {
            img.setImageResource(R.anim.voice_playing_anim);
            voiceAnimation = (AnimationDrawable) img.getDrawable();
            voiceAnimation.start();
        }

        public void stopVoicePlayAnimation() {
            if (voiceAnimation != null) {
                voiceAnimation.stop();
            }
            img.setImageResource(R.mipmap.voice_playing);
        }

        private void voiceItemClick() {
            String msgId = msg.fingerPrint;
            if (voicePlayer.isPlaying()) {
                voicePlayer.stop();
                stopVoicePlayAnimation();
                String playingId = voicePlayer.getCurrentPlayingId();
                if (msgId.equals(playingId)) {
                    return;
                }
            }
            String localPath = msg.localPath;
            File file = new File(localPath);
            if (file.exists() && file.isFile()) {
                playVoice();
                startVoicePlayAnimation();
            } else {
                Toast.makeText(img.getContext(), "语音消息下载中...", Toast.LENGTH_SHORT).show();
                msg.download();
            }
        }

        private void playVoice() {
            voicePlayer.play(msg, new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopVoicePlayAnimation();
                }
            });
        }
    }
}
