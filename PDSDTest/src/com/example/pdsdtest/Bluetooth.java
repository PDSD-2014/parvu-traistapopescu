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

public class Bluetooth extends Thread {
	private Activity activity;
	private BluetoothAdapter ba;
	private Set<BluetoothDevice> pairedDevices;
	private Set<BluetoothDevice> devices;
	private final String uuidString = "2e87ecc1-5e85-42f1-9955-5788c93598ec";
	private final UUID uuid = UUID.fromString(uuidString);
	private BluetoothSocket bs;
	private OutputStream os;
	
	public static Meeting meeting = null;
	
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
//			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//			activity.startActivityForResult(intent, 1);
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
	
	public void accept(String fbid) {
		try {
	    (new AcceptThread(fbid)).start();
	  } catch (Exception ex) {
	    ex.printStackTrace();
	  }
	}
	
	private class AcceptThread extends Thread {
    private BluetoothServerSocket mmServerSocket;
    private final String fbid;
    public AcceptThread(String fbid) {
      this.fbid = fbid;
      try {
        // MY_UUID is the app's UUID string, also used by the client code
      	Log.d("mydebug", "Accepta");
        mmServerSocket = ba.listenUsingRfcommWithServiceRecord("pdsdapp", uuid);
      } catch (IOException e) { 
        System.out.println(e.getStackTrace());
      }
    }
	 
    public void run() {
      while (true) {
        try {
          if (ba.isEnabled() == false) {
            Thread.sleep(5000);
          }
          
          final BluetoothSocket socket = mmServerSocket.accept();
          Log.d("mydebug", "Socket acceptat");
          
          (new Thread() {
            public void run() {
              try {
                OutputStream os = socket.getOutputStream();
                os.write(fbid.getBytes());
                os.flush();
              } catch (IOException ex) {}
            }
          }).start();
        } catch (Exception e) {}
      }
    }
	}
	
	public void run() {
	  try {
  	  while (true) {
  	    if (ba.isEnabled()) {
      	  for (BluetoothDevice d : getPairedDevices()) {
      	    try {
      	      String user = connect(d);
  //    	      Log.d("mydebug", "connect to " + d.getName());
      	      Log.d("mydebug", "deleting " + user);
      	      if (meeting != null) {
      	        meeting.deleteEntry(user);
      	        Log.d("mydebug", "deleting " + user);
      	      }
      	    } catch (Exception ex) {
      	      Log.d("mydebug", "could not connect to " + d.getName());
      	    }
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
}
