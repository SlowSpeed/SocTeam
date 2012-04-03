package com.socteam.radio102fm;

import java.io.StringReader;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.socteam.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

public class Radio102fm_ServerAPI
{
	// Server API urls
	private String m_Encoding;
	private String m_bannerBigAddr;
	private String m_bannerSmallAddr;
	private String m_bannerClickedAddrPrefix;
	private String m_appStartedAddr;
	private String m_streamsAddr;
	private String m_scheduleAddr;
	private String m_currProgramAddr;
	
	// Streams url
	private String m_audioStream = null;
	private String m_videoStream = null;
	
	// Banners
	private BannerInfo m_smallBanner;
	private BannerInfo m_bigBanner;
	
	private boolean m_isConnected;
	
	private Context m_context;
	
	public Radio102fm_ServerAPI(Context c)
	{
		m_isConnected = false;
		m_context = c;
		
		m_Encoding = "Utf-8";
		
		m_appStartedAddr = String.format("%s%s%s", c.getString(R.string.api_serverAddress),
				c.getString(R.string.api_appActivation), c.getString(R.string.api_sid));
		
		m_bannerSmallAddr = String.format("%s%s%s%s%d", c.getString(R.string.api_serverAddress),
				c.getString(R.string.api_banner), c.getString(R.string.api_sid),
				c.getString(R.string.api_bannerType), 1);
		
		m_bannerBigAddr = String.format("%s%s%s%s%d", c.getString(R.string.api_serverAddress),
				c.getString(R.string.api_banner), c.getString(R.string.api_sid),
				c.getString(R.string.api_bannerType), 2);
		
		m_bannerClickedAddrPrefix = String.format("%s%s%s%s",
				c.getString(R.string.api_serverAddress), c.getString(R.string.api_bannerClicked),
				c.getString(R.string.api_sid), c.getString(R.string.api_bannerId));
		
		m_streamsAddr = String.format("%s%s%s", c.getString(R.string.api_serverAddress),
				c.getString(R.string.api_streams), c.getString(R.string.api_sid));
		
		m_scheduleAddr = String.format("%s%s%s%s", c.getString(R.string.api_serverAddress),
				c.getString(R.string.api_schedule), c.getString(R.string.api_sid),
				c.getString(R.string.api_scheduleVer));
		
		m_currProgramAddr = String.format("%s%s%s%s", c.getString(R.string.api_serverAddress),
				c.getString(R.string.api_currProgram), c.getString(R.string.api_sid),
				c.getString(R.string.api_scheduleVer));
		
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				setStreamsAddresses();
				
				RefreshBanners(true, true);
			}
		}).start();
	}
	
	/* ******************* Public Methods ************************ */
	
	/** Send 'Banner clicked' command to the server
	 * @param bannerId
	 * The id of the banner that has been clicked */
	public void asyncSendBannerClick(final int bannerId)
	{
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				String bannerClickUrl = String.format("%s%s", m_bannerClickedAddrPrefix, bannerId);
				getResponse(bannerClickUrl);
			}
		}).start();
	}
	
	/** Send 'Application activated' command to the server */
	public void asyncSendAppActivated()
	{
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				getResponse(m_appStartedAddr);
			}
		}).start();
	}
	
	/** Get a random banner information from the server
	 * @param getSmallBanner
	 * Whether to get the small banner or the big banner
	 * @return A BannerInfo class with the info from the server */
	public BannerInfo getBanner(final boolean getSmallBanner)
	{
		BannerInfo retVal = null;
		
		try
		{
			if (getSmallBanner)
			{
				if (m_smallBanner == null)
				{
					RefreshBanners(true, false);
				}
				retVal = m_smallBanner;
			}
			else
			{
				if (m_bigBanner == null)
				{
					RefreshBanners(false, true);
				}
				retVal = m_bigBanner;
			}
			
			new Thread(new Runnable() {
				@Override
				public void run()
				{
					RefreshBanners(getSmallBanner, !getSmallBanner);
				}
			}).start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return retVal;
	}
	
	/** Get the url of the radio audio stream
	 * @return Url of the radio audio stream */
	public String getRadioStreamUrl()
	{
		if (m_audioStream == null)
		{
			setStreamsAddresses();
		}
		
		return m_audioStream;
	}
	
	/** Get the url of the radio video stream
	 * @return Url of the radio video stream */
	public String getVideoStreamUrl()
	{
		if (m_videoStream == null)
		{
			setStreamsAddresses();
		}
		
		return m_videoStream;
	}
	
	/** Set the video and audio stream */
	public void setStreamsAddresses()
	{
		NodeList streamsXml = getXmlNodeList(getResponsePageString(m_streamsAddr));
		
		if (streamsXml != null)
		{
			// Get the XML node
			Node xmlNode = streamsXml.item(0);
			
			if (xmlNode != null && xmlNode.getNodeType() == Node.ELEMENT_NODE)
			{
				Element xmlElement = (Element) xmlNode;
				
				int audioIdx = 0;
				int videoIdx = 0;
				
				// Get the audio and video streams idx in the xml
				NodeList streamsType = xmlElement.getElementsByTagName("type");
				
				if (streamsType != null)
				{
					// Go over all the types of streams in the xml
					// and find which idx is for which stream
					for (int idx = 0; idx < streamsType.getLength(); idx++)
					{
						Element currElement = (Element) streamsType.item(idx);
						
						// Check if the curr idx is for the audio stream
						if (currElement.getChildNodes().item(0).getNodeValue().equals("audio"))
						{
							audioIdx = idx;
						}
						// Check if the curr idx is for the video stream
						else if (currElement.getChildNodes().item(0).getNodeValue().equals("video"))
						{
							videoIdx = idx;
						}
					}
					
					// Get the urls of the audio and video streams
					NodeList streamsUrl = xmlElement.getElementsByTagName("url");
					
					if (streamsUrl != null)
					{
						// Get audio url
						Element currStream = (Element) streamsUrl.item(audioIdx);
						m_audioStream = currStream.getChildNodes().item(0).getNodeValue();
						
						// Get video url
						currStream = (Element) streamsUrl.item(videoIdx);
						m_videoStream = currStream.getChildNodes().item(0).getNodeValue();
					}
				}
			}
		}
	}
	
	/** Get the current schedule of the radio programs
	 * @return List of days, each day has a list of programs */
	public List<List<RadioProgram>> getScheduleList()
	{
		List<List<RadioProgram>> retVal = null;
		
		NodeList scheduleXml = getXmlNodeList(getResponsePageString(m_scheduleAddr));
		
		if (scheduleXml != null)
		{
			// Get the XML node
			Node xmlNode = scheduleXml.item(0);
			
			if (xmlNode != null && xmlNode.getNodeType() == Node.ELEMENT_NODE)
			{
				Element xmlElement = (Element) xmlNode;
				
				// Get the node list of the current week
				NodeList daysNode = xmlElement.getElementsByTagName("array");
				
				if (daysNode != null)
				{
					retVal = new LinkedList<List<RadioProgram>>();
					
					for (int idx = 0; idx < daysNode.getLength(); idx++)
					{
						List<RadioProgram> oneDayList = parseXmlToRadioProgramList((Element) daysNode
								.item(idx));
						
						if (oneDayList != null)
						{
							retVal.add(oneDayList);
						}
					}
				}
			}
		}
		
		return retVal;
	}
	
	/** Get the current radio program from the server
	 * @return The current radio program without the start and end time */
	public RadioProgram getCurrentProgram()
	{
		RadioProgram retVal = null;
		
		NodeList pageNodes = getXmlNodeList(getResponsePageString(m_currProgramAddr));
		
		if (pageNodes != null)
		{
			// Get the XML node
			Node xmlNode = pageNodes.item(0);
			
			if (xmlNode != null && xmlNode.getNodeType() == Node.ELEMENT_NODE)
			{
				String name = "Unknown";
				String broadcaster = "Unknown";
				
				Element xmlElement = (Element) xmlNode;
				
				// Get the node of the current program name
				NodeList programNode = xmlElement.getElementsByTagName("program_title");
				
				if (programNode != null)
				{
					name = programNode.item(0).getChildNodes().item(0).getNodeValue();
				}
				
				// Get the node of the current broadcaster
				NodeList broadcasterNode = xmlElement.getElementsByTagName("broadcaster");
				
				if (broadcasterNode != null)
				{
					broadcaster = broadcasterNode.item(0).getChildNodes().item(0).getNodeValue();
				}
				
				retVal = new RadioProgram(name, broadcaster);
			}
			
		}
		
		return retVal;
	}
	
	/** Whether the server is available
	 * @return True if the server is available, false otherwise */
	public boolean isConnected()
	{
		return m_isConnected;
	}
	
	/** Get image from a url
	 * @param imageUrl
	 * The url of the image to get
	 * @return A bitmap image */
	public Bitmap getImage(String imageUrl)
	{
		Bitmap retVal = null;
		
		try
		{
			// Get the image web page
			HttpResponse imageContent = getResponse(imageUrl);
			
			// If the image exists
			if (imageContent != null)
			{
				// Create a bitmap image from the web page
				retVal = BitmapFactory.decodeStream(imageContent.getEntity().getContent());
			}
			else
			{
				// TODO remove!!!
				Toast.makeText(m_context, "Can't get banner image", Toast.LENGTH_LONG);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return retVal;
	}
	
	/* ******************* Private Methods ************************ */
	
	/** Refreshing both of the banners' info */
	private void RefreshBanners(boolean refreshSmall, boolean refreshBig)
	{
		try
		{
			if (refreshSmall)
			{
				m_smallBanner = getBannerInfo(m_bannerSmallAddr);
			}
			
			if (refreshBig)
			{
				m_bigBanner = getBannerInfo(m_bannerBigAddr);
			}
		}
		catch (Exception e)
		{
			Log.e("Fatching banners", "Couldn't fatch banners");
			e.printStackTrace();
		}
	}
	
	/** Get a random banner info from the server
	 * @param url
	 * The url of the server and banner type (small/big)
	 * @return A BannerInfo class with the info from the server
	 * @throws URISyntaxException */
	private BannerInfo getBannerInfo(String url) throws URISyntaxException
	{
		BannerInfo retVal = null;
		
		String xmlString = getResponsePageString(url);
		NodeList bannerXml = getXmlNodeList(xmlString);
		
		if (bannerXml != null)
		{
			int bannerId;
			String imageUrl;
			String bannerLink;
			
			// 'banner' node
			try
			{
				Element bannerElement = (Element) bannerXml.item(0);
				
				// Get the id value
				Element currElement = (Element) bannerElement.getElementsByTagName("id").item(0);
				bannerId = Integer.parseInt(currElement.getChildNodes().item(0).getNodeValue());
				
				// Get the image url value
				currElement = (Element) bannerElement.getElementsByTagName("link").item(0);
				bannerLink = currElement.getChildNodes().item(0).getNodeValue();
				
				// Get the banner link value
				currElement = (Element) bannerElement.getElementsByTagName("image").item(0);
				imageUrl = currElement.getChildNodes().item(0).getNodeValue();
				
				retVal = new BannerInfo(bannerLink, getImage(imageUrl), bannerId);
			}
			catch (Exception e)
			{
				Log.e("getBannerInfo", "xmlMaleFormed: " + xmlString);
			}
		}
		
		return retVal;
	}
	
	/** Get a response from the url sent
	 * @param url
	 * The url to get response from
	 * @return A HTML response that holds the returned web page */
	private HttpResponse getResponse(String url)
	{
		HttpResponse retVal = null;
		
		// Check if there's a connection to the Internet
		if (((Radio102fm_MainActivity) m_context).isNetworkAvailable())
		{
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url);
			
			try
			{
				retVal = httpClient.execute(httpGet);
				m_isConnected = true;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				m_isConnected = false;
				Toast.makeText(m_context, m_context.getString(R.string.no_internet_connection_msg),
						Toast.LENGTH_LONG);
			}
		}
		else
		{
			Toast.makeText(m_context, m_context.getString(R.string.no_internet_connection_msg),
					Toast.LENGTH_LONG);
			m_isConnected = false;
		}
		
		return retVal;
	}
	
	/** Get the web page as a string
	 * @param url
	 * The url of the web page to get
	 * @return A string of the web page, or null if failed */
	private String getResponsePageString(String url)
	{
		String retVal = null;
		
		try
		{
			HttpResponse httpResponse = getResponse(url);
			
			if (httpResponse != null)
			{
				HttpEntity httpEntity = httpResponse.getEntity();
				retVal = EntityUtils.toString(httpEntity, m_Encoding);
			}
		}
		catch (Exception e)
		{
			// retVal is null
			e.printStackTrace();
		}
		
		return retVal;
		
	}
	
	/** Parse the XML string to a node list
	 * @param xmlString
	 * The XML string to parse
	 * @return A node list of the XML, or null if failed */
	private NodeList getXmlNodeList(String xmlString)
	{
		NodeList retVal = null;
		
		if (xmlString != null)
		{
			// Remove any extra XML declaration in the page
			final String xmlDeclaration = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
			final String xmlDeclaration2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			
			if (xmlString.lastIndexOf(xmlDeclaration) > 0)
			{
				xmlString = xmlString.replace(xmlDeclaration, "");
			}
			
			if (xmlString.lastIndexOf(xmlDeclaration2) > 0)
			{
				xmlString = xmlString.replace(xmlDeclaration2, "");
			}
			
			if (xmlString.contains(xmlDeclaration) == false
					&& xmlString.contains(xmlDeclaration2) == false)
			{
				xmlString = xmlDeclaration + xmlString;
			}
			
			try
			{
				// Parse the XML file to Node list
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				
				Document doc = db.parse(new InputSource(new StringReader(xmlString)));
				doc.getDocumentElement().normalize();
				
				retVal = doc.getChildNodes();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				Log.e("getXmlNodeList", "xmlString: " + xmlString);
			}
		}
		
		return retVal;
	}
	
	/** Parse the XML node of one day of the schedule to a list of radio programs
	 * @param xmlNode
	 * The XML node of the 'array' node
	 * @return List of radio programs */
	private List<RadioProgram> parseXmlToRadioProgramList(Element xmlNode)
	{
		List<RadioProgram> retVal = null;
		
		if (xmlNode != null && xmlNode.getChildNodes().getLength() > 0)
		{
			NodeList programs = xmlNode.getElementsByTagName("dict");
			
			if (programs != null)
			{
				retVal = new LinkedList<RadioProgram>();
				
				for (int idx = 0; idx < programs.getLength(); idx++)
				{
					RadioProgram program = parseXmlToRadioProgram((Element) programs.item(idx));
					
					if (program != null)
					{
						retVal.add(program);
					}
				}
			}
		}
		
		return retVal;
	}
	
	/** Parse the XML node of a radio program
	 * @param xmlNode
	 * The XML node of 'dictionary'
	 * @return A RadioProgram object */
	private RadioProgram parseXmlToRadioProgram(Element xmlNode)
	{
		RadioProgram retVal = null;
		
		if (xmlNode != null)
		{
			int nameIdx = 0;
			int broadcasterIdx = 0;
			int timeIdx = 0;
			
			// Because that the one that built the server is stupid we have to
			// do a very weird thing right now so...don't worry it works ;)
			
			NodeList keys = xmlNode.getElementsByTagName("key");
			
			if (keys != null)
			{
				// Get the index of each value in the XML
				for (int idx = 0; idx < keys.getLength(); idx++)
				{
					if (keys.item(idx).getNodeType() == Node.ELEMENT_NODE)
					{
						Element currElement = (Element) keys.item(idx);
						
						if (currElement.getChildNodes().item(0).getNodeValue().equals("Time"))
						{
							timeIdx = idx;
						}
						else if (currElement.getChildNodes().item(0).getNodeValue()
								.equals("Broadcaster"))
						{
							broadcasterIdx = idx;
						}
						else if (currElement.getChildNodes().item(0).getNodeValue()
								.equals("Program"))
						{
							nameIdx = idx;
						}
					}
				}
				
				// Check if the index of each value has been fetched
				if (timeIdx != broadcasterIdx && timeIdx != nameIdx)
				{
					NodeList values = xmlNode.getElementsByTagName("string");
					
					if (values != null)
					{
						String name;
						String broadcaster;
						String time;
						
						Element currValue = (Element) values.item(nameIdx);
						name = currValue.getChildNodes().item(0).getNodeValue();
						
						currValue = (Element) values.item(broadcasterIdx);
						broadcaster = currValue.getChildNodes().item(0).getNodeValue();
						
						currValue = (Element) values.item(timeIdx);
						time = currValue.getChildNodes().item(0).getNodeValue();
						
						retVal = new RadioProgram(name, broadcaster, time);
					}
				}
			}
		}
		
		return retVal;
	}
	
	public final class BannerInfo
	{
		private Bitmap m_image;
		private String m_bannerLink;
		private int m_bannerId;
		
		public BannerInfo(String bannerLink, Bitmap image, int bannerId)
		{
			setImage(image);
			setBannerLink(bannerLink);
			setBannerId(bannerId);
		}
		
		public String getBannerLink()
		{
			return m_bannerLink;
		}
		
		private void setBannerLink(String bannerLink)
		{
			this.m_bannerLink = bannerLink;
		}
		
		public int getBannerId()
		{
			return m_bannerId;
		}
		
		private void setBannerId(int bannerId)
		{
			this.m_bannerId = bannerId;
		}
		
		public Bitmap getImage()
		{
			return m_image;
		}
		
		private void setImage(Bitmap image)
		{
			this.m_image = image;
		}
		
	}
	
	public final class RadioProgram
	{
		private String m_name;
		private String m_broadcaster;
		private String m_timeString;
		
		/** Create a radio program object
		 * @param name
		 * Name of the radio program
		 * @param broadcaster
		 * Name of the broadcaster
		 * @param startTime
		 * Start time of the program in a hh:mm format
		 * @param endTime
		 * End time of the program in a hh:mm format
		 * @throws ParseException
		 * If couldn't parse the start and end time throws exception */
		private RadioProgram(String name, String broadcaster, String timeString)
		{
			this(name, broadcaster);
			
			setTime(timeString);
		}
		
		private RadioProgram(String name, String broadcaster)
		{
			setName(name);
			setBroadcaster(broadcaster);
		}
		
		public String getName()
		{
			return m_name;
		}
		
		private void setName(String name)
		{
			this.m_name = name;
		}
		
		public String getTimeString()
		{
			return m_timeString;
		}
		
		private void setTime(String timeString)
		{
			m_timeString = timeString;
		}
		
		public String getBroadcaster()
		{
			return m_broadcaster;
		}
		
		private void setBroadcaster(String broadcaster)
		{
			m_broadcaster = broadcaster;
		}
		
	}
}
