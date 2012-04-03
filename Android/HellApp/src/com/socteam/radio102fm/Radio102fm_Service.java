package com.socteam.radio102fm;

import com.socteam.R;
import com.spoledge.aacdecoder.AACPlayer;
import com.spoledge.aacdecoder.PlayerCallback;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class Radio102fm_Service extends Service
{
	private NotificationManager m_NotifManager;
	private String m_lastUri;
	private Radio102fm_PlayerCallBack m_callback;
	private AACPlayer m_player;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
		m_NotifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		m_callback = new Radio102fm_PlayerCallBack();
		m_player = new AACPlayer(m_callback);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				int ic_notif = R.drawable.logo_small;
				CharSequence notif_text = getText(R.string.notif_ticker_text);
				long when = System.currentTimeMillis();
				
				Notification notif = new Notification(ic_notif, notif_text, when);
				
				Context context = getApplicationContext();
				CharSequence contentTitle = notif_text;
				CharSequence contentText = getText(R.string.notif_subtext);
				Intent notificationIntent = new Intent(Radio102fm_Service.this, Radio102fm_MainActivity.class);
				PendingIntent contentIntent = PendingIntent.getActivity(Radio102fm_Service.this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

				notif.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
				
				startForeground(R.string.notif_ticker_text, notif);
			}
		}).start();
		
		m_lastUri = intent.getDataString();
		m_player.playAsync(m_lastUri);
		
		return START_REDELIVER_INTENT;
	}
	
	@Override
	public void onDestroy()
	{
		m_player.stop();
		m_NotifManager.cancel(R.string.notif_ticker_text);
		
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
	
	private class Radio102fm_PlayerCallBack implements PlayerCallback
	{
		@Override
		public void playerException(Throwable e)
		{
			m_player.stop();
			m_player = new AACPlayer(m_callback);
			m_player.playAsync(m_lastUri);
		}
		
		@Override
		public void playerPCMFeedBuffer(boolean arg0, int arg1, int arg2)
		{
		}
		
		@Override
		public void playerStarted()
		{
		}
		
		@Override
		public void playerStopped(int arg0)
		{
		}
	}
}