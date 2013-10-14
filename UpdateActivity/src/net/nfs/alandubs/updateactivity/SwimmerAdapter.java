package net.nfs.alandubs.updateactivity;

import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SwimmerAdapter extends BaseAdapter {
	private final String TAG = "swimmeradapter";
	private int maxLaps;
	private List<Swimmer> swimmers = Collections.emptyList();
	private long start;
	private final Context context;
	

	public SwimmerAdapter(Context context, int maxLaps) {
		super();
		this.context = context;
		this.maxLaps = maxLaps;
		this.start = 0L;
	}
	
	public void updateSwimmers(List<Swimmer> swimmers){
		ThreadPreconditions.checkOnMainThread();
		this.swimmers = swimmers;
		notifyDataSetChanged();
	}
	
	public void updateSwimmers(List<Swimmer> swimmers, long start){
		ThreadPreconditions.checkOnMainThread();
		this.swimmers = swimmers;
		this.start = start;
		notifyDataSetChanged();
	}
	
	public void setStart(Long start) {
		this.start = start;
	}
	
	public void setLaps(int maxLaps){
		this.maxLaps = maxLaps;
	}
	
	@Override
	public int getCount() {
		return swimmers.size();
	}

	@Override
	public Object getItem(int position) {
		return swimmers.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	private class ViewHolder {
        TextView txtTitle;
        TextView txtDesc;
    }
    
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHolder holder = null;

		//link to existing view if available, other make new
		if(convertView == null){
			convertView = LayoutInflater.from(context).inflate(R.layout.swimlist_item, parent, false);
			holder = new ViewHolder();
			holder.txtDesc = (TextView) convertView.findViewById(R.id.desc);
			holder.txtTitle = (TextView) convertView.findViewById(R.id.title);
			convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		try{
			Swimmer swimmer = (Swimmer) getItem(position);
			StringBuilder sb = new StringBuilder(25);
			
			if(start != 0L){ 
				
				sb.append(Math.max (maxLaps - swimmer.getLaps(), 0));
				sb.append(" laps to go. ");
				
				if(swimmer.getLaps() > 0){
					sb.append("Last lap was "); 
					sb.append(swimmer.getLastLap());
					sb.append("ms");
				}
			}
			else{
				sb.append("Ready");
			}
			
			//Log.d(TAG, "" + start + ' ' + swimmer.getLastLapTime());
			
			holder.txtDesc.setText(sb.toString());
			holder.txtTitle.setText(swimmer.getName());
		}
		catch(NumberFormatException e){
			Log.e(TAG, "Swimmer name not number" + e.getMessage());
		}
		
		return convertView; 
	}

}
