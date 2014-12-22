package com.emos.trans;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

import android.os.Handler;
import android.util.Log;

public class TransWriteThread implements Runnable {

	private boolean isToStop = true;
	private Handler mHandler;
	private OutputStream os;

	Queue<MMsg> mQueue = new LinkedList<MMsg>();

	Object queueLock = new Object();

	public TransWriteThread(Handler mHandler, OutputStream os) {
		super();
		this.mHandler = mHandler;
		this.os = os;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		isToStop = false;
		MMsg msg = null;
		mHandler.sendEmptyMessage(TransClient.MSG_WR_THREAD_READY);
		while (!isToStop) {
			synchronized (queueLock) {
				msg = mQueue.poll();
				if (msg == null) {
					// wait for adding ELEMENT into Queue
					try {
						Log.v("mango", "queue waiting....");
						queueLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}
			}/* end of sync */

			/* if has Element in Queue, then send it! */
			try {
				sendMsg(this.os, msg.content, msg.msgType);
			} catch (IOException e) {
				mHandler.sendEmptyMessage(TransClient.MSG_WR_SEND_ERR);
				e.printStackTrace();
			}
		}
		Log.v("mango", "Write THread stop........");
	}

	public static void sendMsg(OutputStream os, String s, short type)
			throws IOException {

		byte[] strBytes = s.getBytes();
		int len = strBytes.length;

		byte[] head = new byte[4 + len];
		head[0] = (byte) type;
		head[1] = 0x00;
		head[2] = (byte) ((len >> 8) & 0xff);
		head[3] = (byte) (len & 0xff);

		for (int i = 0, j = 4; i < len; i++, j++) {
			head[j] = strBytes[i];
		}
		os.write(head, 0, len + 4);
		os.flush();
	}

	public void setToStop() {
		this.isToStop = true;
		synchronized (queueLock) {
			queueLock.notify();
		}
	}

	public void write(MMsg msg) {
		synchronized (queueLock) {
			mQueue.add(msg);
			queueLock.notify();
		}
	}
}
