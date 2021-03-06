package net.nfs.alandubs.updateactivity;

import java.util.ArrayList;
import java.util.List;

import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;

public class RaceEvent {
	private final String TAG = "RaceEvent";
	//Control the minimum interval between laps (RFID reader will send multiple events for the duration of proximity)
	private final long interval = NanoTime.second;

	private int totalLaps;
	private int completed;

	//private Map<Integer, Swimmer> swimmers = new HashMap<Integer, Swimmer>();
	private SparseArray<Swimmer> swimmers = new SparseArray<Swimmer>(10);

	private Long startTime;
	private Long endTime;

	public RaceEvent() {
		completed = 0;
	}
	
	private void setLaps(int laps){
		totalLaps = Math.max(1, laps);	
	}
	
	public int getMaxLaps() {
		return totalLaps;
	}
	
	public Long getStart(){
		return startTime;
	}
	
	public boolean isStarted(){
		return !(startTime == null || endTime != null);
	}
	
	public boolean isOver(){
		return !(endTime == null);
	}
	
	public void addSwimmer(int id) {
		if(startTime == null && validId(id)){
			Swimmer s = new Swimmer(id);
			swimmers.put(id, s);
			Log.i(TAG, "Swimmer " + id + " added");
		}
	}
	
	public SparseArray<Swimmer> getSwimmersArr() {
		return swimmers;
	}
	
	//Manually cast to List of objects for use in Adapter
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
	
	public int getSwimmers(int id){
		return getSwimmersLaps(id);
	}
	public int getSwimmersLaps(int id){
		if(swimmers.indexOfKey(id) >= 0){
			return swimmers.get(id).getLaps();
		}
		else {
			return 0;
		}
	}
	
	public boolean start(int laps){
		int size = swimmers.size();
		if(size >= 1){
			startTime = System.nanoTime();
			setLaps(laps);
			Log.d(TAG, "Started at: " + Long.toString(startTime / NanoTime.milli));
			for(int i = 0; i < size; i++){
				swimmers.valueAt(i).start(startTime);
			}
			return true;
		}
		else {
			Log.d(TAG, "No swimmers set");
		}
		return false;
	}
	
	public boolean restart(){
		int size = swimmers.size();
		if(size >= 1){
			startTime = null;
			endTime = null;
			completed = 0;
			for(int i = 0; i < size; i++){
				swimmers.valueAt(i).restart();
			}
			return true;
		}
		return false;
	}
	
	public void lap(int id){
		long now = System.nanoTime(); //consistent time in case race completes.

		if(startTime != null && endTime == null){
			Swimmer swimmer = swimmers.get(id);
			if(swimmer != null && (swimmer.getLastLapTime() < (now - interval))){
				if((swimmer.getLaps() < totalLaps) ){
					
					swimmer.setLapComplete(now);
				
					if(swimmer.getLaps() == totalLaps){
						completed++;

						if(allCompleted()){
							endTime = now;
						}
					}
					
					Log.d(TAG, swimmer.getName() + " completed lap " + swimmer.getLaps() + " at: " + Long.toString(swimmer.getLastLap()));
					if(endTime != null)
						Log.i(TAG, "Race over at: " + endTime);
				}
				else{
					Log.d(TAG, swimmer.getName() + " already finished." + swimmer.getLaps() );
				}
			}
		}
		else{
			Log.d(TAG, completed + " have already completed all " + totalLaps + " laps");
		}
	}
	
	public boolean allCompleted() {
		if(completed > swimmers.size()) {
			Log.e(this.getClass().getName(), "More completed than allowed");
		}
		Log.d(TAG, "Completed: " + completed + ' ' + (startTime != null && completed >= swimmers.size()));
		
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
