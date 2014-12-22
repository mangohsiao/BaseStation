package com.emos.trans;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class TransClient {
	
	/* Handler MSG definition */
	
	/**
	 * 网络读取 TIME OUT
	 */
	public static final int MSG_RD_TIMEOUT = 414;
	
	/**
	 * 网络域名解析出错
	 */
	public static final int MSG_SOCK_UNKOWN_HOST = 415;

	/**
	 * 网络Socket 出错
	 */
	public static final int MSG_SOCK_ERR = 416;

	/**
	 * 网络Socket写入 ERR
	 */
	public static final int MSG_WR_SEND_ERR = 417;

	/**
	 * 网络 IO 错误
	 */
	public static final int MSG_IO_ERR = 418;

	/**
	 * 心跳发送错误
	 */
	public static final int MSG_HB_SEND_ERR = 419;
	
	public static final int MSG_SOCK_CLOSED = 420;
	
	public static final int MSG_WR_THREAD_READY = 201;

	public static final int MSG_RD_THREAD_RECEIVED = 200;
	
	private Handler mHandler;
	private Context mContext;

	public static boolean isToStop = true;
	public static String ip = "125.216.243.250";
	
	/**
	 * 读取线程 Runnable
	 */
	private TransReadThread readThread;
	
	public TransClient(Context ctx, Handler mHandler) {
		super();
		this.mContext = ctx;
		this.mHandler = mHandler;
	}
	
	/**
	 * 
	 */
	public void start() {
		//启动READ线程，连接Server
		readThread = new TransReadThread(mContext, mHandler);
		Thread t_rd = new Thread(readThread);
		t_rd.setName("TransReadThread");
		t_rd.start();
		//启动写线程，准备写入，发消息通过handler
	}
	
	public void stop(){
		isToStop = true;
		//stop read thread and write Thread.
		readThread.stop();
		//stop HB alarm
		
	}
	
	/*
	 * register USER && PSWD && UUID
	 */
	public void login(){
		
	}
	
	/**/
	public void sendHeartBeat() throws IOException{
		OutputStream os = readThread.getOs();
		if(os == null){
			Log.e("mango", "OutputStream == null.");
			return;
		}
		os.write(headHB, 0, 2);
		os.flush();
		Log.v("mango", "flush HB.");
	}
	private static byte[] headHB = new byte[2];
	static{
		headHB[0] = 0x01;
		headHB[1] = 0x00;
	}
	/*
	 * 
	 */
	public boolean sendMessage(MMsg msg){
		TransWriteThread writer = readThread.getWriteThread();
		if(writer == null){
			return false;
		}else{
			writer.write(msg);
			return true;
		}
	}
}
