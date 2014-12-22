package com.emos.trans;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * HeartBeatBroadcastReceiver, to receive the HeartBeat Alarm Broadcast.
 * when received a HeartBeat broadcast, send intent to TransService
 * to doing HeartBeat.
 * 
 * @author xiao
 *
 */
public class HeartBeatBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.v("mango", "BROADCAST !");
		Intent it = new Intent(context, TransService.class);
		it.putExtra("CODE", TransService.ACT_CODE_SEND_HB);
		context.startService(it);
	}

}
