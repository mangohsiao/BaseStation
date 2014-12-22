package com.emos.trans;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TransReadThread implements Runnable {

	// public static final String IP = "125.216.237.248";
	public static final String IP = "125.216.243.250";
	public static final int PORT = 8002;

	private Handler mHandler;
	private Context mContext;
	private boolean isToStop = true;
	private TransWriteThread writeThread = null;
	private OutputStream os = null;

	private Socket socket;

	public TransReadThread(Context ctx, Handler mHandler) {
		super();
		this.mContext = ctx;
		this.mHandler = mHandler;
	}

	public OutputStream getOs() {
		return os;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		isToStop = false;
		// establish Socket
		try {
			Log.v("mango", "TransReadThread is starting....");
			socket = new Socket(TransClient.ip, PORT);
			if(socket == null){
				return;
			}
			if(!socket.isClosed()){
				Log.e("mango", "socket is connected.");
			}else {
				Log.e("mango", "socket == not connected.");
				return;
			}
			socket.setSoTimeout(TransCommon.PHONE_SO_READ_TIMEOUT);
			os = socket.getOutputStream();

			InputStream is = socket.getInputStream();

			Log.v("mango", "TransWriteThread is starting.... ");
			writeThread = new TransWriteThread(mHandler, os);
			Thread t_write = new Thread(writeThread);
			t_write.setName("TransWriteThread");
			t_write.start();

			// Thread t_hb = new Thread(new HeartBeatThread(mContext,os));
			// t_hb.setName("HB Thread");
			// t_hb.start();

			// temp var
			byte[] buffer = new byte[8192];
			int rtvl = -1;
			int len = 0;
			String strIn;
			while (!isToStop) {
				// Read header - 2 Bytes
				rtvl = is.read(buffer, 0, 2);
				if (rtvl < 0) {
					Log.d("mango", "HB rtvl < 0");
					break;
				} else {
					Log.d("mango", "read HB > 0, rtvl = " + rtvl);
				}

				if (buffer[0] == 0x02) {
					// it's a HeartBeat msg （2 Bytes）
					Log.d("mango", "HB");
					continue;
				}
				
				rtvl = is.read(buffer, 2, 2);
				if (rtvl < 0) {
					Log.d("mango", "rtvl < 0");
					break;
				} else {
					Log.d("mango", "read over HB, rtvl = " + rtvl);
				}
				// 读取payload长度
				len = (buffer[2] & 0xff) << 8 | buffer[3] & 0xff;
				System.out.println("len = ");
				rtvl = is.read(buffer, 4, len);
				if (rtvl < 0)
					break;
				Log.v("mango", "len=" + len);
				strIn = new String(buffer, 4, len);
				Log.v("mango", "content: " + strIn);
				Bundle bd = new Bundle();
				bd.putString("MSG", strIn);
				if (mHandler != null) {
					Message msg = mHandler.obtainMessage();
					msg.what = TransClient.MSG_RD_THREAD_RECEIVED;
					msg.setData(bd);
					mHandler.sendMessage(msg);
				}
			}
			Log.v("mango", "break While().");
			// stop write Thread
			writeThread.setToStop();
		} catch (SocketTimeoutException e) {
			Log.v("mango", "TIMEOUT exception");
			if (mHandler != null)
				mHandler.sendEmptyMessage(TransClient.MSG_RD_TIMEOUT);
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
			Log.v("mango", "Socket exception");
			if (mHandler != null){
				mHandler.sendEmptyMessage(TransClient.MSG_SOCK_ERR);
			}
		} catch (UnknownHostException e) {
			if (mHandler != null){
				mHandler.sendEmptyMessage(TransClient.MSG_SOCK_UNKOWN_HOST);
			}
			e.printStackTrace();
		} catch (IOException e) {
			if (mHandler != null){
				mHandler.sendEmptyMessage(TransClient.MSG_IO_ERR);
			}
			e.printStackTrace();
		} finally {
			Log.v("mango", "in finally");
			try {
				if (socket != null)
					socket.close();
					mHandler.sendEmptyMessage(TransClient.MSG_SOCK_CLOSED);
			} catch (IOException e) {
				if (mHandler != null){
					mHandler.sendEmptyMessage(TransClient.MSG_IO_ERR);
				}
				e.printStackTrace();
			}
		}
		Log.v("mango", "Read THread stop........");
	}

	public void setToStop(boolean isToStop) {
		this.isToStop = isToStop;
	}

	public TransWriteThread getWriteThread() {
		return writeThread;
	}

	public Socket getSocket() {
		return socket;
	}

	/**
	 * 终止连接，停止读写线程。
	 */
	public void stop() {
		this.isToStop = true;
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				Log.v("mango", "Client.stop().. socket closed error.");
				e.printStackTrace();
			}
			Log.v("mango", "Client.stop().. socket closed OK.");
		}
		// stop writeThread
		if (writeThread != null)
			writeThread.setToStop();
	}

}
