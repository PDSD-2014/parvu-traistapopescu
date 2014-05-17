package com.example.pdsdtest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.facebook.Request;
import com.facebook.Request.GraphUserListCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;

public class MainActivity extends Activity {
	private Socket socket;
	ObjectOutputStream out;
    ObjectInputStream in;
    String fbid;
    HashSet<String> selectedFriends = new HashSet<String>();
	
    GraphUser me;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Backend.activity = this;
		// start Facebook Login
//		Session.openActiveSession(this, true, new Session.StatusCallback() {
//	
//		    // callback when session changes state
//		    @Override
//		    public void call(Session session, SessionState state, Exception exception) {
//		    	Log.d("mydebug", "aici1");
//		    	if (session.isOpened()) {
//		    		// make request to the /me API
//		    		Log.d("mydebug", "aici2");
//		    		Request.newMeRequest(session, new Request.GraphUserCallback() {
//
//		    		  // callback after Graph API response with user object
//		    		  @Override
//		    		  public void onCompleted(GraphUser user, Response response) {
//		    			  Log.d("mydebug", "aici");
//		    			  if (user != null) {
//		    				  me = user;
//		    				  
//		    				  TextView welcome = (TextView)findViewById(R.id.welcome);
//		    				  welcome.setText("Hello " + user.getId() + "!");
//		    				  
//		    				  MainActivity.this.connect(user.getId());
//		    			  }
//		    		  }
//		    		}).executeAsync();
//		    	}
//		    }
//		  });

		  Bluetooth b = new Bluetooth(this);
		  (new Thread() {
		    public void run() {
		      try {
		        Bluetooth.enableBluetooth();
		        
		        Log.d("mydebug", "aici");
		        Bluetooth.discoverDevices();
		        
		        Thread.sleep(5000);
		        Log.d("mydebug", "aici2");
		        for (BluetoothDevice dinfo : Bluetooth.getDevices()) {
              Log.d("mydebug", dinfo.getName());
            }
		       
		        Bluetooth.accept();
		        for (BluetoothDevice dinfo : Bluetooth.getPairedDevices()) {
		          Log.d("mydebug", dinfo.getName());
		          
		          if (dinfo.getName().compareTo("GT-I8190") == 0) {
                Log.d("mydebug", "trying to connect");
                Bluetooth.connect(dinfo);
              }
		        }
		      } catch (Exception ex) {
		        ex.printStackTrace();
		      }
		    }
		  }).start();
	}

	public void logout(View v) {
		if (Session.getActiveSession() != null) {
		    Session.getActiveSession().closeAndClearTokenInformation();
		    Log.d("mydebug", "sa zicem");
		}

		Session.setActiveSession(null);
	}
	
	private class Connect extends Thread {
		private String fbid;
		
		public Connect(String fbid) {
			this.fbid = fbid;
		}
		
		public void run() {
			try {
				Log.d("mydebug", "pai nu " + fbid);
				socket = new Socket("172.18.100.78", 10000);
				
				Log.d("mydebug", "aici nu prea");
				out = new ObjectOutputStream(socket.getOutputStream());
			    in = new ObjectInputStream(socket.getInputStream());
		
			    (new Backend(out, in)).start();
			    
			    DataRequest request = new DataRequest(DataRequest.CONNECT, fbid);
			    out.writeObject(request);
			    out.flush();
			    
//			    out.writeObject(new MessageRequest(fbid, fbid, "ce mai faci?"));
//			    out.flush();
			    
			    Log.d("mydebug", "ok");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public void sendMeeting(View view) {
		try {
			SendMeeting meeting = new SendMeeting(selectedFriends.toString());
			meeting.start();
			meeting.join();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private class SendMeeting extends Thread {
		String friends;
		
		public SendMeeting(String friends) {
			this.friends = friends;
		}
		
		public void run() {
			try {
				out.writeObject(new MeetingRequest(fbid, friends));
				out.flush();
				
				Log.d("mydebug", "am trimis meeting");
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private class MyGraphUserCallback implements GraphUserListCallback {
		ArrayList<User> usersArray = new ArrayList<User>();
		ListView listView = (ListView)findViewById(R.id.listView1);
		
		public MyGraphUserCallback() {
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			
			selectedFriends.add(fbid);
			
        	listView.setOnItemClickListener(new OnItemClickListener() {
        		public void onItemClick(AdapterView<?> parent, View view,
        			    int position, long id) {
        			String friend = usersArray.get(position).id;
        			if (selectedFriends.contains(friend) == true) {
        				selectedFriends.remove(friend);
        			} else {
        				selectedFriends.add(friend);
        				Log.d("mydebug", "am adaugat " + usersArray.get(position).name);
        			}
        		}
        	});
		}
		
		public void onCompleted(List<GraphUser> users,
                Response response) {
			int i = 0;
			  
			Backend.allFriends.put(fbid, me);
        	for (GraphUser u : users) {
        		Log.d("mydebug", u.getId());
        		i++;
        		
        		Backend.allFriends.put(u.getId(), u);
        		
        		usersArray.add(new User(u.getId(), u.getName()));
        		if (i == 10) {
        			break;
        		}
        	}
        	
        	ArrayAdapter<User> adapter = new ArrayAdapter<User>(MainActivity.this, 
        	        android.R.layout.simple_list_item_multiple_choice, usersArray);
        	
        	listView.setAdapter(adapter);
		}
	}
	
	private void connect(String fbid) {
		Log.d("mydebug", "ce naiba");
		
		this.fbid = fbid;
		Backend.fbid = fbid;
		
		Request friendRequest = Request.newMyFriendsRequest(Session.getActiveSession(),
            new MyGraphUserCallback());
		
        Bundle params = new Bundle();
        params.putString("fields", "id, name");
        friendRequest.setParameters(params);
        friendRequest.executeAsync();
             
        try {
        	Connect c = new Connect(fbid);
        	c.start();
		
        	c.join();
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
		
//		(new ServerMessages()).start();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	  super.onActivityResult(requestCode, resultCode, data);
	  // check this
//	  Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}

}
