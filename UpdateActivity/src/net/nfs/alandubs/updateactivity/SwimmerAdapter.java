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
	}
	
	public void updateSwimmers(List<Swimmer> swimmers){
		ThreadPreconditions.checkOnMainThread();
		this.swimmers = swimmers; //other checks?
		notifyDataSetChanged();
	}
	
	public void setStart(Long start) {
		this.start = start;
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
	
	private int nanoToSec(long l) {
		if(l == 0){
			return 0;
		}
		else if(l < NanoTime.second){
			Log.d(TAG, "nanoToSec was passed small value");
			return 0;
		}
		else {
			l = l / NanoTime.second;
			if(l > Integer.MAX_VALUE) {
				Log.d(TAG, "value too big!");
				return 0;
			}
			
			return (int) l;
		}
	}
    
	private class ViewHolder {
        TextView txtTitle;
        TextView txtDesc;
    }
    
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		int remains;
		
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
			String lastLap = swimmer.getLaps() > 0 ? Long.toString(swimmer.getLastLap()) : "N/A";
	
	        remains = Math.max( this.maxLaps - swimmer.getLaps(), 0);	
			
			holder.txtDesc.setText(Integer.toString(remains) + " to go, last lap: " + lastLap);
			holder.txtTitle.setText(swimmer.getName());
		}
		catch(NumberFormatException e){
			Log.e(TAG, "Swimmer name not number" + e.getMessage());
		}
		
		return convertView; 
	}

}
