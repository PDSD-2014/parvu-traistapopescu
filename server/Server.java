import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.*;
import javax.net.*;

import java.awt.image.*;
import javax.imageio.*;

// import java.nio.file.*;

import com.example.pdsdtest.*;

public class Server extends Thread {
  private ServerSocket serverSocket;
  private ExecutorService pool = Executors.newCachedThreadPool();
  private HashMap<String, ObjectOutputStream> connected = new HashMap<String, ObjectOutputStream>();

  public static void main(String[] args) {
    (new Server()).run();
  }

  public Server() {
    try {
      serverSocket = ServerSocketFactory.getDefault().createServerSocket(10000);

      if (serverSocket == null) {
        System.out.println("Nu e bine\n");
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void run() {
    while (true) {
      try {
        Socket s = serverSocket.accept();

        System.out.println("Am primit ceva ok");
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

    public void run() {
      while (true) {
        try {
          DataRequest request = (DataRequest)in.readObject();

          switch (request.type) {
            case DataRequest.CONNECT: {
              System.out.println("Yes, first message " + request.fbid);
              fbid = fbid;

              connected.put(request.fbid, out);

              break;
            }
            case DataRequest.MESSAGE: {
              MessageRequest msg = (MessageRequest)request;
              System.out.println("Seding message to " + msg.to);

              if (connected.get(msg.to) == null) {
                System.out.println("No one");
              } else {
                ObjectOutputStream sout = connected.get(msg.to);

                sout.writeObject(request);
                sout.flush();
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
                }
              }
              break;
            }
          }
        } catch (IOException ex) {
          System.out.println("asta e " + fbid);
          connected.remove(fbid);

          break;
        } catch (ClassNotFoundException ex) {}
      }
    }
  }
}