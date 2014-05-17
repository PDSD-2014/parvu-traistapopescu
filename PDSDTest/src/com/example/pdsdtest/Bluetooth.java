package com.example.pdsdtest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

class ExceptionHandler extends Exception {
	private static final long serialVersionUID = 1L;

	public ExceptionHandler(String message){
		super(message);
	}
}

public class Bluetooth {
	private Activity activity;
	private BluetoothAdapter ba;
	private Set<BluetoothDevice> pairedDevices;
	private Set<BluetoothDevice> devices;
	private final String uuidString = "2e87ecc1-5e85-42f1-9955-5788c93598ec";
	private final UUID uuid = UUID.fromString(uuidString);
	private BluetoothSocket bs;
	private OutputStream os;
	
	private Meeting meeting;
	
	public Bluetooth(final Activity activity) {
		this.activity = activity;
		ba = BluetoothAdapter.getDefaultAdapter();
		pairedDevices = new HashSet<BluetoothDevice>();
		devices = new HashSet<BluetoothDevice>();
	}
	
	public void enableBluetooth() throws ExceptionHandler {
		// Device does not support Bluetooth
		if (ba == null) {
			Toast.makeText(activity.getApplicationContext(), "The device does" +
					"not support Bluetooth", Toast.LENGTH_LONG).show();
			throw new ExceptionHandler("The device does not support Bluetooth");
		}
		
		// Enable bluetooth
		if (ba.isEnabled() == false) {
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(intent, 1);
		}
	}

	public void disableBluetooth(){
		ba.disable();
	}
	
	public Set<BluetoothDevice> getPairedDevices(){
		pairedDevices = ba.getBondedDevices();
		
		Log.d("mydebug", "get paired");
		for (BluetoothDevice bd : pairedDevices){
//			devices.add(new DeviceInfo( bd.getName(), bd.getAddress()));
		  devices.add(bd);
		}
		return pairedDevices;
	}
	
	BroadcastReceiver br = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
		  Log.d("mydebug", "received");
			// TODO Auto-generated method stub
			String a = intent.getAction(); // vezi ce face asta
			// Discover a device
//			if(BluetoothDevice.ACTION_FOUND.equals(a)){
				BluetoothDevice dev = intent.getParcelableExtra(
						BluetoothDevice.EXTRA_DEVICE);
//				devices.add(new DeviceInfo(dev.getName(), dev.getAddress()));
				devices.add(dev);
//			}
		}
		
	};
	
	// Unregister se face in onDestroy
	public void unregister() {
		activity.unregisterReceiver(br);
	}
	
	public void discoverDevices(){
		devices.clear();
		ba.startDiscovery();
		activity.registerReceiver(br, new IntentFilter(
				BluetoothDevice.ACTION_FOUND));
		
	}
	
	public Set<BluetoothDevice> getDevices(){
		return devices;
	}
	
	public void run() {
	  try {
  	  while (true) {
    	  for (BluetoothDevice d : getPairedDevices()) {
    	    try {
    	      String user = connect(d);
    	      
    	      meeting.deleteEntry(user);
    	    } catch (Exception ex) {
    	      Log.d("mydebug", "could not connect to " + d.getName());
    	    }
    	  }
    	  
    	  Thread.sleep(5000);
  	  }
	  } catch (Exception ex) {
	    ex.printStackTrace();
	  }
	}
	
	public String connect(BluetoothDevice device) throws IOException {
	  BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
	  byte[] buf = new byte[100];
	  int n = 0;
	  
	  socket.connect();
	  
	  try {
  	  InputStream in = socket.getInputStream();
      
      n = in.read(buf);
      
      socket.close();
	  } catch (IOException ex) {
	    ex.printStackTrace();
	  }
    
    return new String(buf, 0, n);
	}
	
	private class Connect extends Thread {
		private  BluetoothSocket socket;
		private BluetoothDevice device;
		
		public Connect(final BluetoothDevice device) 
				throws IOException{
			this.device = device;
//			BluetoothSocket temp = device.createRfcommSocketToServiceRecord(uuid);
//			socket = ba.getRemoteDevice(device.getAddress());
//			socket = 
			
//			if (socket == null) {
//			  Log.d()
//			}
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			ba.cancelDiscovery();
			
			try{
				socket.connect();
				Log.d("mydebug", "s-a acceptat");
				
        InputStream in = socket.getInputStream();
        
        byte[] buf = new byte[100];
        
        int x = in.read(buf);
        
        Log.d("mydebug", "a venit " + new String(buf, 0, x));
			} catch(Exception e){
//				System.out.println(e.getLocalizedMessage());
			  e.printStackTrace();
//				try {
//					socket.close();
//				} catch(Exception ex){
////					System.out.println(ex.getLocalizedMessage());
//				  ex.printStackTrace();
//				}
			}
		}
		
		public void closeSocket(){
			try{
				socket.close();
			} catch(Exception e){
				System.out.println(e.getLocalizedMessage());
			}
		}
	}
	
	private class Send implements Runnable {
		BluetoothSocket socket;
		byte[] message;
		
		public Send(BluetoothSocket socket, byte[] message) {
			this.socket = socket;
			this.message = message;
			os = null;
			
			try {
				os = socket.getOutputStream();
			} catch(Exception e){
				System.out.println(e.getLocalizedMessage());
			}
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try{
				os.write(message);
			} catch(Exception e){
				System.out.println(e.getLocalizedMessage());
				try {
					os.close();
				} catch(Exception ex){
					System.out.println(ex.getLocalizedMessage());
				}
			}
		}
	}
}
