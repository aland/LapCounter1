package net.nfs.alandubs.updateactivity;

import net.nfs.alandubs.updateactivity.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
//import android.view.Menu;

public class MainActivity extends Activity {
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private static TextView mTitle;
    
    // Name of the connected device
    private String mConnectedDeviceName = null;

    /**
     * Set to true to add debugging code and logging.
     */
    public static final boolean DEBUG = true;

    /**
     * Set to true to log each character received from the remote process to the
     * android log, which makes it easier to debug some kinds of problems with
     * emulating escape sequences and control codes.
     */
    public static final boolean LOG_CHARACTERS_FLAG = DEBUG && false;

    /**
     * Set to true to log unknown escape sequences.
     */
    public static final boolean LOG_UNKNOWN_ESCAPE_SEQUENCES = DEBUG && false;

	public static final String LOG_TAG = "BlueSwim";
	

    // Message types sent from the BluetoothReadService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Application message types
    public static final int MESSAGE_GETTAG = 6;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    // Application key names
    public static final String RFIDTAG = "tag";
    public static final String MAXLAPS_KEY = "maxlaps";
	
	private BluetoothAdapter mBluetoothAdapter = null;
    
	private static BluetoothSerialService mSerialService = null;
    
	private boolean mEnablingBT;
    private int mMaxLaps = 9;
    

    private SharedPreferences mPrefs;
	
    private MenuItem mMenuItemConnect;
    

	/**
	 * Application classes
	 */
	private RaceEvent race;
	private ListView listView;
	private SwimmerAdapter adapter;

