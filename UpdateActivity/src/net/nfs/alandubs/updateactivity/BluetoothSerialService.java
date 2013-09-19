/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nfs.alandubs.updateactivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
//import es.pymasde.blueterm.BlueTerm;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothSerialService {
    // Debugging
    private static final String TAG = "BluetoothReadService";
    private static final boolean D = true;

    // UUID is well known SSP
	private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private ConnectThread mConnectThread; //one connection thread should be enough
    //private ConnectedThread mConnectedThread; //need array of these
    private HashMap<String, ConnectedThread> mConnectedThreads;
    private int mState;
    private HashMap<String, Integer> mStates;
    
    //private EmulatorView mEmulatorView;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothSerialService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mStates = new HashMap<String, Integer>();
        mHandler = handler;
        mConnectedThreads = new HashMap<String, ConnectedThread>(); 
    }

    /**
     * Set the overall current state of the connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    
    /**
     * Set the current state of a device connection
     * @param addr   A String which represents the MAC address of BT device
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(String addr, int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);

       	mStates.put(addr, state);

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the overall connection state. */
    public synchronized int getState() {
        return mState;
    }
    
    /**
     * Return the current device connection state */
    public synchronized int getState(String addr) {
        if(mStates.containsKey(addr)){
        	return mStates.get(addr);
        }
        else{
        	return STATE_NONE;
        }
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        // Cancel all threads currently running a connection
        cancelAllConnected();
        
        // Cancel any thread currently running a connection
        /*
        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }*/

        setState(STATE_NONE);
        mStates.clear();
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * First point of connection
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device + " > " + device.getAddress());
        
        String address = device.getAddress();
        //int state = getState(address);
        
        // Cancel any thread attempting to make a connection
       /* if(state == STATE_CONNECTING && mConnectedThreads.get(address) != null){
       		mConnectedThreads.get(address).cancel();
        }*/
        if (mState == STATE_CONNECTING && mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        //if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (mConnectedThreads.get(address) != null) {
        	mConnectedThreads.get(address).cancel();
        	mConnectedThreads.remove(address);
        }

        //mConnectThread as array with device.getAddress() as key? no, mConnectedThreads should be array
        
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
        setState(address, STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connectedto: " + device + " > " + device.getAddress());
        String address = device.getAddress();

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThreads.get(address) != null) {
        	mConnectedThreads.get(address).cancel();
        	mConnectedThreads.remove(address);
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThreads.put(address, new ConnectedThread(socket, address));
        mConnectedThreads.get(address).start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
        setState(address, STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        // Cancel all threads currently running a connection
        cancelAllConnected();

        setState(STATE_NONE);
    }
    
    /*
     * Cancel all threads currently running a connection
     */
    private synchronized void cancelAllConnected(){
		Iterator<Entry<String, ConnectedThread>> it = mConnectedThreads.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String, ConnectedThread> pairs = (Map.Entry<String, ConnectedThread>)it.next();
			pairs.getValue().cancel();
		}
		
		mConnectedThreads.clear();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])

    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }     */
    
    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed(String addr) {
        setState(addr, STATE_NONE);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost(String addr) {
        setState(addr, STATE_NONE);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Device connection to: " + addr + " was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    
    private void sendTag(int tag) {
    	Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_GETTAG);
    	Bundle bundle = new Bundle();
    	bundle.putInt(MainActivity.RFIDTAG, tag);
    	msg.setData(bundle);
    	mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            
            Log.d(TAG, "Name: " + mmDevice.getName() + ", Address: " + mmDevice.getAddress());
            
            BluetoothSocket tmp = null;
            /*
            // Get a BluetoothSocket using Reflection, but I don't know it's needed
            Method m;
            try {
            	m = mmDevice.getClass().getMethod("createRfcommSocketToServiceRecord", new Class[] {UUID.class});
            	tmp = (BluetoothSocket) m.invoke(mmDevice, SerialPortServiceClass_UUID);
            } catch (SecurityException e) {
            	Log.e(TAG, "create() failed", e);
            } catch (NoSuchMethodException e) {
            	Log.e(TAG, "create() failed", e);
            } catch (IllegalArgumentException e) {
            	Log.e(TAG, "create() failed", e);
            } catch (IllegalAccessException e) {
            	Log.e(TAG, "create() failed", e);
            } catch (InvocationTargetException e) {
            	Log.e(TAG, "create() failed", e);
            }  
            */
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(SerialPortServiceClass_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            } 
            
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed(mmDevice.getAddress());
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }

                return;
            }
            catch (NullPointerException e){
            	Log.e(TAG, "no socket to connect to");
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothSerialService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        //private final OutputStream mmOutStream;
        private final String mmAddress;
        private int bytesread;
        private int checksum;
        private byte tempbyte;
        private String bytecode;
        private int[] code;
        private List<String> results;
        

        public ConnectedThread(BluetoothSocket socket, String address) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            mmAddress = address;
            InputStream tmpIn = null;
            //OutputStream tmpOut = null;
            results = new ArrayList<String>();

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                //tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            //mmOutStream = tmpOut;
            
            init();
        }
        
        private void init(){
            code = new int[6];
            checksum = 0;
            tempbyte = 0;
            bytesread = -1;
            bytecode = "";
           	//Log.i(TAG, "Already have found: " + results.size() + " tags.");
        }

        //This can run once per byte, or process multiple bytes in one go
        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
            	Log.d(TAG, "Running");
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    for (int i = 0; i < bytes; i++) {
                    	//Log.d(TAG, "Reading: " + (i+1) + " of " + bytes + " from input stream, read: " + bytesread);

                        byte b = buffer[i];
                        try {
                        	if(bytesread >= 0 && bytesread <= 12){
                        		
                            	char printableB = (char) b;
                                if (b < 32 || b > 126)
                                    printableB = ' ';
                                //Log.d(TAG, "'" + Character.toString(printableB) + "' (" + Integer.toString(b) + ")");
                                bytecode += "#0" + Integer.toString(b);

                                if((b == 0x0D)||(b == 0x0A)||(b == 0x03)||(b == 0x02)) { // if header or stop bytes before the 10 digit reading 
                                	Log.e(TAG, i + " Unexpected header while processing character "
                                            + Integer.toString(b));
                                }
                                else {
                                	// Do ASCII/Hex conversion
                                    if ((b >= '0') && (b <= '9')) {
                                        b = (byte) (b - '0');
                                    } else if ((b >= 'A') && (b <= 'F')) {
                                        b = (byte) (10 + b - 'A');
                                    }
                                    
                                	if ((bytesread & 1) == 1) { //if isOdd(bytesread)
                                        // make some space for this hex-digit by shifting the previous hex-digit with 4 bits to the left:
                            			//code[bytesread >> 1] means it goes in sequential 1,2,3,4.. for values 2,4,6,8..
                                        code[bytesread >> 1] = (b | tempbyte << 4);
                                        if (bytesread >> 1 != 5) {                // If we're not at the checksum byte,
                                            checksum ^= code[bytesread >> 1];       // Calculate the checksum... (XOR)
                                        }
                                	}
                                	else { // Store the first digit first
                                		tempbyte = b; 
                                	}
                                }
                                
                                bytesread++;
                        	}
                        	else if(b == 2){ //does the extra condition above break this?
                        		init();
                        		bytesread = 0;
                        		Log.d(TAG, "Header found! (" + Integer.toString(b) + ")");
                        	}
                        	
                        	if(bytesread == 12){
                        		if(checksum < 0){
                        			Log.i(TAG, "Checksum negative: "+checksum);
                        		}
                        		//String check = (code[5] == checksum ? "-passed" : "-error");
                                //Log.d(TAG, "#002"+bytecode);
                                //Log.d(TAG, "Check: " + code[5] + (code[5] == checksum ? "-passed" : "-error"));
                                if(code[5] == checksum) {
                                	/*if(!results.contains(r)){
                                		results.add(r);
                                	}*/
                                	//tell my mainactivity the good news
                                	sendTag(code[5]);
                                }
                                init();
                        	}
                        	else if(bytesread > 12){
                        		Log.e(TAG, "Too many bytes!");
                        	}

                        } catch (Exception e) {
                            Log.e(TAG, i + " Exception while processing character "
                                    + Integer.toString(b), e);
                        }
                    }


                    //not sure what this does, toString is some native function that doesn't do what I would've thought
                    String a = buffer.toString();                    
                    a = ""; //does this somehow free memory or what?
                    
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost(mmAddress);
                    break;
                }
            }

            Log.i(TAG, "END mConnectedThread");
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, buffer.length, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
