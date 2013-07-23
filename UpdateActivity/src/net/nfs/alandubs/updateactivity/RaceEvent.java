package net.nfs.alandubs.updateactivity;

import java.util.ArrayList;
import java.util.List;

import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;

public class RaceEvent {
	private int totalLaps;
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
	}
	
	public int getMaxLaps() {
		return totalLaps;
	}
	
	public Long getStart(){
		return startTime;
	}
	
	public void addSwimmer(int id) { //assuming the unique identifier is int
		if(validId(id)){
			Swimmer s = new Swimmer();
			swimmers.put(id, s);
		}
		else {
			Log.d("debug", "not valid or already used id");
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
		if(startTime == null && swimmers.size() > 1) {
			startTime = System.nanoTime();
			Log.d("debug", "Started at: " + Long.toString(startTime / 1000000L));
		} // else restart?
		else {
			Log.d("debug", "start time already set or no swimmers set");
		}
	}
	
	public void lap(int id){
		if(startTime != null){
			Swimmer swimmer = swimmers.get(id);
			if(swimmer != null){
				swimmer.setLapComplete(System.nanoTime()); 
				Log.d("debug", swimmer.getName() + " completed lap at: " + Long.toString(swimmer.getLastLap() / 1000000L));
				
				if(allCompleted()){
					endTime = System.nanoTime();
				}
			}
			else{
				Log.d("debug", "swimmer not found for lap completed");
			}
		}
		else{
			Log.d("debug", "no start time set for lap completed");
		}
	}
	
	private boolean allCompleted() {
		//TODO iterate over swimmers and return true if all swimmers.laps == totalLaps;
		return false;
	}
	
	private boolean validId(int id) {
		if(id > 0 && swimmers.indexOfKey(id) < 0) {
			return true;
		}
		return false;
	}
	

}
