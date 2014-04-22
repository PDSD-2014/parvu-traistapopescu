package com.example.bluetoothofficial;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
	private static HashSet<BluetoothDevice> pairedDevices;
	private static HashSet<DeviceInfo> devices;
	private static final String uuidString = "2e87ecc1-5e85-42f1-9955-5788c93598ec";
	private static final UUID uuid = UUID.fromString(uuidString);
	private static BluetoothSocket bs;
	private static OutputStream os;
	
	public Bluetooth(final Activity activity){
		Bluetooth.activity = activity;
		ba = BluetoothAdapter.getDefaultAdapter();
		pairedDevices = new HashSet<BluetoothDevice>();
		devices = new HashSet<DeviceInfo>();
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
	
	public static HashSet<BluetoothDevice> getPairedDevices(){
		pairedDevices = (HashSet<BluetoothDevice>) ba.getBondedDevices();
		
		for( BluetoothDevice bd: pairedDevices ){
			devices.add(new DeviceInfo( bd.getName(), bd.getAddress()));
		}
		return pairedDevices;
	}
	
	static BroadcastReceiver br = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String a = intent.getAction();
			// Discover a device
			if(BluetoothDevice.ACTION_FOUND.equals(a)){
				BluetoothDevice dev = intent.getParcelableExtra(
						BluetoothDevice.EXTRA_DEVICE);
				devices.add(new DeviceInfo(dev.getName(), dev.getAddress()));
			}
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
	
	public static HashSet<DeviceInfo> getDevices(){
		return devices;
	}
	
	private static class Connect implements Runnable {
		private  BluetoothSocket socket;
		private BluetoothDevice device;
		
		public Connect(final BluetoothDevice device, BluetoothSocket socket) 
				throws IOException{
			this.device = device;
			this.socket = socket;
			BluetoothSocket temp = null;
			temp = device.createRfcommSocketToServiceRecord(uuid);
			this.socket = temp;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			ba.cancelDiscovery();
			
			try{
				socket.connect();
			} catch(Exception e){
				System.out.println(e.getLocalizedMessage());
				try {
					socket.close();
				} catch(Exception ex){
					System.out.println(ex.getLocalizedMessage());
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
		Thread connectThread = new Thread(new Bluetooth.Connect(device, bs));
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
