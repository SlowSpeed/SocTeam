package com.socteam.radio102fm;

import com.socteam.R;
import com.socteam.radio102fm.Radio102fm_ServerAPI.BannerInfo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.VideoView;

public class Radio102fm_VideoActivity extends Activity
{
	private VideoView m_myView;
	private ProgressDialog m_pd;
	private Runnable m_bufferThread;
	private String m_videoUrl;
	
	private final int BANNER_SHOW_TIME = 5000;
	
	private final int BUFFER_CHECK_INTERVAL = 100;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_layout);
		
		m_videoUrl = Radio102fm_MainActivity.ServerAPI.getVideoStreamUrl();
		final ImageView bannerView = (ImageView) findViewById(R.id.bannerView);
		
		final BannerInfo bi = Radio102fm_MainActivity.ServerAPI.getBanner(false);
		bannerView.setImageBitmap(bi.getImage());
		bannerView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0)
			{
				Radio102fm_MainActivity.ServerAPI.asyncSendBannerClick(bi.getBannerId());
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(bi.getBannerLink()));
				startActivity(intent);
			}
			
		});
		
		m_myView = (VideoView) findViewById(R.id.videoView);
		
		if (m_myView != null)
		{
			final Handler handler = new Handler();
			
			m_bufferThread = new Runnable() {
				public void run()
				{
					// Remove the banner image
					// and set the image to show during the streaming video
					// also removed the on click action
					bannerView.setImageBitmap(Radio102fm_MainActivity.ServerAPI
							.getImage(getString(R.string.video_sideImage)));
					bannerView.setOnClickListener(null);
					
					// Set the buffer thread to show the buffering msg
					m_bufferThread = new Runnable() {
						private int prevPosition = 0;
						
						public void run()
						{
							if (m_bufferThread != null)
							{
								int currPosition = m_myView.getCurrentPosition();
								
								if (prevPosition == currPosition)
								{
									if (m_pd == null)
									{
										m_pd = ProgressDialog.show(Radio102fm_VideoActivity.this,
												"", "Buffering...", true, false);
									}
								}
								else
								{
									if (m_pd != null)
									{
										m_pd.dismiss();
										m_pd = null;
									}
									prevPosition = currPosition;
								}
								handler.postDelayed(m_bufferThread, BUFFER_CHECK_INTERVAL);
							}
						}
					};
					
					// Start the buffer thread
					handler.postDelayed(m_bufferThread, BUFFER_CHECK_INTERVAL);
				}
			};
			
			// Start the thread that show the buffering msg and remove the
			// banner
			handler.postDelayed(m_bufferThread, BANNER_SHOW_TIME);
			
			try
			{
				m_myView.setVideoPath(m_videoUrl);
				m_myView.start();
			}
			catch (Exception e)
			{
				Log.i("Video Exception", "Couldn't connect to server");
				
				onDestroy();
			}
		}
		
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		
		if (m_myView != null)
		{
			m_myView.stopPlayback();
			m_myView = null;
		}
		
		m_bufferThread = null;
		
		if (m_pd != null)
		{
			m_pd.dismiss();
			m_pd = null;
		}
		
	}
}
