package youme.im.demo;

import java.util.UUID;

import com.youme.imsdk.YIMClient;
import com.youme.imsdk.YIMConstInfo;
import com.youme.imsdk.YIMMessage;
import com.youme.imsdk.YIMMessageBodyAudio;
import com.youme.imsdk.YIMMessageBodyText;
import com.youme.imsdk.callback.YIMEventCallback;
import com.youme.imsdk.internal.ChatRoom;
import com.youme.imsdk.internal.SendMessage;
import com.youme.imsdk.internal.SendVoiceMsgInfo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;


public class MainActivity extends Activity {
    private final static String TAG = "YMMainActivity";
    private final static String strAppKey = "YOUMEBC2B3171A7A165DC10918A7B50A4B939F2A187D0";
    private final static String strSecrect = "r1+ih9rvMEDD3jUoU+nj8C7VljQr7Tuk4TtcByIdyAqjdl5lhlESU0D+SoRZ30sopoaOBg9EsiIMdc8R16WpJPNwLYx2WDT5hI/HsLl1NJjQfa9ZPuz7c/xVb8GHJlMf/wtmuog3bHCpuninqsm3DRWiZZugBTEj2ryrhK7oZncBAAE=";
    private static String mUserId = "444455555";
    private final static String mPassword = "123456";
    private final static String mChatRoomId = "12345";
    private String mMsgContent = null;
    private String mStoragePath = null;
    private Button mSendTextButton = null;
    private Button mPTTButton = null;
    private Button loginButton = null;
    private EditText myUserIdText = null;
    private EditText recvUserIdText = null;
    private EditText messageText = null;

    private TextView mRecvView = null;
    private TextView mSendView = null;
    private Button mDownloadButton = null;
    private Button mAudioPlayerButton = null;
    private boolean mAudioDownloaded = false;
    private long mRecvAudioMsgId = 0;
    private String mRecvAudioPath = null;
    private String mSendAudioPath = null;
    private long mPTTStartTime = 0;
    private YouMeIMCallback mCallback = new YouMeIMCallback();
    private boolean isPlaying = false;

    protected void onCreate(Bundle savedInstanceState) {
        initYouMeIMEngine();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSendTextButton = (Button) findViewById(R.id.sendTextButton);
        mSendTextButton.setOnClickListener(mSendTextListener);
        mPTTButton = (Button) findViewById(R.id.pttButton);
        mPTTButton.setOnTouchListener(mPTTListener);
        mDownloadButton = (Button) findViewById(R.id.download);
        mDownloadButton.setOnClickListener(mDownloadListener);
        mDownloadButton.setEnabled(false);
        mAudioPlayerButton = (Button) findViewById(R.id.player);
        mAudioPlayerButton.setOnClickListener(mAudioPlayerListener);
        mAudioPlayerButton.setEnabled(false);
        mRecvView = (TextView) findViewById(R.id.recv);
        mSendView = (TextView) findViewById(R.id.send);
//		mMsgContent = getResources().getString(R.string.app_name);

        loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(commonClickListener);
        myUserIdText = (EditText) findViewById(R.id.myUserId);
        recvUserIdText = (EditText) findViewById(R.id.recvID);
        messageText = (EditText) findViewById(R.id.msgText);

        myUserIdText.setText(mUserId);
        recvUserIdText.setText(mUserId);
        messageText.setText("这是一条文本测试消息");
        /**
         * 设置本地存储路径
         */
        setStoragePath();

    }

