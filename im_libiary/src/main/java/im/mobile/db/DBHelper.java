package im.mobile.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import net.openmob.mobileimsdk.android.ClientCoreSDK;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import im.mobile.IMClientManager;
import im.mobile.model.Conversation;
import im.mobile.model.IMessage;


public class DBHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "m_h.db";
    public static final String MSG_TABLE_NAME = "_msg_history";
    public static final String CONVERSATION_TABLE_NAME = "_conversation_list";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    private ContentValues msgToContentValues(IMessage msg) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("_from", msg.from);
        contentValues.put("_to", msg.to);
        contentValues.put("content", msg.content);
        contentValues.put("finger_print", msg.fingerPrint);
        contentValues.put("type", msg.type.ordinal());
        contentValues.put("server_time", msg.serverTime);
        contentValues.put("read_state", msg.readState);
        contentValues.put("state", msg.state.ordinal());
        return contentValues;
    }


    private ContentValues createConversationContentValue(String friendUsername, String editTxt, String last_finger_print, long last_time) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("friendUsername", friendUsername);
        contentValues.put("edit_txt", editTxt);
        contentValues.put("last_finger_print", last_finger_print);
        contentValues.put("last_time", last_time);
        return contentValues;
    }

    private static String getMsgTableName() {
        return ClientCoreSDK.getInstance().getCurrentLoginUsername() + MSG_TABLE_NAME;
    }

    private static String getConversationTableName() {
        return ClientCoreSDK.getInstance().getCurrentLoginUsername() + CONVERSATION_TABLE_NAME;
    }

    public void saveMsg(IMessage msg) {
        SQLiteDatabase db = getWritableDatabase();
        long id = db.insert(getMsgTableName(), null, msgToContentValues(msg));
        String friendUsername = msg.to;
        if (msg.to.equals(ClientCoreSDK.getInstance().getCurrentLoginUsername())) {
            friendUsername = msg.from;
        }
        saveConversation(friendUsername, "", msg.fingerPrint, msg.serverTime);
    }


    public void saveOfflineMsgs(List<IMessage> msgs) {
        if (msgs.isEmpty()) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        for (IMessage message : msgs) {
            db.insert(getMsgTableName(), null, msgToContentValues(message));
        }
        //这里存在消息插入，需要更新当前聊天列表
        IMClientManager.getInstance().doNotifyOfflineLoad();

        IMessage last = msgs.get(msgs.size() - 1);
        last = getLatestMsg(last.from);
        saveConversation(last.from, "", last.fingerPrint, last.serverTime);
    }


    public IMessage getLatestMsg(String friendUsername) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "select *from " + getMsgTableName() + " where _from=? order by server_time desc limit 1";
        Cursor c = db.rawQuery(sql, new String[]{friendUsername});
        IMessage msg = null;
        if (c != null && c.moveToFirst()) {
            msg = cursorToMsg(c);
            c.close();
        }
        return msg;
    }

    public IMessage loadMsgByFingerPrint(String finger_print) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "select *from " + getMsgTableName() + " where finger_print=?";
        Cursor c = db.rawQuery(sql, new String[]{finger_print});
        IMessage msg = null;
        if (c != null && c.moveToFirst()) {
            msg = cursorToMsg(c);
            c.close();
        }
        return msg;
    }


    private IMessage cursorToMsg(Cursor c) {
        IMessage msg = new IMessage();
        msg.from = c.getString(c.getColumnIndex("_from"));
        msg.to = c.getString(c.getColumnIndex("_to"));
        msg.content = c.getString(c.getColumnIndex("content"));
        msg.type = IMessage.IMessageType.values()[c.getInt(c.getColumnIndex("type"))];
        msg.fingerPrint = c.getString(c.getColumnIndex("finger_print"));
        msg.state = IMessage.IMessageState.values()[c.getInt(c.getColumnIndex("state"))];
        long time = Long.parseLong(c.getString(c.getColumnIndex("server_time")));
        msg.serverTime = time;
        return msg;
    }

    private Conversation cursorToConversation(Cursor c) {
        Conversation conversation = new Conversation();
        conversation.friendUsername = c.getString(c.getColumnIndex("friendUsername"));
        conversation.editTxt = c.getString(c.getColumnIndex("edit_txt"));
        conversation.last_finger_print = c.getString(c.getColumnIndex("last_finger_print"));
        return conversation;
    }

    public List<IMessage> loadMessages(String with) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "select *from " + getMsgTableName() + " where (_from=? and _to=?) or (_from=? and _to=?)  order by server_time";
        String i = ClientCoreSDK.getInstance().getCurrentLoginUsername();
        Cursor c = db.rawQuery(sql, new String[]{i, with, with, i});
        List<IMessage> list = new ArrayList<>();
        if (c != null) {
            while (c.moveToNext()) {
                IMessage msg = cursorToMsg(c);
                list.add(msg);
            }
            c.close();
        }
        return list;
    }

    public void updateMsgStateBeReceived(String theFingerPrint, long serverTime) {
        updateMsgState(theFingerPrint, serverTime, IMessage.IMessageState.BERECEIVED.ordinal());
    }

    public void updateMsgState(String theFingerPrint, long serverTime, int state) {
        String tableName = getMsgTableName();
        ContentValues contentValues = new ContentValues();
        contentValues.put("server_time", serverTime);
        contentValues.put("state", state);
        getWritableDatabase().update(tableName, contentValues, "finger_print=?", new String[]{theFingerPrint});
        updateConversation(theFingerPrint, serverTime);
    }

    public int getUnReadCount(String friendUsername) {
        String tableName = getMsgTableName();
        String sql = "select count(*) from " + tableName + " where _from=? and read_state=?";
        Cursor c = getReadableDatabase().rawQuery(sql, new String[]{friendUsername, String.valueOf(1)});
        int count = 0;
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        return count;
    }

    public void updateRead(String friendUsername) {
        String tableName = getMsgTableName();
        ContentValues contentValues = new ContentValues();
        contentValues.put("read_state", 0);
        getWritableDatabase().update(tableName, contentValues, "_from=?", new String[]{friendUsername});
    }

    public List<Conversation> loadConversations() {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "select *from " + getConversationTableName() + " order by last_time desc";
        Cursor cursor = db.rawQuery(sql, null);
        List<Conversation> list = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Conversation conversation = cursorToConversation(cursor);
                conversation.lastMsg = loadMsgByFingerPrint(conversation.last_finger_print);
                list.add(conversation);
            }
            cursor.close();
        }
        return list;
    }


    public synchronized void saveConversation(String friendUsername, String preTxt, String last_finger_print, long lastTime) {
        String table = getConversationTableName();
        String existSql = "select count(*) from " + table + " where friendUsername=?";
        Cursor c = getReadableDatabase().rawQuery(existSql, new String[]{friendUsername});

        if (c != null && c.moveToFirst()) {
            int count = c.getInt(0);
            c.close();
            if (count > 0) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("edit_txt", preTxt);
                contentValues.put("last_finger_print", last_finger_print);
                contentValues.put("last_time", lastTime);
                getWritableDatabase().update(table, contentValues, "friendUsername=?", new String[]{friendUsername});
                IMClientManager.getInstance().doNotifyConversationRefresh();
                return;
            }
        }
        getWritableDatabase().insert(table, null, createConversationContentValue(friendUsername, preTxt, last_finger_print, lastTime));
        IMClientManager.getInstance().doNotifyConversationRefresh();
    }

    private void updateConversation(String theFingerPrint, long serverTime) {
        String tableName = getConversationTableName();
        ContentValues c = new ContentValues();
        c.put("last_time", serverTime);
        getWritableDatabase().update(tableName, c, "last_finger_print=?", new String[]{theFingerPrint});
        IMClientManager.getInstance().doNotifyConversationRefresh();
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // create table Orders(Id integer primary key, CustomName text, OrderPrice integer, Country text);
        //创建本地消息缓存表
        String realTableName = getMsgTableName();
        String sql = "create table if not exists " + realTableName + " (" +
                " _from text," +
                " _to text," +
                " content text," +
                " finger_print text," +
                " type integer," +
                " state integer," +
                " read_state integer," +
                "server_time text)";
        sqLiteDatabase.execSQL(sql);

        //创建会话表
        realTableName = getConversationTableName();
        sql = "create table if not exists " + realTableName + " (" +
                "friendUsername text primary key," +
                "last_finger_print text," +
                "last_time integer," +
                "edit_txt text)";
        sqLiteDatabase.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        String realTableName = getMsgTableName();
        String sql = "DROP TABLE IF EXISTS " + realTableName;
        sqLiteDatabase.execSQL(sql);

        realTableName = getConversationTableName();
        sql = "DROP TABLE IF EXISTS " + realTableName;
        sqLiteDatabase.execSQL(sql);
        onCreate(sqLiteDatabase);
    }

    public void init() {
        //每次登陆都重新创建表
        onCreate(getWritableDatabase());
    }


}