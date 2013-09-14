package net.nfs.alandubs.updateactivity;

import java.util.ArrayList;
import java.util.List;

import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;

public class RaceEvent {
	//Control the minimum interval between laps (RFID reader will send multiple events for the duration of proximity)
	private final long micro	= 1000L;
	private final long milli	= 1000000L;
	private final long second	= 1000000000L;
	
	private final long interval = second;

	private int totalLaps;
	private int completed;

	//private Map<Integer, Swimmer> swimmers = new HashMap<Integer, Swimmer>();
	private SparseArray<Swimmer> swimmers = new SparseArray<Swimmer>(10);

	private Long startTime;
	private Long endTime;

	public RaceEvent(int laps) {
		if(laps < 1){
			laps = 1;
		}
		totalLaps = laps;
		completed = 0;
	}
	
	public int getMaxLaps() {
		return totalLaps;
	}
	
	public Long getStart(){
		return startTime;
	}
	
	public boolean isStarted(){
		return !(startTime == null);
	}
	
	public void addSwimmer(int id) { //assuming the unique identifier is int
		if(startTime == null && validId(id)){
			Swimmer s = new Swimmer();
			swimmers.put(id, s);
			Log.i("debug", "Swimmer " + id + " added");
		}
	}
	
	//Manually cast to List of objects for fun
	public List<Swimmer> getAllSwimmers() {
		//List<Swimmer> s = new List<Swimmer>();
		ArrayList<Swimmer> s = new ArrayList<Swimmer>();
		
		int max = swimmers.size();
		for(int i = 0; i < max; i++) {
			s.add(swimmers.valueAt(i));
		}
		
		return s;
	}
	
	public int getSwimmers(){
		return swimmers.size();
	}
	
	public int getSwimmersLaps(int id){
		if(swimmers.indexOfKey(id) >= 0){
			return swimmers.get(id).getLaps();
		}
		else {
			return 0;
		}
	}
	
	public void start(){
		if(startTime == null && swimmers.size() >= 1) {
			startTime = System.nanoTime();
			Log.d("debug", "Started at: " + Long.toString(startTime / milli));
		} // else restart?
		else {
			Log.d("debug", "start time already set or no swimmers set");
		}
	}
	
	public void lap(int id){
		long now = System.nanoTime(); //consistent time in case race completes.

		if(startTime != null && endTime == null){
			Swimmer swimmer = swimmers.get(id);
			if(swimmer != null){
				if((swimmer.getLaps() < totalLaps) && (swimmer.getLastLap() < (now - interval))){
					swimmer.setLapComplete(now); 
				
					if(swimmer.getLaps() == totalLaps){
						completed++;

						if(allCompleted()){
							endTime = now;
						}
					}
					
					Log.d("debug", swimmer.getName() + " completed lap at: " + Long.toString(swimmer.getLastLap() / 1000000L));
					if(endTime != null)
						Log.d("debug", "Race over at: " + endTime);
				}
			}
			else{
				Log.d("debug", "Swimmer not found");
			}
		}
		else{
			Log.d("debug", "No start time set or end already set");
		}
	}
	
	public boolean allCompleted() {
		if(completed > totalLaps) {
			Log.d(this.getClass().getName(), "More completed than allowed");
		}
		
		return completed >= totalLaps;
	}
	
	private boolean validId(int id) {
		if(id > 0 && swimmers.indexOfKey(id) < 0) {
			return true;
		}
		return false;
	}
	

}
