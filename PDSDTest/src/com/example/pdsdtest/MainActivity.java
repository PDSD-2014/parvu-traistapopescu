package com.example.pdsdtest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Request.GraphUserListCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

public class MainActivity extends Activity {
  private Socket socket;
  ObjectOutputStream out;
  ObjectInputStream in;
  String fbid;
  HashSet<String> selectedFriends = new HashSet<String>();
  ArrayList<User> usersArray = new ArrayList<User>();

  GraphUser me;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    Backend.activity = this;
    // start Facebook Login
    Session.openActiveSession(this, true, new Session.StatusCallback() {

      // callback when session changes state
      @Override
      public void call(Session session, SessionState state, Exception exception) {
        if (session.isOpened()) {
          // make request to the /me API
          Request.newMeRequest(session, new Request.GraphUserCallback() {

            // callback after Graph API response with user object
            @Override
            public void onCompleted(GraphUser user, Response response) {
              if (user != null) {
                me = user;

                TextView welcome = (TextView)findViewById(R.id.welcome);
                welcome.setText("Hello " + user.getName() + "!");

                connect(me.getId());
              }
            }
          }).executeAsync();
        }
      }
    });
  }

  public void logout(View v) {
    if (Session.getActiveSession() != null) {
      Session.getActiveSession().closeAndClearTokenInformation();
    }

    Session.setActiveSession(null);
  }

  private class Connect extends Thread {
    final private String IP = "169.254.239.214";
    final private int PORT = 10000;
    private String fbid;

    public Connect(String fbid) {
      this.fbid = fbid;

      // start accepting and probing for devices
      final Bluetooth b = new Bluetooth(MainActivity.this);
      b.accept(fbid);

      b.start();
    }

    public void run() {
      try {
        socket = new Socket(IP, PORT);

        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        (new Backend(out, in)).start();

        // send connect request to server
        DataRequest request = new DataRequest(DataRequest.CONNECT, fbid);
        out.writeObject(request);
        out.flush();
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
        // send meeting request to server
        out.writeObject(new MeetingRequest(fbid, friends));
        out.flush();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }

  private class MyGraphUserCallback implements GraphUserListCallback {
    ListView listView = (ListView)findViewById(R.id.listView1);

    public MyGraphUserCallback() {
      listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

      selectedFriends.add(fbid);

      listView.setOnItemClickListener(new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view,
          int position, long id) {
          String friend = usersArray.get(position).id;

          // Add selected friends to list for meeting creation
          if (selectedFriends.contains(friend) == true) {
            selectedFriends.remove(friend);
          } else {
            selectedFriends.add(friend);
          }
        }
      });
    }

    public void onCompleted(List<GraphUser> users,
        Response response) {
      // get all Facebook friends
      for (GraphUser u : users) {
        Backend.allFriends.put(u.getId(), u);

        usersArray.add(new User(u.getId(), u.getName()));
      }

      // sort them and create list to show
      Collections.sort(usersArray);

      ArrayAdapter<User> adapter = new ArrayAdapter<User>(MainActivity.this,
          android.R.layout.simple_list_item_multiple_choice, usersArray);

      listView.setAdapter(adapter);
      try {
        Connect c = new Connect(fbid);
        c.start();

        c.join();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  private void connect(String fbid) {
    this.fbid = fbid;
    Backend.fbid = fbid;

    // Request all friends from Facebook
    Request friendRequest = Request.newMyFriendsRequest(Session.getActiveSession(),
        new MyGraphUserCallback());

    Bundle params = new Bundle();
    params.putString("fields", "id, name");
    friendRequest.setParameters(params);
    friendRequest.executeAsync();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
  }

}
