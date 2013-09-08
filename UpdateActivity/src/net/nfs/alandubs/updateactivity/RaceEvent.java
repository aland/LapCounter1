package net.nfs.alandubs.updateactivity;

import java.util.ArrayList;
import java.util.List;

import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;

public class RaceEvent {
	//Control the minimum interval between laps (RFID reader will send multiple events for the duration of proximity)
	private final long interval = 1000000000L; 
	private int totalLaps;
	private int completed;
	//Map<String, Integer> aMap = new HashMap<String, Integer>();
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
		//startTime = 0L;
		//endTime = 0L;
	}
	
	public int getMaxLaps() {
		return totalLaps;
	}
	
	public Long getStart(){
		return startTime;
	}
	
	public boolean isStarted(){
		return startTime != null ? true : false;
	}
	
	public void addSwimmer(int id) { //assuming the unique identifier is int
		Log.i("debug", "addSwimmer("+id+") called");
		if(startTime == null && validId(id)){
			Swimmer s = new Swimmer();
			swimmers.put(id, s);
		}
		else {
			Log.d("debug", "Didn't add: "+ id +" not valid or already used id or race already started");
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
			Log.d("debug", "Started at: " + Long.toString(startTime / 1000000L));
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
					Log.d("debug", swimmer.getName() + " completed lap at: " + Long.toString(swimmer.getLastLap() / 1000000L));
				
					//if(allCompleted()){
					//	endTime = now;
					//}
					if(swimmer.getLaps() == totalLaps){
						completed++;
					}
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
	
	private boolean allCompleted() {
		//TODO iterate over swimmers and return true if all swimmers.laps == totalLaps;
		return completed >= totalLaps;
		/*
		int size = swimmers.size();
		if(size == 0)
			return false;
		
		boolean complete = true;
		for(int i = 0; i < size; i++) {
			if(swimmers.valueAt(i).getLaps() < totalLaps){
				complete = false;
				break; //fail early
			}
		}
		return complete; */
	}
	
	private boolean validId(int id) {
		if(id > 0 && swimmers.indexOfKey(id) < 0) {
			return true;
		}
		return false;
	}
	

}