    /**
     * 点击登录按钮执行逻辑
     */
    private OnClickListener commonClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getId() == loginButton.getId()) {
                MainActivity.mUserId = myUserIdText.getText().toString();
                login();
            }
        }
    };

    @Override
    protected void onDestroy() {
        leaveChatRoom();
        logout();
        super.onDestroy();
    }


    /**
     * 初始化引擎
     */
    private void initYouMeIMEngine() {
        YIMClient.getInstance().init(this, strAppKey, strSecrect, 0);
        YIMClient.getInstance().registerReconnectCallback(mCallback);
        YIMClient.getInstance().registerKickOffCallback(mCallback);
        YIMClient.getInstance().registerMsgEventCallback(mCallback);
    }

    private void login() {
        YIMClient.getInstance().login(mUserId, mPassword, "", new YIMEventCallback.ResultCallback<String>() {
            @Override
            public void onSuccess(String userId) {
                showSend("用户: " + userId + " 登录成功");
                joinChatRoom();
            }

            @Override
            public void onFailed(int errorCode, String userId) {
                showSend("用户: " + userId + " 登录失败:" + errorCode);
            }
        });
    }

    private void logout() {
        YIMClient.getInstance().logout(new YIMEventCallback.OperationCallback() {
            @Override
            public void onSuccess() {
                showSend("用户: " + mUserId + " 已经退出登录");
            }

            @Override
            public void onFailed(int i) {
                showSend("用户: " + mUserId + " 已经退出登录，但是可能本来就没有登录");
            }
        });
    }

    private void joinChatRoom() {
        YIMClient.getInstance().joinChatRoom(mChatRoomId, new YIMEventCallback.ResultCallback<ChatRoom>() {
            @Override
            public void onSuccess(ChatRoom chatRoom) {
                showSend("进入频道: " + chatRoom.groupId);
            }

            @Override
            public void onFailed(int errorCode, ChatRoom chatRoom) {
                showSend("进入频道失败: " + chatRoom.groupId);
            }
        });
    }

    private void leaveChatRoom() {
        YIMClient.getInstance().leaveChatRoom(mChatRoomId, new YIMEventCallback.ResultCallback<ChatRoom>() {
            @Override
            public void onSuccess(ChatRoom chatRoom) {
                showSend("离开频道: " + chatRoom);
            }

            @Override
            public void onFailed(int errorCode, ChatRoom chatRoom) {

            }
        });
    }

    private void sendTextMessage() {
        String recver = recvUserIdText.getText().toString();
        mMsgContent = messageText.getText().toString();
        if ("".equalsIgnoreCase(recver) || "".equalsIgnoreCase(mMsgContent)) {
            return;
        }
        YIMClient.getInstance().sendTextMessage(recver, YIMConstInfo.ChatType.PrivateChat, mMsgContent,"", new YIMEventCallback.ResultCallback<SendMessage>() {
            @Override
            public void onSuccess(SendMessage sendMessage) {
                showSend("发送消息成功: " + mMsgContent);
            }

            @Override
            public void onFailed(int i, SendMessage sendMessage) {
                showSend("发送消息失败: " + mMsgContent);
            }
        });
    }

    private void startRecordAudio() {
        String recver = recvUserIdText.getText().toString();
        if ("".equalsIgnoreCase(recver)) {
            return;
        }
        YIMClient.getInstance().startRecordAudioMessage(mChatRoomId, YIMConstInfo.ChatType.RoomChat, "attach message", true, false);
    }

    private void stopAndSendAudio() {
        YIMClient.getInstance().stopAndSendAudioMessage(new YIMEventCallback.AudioMsgEventCallback() {
            @Override
            public void onStartSendAudioMessage(long requestID, int errorcode, String strText, String strAudioPath, int audioTime) {
                //本地音频处理完成，准备发送，此时在本地已经可以获取到文件，可以展示到UI上
                showSend("录音成功，文本：" + strText);
                //回调是在子线程，渲染需要回到主线程
                mAudioPlayerButton.post(new Runnable() {
                    @Override
                    public void run() {
                        mAudioPlayerButton.setEnabled(true);
                    }
                });
                mSendAudioPath = strAudioPath;
            }

            @Override
            public void onSendAudioMessageStatus(int errorcode, SendVoiceMsgInfo voiceMsgInfo) {
                //语音发送完成
                showSend(errorcode == 0 ? "成功发送语音" : "发送失败" + errorcode);
            }
        });
    }

    private void cancelRecordAudio() {
        YIMClient.getInstance().cancleAudioMessage();
    }

    private void downloadAudioMessage() {
        YIMClient.getInstance().downloadAudioMessage(mRecvAudioMsgId, getAudioPath(), new YIMEventCallback.DownloadFileCallback() {
            @Override
            public void onDownload(int errorCode, YIMMessage yimMessage, String savedPath) {
                if (errorCode == YIMConstInfo.Errorcode.Success) {
                    showRecv(getResources().getString(R.string.downloadSuccess));
                    mDownloadButton.post(new Runnable() {
                        @Override
                        public void run() {
                            mDownloadButton.setText(R.string.recvplayer);
                        }
                    });
                    mAudioDownloaded = true;
                    mRecvAudioPath = savedPath;
                } else {
                    showRecv("下载失败:" + errorCode);
                }
            }
        });
    }

    private void setStoragePath() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            mStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
    }

    private String getAudioPath() {
        String audioPath = mStoragePath + "/";
        audioPath += UUID.randomUUID().toString() + ".wav";
        return audioPath;
    }


    /**
     * 发送消息执行逻辑
     */
    private OnClickListener mSendTextListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getId() != mSendTextButton.getId())
                return;
            //发送文本清空
            showSend("");
            //接收文本清空
            showRecv("");
            sendTextMessage();
        }
    };


    /**
     * 按住说话按钮处理逻辑
     */
    private OnTouchListener mPTTListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mSendView.setText("");
                    mRecvView.setText("");
                    mDownloadButton.setText(R.string.download);
                    mDownloadButton.setEnabled(false);
                    mAudioPlayerButton.setEnabled(false);
                    mSendView.setText("开始录音");
                    mPTTStartTime = System.currentTimeMillis();
                    startRecordAudio();
                    return true;
                case MotionEvent.ACTION_UP:
                    mAudioDownloaded = false;
                    long endTime = System.currentTimeMillis();
                    if (endTime - mPTTStartTime < 1000) {
                        mSendView.setText("录音时间过短");
                    } else {
                        mSendView.setText("正在发送语音");
                        stopAndSendAudio();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    mSendView.setText("取消录音");
                    cancelRecordAudio();
                    return true;
                default:
                    break;
            }
            return false;
        }
    };


    /**
     * 点击了下载语音按钮
     */
    private OnClickListener mDownloadListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getId() != mDownloadButton.getId())
                return;
            if (mAudioDownloaded) {
                if (!isPlaying) {
                    isPlaying = true;
                    YIMClient.getInstance().startPlayAudio(mRecvAudioPath, new YIMEventCallback.ResultCallback<String>() {
                        @Override
                        public void onSuccess(String audioPath) {
                            Log.d(TAG, "播放结束");
                            isPlaying = false;
                        }

                        @Override
                        public void onFailed(int errorcode, String audioPath) {
                            Log.d(TAG, "播放失败");
                            isPlaying = false;
                        }
                    });
                } else {
                    YIMClient.getInstance().stopPlayAudio();
                }
            } else {
                downloadAudioMessage();
            }
        }
    };

    /**
     * 播放按钮监听
     */
    private OnClickListener mAudioPlayerListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getId() != mAudioPlayerButton.getId())
                return;
            YIMClient.getInstance().startPlayAudio(mSendAudioPath, new YIMEventCallback.ResultCallback<String>() {
                @Override
                public void onSuccess(String audioPath) {
                    Log.d(TAG, "播放结束");
                    isPlaying = false;
                }

                @Override
                public void onFailed(int errorcode, String audioPath) {
                    Log.d(TAG, "播放失败");
                    isPlaying = false;
                }
            });
        }
    };

    private void showRecv(final String recv) {
        if (null != mRecvView)
            mRecvView.post(new Runnable() {
                @Override
                public void run() {
                    mRecvView.setText(recv);
                }
            });
    }

    private void showSend(final String send) {
        if (null != mSendView)
            mSendView.post(new Runnable() {
                @Override
                public void run() {
                    mSendView.setText(send);
                }
            });
    }

    private class YouMeIMCallback implements YIMEventCallback.ReconnectCallback, YIMEventCallback.MessageEventCallback, YIMEventCallback.KickOffCallback {


        /**
         * 接收到用户发来的消息
         *
         * @param message 消息内容结构体
         */
        public void onRecvMessage(YIMMessage message) {
            if (null == message)
                return;
            int msgType = message.getMessageType();
            if (YIMConstInfo.MessageBodyType.TXT == msgType) {
                showRecv("接收到一条文本消息： " + ((YIMMessageBodyText) message.getMessageBody()).getMessageContent());
            } else if (YIMConstInfo.MessageBodyType.Voice == msgType) {
                YIMMessageBodyAudio audioMessage = (YIMMessageBodyAudio) message.getMessageBody();

                int audioDuration = audioMessage.getAudioTime();//录音时长，秒
                String param = audioMessage.getParam();//自定义内容
                String recognizeText = audioMessage.getText();//语音转文字的识别结果

                showRecv("接收到一条语音消息请下载");
                mAudioDownloaded = false;
                //回调是在子线程，渲染需要回到主线程
                mDownloadButton.post(new Runnable() {
                    @Override
                    public void run() {
                        mDownloadButton.setEnabled(true);
                        mDownloadButton.setText(R.string.download);
                    }
                });

                mRecvAudioMsgId = message.getMessageID();//下载用的消息id
            }
        }

        /**
         * 语音的识别文本回调
         *
         * @param errorcode 错误码
         * @param requestID 消息ID
         * @param text      返回的语音识别文本
         */
        @Override
        public void onGetRecognizeSpeechText(int errorcode, long requestID, String text) {

        }

        /*
         * 功能：录音音量变化回调, 频率:1s 约8次
         * @param volume：音量值(0到1)
         */
        @Override
        public void onRecordVolume(float volume) {

        }

        /**
         * 开始重连通知
         */
        @Override
        public void onStartReconnect() {

        }

        /**
         * 重连结果通知
         *
         * @param result 0-重连成功，1-重连失败，再次重连，2-重连失败
         */
        @Override
        public void onRecvReconnectResult(int result) {

        }

        /**
         * 被踢下线通知
         */
        @Override
        public void onKickOff() {

        }

        /**
         * 功能：新消息通知（默认自动接收消息，只有调用setReceiveMessageSwitch设置为不自动接收消息，才会收到该回调），有新消息的时候会通知该回调，频道消息会通知消息来自哪个频道ID
         *
         * @param chatType：聊天类型
         * @param targetID：频道ID
         */
        @Override
        public void onRecvNewMessage(int chatType, String targetID) {

        }
    }
}
