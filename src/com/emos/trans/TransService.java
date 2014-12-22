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
	
	//�¼�
	/**
	 * ����Ͽ�
	 */
	public static final int EVENT_NET_OFFLINE = 10;
	
	/**
	 * ��������
	 */
	public static final int EVENT_NET_ONLINE = 11;

	/**
	 * ����������
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
	 * ֹͣ�����㲥
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
		//ֹͣ����
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
			//ֹͣ����
			stopHeartBeatBroadcast();
			return -1;
		}
		
		int code = intent.getIntExtra("CODE", -1);
		Log.v("mango", "CODE == " + code);
		switch (code) {
		/* ����Alarm���������� */
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

		/* ȡ��Alarm */
		case ACT_CODE_CANCEL_ALARM:
			//ֹͣ����
			stopHeartBeatBroadcast();
			break;

		/* ���յ�Alarm������intent���������� */
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

		/* ���������� */
		case ACT_CODE_RESTART_CLIENT:
			restartConn();
			break;

		/* �ر����� */
		case ACT_CODE_STOP_CLIENT:
			//�ر�����
			if (client != null) {
				client.stop();
				client = null;
			}
			//ֹͣ�����㲥
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
		Log.d("mango", "ע�� UI����");
		this.mUIHandler = mUIHandler;
		
		//���θ���״̬
		mUIHandler.sendEmptyMessage(netStatus);
	}
	
	public void unregisterUIHandler(){
		Log.d("mango", "ע�� UI����");
		this.mUIHandler = null;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * service�ڵ�handler
	 */
	private Handler mSrvHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
//			if (mUIHandler != null) {
//				// ԭ�ⲻ�����ݸ�UI
//				Message msg_02 = mUIHandler.obtainMessage();
//				msg_02.copyFrom(msg);
//				mUIHandler.sendMessage(msg_02);
//			}
			switch (msg.what) {
			/**
			 * ����δ�ظ�,���ж�
			 */
			case TransClient.MSG_RD_TIMEOUT:
				netStatus = STATUS_OFFLINE;
				mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
				Log.e("mango", "TransClient.MSG_RD_TIMEOUT");
//				restartConn();
				break;
			/**
			 * socket����
			 */
			case TransClient.MSG_SOCK_ERR:
				netStatus = STATUS_OFFLINE;
				mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
				Log.e("mango", "TransClient.MSG_SOCK_ERR");
//				restartConn();
				break;
			/**
			 * ������������
			 */
			case TransClient.MSG_SOCK_UNKOWN_HOST:
				netStatus = STATUS_OFFLINE;
				mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
				Log.e("mango", "TransClient.MSG_SOCK_UNKOWN_HOST");
				Toast.makeText(TransService.this, "������������,���������������", Toast.LENGTH_SHORT).show();
				break;
			/**
			 * д�����
			 */
			case TransClient.MSG_WR_SEND_ERR:
				netStatus = STATUS_OFFLINE;
				mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
//				restartConn();
				break;

			/**
			 * д�����
			 */
			case TransClient.MSG_HB_SEND_ERR:
				Toast.makeText(TransService.this, "�������Ӵ���,HB_send_err", Toast.LENGTH_SHORT).show();
				netStatus = STATUS_OFFLINE;
				mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
//				restartConn();
				break;

			/**
			 * socket �ر�
			 */
			case TransClient.MSG_SOCK_CLOSED:
				Toast.makeText(TransService.this, "�������ӶϿ�,socket closed", Toast.LENGTH_SHORT).show();
				netStatus = STATUS_OFFLINE;
				mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
				break;
				
			/**
			 * ��������
			 */
			case TransClient.MSG_WR_THREAD_READY:
				// ���������ѽ���
//				reConnectTimes = 0;	//�����������
				// ������ϣ�����ע����Ϣ
				// �ж��Ƿ���Ҫ���Ͱ���Ϣ�� TODO
				
				break;
			/**
			 * ������Ϣ
			 */
			case TransClient.MSG_RD_THREAD_RECEIVED:
				Log.d("mango", "�յ���Ϣ");
				//�����ж�����,���Ƿ��ǵ�¼��Ϣ��������ͨ��Ϣ
				Bundle bd = msg.getData();
				String str = bd.getString("MSG");
				JSONObject resJson = null;
				int res = -1;
				try {
					resJson = new JSONObject(str);
					res = resJson.getInt("COD");
				} catch (JSONException e) {
					Toast.makeText(TransService.this, "Json��������", Toast.LENGTH_LONG).show();
					e.printStackTrace();
					break;
				}
				//����ǵ�¼��Ϣ�ظ�
				if(res == 200){
					Toast.makeText(TransService.this, "��¼�ɹ�", Toast.LENGTH_LONG).show();
					Log.d("mango", "cod == 200, ��¼�ɹ�");
					reConnectTimes = 0;
					netStatus = STATUS_ONLINE;
					if(mUIHandler!=null){
						mUIHandler.sendEmptyMessage(EVENT_NET_ONLINE);
					}
					break;
				}else if(res == 400){
					Log.d("mango", "cod == 400, ��֤����");
					Toast.makeText(TransService.this, "��֤����", Toast.LENGTH_LONG).show();
					netStatus = STATUS_OFFLINE;
					if(mUIHandler!=null){
						mUIHandler.sendEmptyMessage(EVENT_NET_OFFLINE);
					}
					break;
				}
				
				// �������ͨ��Ϣ,��Ҫ����,��ԭ�ⲻ�����ݸ�UI
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
		//ֹͣ����
		Intent intent = new Intent(this, TransService.class);
		intent.putExtra("CODE", TransService.ACT_CODE_STOP_CLIENT);
		startService(intent);
		
		if(reConnectTimes >= MAX_RECONNECT_TIME){
			Log.d("mango", "���������ﵽ���� " + MAX_RECONNECT_TIME);
			return;
		}
		
		reConnectTimes += 1;
		Log.d("mango", "����,�� " + reConnectTimes);
		//����������
		new Timer().schedule(new StartTask(), 500);
	}
	
	/**
	 * ����ע����Ϣ
	 */
	public void register() {
		if(client == null){
			Toast.makeText(this, "���紦������״̬", Toast.LENGTH_SHORT).show();
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
			Toast.makeText(this, "Json��ʽ����", Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * ���Ͱ���Ϣ
	 */
	public void bindSecurity() {
		if(client == null){
			Toast.makeText(this, "���紦������״̬", Toast.LENGTH_SHORT).show();
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
			// ��������
			Intent intent = new Intent(TransService.this, TransService.class);
			intent.putExtra("CODE", TransService.ACT_CODE_START_CLIENT);
			startService(intent);
			// ����ע��
			
		}

	}
}
