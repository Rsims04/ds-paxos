package build;

/**
 * Server.java
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {

  Integer connectionCount = 0;
  Stack<Integer> connectionIDs = new Stack<Integer>();
  ServerSocket serverSocket;

  Socket memberSocket;
  String memberSocketName;
  BufferedReader in;
  PrintWriter out;

  Boolean roundStarted = false;
  Boolean roundStop = false;

  ArrayList<Connection> connections = new ArrayList<Connection>();

  Runnable checkConnections = new Runnable() {
    public void run() {
      Integer size = connections.size();
      System.out.println("\n---\nconnections: " + connectionCount);
      for (int i = 0; i < connections.size(); i++) {
        if (connections.get(i).t.isAlive()) {
          System.out.println(
            connections.get(i).name +
            ": " +
            connections.get(i).ID +
            ": is alive! : leader: " +
            connections.get(i).t.getIsLeader()
          );
        } else {
          System.out.println(connections.get(i).ID + ": Not Alive");
          connectionIDs.push(connections.get(i).ID);
          connections.remove(connections.get(i));
          connectionCount--;
        }
      }
      System.out.println("---\n\n");
      if (size > connectionCount || size < connectionCount) {
        updateConnections();
      }
      if (connectionCount <= 0) {
        roundStop = true;
      }
    }
  };

  public void acceptConnection() throws IOException {
    this.memberSocket = this.serverSocket.accept();
    this.in =
      new BufferedReader(new InputStreamReader(memberSocket.getInputStream()));
    this.out = new PrintWriter(memberSocket.getOutputStream(), true);
  }

  public void updateConnections() {
    for (Connection connection : connections) {
      connection.t.setConnections(connections);
    }
  }

  public void getID() throws IOException {
    String msg;
    try {
      msg = in.readLine();
      if (!msg.contains("ID")) {
        msg = in.readLine();
      }
      String[] details = msg.split(";");
      this.memberSocketName = details[0];
      System.out.println(
        "Assigning " +
        details[0] +
        " ID: " +
        details[2] +
        " " +
        (connectionCount + 1)
      );
      out.println(details[2] + (connectionCount + 1));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void start(Integer port) throws IOException {
    this.memberSocket = null;
    this.serverSocket = new ServerSocket(port);
    System.out.println("Server listening on port: " + port);

    while (true) {
      if (!connectionIDs.empty()) {
        try {
          if (roundStarted && roundStop && connectionCount <= 0) {
            break;
          }
          this.acceptConnection();

          Integer threadID = connectionIDs.pop();
          this.getID();

          ConnectionThread t = new ConnectionThread(
            memberSocketName,
            threadID,
            in,
            out
          );
          connections.add(
            new Connection(memberSocket, memberSocketName, t, threadID)
          );
          connectionCount = connections.size();
          t.start();
          updateConnections();
          roundStarted = true;
        } catch (IOException e) {
          memberSocket.close();
          System.err.println("IO server failure.");
          // e.printStackTrace();
        }
      }
    }
    for (Connection connection : connections) {
      connection.memberSocket.close();
    }
  }

  public static void main(String[] args) throws IOException {
    // Default port
    int port = 4567;
    Server server = new Server();

    // Monitor Connections Every 3 seconds
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    executor.scheduleAtFixedRate(
      server.checkConnections,
      0,
      3,
      TimeUnit.SECONDS
    );
    for (int i = 9; i > 0; i--) {
      server.connectionIDs.add(i);
    }
    // Start server
    server.start(port);
  }
}

class Connection {

  Socket memberSocket;
  String name;
  ConnectionThread t;
  Integer ID;

  Connection(Socket memberSocket, String name, ConnectionThread t, Integer ID) {
    this.memberSocket = memberSocket;
    this.name = name;
    this.t = t;
    this.ID = ID;
  }
}

class ConnectionThread extends Thread {

  Integer ID;
  String name;

  BufferedReader in;
  PrintWriter out;

  String msg;
  Queue<String> msgs = new ArrayDeque<String>();
  Boolean hasMsg = false;

  // Leader
  Boolean isLeader = false;
  Connection leader = null;

  ArrayList<Connection> connections = new ArrayList<Connection>();
  Integer majority;

  ConnectionThread(
    String name,
    Integer ID,
    BufferedReader in,
    PrintWriter out
  ) {
    this.name = name;
    this.ID = ID;
    this.in = in;
    this.out = out;
  }

  public Integer getConnections() {
    return connections.size();
  }

  public void sendAll(String msg) {
    for (Connection connection : connections) {
      if (connection.ID != ID) {
        System.out.println(name + " --> " + msg + " --> " + connection.name);
        connection.t.out.println(msg);
      }
    }
    System.out.println(msg + " forwarded.");
  }

  public synchronized void sendLeader(String msg) {
    if (this.leader != null) {
      System.out.println(name + " --> " + msg + " --> leader: " + leader.name);
      leader.t.out.println(msg);
      // } else {
      //   sendAll("NO LEADER");
      // }
    }
  }

  public Boolean getIsLeader() {
    return isLeader;
  }

  public synchronized void setLeader(String leaderName, String leaderID) {
    for (Connection connection : connections) {
      connection.t.isLeader = false;
      if (connection.t.ID.equals(this.ID)) {
        connection.t.leader = connection;
        this.isLeader = true;
        sendAll(leaderName + ";LEADER FOUND;" + leaderID);
        out.println(leaderName + ";LEADER FOUND;" + leaderID);
      }
    }
  }

  public void setConnections(ArrayList<Connection> a) {
    connections = a;
    out.println("CONNECTIONS;" + connections.size());
  }

  public void getLeader() {
    Boolean leaderFound = false;
    for (Connection connection : connections) {
      if (connection.t.isLeader) {
        this.leader = connection;
        leaderFound = true;
        break;
      }
    }
    // if (!leaderFound) {
    //   out.println("NO LEADER");
    // } else {
    //   out.println(this.leader.name + ";LEADER FOUND");
    //   System.out.println(name + " Leader found.");
    //   // for (Connection connection : connections) {
    //   //   System.out.println(
    //   //     "connection" + connection.t.ID + connection.t.isLeader
    //   //   );
    //   // }
    // }
    if (!leaderFound) {
      out.println("NO LEADER");
    }
  }

  @Override
  public synchronized void run() {
    String line;

    try {
      while (
        (line = in.readLine()) != null &&
        !Thread.currentThread().isInterrupted()
      ) {
        if (line.equals("DONE")) {
          System.out.print(name + ": DONE.");
          out.println("OK");
          break;
        }

        if (!msgs.isEmpty()) {
          System.out.println(name + " messages: " + msgs);
          String message = msgs.poll();

          if (message.contains("EXIT")) {
            sendAll(message);
          }

          if (message.contains("ACKCONSENSUS")) {
            getLeader();
            sendLeader(message);
          }

          if (message.contains("PREPARE")) {
            sendAll(message);
          }

          if (message.contains("PROMISE")) {
            getLeader();
            sendLeader(message);
          }

          if (message.contains("PROPOSE")) {
            sendAll(message);
          }

          if (message.contains("ACCEPT")) {
            getLeader();
            sendLeader(message);
          }

          if (message.equals("CONSENSUS")) {
            sendAll(message);
          }

          if (message.contains("VOTELEADER")) {
            sendAll(message);
          }

          if (message.contains("I AM THE LEADER")) {
            String split[] = message.split(";");
            String leaderName = split[0];
            String leaderID = split[2];
            System.out.println("--- " + ID + " is the new leader. ---");
            setLeader(leaderName, leaderID);
          }

          if (message.contains("ACKLEADER")) {
            getLeader();
            sendLeader(message);
          }

          // if (message.equals("LEADER FOUND")) {
          //   getLeader();
          // }

          if (message.equals("IS LEADER?")) {
            getLeader();
          }
          if (message.equals("CONNECTIONS")) {
            out.println("CONNECTIONS;" + getConnections());
          }
        }
        if (line.contains("hello")) {
          continue;
        }
        msgs.add(line);
      }
      this.interrupt();
    } catch (IOException e) {
      System.out.println(name + " thread stopped.");
      this.interrupt();
    }
    try {
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    out.close();
    System.out.println(name + " thread stopped.");
  }
}
