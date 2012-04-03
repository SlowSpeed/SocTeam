package com.socteam.extend;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class VerticalSeekBar extends SeekBar
{
	//TODO do it better...
	private int x = 3;
	
	public VerticalSeekBar(Context context)
	{
		super(context);
		setThumbOffset(x);
	}
	
	public VerticalSeekBar(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setThumbOffset(x);
	}
	
	public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		setThumbOffset(x);
	}
	
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(h, w, oldh, oldw);
	}
	
	@Override
	protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}
	
	protected void onDraw(Canvas c)
	{
		float progressBarWidth = (float) (getWidth() * 0.45);
		float progressBarHeight = (float) (getHeight() - 15);
		float spacing = (float) ((getWidth() - progressBarWidth) / 1.7);
		
		Bitmap b = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
		Shape progressBar = ((ShapeDrawable) getProgressDrawable()).getShape();
		progressBar.resize(progressBarWidth, progressBarHeight);
		Canvas c1 = new Canvas(b);
		progressBar.draw(c1, new Paint());
		
		RectF bounds = new RectF(spacing, 0, spacing + progressBarWidth, progressBarHeight);
		c.drawBitmap(b, null, bounds, null);
		
		c.rotate(-90);
		c.translate(-getHeight(), 0);
		
		Drawable progBar = getProgressDrawable();
		setProgressDrawable(null);
		super.onDraw(c);
		setProgressDrawable(progBar);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if (!isEnabled()) { return false; }
		
		switch (event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
			case MotionEvent.ACTION_UP:
				setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
				onSizeChanged(getWidth(), getHeight(), 0, 0);
				break;
			
			case MotionEvent.ACTION_CANCEL:
				break;
		}
		return true;
	}
}