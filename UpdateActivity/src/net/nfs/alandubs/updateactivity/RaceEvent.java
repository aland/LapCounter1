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
	
	public boolean isOver(){
		return !(endTime == null);
	}
	
	public void addSwimmer(int id) { //assuming the unique identifier is int
		if(startTime == null && validId(id)){
			Swimmer s = new Swimmer();
			swimmers.put(id, s);
			Log.i("debug", "Swimmer " + id + " added");
		}
	}
	
	public SparseArray<Swimmer> getSwimmersArr() {
		//make sure to return null, probably a terrible idea..
		//return (swimmers.size() == 0 ? null : swimmers);
		return swimmers;
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
	
	//Dump info for export
	@Override
	public String toString(){
		String out = "";
		int max = swimmers.size();
		for(int i = 0; i < max; i++) {
			out += swimmers.valueAt(i).toString() + "\n";
		}
		return out;
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
			if(swimmer != null && (swimmer.getLastLap() < (now - interval))){
				if((swimmer.getLaps() < totalLaps) ){
					Log.d("debug", swimmer.getName() + " last lap: " + swimmer.getLastLap() + ". Now it's: " + now);
					Log.i("debug", "Lap time for " + swimmer.getName() + ':' + (now - swimmer.getLastLap()));
					
					swimmer.setLapComplete(now); 
				
					if(swimmer.getLaps() == totalLaps){
						completed++;

						if(allCompleted()){
							endTime = now;
						}
					}
					
					Log.d("debug", swimmer.getName() + " completed lap " + swimmer.getLaps() + " at: " + Long.toString(swimmer.getLastLap() / 1000000L));
					if(endTime != null)
						Log.i("debug", "Race over at: " + endTime);
				}
				else{
					Log.d("debug", swimmer.getName() + " already finished." + swimmer.getLaps() );
				}
			}

		}
		else{
			Log.d("debug", completed + " have already completed all " + totalLaps + " laps");
		}
	}
	
	public boolean allCompleted() {
		if(completed > swimmers.size()) {
			Log.e(this.getClass().getName(), "More completed than allowed");
		}
		Log.d("debug", "Completed: " + completed + ' ' + (completed >= swimmers.size()));
		
		return startTime != null && completed >= swimmers.size();
	}
	
	private boolean validId(int id) {
		if(isValidId(id) && swimmers.indexOfKey(id) < 0) {
			return true;
		}
		return false;
	}
	
	public boolean isValidId(int id){
		return id > 0;
	}
	

}
