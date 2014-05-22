import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.*;
import javax.net.*;

import java.awt.image.*;
import javax.imageio.*;

import com.example.pdsdtest.*;

public class Server extends Thread {
  private ServerSocket serverSocket;
  private ExecutorService pool = Executors.newCachedThreadPool();
  private HashMap<String, ObjectOutputStream> connected = new HashMap<String, ObjectOutputStream>();
  private HashMap<String, LinkedList<DataRequest>> pendingMessages = new HashMap<>();

  public static void main(String[] args) {
    (new Server()).run();
  }

  public Server() {
    try {
      serverSocket = ServerSocketFactory.getDefault().createServerSocket(10000);

      if (serverSocket == null) {
        System.out.println("Bad socket!");
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void run() {
    while (true) {
      try {
        Socket s = serverSocket.accept();

        System.out.println("New connection...");
        pool.execute(new ClientThread(s));
      } catch (IOException ex) {}
    }
  }

  private class ClientThread extends Thread {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private String fbid;

    public ClientThread(Socket s) {
      socket = s;

      try {
        out = new ObjectOutputStream(s.getOutputStream());
        in = new ObjectInputStream(s.getInputStream());
      } catch (IOException ex) {}
    }

    private void addToWaitQueue(DataRequest msg, String user) {      
      if (pendingMessages.get(user) == null) {
        pendingMessages.put(user, new LinkedList<DataRequest>());
      }
      pendingMessages.get(user).add(msg);
    }

    public void run() {
      while (true) {
        try {
          DataRequest request = (DataRequest)in.readObject();

          switch (request.type) {
            case DataRequest.CONNECT: {
              System.out.println("Connected " + request.fbid);
              fbid = request.fbid;

              connected.put(request.fbid, out);

              if (pendingMessages.get(fbid) != null) {
                LinkedList<DataRequest> curList = pendingMessages.get(fbid);

                while (curList.size() > 0) {
                  try {
                    DataRequest curMessage = curList.peek();
                    out.writeObject(curMessage);
                    out.flush();

                    System.out.println("Buffered send to " + fbid);

                    curList.poll();
                  } catch (Exception ex) {
                    break;
                  }
                }
              }
              break;
            }
            case DataRequest.MESSAGE: {
              MessageRequest msg = (MessageRequest)request;
              System.out.println("Seding message to " + msg.to);

              if (connected.get(msg.to) != null) {
                ObjectOutputStream sout = connected.get(msg.to);

                sout.writeObject(request);
                sout.flush();
              } else {
                addToWaitQueue(msg, msg.to);
              }

              break;
            }
            case DataRequest.MEETING: {
              MeetingRequest meeting = (MeetingRequest)request;
              System.out.println("Meeting from " + meeting.users);

              StringTokenizer st = new StringTokenizer(meeting.users, " [,]");

              for (; st.hasMoreTokens(); ) {
                String user = st.nextToken();

                if (connected.get(user) != null) {
                  ObjectOutputStream sout = connected.get(user);

                  sout.writeObject(meeting);
                  sout.flush();
                } else {
                  addToWaitQueue(meeting, user);
                }
              }
              break;
            }
          }
        } catch (IOException ex) {
          System.out.println("Logout " + fbid);
          connected.remove(fbid);

          break;
        } catch (ClassNotFoundException ex) {}
      }
    }
  }
}