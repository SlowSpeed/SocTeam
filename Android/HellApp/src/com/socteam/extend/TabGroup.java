package com.socteam.extend;

import java.util.HashMap;
import java.util.Map;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;

public class TabGroup
{
	private Map<Tab, View> m_tabs = new HashMap<Tab, View>();
	private FrameLayout m_tabContent;
	
	public TabGroup(FrameLayout tabContent)
	{
		m_tabContent = tabContent;
	}
	
	public void addTab(Tab newTab, View tabContent)
	{
		if (m_tabs.get(newTab) != null) return;
		
		newTab.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				Tab tab = (Tab) v;
				tabPressed(tab);
				tab.setPressed();
			}
		});
		
		m_tabs.put(newTab, tabContent);
	}
	
	public void tabPressed(Tab tab)
	{
		View content = m_tabs.get(tab);
		
		if (content != null)
		{
			m_tabContent.removeAllViews();
			m_tabContent.addView(content);
		}
		
		for (Tab t : m_tabs.keySet())
		{
			t.setUnpressed();
		}
	}
}
