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
  private Set<BluetoothDevice> pairedDevices = null;
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
  }

  public void disableBluetooth(){
    ba.disable();
  }

  public Set<BluetoothDevice> getPairedDevices() {
    pairedDevices = ba.getBondedDevices();

    return pairedDevices;
  }

  BroadcastReceiver br = new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.d("mydebug", "received");
      String a = intent.getAction();
      BluetoothDevice dev = intent.getParcelableExtra(
          BluetoothDevice.EXTRA_DEVICE);
      devices.add(dev);
    }
  };

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
    private boolean isEnabled = false;
    public AcceptThread(String fbid) {
      this.fbid = fbid;
    }

    public void run() {
      while (true) {
        try {
          Log.d("mydebug", "trying");
          if (ba.isEnabled() == false) {
            isEnabled = false;
            // Wait for bluetooth to be enabled - for testing purposes
            Thread.sleep(5000);
          } else {
            if (isEnabled == false) {
              // init a server socket
              isEnabled = true;
              mmServerSocket = ba.listenUsingRfcommWithServiceRecord("pdsdapp", uuid);
            }
          }

          final BluetoothSocket socket = mmServerSocket.accept();
          Log.d("mydebug", "Socket accepted");

          (new Thread() {
            public void run() {
              try {
                // Write fbid to the device that initiated the connection
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
              // try to connect to a paired device
              final String user = connect(d);
              Log.d("mydebug", "deleting " + user);
              if (meeting != null) {
                // delete the corresponding entry
                activity.runOnUiThread(new Thread() {
                  public void run() {
                    meeting.deleteEntry(user);
                  }
                });
              }
            } catch (Exception ex) {
              Log.d("mydebug", "could not connect to " + d.getName());
            }
          }
        }

        // wait until next scan
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

    // connect and read message
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
