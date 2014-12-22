package com.emos.trans;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.basestation.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class TransService extends Service {

	/* Constant Value */
	public static final int ACT_CODE_START_CLIENT = 101;
	public static final int ACT_CODE_CANCEL_ALARM = 102;
	public static final int ACT_CODE_RESTART_CLIENT = 103;
	public static final int ACT_CODE_STOP_CLIENT = 104;
	
	public static final int ACT_CODE_SEND_HB = 201;
	
	//事件
	/**
	 * 网络断开
	 */
	public static final int EVENT_NET_OFFLINE = 10;
	
	/**
	 * 网络在线
	 */
	public static final int EVENT_NET_ONLINE = 11;

	/**
	 * 网络连接中
	 */
	public static final int EVENT_NET_CONNECTING = 12;


	private IBinder binder = new TransService.LocalBinder();

	private TransClient client = null;
	private AlarmManager am = null;
	private PendingIntent pi = null;
	private Handler mUIHandler;
	private Handler msgHandler;
	private String ip = "125.216.243.250";
	private int port = 8002;

	private int netStatus = -1;
	public static final int STATUS_OFFLINE = EVENT_NET_OFFLINE;
	public static final int STATUS_CONNECTING = EVENT_NET_CONNECTING;
	public static final int STATUS_ONLINE = EVENT_NET_ONLINE;
	
	public int getNetStatus() {
		return netStatus;
	}

	public TransClient getClient() {
		return client;
	}

	public void setClient(TransClient client) {
		this.client = client;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}

	public class LocalBinder extends Binder {
		public TransService getService() {
			return TransService.this;
		}
	}
	
	/**
	 * 停止心跳广播
	 */
	public void stopHeartBeatBroadcast() {
		if (am != null) {
			am.cancel(pi);
		}
		am = null;
	}

	@Override
	public void onCreate() {
		Log.v("mango", "service onCreate()");
		Toast.makeText(this, "service onCreate()", Toast.LENGTH_LONG).show();
		/* get the ip and port and URL */
		pi = PendingIntent.getBroadcast(this, 0, new Intent(this,
				HeartBeatBroadcastReceiver.class),
				Intent.FLAG_ACTIVITY_NEW_TASK);
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		Log.v("mango", "service onDestroy()");
		//停止心跳
		stopHeartBeatBroadcast();
		super.onDestroy();
	}

	@Override
	public void onLowMemory() {
		Log.v("mango", "service onLowMemory()");
		super.onLowMemory();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v("mango", "service onStartCommand()");
		
		if (intent == null) {
			//停止心跳
			stopHeartBeatBroadcast();
			return -1;
		}
		
		int code = intent.getIntExtra("CODE", -1);
		Log.v("mango", "CODE == " + code);
		switch (code) {
		/* 开启Alarm，负责心跳 */
		case ACT_CODE_START_CLIENT:
			netStatus = STATUS_CONNECTING;
			mUIHandler.sendEmptyMessage(EVENT_NET_CONNECTING);
			// connect
			client = new TransClient(this, mSrvHandler);
			TransClient.ip = ip;
			client.start();
			/* start HeartBeat Alarm */
			if (am == null) {
				am = (AlarmManager) this
						.getSystemService(Context.ALARM_SERVICE);
			}
			long now = System.currentTimeMillis();
			am.setInexactRepeating(AlarmManager.RTC_WAKEUP, now,
					TransCommon.PHONE_HEART_BEAT_ITVL, pi);
			break;

		/* 取消Alarm */
		case ACT_CODE_CANCEL_ALARM:
			//停止心跳
			stopHeartBeatBroadcast();
			break;

		/* 接收到Alarm发来的intent，发送心跳 */
		case ACT_CODE_SEND_HB:
			if (client != null) {
				try {
					client.sendHeartBeat();
				} catch (IOException e) {
					e.printStackTrace();
					mSrvHandler.sendEmptyMessage(TransClient.MSG_HB_SEND_ERR);
				}
			}
			break;

		/* 重启长连接 */
		case ACT_CODE_RESTART_CLIENT:
			restartConn();
			break;

		/* 关闭连接 */
		case ACT_CODE_STOP_CLIENT:
			//关闭连接
			if (client != null) {
				client.stop();
				client = null;
			}
			//停止心跳广播
			stopHeartBeatBroadcast();
			break;

		default:
			break;
		}
		return super.onStartCommand(intent, flags, startId);
	}

	public void closeConnection() {
		
	}
	
	public void registerUIHandler(Handler mUIHandler) {
		Log.d("mango", "注册 UI监听");
		this.mUIHandler = mUIHandler;
		
		//初次更新状态
		mUIHandler.sendEmptyMessage(netStatus);
	}
	
	public void unregisterUIHandler(){
		Log.d("mango", "注销 UI监听");
		this.mUIHandler = null;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * service内的handler
	 */
	private Handler mSrvHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
//			if (mUIHandler != null) {
//				// 原封不动传递给UI
//				Message msg_02 = mUIHandler.obtainMessage();
//				msg_02.copyFrom(msg);
//				mUIHandler.sendMessage(msg_02);
//			}
			switch (msg.what) {
			/**
			 * 心跳未回复,读中断
			 */
			case TransClient.MSG_RD_TIMEOUT:
				netStatus = STATUS_OFFLINE;
				mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
				Log.e("mango", "TransClient.MSG_RD_TIMEOUT");
//				restartConn();
				break;
			/**
			 * socket错误
			 */
			case TransClient.MSG_SOCK_ERR:
				netStatus = STATUS_OFFLINE;
				mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
				Log.e("mango", "TransClient.MSG_SOCK_ERR");
//				restartConn();
				break;
			/**
			 * 域名解析错误
			 */
			case TransClient.MSG_SOCK_UNKOWN_HOST:
				netStatus = STATUS_OFFLINE;
				mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
				Log.e("mango", "TransClient.MSG_SOCK_UNKOWN_HOST");
				Toast.makeText(TransService.this, "域名解析出错,检查网络连接配置", Toast.LENGTH_SHORT).show();
				break;
			/**
			 * 写入错误
			 */
			case TransClient.MSG_WR_SEND_ERR:
				netStatus = STATUS_OFFLINE;
				mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
//				restartConn();
				break;

			/**
			 * 写入错误
			 */
			case TransClient.MSG_HB_SEND_ERR:
				Toast.makeText(TransService.this, "网络链接错误,HB_send_err", Toast.LENGTH_SHORT).show();
				netStatus = STATUS_OFFLINE;
				mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
//				restartConn();
				break;

			/**
			 * socket 关闭
			 */
			case TransClient.MSG_SOCK_CLOSED:
				Toast.makeText(TransService.this, "网络连接断开,socket closed", Toast.LENGTH_SHORT).show();
				netStatus = STATUS_OFFLINE;
				mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
				break;
				
			/**
			 * 正常连接
			 */
			case TransClient.MSG_WR_THREAD_READY:
				// 网络连接已建立
//				reConnectTimes = 0;	//清空重连次数
				// 启动完毕，发送注册消息
				// 判断是否需要发送绑定信息。 TODO
				
				break;
			/**
			 * 接收消息
			 */
			case TransClient.MSG_RD_THREAD_RECEIVED:
				Log.d("mango", "收到消息");
				//事先判断类型,看是否是登录消息或者是普通消息
				Bundle bd = msg.getData();
				String str = bd.getString("MSG");
				JSONObject resJson = null;
				int res = -1;
				try {
					resJson = new JSONObject(str);
					res = resJson.getInt("COD");
				} catch (JSONException e) {
					Toast.makeText(TransService.this, "Json解析错误", Toast.LENGTH_LONG).show();
					e.printStackTrace();
					break;
				}
				//如果是登录消息回复
				if(res == 200){
					Toast.makeText(TransService.this, "登录成功", Toast.LENGTH_LONG).show();
					Log.d("mango", "cod == 200, 登录成功");
					reConnectTimes = 0;
					netStatus = STATUS_ONLINE;
					if(mUIHandler!=null){
						mUIHandler.sendEmptyMessage(EVENT_NET_ONLINE);
					}
					break;
				}else if(res == 400){
					Log.d("mango", "cod == 400, 验证错误");
					Toast.makeText(TransService.this, "验证错误", Toast.LENGTH_LONG).show();
					netStatus = STATUS_OFFLINE;
					if(mUIHandler!=null){
						mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
					}
					break;
				}
				
				// 如果是普通消息,需要上抛,则原封不动传递给UI
//				if (msgHandler != null) {
//					Message msg_received = msgHandler.obtainMessage();
//					msg_received.copyFrom(msg);
//					msgHandler.sendMessage(msg_received);
//				}
				
				if (mUIHandler != null) {
				Message msg_received = mUIHandler.obtainMessage();
				msg_received.copyFrom(msg);
				mUIHandler.sendMessage(msg_received);
			}
				break;
			default:
				break;
			}
		}

	};

	public final static int MAX_RECONNECT_TIME = 3;
	private int reConnectTimes = 0;
	private void restartConn() {
		//停止连接
		Intent intent = new Intent(this, TransService.class);
		intent.putExtra("CODE", TransService.ACT_CODE_STOP_CLIENT);
		startService(intent);
		
		if(reConnectTimes >= MAX_RECONNECT_TIME){
			Log.d("mango", "重连次数达到上限 " + MAX_RECONNECT_TIME);
			return;
		}
		
		reConnectTimes += 1;
		Log.d("mango", "重连,第 " + reConnectTimes);
		//开启新连接
		new Timer().schedule(new StartTask(), 500);
	}
	
	/**
	 * 发送注册信息
	 */
	public void register() {
		if(client == null){
			Toast.makeText(this, "网络处于离线状态", Toast.LENGTH_SHORT).show();
			return;
		}
		
		try {
			JSONObject json = new JSONObject();
			json.put("USER", "mango1");
			json.put("UUID", "home01");
			json.put("PSWD", "12345678");
			MMsg msg_01 = new MMsg();
			msg_01.msgType = 0x11;
			msg_01.content = json.toString();
			client.sendMessage(msg_01);
		} catch (JSONException e) {
			e.printStackTrace();
			Toast.makeText(this, "Json格式错误", Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * 发送绑定信息
	 */
	public void bindSecurity() {
		if(client == null){
			Toast.makeText(this, "网络处于离线状态", Toast.LENGTH_SHORT).show();
			return;
		}
		
		try {
			JSONObject json = new JSONObject();
			json.put("USER", "mango1");
			json.put("UUID", "home01");
			MMsg msg_02 = new MMsg();
			msg_02.msgType = 0x15;
			msg_02.content = json.toString();
			client.sendMessage(msg_02);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	class StartTask extends TimerTask {

		@Override
		public void run() {
			// 启动连接
			Intent intent = new Intent(TransService.this, TransService.class);
			intent.putExtra("CODE", TransService.ACT_CODE_START_CLIENT);
			startService(intent);
			// 发送注册
			
		}

	}
}
