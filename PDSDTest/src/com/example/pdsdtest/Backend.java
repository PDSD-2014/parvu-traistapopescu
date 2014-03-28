package com.example.pdsdtest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.model.GraphUser;

public class Backend extends Thread {
	static HashMap<String, GraphUser> allFriends = new HashMap<String, GraphUser>();
	public static Lock lock = new ReentrantLock();
	public static Condition sendMsg = lock.newCondition();
	static DataRequest data;
	
	ObjectOutputStream out;
    ObjectInputStream in;
    
    public static Activity activity;
    public static Meeting meeting;
    
    static String fbid;

    static LinkedBlockingDeque<MessageRequest> messages = new LinkedBlockingDeque<MessageRequest>();
    
	public Backend(ObjectOutputStream out_, ObjectInputStream in_) {
		out = out_;
		in = in_;
		
		(new ServerMessages()).start();
	}
	
	static void sendMessage(MessageRequest msg) {
		messages.add(msg);
	}
		
	public void run() {
		try {
			while (true) {
				Log.d("mydebug", "astept");
				
				MessageRequest msg = messages.take();
				
				out.writeObject(msg);
				out.flush();
			}
		} catch (Exception ex) {
			System.out.println("Probleme!");
		}
	}
	
	private class UIRunnable extends Thread {
		String fbid, data;
		
		public UIRunnable(String fbid_, String data_) {
			fbid = fbid_;
			data = data_;
		}
		
		public void run() {
			meeting.addMessage(fbid, data);
		}
	}
	
	private class ServerMessages extends Thread {
		public void run() {
			try {
				while (true) {
					Log.d("mydebug", "waiting!");
			    	DataRequest request = (DataRequest)in.readObject();
			    	
			    	switch (request.type) {
			    		case DataRequest.MEETING:
			    			Log.d("mydebug", "meeting " + ((MeetingRequest)request).users);
			    			
			    			Intent i = new Intent(activity, Meeting.class);
			    			Bundle b = new Bundle();
			    			
			    			b.putString("users", ((MeetingRequest)request).users);
			    			
			    			i.putExtras(b);
			    			activity.startActivity(i);
			    			
			    			break;
			    		case DataRequest.MESSAGE:
			    			Log.d("mydebug", "message " + ((MessageRequest)request).msg);
			    			activity.runOnUiThread(new UIRunnable(((MessageRequest)request).fbid,
			    					((MessageRequest)request).msg));
			    					
			    			break;
			    	}
			    }
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (ClassNotFoundException ex) {
				ex.printStackTrace();
			}
		}
	}
}