	@Deprecated
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
				Log.d("debug", "no bundle");
			}
			//don't pass if no extra data
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Resources r = getResources();
		
		if (DEBUG)
			Log.e(LOG_TAG, "+++ ON CREATE +++");

		mPrefs = getPreferences(Context.MODE_PRIVATE);
        readPrefs();


		//boolean useTitleFeature = false;
	    // If the window has a container, then we are not free to request window features.
	    if(getWindow().getContainer() == null) {
	       // useTitleFeature = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	    	Log.d(LOG_TAG, "Container is null");
	    }

	    requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_main);
        
		// Set up the window layout
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        Log.d(LOG_TAG, "Has FEATURE_CUSTOM_TITLE");
	    // Set up the custom title
	    mTitle = (TextView) findViewById(R.id.title_left_text);
	    mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        mTitle.setText("Hi mTitle");


		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (mBluetoothAdapter == null) {
            finishDialogNoBluetooth();
			return;
		}
		
	    race = new RaceEvent(mMaxLaps);
		listView = (ListView) findViewById(R.id.swimmersView);
		
        mSerialService = new BluetoothSerialService(this, mHandlerBT);   
		//not using adapter?
        adapter = new SwimmerAdapter(getBaseContext(), mMaxLaps);
		
        listView.setAdapter(adapter);
		adapter.updateSwimmers(race.getAllSwimmers());
		
		Button startButton = (Button) this.findViewById(R.id.startButton);
		startButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		        // Do something in response to button click
		    	startRace();
		    }
		});

		if (DEBUG)
			Log.e(LOG_TAG, "+++ DONE IN ON CREATE +++");
		
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (DEBUG)
			Log.e(LOG_TAG, "++ ON START ++");
		
		mEnablingBT = false;
	}
	
	@Override
	public synchronized void onResume(){
		super.onResume();
		
		if (DEBUG) {
			Log.e(LOG_TAG, "+ ON RESUME +");
		}
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(getString(R.string.add_swimmer_action));
		filter.addAction(getString(R.string.lap_complete_action));
		this.registerReceiver(this.mBroadcastReceiver, filter);
		//anything with adapter?
		
		//BlueTerm stuff follows
		if (!mEnablingBT) { // If we are turning on the BT we cannot check if it's enable
		    if ( (mBluetoothAdapter != null)  && (!mBluetoothAdapter.isEnabled()) ) {
			
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.alert_dialog_turn_on_bt)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.alert_dialog_warning_title)
                    .setCancelable( false )
                    .setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int id) {
                    		mEnablingBT = true;
                    		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);			
                    	}
                    })
                    .setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int id) {
                    		finishDialogNoBluetooth();            	
                    	}
                    });
                AlertDialog alert = builder.create();
                alert.show();
		    }		
		
		    if (mSerialService != null) {
		    	// Only if the state is STATE_NONE, do we know that we haven't started already
		    	if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) {
		    		// Start the Bluetooth chat services
		    		mSerialService.start();
		    	}
		    }

		    if (mBluetoothAdapter != null) {
		    	readPrefs();
		    	updatePrefs();

		    	//mEmulatorView.onResume();
		    }
		}
		
	}
	
	@Override
	public synchronized void onPause(){
		super.onPause();
		if (DEBUG)
			Log.e(LOG_TAG, "- ON PAUSE -");
		
		//this.unregisterReceiver(this.mBroadcastReceiver);
	}
	
    @Override
    public void onStop() {
        super.onStop();
        if(DEBUG)
        	Log.e(LOG_TAG, "-- ON STOP --");
    }
    
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (DEBUG)
			Log.e(LOG_TAG, "--- ON DESTROY ---");
		
        if (mSerialService != null)
        	mSerialService.stop();
        
	}
	
    private void readPrefs() {
    	
        mMaxLaps = readIntPref(MAXLAPS_KEY, mMaxLaps, 20);
        
    }
    private void updatePrefs() {
    	Log.d(LOG_TAG, "Todo updatePrefs");
    	//TODO this, do I need to?
    	/*
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mEmulatorView.setTextSize((int) (mFontSize * metrics.density));
        setColors();
        mControlKeyCode = CONTROL_KEY_SCHEMES[mControlKeyId];
        */
    }

    private int readIntPref(String key, int defaultValue, int maxValue) {
        int val;
        try {
            val = Integer.parseInt(
                mPrefs.getString(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            val = defaultValue;
        }
        val = Math.max(0, Math.min(val, maxValue));
        return val;
    }
    
	public int getConnectionState() {
		return mSerialService.getState();
	}

	//don't think I'll need this
    public void send(byte[] out) {
    	mSerialService.write( out );
    }
    // The Handler that gets information back from the BluetoothService
    private final Handler mHandlerBT = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {        	
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(DEBUG) Log.i(LOG_TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothSerialService.STATE_CONNECTED:
                	if (mMenuItemConnect != null) {
                		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
                		mMenuItemConnect.setTitle(R.string.disconnect);
                	}
                	
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    break;
                    
                case BluetoothSerialService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                    
                case BluetoothSerialService.STATE_LISTEN:
                case BluetoothSerialService.STATE_NONE:
                	if (mMenuItemConnect != null) {
                		//mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
                		mMenuItemConnect.setTitle(R.string.connect);
                	}

            		//mInputManager.hideSoftInputFromWindow(mEmulatorView.getWindowToken(), 0);
                	
                    mTitle.setText(R.string.title_not_connected);

                    break;
                }
                break;
              
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_GETTAG:
            	Log.d(LOG_TAG, "Holy shit"+ msg.getData().getInt(RFIDTAG));
            	
            	receivedNewSwimmer(msg.getData().getInt(RFIDTAG));
            	//TODO handle this better
            	break;
            }
        }
    };   
    
    public void finishDialogNoBluetooth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_no_bt)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setTitle(R.string.app_name)
        .setCancelable( false )
        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       finish();            	
                	   }
               });
        AlertDialog alert = builder.create();
        alert.show(); 
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d(LOG_TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        
        case REQUEST_CONNECT_DEVICE:

            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mSerialService.connect(device);                
            }
            break;

        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                Log.d(LOG_TAG, "BT not enabled");
                
                finishDialogNoBluetooth();                
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	Log.d(LOG_TAG, "in onCreateOptionsMenu");
    	Log.d(LOG_TAG, menu.toString());
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        mMenuItemConnect = menu.getItem(0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.connect:
        	
        	if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
        		// Launch the DeviceListActivity to see devices and do scan
        		Intent serverIntent = new Intent(this, DeviceListActivity.class);
        		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        	}
        	else
            	if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
            		mSerialService.stop();
		    		mSerialService.start();
            	}
            return true;
        case R.id.preferences:
        	doPreferences();
            return true;

        }
        return false;
    }
    
    private void doPreferences() {
        startActivity(new Intent(this, SwimPreferences.class));
    }
    
    /**
     * A multi-thread-safe produce-consumer byte array.
     * Only allows one producer and one consumer.
     */

    class ByteQueue {
        public ByteQueue(int size) {
            mBuffer = new byte[size];
        }

        public int getBytesAvailable() {
            synchronized(this) {
                return mStoredBytes;
            }
        }

        public int read(byte[] buffer, int offset, int length)
            throws InterruptedException {
            if (length + offset > buffer.length) {
                throw
                    new IllegalArgumentException("length + offset > buffer.length");
            }
            if (length < 0) {
                throw
                new IllegalArgumentException("length < 0");

            }
            if (length == 0) {
                return 0;
            }
            synchronized(this) {
                while (mStoredBytes == 0) {
                    wait();
                }
                int totalRead = 0;
                int bufferLength = mBuffer.length;
                boolean wasFull = bufferLength == mStoredBytes;
                while (length > 0 && mStoredBytes > 0) {
                    int oneRun = Math.min(bufferLength - mHead, mStoredBytes);
                    int bytesToCopy = Math.min(length, oneRun);
                    System.arraycopy(mBuffer, mHead, buffer, offset, bytesToCopy);
                    mHead += bytesToCopy;
                    if (mHead >= bufferLength) {
                        mHead = 0;
                    }
                    mStoredBytes -= bytesToCopy;
                    length -= bytesToCopy;
                    offset += bytesToCopy;
                    totalRead += bytesToCopy;
                }
                if (wasFull) {
                    notify();
                }
                return totalRead;
            }
        }

        public void write(byte[] buffer, int offset, int length)
        throws InterruptedException {
            if (length + offset > buffer.length) {
                throw
                    new IllegalArgumentException("length + offset > buffer.length");
            }
            if (length < 0) {
                throw
                new IllegalArgumentException("length < 0");

            }
            if (length == 0) {
                return;
            }
            synchronized(this) {
                int bufferLength = mBuffer.length;
                boolean wasEmpty = mStoredBytes == 0;
                while (length > 0) {
                    while(bufferLength == mStoredBytes) {
                        wait();
                    }
                    int tail = mHead + mStoredBytes;
                    int oneRun;
                    if (tail >= bufferLength) {
                        tail = tail - bufferLength;
                        oneRun = mHead - tail;
                    } else {
                        oneRun = bufferLength - tail;
                    }
                    int bytesToCopy = Math.min(oneRun, length);
                    System.arraycopy(buffer, offset, mBuffer, tail, bytesToCopy);
                    offset += bytesToCopy;
                    mStoredBytes += bytesToCopy;
                    length -= bytesToCopy;
                }
                if (wasEmpty) {
                    notify();
                }
            }
        }

        private byte[] mBuffer;
        private int mHead;
        private int mStoredBytes;
    }
    
    /**
     * Application classes 
     */
	private void startRace(){
		if(race.getSwimmers() >= 4) { //TODO: remove hard cap and 'start' the race by click
			race.start();
			adapter.setStart( race.getStart() );
		}
		else {
			Log.d("debug", "Not enough swimmers to start");
		}
	}

	private void receivedNewSwimmer(Intent i, Bundle b){
		if(b.containsKey("swimmer")){
			race.addSwimmer(b.getInt("swimmer"));
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
	
	
	//for fancy new BT service
	private void receivedNewSwimmer(int i){
		if(i > 1){
			race.addSwimmer(i);
			if(race.getSwimmers() >= 4) { //TODO: remove hard cap and 'start' the race by click
				race.start();
				adapter.setStart( race.getStart() );
			}
		}

		TextView swimmersCount = (TextView) findViewById(R.id.numSwimmersView);
		swimmersCount.setText(Integer.toString(race.getSwimmers()));
		adapter.updateSwimmers(race.getAllSwimmers());
	}
	
	private void receivedLap(int i){
		//TODO this
		adapter.updateSwimmers(race.getAllSwimmers());
	}

}
