package com.socteam.radio102fm.tabscontents;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.socteam.R;
import com.socteam.extend.VerticalSeekBar;
import com.socteam.radio102fm.Radio102fm_Activity;
import com.socteam.radio102fm.Radio102fm_Service;
import com.socteam.radio102fm.Radio102fm_VideoActivity;
import com.socteam.radio102fm.Radio102fm_ServerAPI.RadioProgram;

public final class TabContent_NowPlaying
{
	private static Intent s_radioServiceIntent;
	private static Intent s_videoActivityIntent;
	private static int s_nowPlayingTextRefreshTime = (60 * 60 * 1000);// 1 min
	
	public static View getNowPlayingTabContent(final Radio102fm_Activity activity)
	{
		View nowPlayingTabContent = activity.getLayoutInflater().inflate(
				R.layout.tab_content_now_playing, null);
		
		handlePowerAndAirButton(activity, nowPlayingTabContent);
		handleInfoButton(activity, nowPlayingTabContent);
		handleVideoAndArrowAndTvButton(activity, nowPlayingTabContent);
		handleVolumeBar(activity, nowPlayingTabContent);
		handleNowPlayingText(activity, nowPlayingTabContent);
		
		return nowPlayingTabContent;
	}
	
	private static void handlePowerAndAirButton(final Radio102fm_Activity activity,
			final View nowPlayingTabContent)
	{
		// gets the power button from the layout
		final View btnPower = nowPlayingTabContent.findViewById(R.id.power);
		// gets the air icon from the layout
		final View air = nowPlayingTabContent.findViewById(R.id.air);
		// sets the button to present pressed if the radio is already playing
		btnPower.setSelected(activity.isRadioPlaying());
		/* sets the button click to:
		 * - if radio is playing: stop it
		 * - else if no internet connection: present a dialog demanding internet
		 * - else start the radio */
		btnPower.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				if (activity.isRadioPlaying())
				{
					btnPower.setSelected(false);
					activity.findViewById(R.id.mainLayout).setKeepScreenOn(false);
					activity.stopService(getRadioServiceIntent(activity));
					air.setSelected(false);
				}
				else
				{
					if (!Radio102fm_Activity.ServerAPI.isConnected())
					{
						AlertDialog.Builder builder = new Builder(activity);
						builder.setMessage(R.string.no_internet_connection_msg);
						builder.setCancelable(false);
						builder.setPositiveButton(R.string.OK, null);
						builder.show();
					}
					else
					{
						btnPower.setSelected(true);
						activity.findViewById(R.id.mainLayout).setKeepScreenOn(true);
						activity.startService(getRadioServiceIntent(activity));
						air.setSelected(true);
					}
				}
			}
		});
	}
	
	private static void handleInfoButton(final Radio102fm_Activity activity,
			final View nowPlayingTabContent)
	{
		View btnInfo = nowPlayingTabContent.findViewById(R.id.mainInfo);
		
		btnInfo.setOnClickListener(new OnClickListener() {
			private View m_terms;
			private FrameLayout m_frame;
			
			@Override
			public void onClick(View v)
			{
				if (m_terms == null)
				{
					m_terms = activity.getLayoutInflater().inflate(R.layout.terms_layout, null);
					WebView termsWeb = ((WebView) m_terms.findViewById(R.id.termsWeb));
					termsWeb.setWebViewClient(new WebViewClient());
					termsWeb.loadUrl(activity.getString(R.string.terms_url));
				}
				if (m_frame == null) m_frame = (FrameLayout) activity.findViewById(R.id.tabContent);
				
				m_frame.removeAllViews();
				m_frame.addView(m_terms);
			}
		});
	}
	
	private static void handleVideoAndArrowAndTvButton(final Radio102fm_Activity activity,
			final View nowPlayingTabContent)
	{
		// gets the video button from the layout
		final View btnVideo = nowPlayingTabContent.findViewById(R.id.btnMain);
		/* sets the click action of the video button and all the views on it:
		 * - if radio playing - stops it
		 * - starts the video activity */
		OnClickListener viewVideo = new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				btnVideo.setSelected(true);
				
				if (activity.isRadioPlaying()) nowPlayingTabContent.findViewById(R.id.power)
						.performClick();
				
				activity.startActivity(getVideoActivityIntent(activity));
				btnVideo.setSelected(false);
			}
		};
		btnVideo.setOnClickListener(viewVideo);
		nowPlayingTabContent.findViewById(R.id.btnMain_leftArrow).setOnClickListener(viewVideo);
		nowPlayingTabContent.findViewById(R.id.btnMain_tvIcon).setOnClickListener(viewVideo);
	}
	
	private static void handleVolumeBar(final Radio102fm_Activity activity,
			final View nowPlayingTabContent)
	{
		VerticalSeekBar volumeBar = (VerticalSeekBar) nowPlayingTabContent
				.findViewById(R.id.volume_slider);
		setSeekBarByVolume(activity, volumeBar);
		volumeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				setVolumeBySeekBar(activity, seekBar);
			}
		});
	}
	
	private static void handleNowPlayingText(final Radio102fm_Activity activity,
			final View nowPlayingTabContent)
	{
		activity.UIHandler.post(new Runnable() {
			@Override
			public void run()
			{
				if (Radio102fm_Activity.ServerAPI != null)
				{
					RadioProgram prog = Radio102fm_Activity.ServerAPI.getCurrentProgram();
					((TextView) nowPlayingTabContent.findViewById(R.id.nowPlaying_programName))
							.setText(prog.getName());
					((TextView) nowPlayingTabContent.findViewById(R.id.nowPlaying_programWith))
							.setText(prog.getBroadcaster());
					
					activity.UIHandler.postDelayed(this, s_nowPlayingTextRefreshTime);
				}
			}
		});
	}
	
	/* if an existing intent exists: return it
	 * else create a new one and return it */
	public static Intent getRadioServiceIntent(final Radio102fm_Activity activity)
	{
		if (s_radioServiceIntent != null) return s_radioServiceIntent;
		
		return s_radioServiceIntent = new Intent("", Uri.parse(Radio102fm_Activity.ServerAPI
				.getRadioStreamUrl()), activity, Radio102fm_Service.class);
	}
	
	/* if an existing intent exists: return it
	 * else create a new one and return it */
	private static Intent getVideoActivityIntent(final Radio102fm_Activity activity)
	{
		if (s_videoActivityIntent != null) return s_videoActivityIntent;
		
		return s_videoActivityIntent = new Intent(activity, Radio102fm_VideoActivity.class);
	}
	
	/* sets the audio volume to that of the SeekBar */
	private static void setVolumeBySeekBar(Radio102fm_Activity activity, SeekBar seekBar)
	{
		// gets the audio service
		AudioManager audio = (AudioManager) activity
				.getSystemService(Radio102fm_Activity.AUDIO_SERVICE);
		
		// gets the max volume
		int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		// gets the percentage indicating the relative location of the bar in the seek bar
		double percentage = (double) seekBar.getProgress() / seekBar.getMax();
		// sets the audio volume to the percentage equal to the seekBar one
		audio.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (maxVolume * percentage), 0);
	}
	
	/* sets the audio volume to that of the SeekBar */
	private static void setSeekBarByVolume(Radio102fm_Activity activity, SeekBar seekBar)
	{
		// gets the audio service
		AudioManager audio = (AudioManager) activity
				.getSystemService(Radio102fm_Activity.AUDIO_SERVICE);
		
		// gets the max volume
		int maxVolume = seekBar.getMax();
		// gets the percentage indicating the relative location of the bar in the seek bar
		double percentage = (double) audio.getStreamVolume(AudioManager.STREAM_MUSIC)
				/ audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		// sets the audio volume to the percentage equal to the seekBar one
		seekBar.setProgress((int) (maxVolume * percentage));
	}
}
