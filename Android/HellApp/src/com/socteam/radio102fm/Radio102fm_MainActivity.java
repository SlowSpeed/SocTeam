package com.socteam.radio102fm;

import java.util.List;

import com.socteam.R;
import com.socteam.extend.Tab;
import com.socteam.extend.TabGroup;
import com.socteam.radio102fm.Radio102fm_ServerAPI.RadioProgram;
import com.socteam.radio102fm.tabscontents.TabContent_NowPlaying;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class Radio102fm_MainActivity extends Activity
{
	public static Radio102fm_ServerAPI ServerAPI;
	public Handler UIHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		ServerAPI = new Radio102fm_ServerAPI(this);
		UIHandler = new Handler();
		
		findViewById(R.id.mainLayout).setKeepScreenOn(isRadioPlaying());
		InitTabMenu();
		showBanner();
	}
	
	/** Check whether there's a connection to the Internet
	 * @return True if there's a connection to the Internet, false otherwise */
	public boolean isNetworkAvailable()
	{
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}
	
	@Override
	protected void onResume()
	{
		if (ServerAPI == null) ServerAPI = new Radio102fm_ServerAPI(this);
		
		super.onResume();
	}
	
	@Override
	public void onBackPressed()
	{
		if (isRadioPlaying())
		{
			AlertDialog.Builder alertBox = new AlertDialog.Builder(this);
			
			alertBox.setMessage(R.string.SetInBackgroundTextString);
			
			alertBox.setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Radio102fm_MainActivity.super.onBackPressed();
				}
			});
			
			alertBox.setNegativeButton(R.string.NO, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					stopService(TabContent_NowPlaying
							.getRadioServiceIntent(Radio102fm_MainActivity.this));
					
					Radio102fm_MainActivity.super.onBackPressed();
				}
			});
			
			alertBox.show();
		}
		else
		{
			super.onBackPressed();
		}
		ServerAPI = null;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode != KeyEvent.KEYCODE_MENU) return super.onKeyDown(keyCode, event);
		
		AlertDialog.Builder alertBox = new AlertDialog.Builder(this);
		alertBox.setTitle(R.string.about_url);
		
		WebView webTabBrowser = new WebView(this);
		webTabBrowser.setWebViewClient(new WebViewClient());
		webTabBrowser.loadUrl(getString(R.string.about_url));
		
		alertBox.setView(webTabBrowser);
		alertBox.show();
		return true;
	}
	
	private void showBanner()
	{
		final Radio102fm_ServerAPI.BannerInfo bannerInfo = ServerAPI.getBanner(true);
		if (bannerInfo != null)
		{
			ImageView bannerView = (ImageView) Radio102fm_MainActivity.this
					.findViewById(R.id.mainBanner);
			bannerView.setImageBitmap(bannerInfo.getImage());
			bannerView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v)
				{
					ServerAPI.asyncSendBannerClick(bannerInfo.getBannerId());
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(bannerInfo
							.getBannerLink()));
					startActivity(intent);
				}
			});
		}
	}
	
	public boolean isRadioPlaying()
	{
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
		{
			if (Radio102fm_Service.class.getName().equals(service.service.getClassName())) return true;
		}
		return false;
	}
	
	private void InitTabMenu()
	{
		final TabGroup tabGroup = new TabGroup((FrameLayout) findViewById(R.id.tabContent));
		
		View nowPlayingTabContent = TabContent_NowPlaying.getNowPlayingTabContent(this);
		
		View programTabContent = getProgramsTabContent();
		
		View youtubeTabContent = getWebTabContent("http://www.youtube.com/user/radio102fm?feature=watch");
		
		View facebookTabContent = getWebTabContent("http://he-il.facebook.com/102fm");
		
		View ContactTabContent = getContactTabContent();
		
		tabGroup.addTab((Tab) findViewById(R.id.tabRadio), nowPlayingTabContent);
		tabGroup.addTab((Tab) findViewById(R.id.tabProgram), programTabContent);
		tabGroup.addTab((Tab) findViewById(R.id.tabYoutube), youtubeTabContent);
		tabGroup.addTab((Tab) findViewById(R.id.tabFacebook), facebookTabContent);// //
		tabGroup.addTab((Tab) findViewById(R.id.tabContactUs), ContactTabContent);
		
		Tab defaultTab = ((Tab) findViewById(R.id.tabRadio));
		tabGroup.tabPressed(defaultTab);
		defaultTab.setPressed();
	}
	
	public View getWebTabContent(String url)
	{
		View webTabContent = getLayoutInflater().inflate(R.layout.web_tab_content, null);
		final WebView webTabBrowser = (WebView) webTabContent.findViewById(R.id.webViewBrowser);
		webTabBrowser.getSettings().setJavaScriptEnabled(true);
		webTabBrowser.setWebViewClient(new WebViewClient());
		webTabBrowser.loadUrl(url);
		View webTabBtn = webTabContent.findViewById(R.id.webViewMenu_back);
		webTabBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				Toast.makeText(Radio102fm_MainActivity.this, "inBackClick", Toast.LENGTH_SHORT)
						.show();
				if (webTabBrowser.canGoBack())
				{
					webTabBrowser.goBack();
					Toast.makeText(Radio102fm_MainActivity.this, "inBackClick_wentBack",
							Toast.LENGTH_SHORT).show();
				}
			}
		});
		webTabBtn = webTabContent.findViewById(R.id.webViewMenu_forward);
		webTabBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				Toast.makeText(Radio102fm_MainActivity.this, "inForwClick", Toast.LENGTH_SHORT)
						.show();
				if (webTabBrowser.canGoForward())
				{
					Toast.makeText(Radio102fm_MainActivity.this, "inForwClick_wentForw",
							Toast.LENGTH_SHORT).show();
					webTabBrowser.goForward();
				}
			}
		});
		webTabBtn = webTabContent.findViewById(R.id.webViewMenu_refresh);
		webTabBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				Toast.makeText(Radio102fm_MainActivity.this, "inReloadClick", Toast.LENGTH_SHORT)
						.show();
				webTabBrowser.reload();
			}
		});
		webTabBtn = webTabContent.findViewById(R.id.webViewMenu_cancel);
		webTabBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				Toast.makeText(Radio102fm_MainActivity.this, "inStopClick", Toast.LENGTH_SHORT)
						.show();
				webTabBrowser.stopLoading();
			}
		});
		return webTabContent;
	}
	
	private View getContactTabContent()
	{
		View contactTabContent = getLayoutInflater().inflate(R.layout.contact_tab_content, null);
		
		OnClickListener sendMailClick = new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				/* Create the Intent */
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				
				/* Fill it with Data */
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
						new String[] { "102fm@mediacast.co.il" });
				emailIntent.putExtra(android.content.Intent.EXTRA_CC,
						new String[] { "telaviv102fm@gmail.com" });
				
				/* Send it off to the Activity-Chooser */
				startActivity(emailIntent);
			}
		};
		contactTabContent.findViewById(R.id.btnMail).setOnClickListener(sendMailClick);
		contactTabContent.findViewById(R.id.btnMail_leftArrow).setOnClickListener(sendMailClick);
		contactTabContent.findViewById(R.id.btnMail_mailIcon).setOnClickListener(sendMailClick);
		
		OnClickListener phoneClick = new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse(getString(R.string.phone_intent)));
				startActivity(callIntent);
			}
		};
		contactTabContent.findViewById(R.id.btnPhone).setOnClickListener(phoneClick);
		contactTabContent.findViewById(R.id.btnPhone_leftArrow).setOnClickListener(phoneClick);
		contactTabContent.findViewById(R.id.btnPhone_phoneIcon).setOnClickListener(phoneClick);
		
		return contactTabContent;
	}
	
	private View getProgramsTabContent()
	{
		final View retVal = getLayoutInflater().inflate(R.layout.playlist_tab_content, null);
		
		// Initialize the playlist
		// doing it in a thread so the app wouldn't show a black screen for a
		// long time
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				List<List<RadioProgram>> days = ServerAPI.getScheduleList();
				
				if (days != null)
				{
					final TabGroup week = new TabGroup(
							(FrameLayout) retVal.findViewById(R.id.playlist_programs_layout));
					
					Tab dayTab;
					
					int dayNumber = 0;
					for (List<RadioProgram> weekDay : days)
					{
						switch (dayNumber++)
						{
							case 1:
								dayTab = (Tab) retVal.findViewById(R.id.playlist_day1);
								break;
							case 2:
								dayTab = (Tab) retVal.findViewById(R.id.playlist_day2);
								break;
							case 3:
								dayTab = (Tab) retVal.findViewById(R.id.playlist_day3);
								break;
							case 4:
								dayTab = (Tab) retVal.findViewById(R.id.playlist_day4);
								break;
							case 5:
								dayTab = (Tab) retVal.findViewById(R.id.playlist_day5);
								break;
							case 6:
								dayTab = (Tab) retVal.findViewById(R.id.playlist_day6);
								break;
							case 7:
								dayTab = (Tab) retVal.findViewById(R.id.playlist_day7);
								break;
							default:
								dayTab = null;
						}
						
						if (dayTab != null)
						{
							
							ListView programs = (ListView) getLayoutInflater().inflate(
									R.layout.playlist_list, null);
							
							RadioProgram[] itemList = new RadioProgram[weekDay.size()];
							PlaylistAdapter adapter = new PlaylistAdapter(
									Radio102fm_MainActivity.this, R.layout.playlist_row, itemList);
							
							int itemListIdx = 0;
							
							for (RadioProgram weekDayProgram : weekDay)
							{
								if (weekDayProgram != null)
								{
									itemList[itemListIdx] = weekDayProgram;
									itemListIdx++;
								}
							}
							
							programs.setAdapter(adapter);
							
							week.addTab(dayTab, programs);
						}
					}
					
					UIHandler.post(new Runnable()
					{
						@Override
						public void run()
						{
							Tab defaultTab = ((Tab) retVal.findViewById(R.id.playlist_day1));
							week.tabPressed(defaultTab);
							defaultTab.setPressed();
						}
					});
				}
			}
		}).start();
		
		return retVal;
	}
}