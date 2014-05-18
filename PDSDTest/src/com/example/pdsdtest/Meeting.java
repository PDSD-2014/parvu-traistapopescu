package com.example.pdsdtest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.model.GraphUser;

public class Meeting extends Activity {
	ArrayList<User> usersArray = new ArrayList<User>();
	HashMap<String, User> meetingUsers = new HashMap<String, User>();
	
	ArrayAdapter<User> adapter;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.meeting);
	
	    Backend.meeting = this;
	    Bluetooth.meeting = this;
	    
	    TextView v = (TextView)findViewById(R.id.meetingName);
	    
	    v.setText("heeeei");
	    
	    Bundle b = getIntent().getExtras();
	    
	    String users = b.getString("users");
	    
	    StringTokenizer st = new StringTokenizer(users, " [,]");
	    
	    for (; st.hasMoreTokens(); ) {
	    	GraphUser u = Backend.allFriends.get(st.nextToken());
	    	
	    	if (u != null) {
	    	  User curUser = new User(u.getId(), u.getName());
	    	  meetingUsers.put(u.getId(), curUser);
	    	  
	    		usersArray.add(curUser);
	    	}
	    }
	    
	    adapter = new MyListAdapter(Meeting.this, 
    	        R.layout.item_layout, usersArray);
    	
	    ListView listView = (ListView)findViewById(R.id.meetingFriendList);
    	listView.setAdapter(adapter);
	    // TODO Auto-generated method stub
	}
	
	private class MyListAdapter extends ArrayAdapter<User> {
		private Context context;
	    private int resource;
	    private ArrayList<User> content;
	    
		public MyListAdapter(Context context, int resource, ArrayList<User> content) {
			super(context, resource, content);
			
			this.context = context;
			this.resource = resource;
			this.content = content;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView == null) {
		        LayoutInflater layoutinflator = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			    view = layoutinflator.inflate(resource, null);
			}
			else
			    view = convertView;
			
			TextView name = (TextView)view.findViewById(R.id.item_name), status = (TextView)view.findViewById(R.id.item_status);
			
//			if (v == null) {
//				System.out.println("da");
//			}
			
			name.setText(content.get(position).name);
			status.setText(content.get(position).status);
			
			return view;
		}
	}
	
	public void deleteEntry(String fbid) {
	  Log.d("mydebug", "in meeting delete " + fbid);
	  boolean ok = usersArray.remove(meetingUsers.get(fbid));
	  Log.d("mydebug", "ok " + ok);
	  
	  adapter.notifyDataSetChanged();
	}
	
	public void addMessage(String fbid, String msg) {
		
		Log.d("mydebug", "aici");
		for (User u : usersArray) {
			if (u.id.compareTo(fbid) == 0) {
				u.status = msg;
			}
		}
//		Log.d("mydebug", "modificare");
		
		adapter.notifyDataSetChanged();
	}
	
	public void sendMessage(View view) {
		EditText t = (EditText)findViewById(R.id.messageInput);
		
		for (User u : usersArray) {
			MessageRequest r = new MessageRequest(Backend.fbid, u.id, t.getText().toString());
			Backend.sendMessage(r);
		}
		
		t.setText("");
	}

}
