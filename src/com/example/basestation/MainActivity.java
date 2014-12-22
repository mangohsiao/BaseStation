package com.example.basestation;

import org.json.JSONException;
import org.json.JSONObject;

import com.emos.trans.MMsg;
import com.emos.trans.TransClient;
import com.emos.trans.TransService;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	private static final String TAG = "mango";

	private String user = "CANBO_USER";
	
	private TextView txv_content;
	private ScrollView scrollView1;
	private Button button1;
	private Button button2;
	private Button button3;
	private Button button4;
	private Button button5;
	private Button button6;
	private EditText editText1;
	private EditText editText2;
	private TextView textView_status;
//	TransClient client;

	Intent it;
	
	boolean isRegUUID = false;
	TransService localService = null;
	ServiceConnection srvCon = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			if(localService != null){
				localService.unregisterUIHandler();
			}

			localService = null;
		}

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder binder) {
			localService = ((TransService.LocalBinder) binder).getService();
			if(localService != null){
				localService.registerUIHandler(mHandler);
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		txv_content = (TextView) findViewById(R.id.txv_content);
		scrollView1 = (ScrollView) findViewById(R.id.scrollView1);
		textView_status = (TextView)findViewById(R.id.textView_status);
		button1 = (Button) findViewById(R.id.button1);
		button2 = (Button) findViewById(R.id.button2);
		button3 = (Button) findViewById(R.id.button3);
		button4 = (Button) findViewById(R.id.button4);
		button5 = (Button) findViewById(R.id.button5);
		button6 = (Button) findViewById(R.id.button6);
		editText1 = (EditText)findViewById(R.id.editText1);
		editText2 = (EditText)findViewById(R.id.editText2);
		button1.setOnClickListener(this);
		button2.setOnClickListener(this);
		button3.setOnClickListener(this);
		button4.setOnClickListener(this);
		button5.setOnClickListener(this);
		button6.setOnClickListener(this);

		/*
		 * 绑定网络服务
		 */
		it = new Intent(this, TransService.class);
//		bindService(it, srvCon, Context.BIND_AUTO_CREATE);

		/*
		 * TelephonyManager tm =
		 * (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE); String
		 * operator = tm.getNetworkOperator(); int mcc =
		 * Integer.parseInt(operator.substring(0, 3)); int mnc =
		 * Integer.parseInt(operator.substring(3)); GsmCellLocation location =
		 * (GsmCellLocation) tm.getCellLocation(); int lac = location.getLac();
		 * int cellId = location.getCid(); Log.i(TAG, " MCC = " + mcc +
		 * "\t MNC = " + mnc + "\t LAC = " + lac + "\t CID = " + cellId);
		 */
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		//取消绑定
		MainActivity.this.unbindService(srvCon);
//		TestTransServer.setStop(true);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(it != null){
			bindService(it, srvCon, Context.BIND_AUTO_CREATE);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 100:
				Bundle bd = msg.getData();
				txv_content.append("\n" + bd.getString("STR"));
				break;

			case 200:
				Bundle bd2 = msg.getData();
				String msgStr = bd2.getString("MSG");
				txv_content.append("\n" + msgStr);
				NotificationManager nm = (NotificationManager) MainActivity.this
						.getSystemService(Context.NOTIFICATION_SERVICE);
				Notification n = new Notification(R.drawable.ic_launcher,
						msgStr, System.currentTimeMillis());
				n.setLatestEventInfo(MainActivity.this, "okok", msgStr, null);
				n.defaults |= Notification.DEFAULT_ALL;
				nm.notify(R.string.app_name, n);

				break;

			case 400:
				txv_content.append("\nRead error.");
				break;
			case 404:
				txv_content.append("TIMEOUT EXCEPTION.");
				Intent it02 = new Intent(MainActivity.this, TransService.class);
				it02.putExtra("CODE", 102);
				startService(it02);
				break;
			case TransService.EVENT_NET_OFFLINE:
				textView_status.setText("离线");
				break;
			case TransService.EVENT_NET_CONNECTING:
				textView_status.setText("连接中....");
				break;
			case TransService.EVENT_NET_ONLINE:
				textView_status.setText("在线");
				break;
			default:
				break;
			}
			scrollView1.post(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					scrollView1.fullScroll(ScrollView.FOCUS_DOWN);
				}
			});
		}

	};

	@Override
	public void onClick(View v) {
		TransClient client = localService.getClient();
		switch (v.getId()) {
		/*
		 * 启动服务
		 */
		case R.id.button1:
			if(localService != null){
				localService.setIp(editText2.getText().toString());
//				localService.registerUIHandler(mHandler);
				Intent it = new Intent(this, TransService.class);
				it.putExtra("CODE", TransService.ACT_CODE_START_CLIENT);
				startService(it);
			}
			break;

		/*
		 * 发送0x11消息，注册用户
		 */
		case R.id.button2:
			try {
				user = editText1.getText().toString();
				JSONObject json = new JSONObject();
				json.put("USER", user);
				json.put("UUID", "30001");
				json.put("PSWD", "12345678");
				MMsg msg = new MMsg();
				msg.msgType = 0x11;
				msg.content = json.toString();
				client.sendMessage(msg);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			break;

		/*
		 * 发送0x15或者0x17，注册或者取消
		 */
		case R.id.button3:
			try {
				user = editText1.getText().toString();
				JSONObject json = new JSONObject();
				json.put("USER", user);
				json.put("UUID", "30001");
				MMsg msg = new MMsg();
				msg.msgType = (short) ((isRegUUID == true) ? 0x17 : 0x15);
				msg.content = json.toString();
				client.sendMessage(msg);
				isRegUUID = isRegUUID ? false : true;
			} catch (JSONException e) {
				e.printStackTrace();
			}
			break;

		/*
		 * CODE=102,服务指令：取消心跳；
		 */
		case R.id.button4:
			Intent it01 = new Intent(this, TransService.class);
			it01.putExtra("CODE", 102);
			startService(it01);
			break;
			
		/*
		 * CODE 104 服务指令：停止网络client
		 */
		case R.id.button5:
			Intent it05 = new Intent(this, TransService.class);
			it05.putExtra("CODE", TransService.ACT_CODE_STOP_CLIENT);
			startService(it05);
			break;

		/*
		 * CODE 103 服务指令：重启网络client
		 */
		case R.id.button6:
			Intent it06 = new Intent(this, TransService.class);
			it06.putExtra("CODE", TransService.ACT_CODE_RESTART_CLIENT);
			startService(it06);
			break;
		default:
			break;
		}
	}
}
