package net.nfs.alandubs.updateactivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
//import android.view.Menu;

public class MainActivity extends Activity {
	private RaceEvent race;
	private ListView listView;
	private SwimmerAdapter adapter;

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent){
			Bundle bundle = intent.getExtras();
			
			if(bundle != null){
				if(intent.getAction().equals(getString(R.string.add_swimmer_action))){
					MainActivity.this.receivedNewSwimmer(intent, bundle);
				}
				else if(intent.getAction().equals(getString(R.string.lap_complete_action))){
					MainActivity.this.receivedLap(intent, bundle);
				}
				else {
					Log.d("debug", intent.getAction());
				}
			}
			else {
				Log.d("activity", "no bundle");
			}
			//don't pass if no extra data
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		Resources r = getResources();
		
	    race = new RaceEvent(r.getInteger(R.integer.maxlaps));
		
		listView = (ListView) findViewById(R.id.swimmersView);
		adapter = new SwimmerAdapter(getBaseContext(), r.getInteger(R.integer.maxlaps));
		listView.setAdapter(adapter);
		adapter.updateSwimmers(race.getAllSwimmers());

	}
	
	@Override
	public void onResume(){
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(getString(R.string.add_swimmer_action));
		filter.addAction(getString(R.string.lap_complete_action));
		this.registerReceiver(this.mBroadcastReceiver, filter);
		
		//anything with adapter?
	}
	
	@Override
	public void onPause(){
		super.onPause();
		this.unregisterReceiver(this.mBroadcastReceiver);
	}
	
	private void receivedNewSwimmer(Intent i, Bundle b){

		if(b.containsKey("swimmer")){
			race.addSwimmer(b.getInt("swimmer"));
			if(race.getSwimmers() >= 4) { //TODO: remove hard cap and 'start' the race by click
				race.start();
				adapter.setStart( race.getStart() );
			}
		}

		TextView swimmersCount = (TextView) findViewById(R.id.numSwimmersView);
		swimmersCount.setText(Integer.toString(race.getSwimmers()));
		adapter.updateSwimmers(race.getAllSwimmers());
		
	}
	
	private void receivedLap(Intent i, Bundle b){
		if(b.containsKey("swimmer")){
			race.lap(b.getInt("swimmer"));
		}
		adapter.updateSwimmers(race.getAllSwimmers());
	}

}
