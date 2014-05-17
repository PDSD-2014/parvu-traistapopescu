package com.example.pdsdtest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
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

class DeviceInfo {
	private String deviceName;
	private String deviceMAC;
	
	public DeviceInfo(String deviceName, String deviceMAC){
		this.deviceName = deviceName;
		this.deviceMAC = deviceMAC;
	}
	
	public String getDeviceName(){
		return deviceName;
	}
	
	public String getDeviceMAC(){
		return deviceMAC;
	}
	
	public void setDeviceName(String deviceName){
		this.deviceName = deviceName;
	}
	
	public void setDeviceMAC(String deviceMAC){
		this.deviceMAC = deviceMAC;
	}
}

public class Bluetooth {
	private static Activity activity;
	private static BluetoothAdapter ba;
	private static Set<BluetoothDevice> pairedDevices;
	private static Set<BluetoothDevice> devices;
	private static final String uuidString = "2e87ecc1-5e85-42f1-9955-5788c93598ec";
	private static String serviceName = "pdsdtest";
	private static final UUID uuid = UUID.fromString(uuidString);
	private static BluetoothSocket bs;
	private static OutputStream os;
	
	public Bluetooth(final Activity activity){
		Bluetooth.activity = activity;
		ba = BluetoothAdapter.getDefaultAdapter();
		pairedDevices = new HashSet<BluetoothDevice>();
		devices = new HashSet<BluetoothDevice>();
	}
	
	public static void enableBluetooth() throws ExceptionHandler{
		// Device does not support Bluetooth
		if( ba == null ){
			Toast.makeText(activity.getApplicationContext(), "The device does" +
					"not support Bluetooth", Toast.LENGTH_LONG).show();
			throw new ExceptionHandler("The device does not support Bluetooth");
		}
		
		// Enable bluetooth
		if( ba.isEnabled() == false){
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(intent, 1);
		}
	}
	
	public static void disableBluetooth(){
		ba.disable();
	}
	
	public static Set<BluetoothDevice> getPairedDevices(){
		pairedDevices = ba.getBondedDevices();
		
		Log.d("mydebug", "get paired");
		for (BluetoothDevice bd : pairedDevices){
//			devices.add(new DeviceInfo( bd.getName(), bd.getAddress()));
		  devices.add(bd);
		}
		return pairedDevices;
	}
	
	static BroadcastReceiver br = new BroadcastReceiver(){
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
	public static void unregister(){
		activity.unregisterReceiver(br);
	}
	
	public static void discoverDevices(){
		devices.clear();
		ba.startDiscovery();
		activity.registerReceiver(br, new IntentFilter(
				BluetoothDevice.ACTION_FOUND));
		
	}
	
	public static Set<BluetoothDevice> getDevices(){
		return devices;
	}
	
	
	public static void accept(){
		try {
		    (new AcceptThread()).start();
		  } catch (Exception ex) {
		    ex.printStackTrace();
		  }
	}
	
	private static class AcceptThread extends Thread {
	    private final BluetoothServerSocket mmServerSocket;
	 
	    public AcceptThread() {
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try {
	            // MY_UUID is the app's UUID string, also used by the client code
	        	Log.d("mydebug", "Accepta");
	            tmp = ba.listenUsingRfcommWithServiceRecord(serviceName, uuid);
	        } catch (IOException e) { 
	        	System.out.println(e.getStackTrace());
	        }
	        mmServerSocket = tmp;
	    }
	 
	    public void run() {
	        BluetoothSocket socket = null;
	        // Keep listening until exception occurs or a socket is returned
	        while (true) {
	            try {
	                socket = mmServerSocket.accept();
	                Log.d("mydebug", "Socket acceptat");
	            } catch (IOException e) {
	                break;
	            }
	            // If a connection was accepted
	            if (socket != null) {
	                // Do work to manage the connection (in a separate thread)
	                Log.d("mydebug", "S-a conectat");
	                try {
	                	mmServerSocket.close();
	                } catch(Exception e){
	                	System.out.println(e.getStackTrace());
	                }
	                break;
	            }
	        }
	    }
	 
	    /** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() {
	        try {
	            mmServerSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	
	
	
	
	
	
	
	public static void connect(BluetoothDevice d) {
	  try {
	    (new Connect(d)).start();
	  } catch (Exception ex) {
	    ex.printStackTrace();
	  }
	}
	
	private static class Connect extends Thread {
		private  BluetoothSocket socket;
		private BluetoothDevice device;
		
		public Connect(final BluetoothDevice device) 
				throws IOException{
			this.device = device;
//			BluetoothSocket temp = device.createRfcommSocketToServiceRecord(uuid);
			BluetoothDevice actual = ba.getRemoteDevice(device.getAddress());
			BluetoothSocket temp = actual.createRfcommSocketToServiceRecord(uuid);
			this.socket = temp;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			ba.cancelDiscovery();
			
			try{
				socket.connect();
			} catch(Exception e){
//				System.out.println(e.getLocalizedMessage());
			  e.printStackTrace();
				try {
					socket.close();
				} catch(Exception ex){
//					System.out.println(ex.getLocalizedMessage());
				  ex.printStackTrace();
				}
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
	
	public static void connectToDevice(BluetoothDevice device) throws IOException{
		Thread connectThread = new Thread(new Bluetooth.Connect(device));
		connectThread.start();
	}
	
	private static class Send implements Runnable{
		BluetoothSocket socket;
		byte[] message;
		
		public Send(BluetoothSocket socket, byte[] message){
			this.socket = socket;
			this.message = message;
			os = null;
			
			try{
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
	
	public static void sendMessage(String message){
		Thread send = new Thread(new Bluetooth.Send(bs, message.getBytes()));
	}
}
