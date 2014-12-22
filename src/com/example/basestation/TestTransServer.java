package com.example.basestation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TestTransServer implements Runnable {

	private static final String TAG = "mango";

	private Handler handler;
	public static final String IP = "125.216.243.235";
	public static final int PORT = 8002;
	private static boolean stop = true;

	private static final String CONTENT = "11111辣椒三大类附件三路口附近抗衰老的解放路口建设路交罚款了交水电费路口见萨拉看到房间里肯定撒解放路克水煎服的路口见萨菲路口见山东龙口福建路撒解放路卡萨丁解放路口京东上离开福建省离开房间了凯撒解放路口撒娇法律框架撒老看见的撒解放路凯撒件法律框架撒发大水了敬爱的看房间里肯定撒解放路口就撒旦离开福建省凉快的减肥绿卡建设路款到即发来看睡觉了卡萨罚单路口见山东龙口分建设路扩大姐夫 11111";

	public TestTransServer(Handler handler) {
		super();
		this.handler = handler;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		stop = false;
		Socket socket;
		try {
			socket = new Socket(IP, PORT);
			OutputStream os = socket.getOutputStream();
			new Thread(new Reader(socket, handler)).start();
			byte[] header = new byte[4];
			header[0] = 0x2C;
			header[1] = 0x00;

			int count = 0;
			int len;
			byte[] jBytes;
			/* get Length */
			while (!stop) {
				JSONObject json = new JSONObject();
				json.put("STR", count++);
//				json.put("CONTENT", CONTENT);
				jBytes = json.toString().getBytes();
				len = jBytes.length;
				header[2] = (byte) ((len & 0x0000FF00) >> 8);
				header[3] = (byte) (len & 0x000000FF);
				os.write(header, 0, 4);
				os.write(jBytes, 0, len);
				Thread.sleep(1000);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void setStop(boolean stop) {
		TestTransServer.stop = stop;
	}
	
	class Reader implements Runnable{

		Socket socket;
		Handler mHandler;
		InputStream is;
		
		public Reader(Socket socket, Handler mHandler) throws IOException {
			super();
			this.socket = socket;
			this.mHandler = mHandler;
			this.is = socket.getInputStream();
		}

		@Override
		public void run() {
			byte[] rBuf = new byte[8192];
			short len;int rCount;
			int rtvl;
			while(!stop){
				try {
					/* read head */
					rtvl = is.read(rBuf, 0, 4);
					if(rtvl < 0)
						break;
					//parsing header
					short l1 = (short)rBuf[2];
					short l0 = (short)rBuf[3];
					l1 <<= 8;
					len = (short)(l1|l0);
					Log.v(TAG, "len: " + len);
					/* read content */
					rtvl = is.read(rBuf, 0, len);
					if(rtvl < 0)
						break;
					String s = new String(rBuf, 0, len);
					Log.v(TAG, "received: " + s);
//					rCount = is.read(rBuf);
//					String s = new String(rBuf, 0, rCount);
					Bundle bd = new Bundle();
					bd.putString("STR", s);
					Message msg = mHandler.obtainMessage();
					msg.setData(bd);
					msg.what = 100;
					mHandler.sendMessage(msg);
				} catch (IOException e) {
					mHandler.sendEmptyMessage(400);
					e.printStackTrace();
				}
			}
		}
		
	}
}
