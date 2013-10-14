package net.nfs.alandubs.updateactivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.util.Log;


public class Swimmer {
	private final String TAG = "swimmer";
	private long lastLapTime; //keep the timestamp for previous lap / race start 
	private List<Long> lapTimes = new ArrayList<Long>();
	private String name; //RFID checksum (used for id)

	@Deprecated
	public Swimmer() {
		Random r = new Random();
		name = "SwimmerR" + Integer.toString(r.nextInt(100));
	}
	
	public Swimmer(int id) {
		//pad so that name is 3 digits, hopefully there's no checksum > 999 but I think we'll be alright
		name = id < 100 ? String.format("%03d", id) : Integer.toString(id);
		lastLapTime = 0L;
	}
	
	public String getName() {
		return name;
	}

	public int getLaps(){
		return lapTimes.size();
	}
	
	public void start(long time) {
		lastLapTime = time;
	}
	
	public void setLapComplete(long time) {
		if(lastLapTime <= 0L){
			Log.e(TAG, "No start time / previous lap time set");
		}
		lapTimes.add((time - lastLapTime) / NanoTime.milli);
		lastLapTime = time;
	}

	
	public long getLastLapTime() {
		return lastLapTime;
	}
	
	public long getLastLap() {
		return lapTimes.size() > 0 ? lapTimes.get(lapTimes.size() - 1) : 0L;
		/*
		if(lapTimes.size() == 0) {
			Log.e(TAG, "No laps completed");
			return 0L;
		}
		else {
			return lapTimes.get(lapTimes.size() - 1);
		}*/
	}
	
	@Deprecated // no need for it
	public long getLastLap(int i) {
		return lapTimes.get(i - 1);
	}
	
	@Override
	public String toString(){
		int max = getLaps();
		StringBuilder out = new StringBuilder(max * 10); //rough estimate of needed length, probably way over
		out.append(getName());
		
		for(int i = 0; i < max; i++) {
			out.append(',').append(lapTimes.get(i));
		}
		return out.toString();
	}


}
