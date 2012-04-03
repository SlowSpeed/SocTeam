package com.socteam.radio102fm;

import com.socteam.R;
import com.socteam.radio102fm.Radio102fm_ServerAPI.RadioProgram;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PlaylistAdapter extends ArrayAdapter<RadioProgram>
{

	private Context context;
	private int layoutResourceId;
	private RadioProgram data[] = null;

	public PlaylistAdapter(Context context, int layoutResourceId, RadioProgram[] data)
	{
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View row = convertView;
		ProgramHolder holder = null;

		if (row == null)
		{
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
 
			holder = new ProgramHolder();
			holder.programTime = (TextView) row.findViewById(R.id.playlist_row_time);
			holder.programName = (TextView) row.findViewById(R.id.playlist_row_name);
			holder.broadcaster = (TextView) row.findViewById(R.id.playlist_row_broadcaster);

			row.setTag(holder);
		}
		else
		{
			holder = (ProgramHolder) row.getTag();
		}

		RadioProgram program = data[position];
		holder.programTime.setText(program.getTimeString());
		holder.programName.setText(program.getName());
		holder.broadcaster.setText(context.getString(R.string.WITH) + ": " + program.getBroadcaster());

		return row;
	}

	static class ProgramHolder
	{
		TextView programTime;
		TextView programName;
		TextView broadcaster;
	}
}