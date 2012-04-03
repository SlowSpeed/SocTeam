package com.socteam.extend;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.Button;

public class Tab extends Button
{
	private final static int BOLD = 1;
	private final static int NORMAL = 0;
	
	public Tab(Context context)
	{
		super(context);
	}
	
	public Tab(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}
	
	public void setPressed()
	{
		setSelected(true);
		setTypeface(getTypeface(), BOLD);
	}
	
	public void setUnpressed()
	{
		setSelected(false);
		setTypeface(getTypeface(), NORMAL);
	}
	
	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
	}
}