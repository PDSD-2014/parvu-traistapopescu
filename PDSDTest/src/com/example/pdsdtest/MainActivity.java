package com.example.pdsdtest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

public class MainActivity extends Activity {
	private Socket socket;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// start Facebook Login
		  Session.openActiveSession(this, true, new Session.StatusCallback() {
	
		    // callback when session changes state
		    @Override
		    public void call(Session session, SessionState state, Exception exception) {
		    	Log.d("mydebug", "aici1");
		    	if (session.isOpened()) {
		    		// make request to the /me API
		    		Log.d("mydebug", "aici2");
		    		Request.newMeRequest(session, new Request.GraphUserCallback() {

		    		  // callback after Graph API response with user object
		    		  @Override
		    		  public void onCompleted(GraphUser user, Response response) {
		    			  Log.d("mydebug", "aici");
		    			  if (user != null) {
		    				  TextView welcome = (TextView) findViewById(R.id.welcome);
		    				  welcome.setText("Hello " + user.getId() + "!");
		    				  
		    				  MainActivity.this.connect(user.getId());
		    			  }
		    		  }
		    		}).executeAsync();
		    	}
		    }
		  });
	}

	private class Connect extends Thread {
		private String fbid;
		
		public Connect(String fbid) {
			this.fbid = fbid;
		}
		
		public void run() {
			try {
				Log.d("mydebug", "pai nu " + fbid);
				socket = new Socket("192.168.2.8", 10000);
				
				Log.d("mydebug", "aici nu prea");
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
		
			    DataRequest request = new DataRequest(DataRequest.CONNECT, fbid);
			    out.writeObject(request);
			    out.flush();
			    
			    out.writeObject(new MessageRequest(fbid, fbid, "ce mai faci?"));
			    out.flush();
			    
			    Log.d("mydebug", "ok");
			    while (true) {
			    	request = (DataRequest)in.readObject();
			    	
			    	switch (request.type) {
			    		case DataRequest.MESSAGE:
			    			Log.d("mydebug", ((MessageRequest)request).msg + " from " + ((MessageRequest)request).fbid);
			    			break;
			    	}
			    }
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	private void connect(String fbid) {
		Log.d("mydebug", "ce naiba");
		(new Connect(fbid)).start();
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
	  Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}

}
