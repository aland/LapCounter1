package net.nfs.alandubs.updateactivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.R.string;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.Log;


public class Swimmer {
	private List<Long> lapTimes = new ArrayList<Long>();
	private String name; //just for something to print

	public Swimmer() {
		Random r = new Random();
		name = "Swimmer" + Integer.toString(r.nextInt(100));
	}
	
	public String getName() {
		return name;
	}

	public int getLaps(){
		return lapTimes.size();
	}
	
	public void setLapComplete(long time) {
		lapTimes.add(time);
	}
	
	public long getLastLapTime(long start) {
		long l = 0L;
		
		if (lapTimes.size() == 0) {
		 //nothing?
		}
		else if (lapTimes.size() == 1) {
			l = this.getLastLap() - start;
		}
		else if (lapTimes.size() > 1) {
			l = this.getLastLap() - this.getLastLap(lapTimes.size() - 1);
		}
		else if (lapTimes.size() < 0) {
			//problem
			Log.d("swimmer","getting lap time problem");
		}
		
		return l;
	}
	
	public long getLastLap() {
		if(lapTimes.size() == 0) {
			return 0;
		}
		else {
			return getLastLap(lapTimes.size());
		}
	}
	public long getLastLap(int i) {
		return lapTimes.get(i - 1);
	}
	
	@Override
	public String toString(){
		String out = "";
		int max = getLaps();
		for(int i = 0; i < max; i++) {
			out += Long.toString(lapTimes.get(i)) + ',';
		}
		return out;
	}


}
