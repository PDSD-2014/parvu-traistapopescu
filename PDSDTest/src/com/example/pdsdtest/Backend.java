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

  static LinkedBlockingDeque<MessageRequest> messages =
    new LinkedBlockingDeque<MessageRequest>();

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
        // Wait for message to send to server
        MessageRequest msg = messages.take();

        out.writeObject(msg);
        out.flush();
      }
    } catch (Exception ex) {
      ex.printStackTrace();
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
          DataRequest request = (DataRequest)in.readObject();

          Log.d("mydebug", "cacat");
          System.out.println("fuuu");
          switch (request.type) {
            case DataRequest.MEETING:
              Intent i = new Intent(activity, Meeting.class);
              Bundle b = new Bundle();

              Log.d("mydebug", "meeting " + ((MeetingRequest)request).users);
              b.putString("users", ((MeetingRequest)request).users);

              // Start meeting activity
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
