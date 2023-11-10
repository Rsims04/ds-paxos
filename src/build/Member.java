package build;

/**
 * Member.java
 *
 */

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Member {

  // ANSI Colours for trace
  public static final String ANSI_RESET = "\u001B[0m"; // Reset
  public static final String ANSI_PURPLE = "\u001B[35m"; // PROPOSERS
  public static final String ANSI_YELLOW = "\u001B[33m"; // SEND
  public static final String ANSI_CYAN = "\u001B[36m"; // RECEIVE

  protected String name = "";
  protected String state = "";
  protected Integer delay;
  Behaviour behaviour = new Behaviour();
  Boolean goesOffline = false;
  protected Integer ID = 0;
  protected Socket clientSocket;
  protected BufferedReader in;
  protected PrintWriter out;

  public Member() {}

  /**
   * Try to connect to server at ip, port.
   */
  public void connect(String ip, int port)
    throws UnknownHostException, IOException {
    this.clientSocket = new Socket(ip, port);

    if (delay != null) {
      this.clientSocket.setSoTimeout(delay + 10000);
    } else {
      this.clientSocket.setSoTimeout(10000);
    }
    this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
    this.in =
      new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
  }

  /**
   * Send a message to connection.
   * Applys Delay Behaviour.
   * Returns the response.
   */
  public String sendMsg(String msg) throws IOException {
    if (delay != null) {
      applyBehaviourDelay();
      out.println(msg);
      String res = "";
      String line = in.readLine();
      res = line;
      return res;
    }
    return "";
  }

  /**
   * Send a message to connection.
   * Does Not Apply Delay Behaviour.
   */
  public void sendWithoutDelay(String msg) throws IOException {
    out.println(msg);
  }

  /**
   * Send a message to connection.
   * Applys Delay Behaviour.
   */
  public void send(String msg) throws IOException {
    if (delay != null) {
      applyBehaviourDelay();
      out.println(msg);
    }
  }

  /**
   * Waits a short random amount of time.
   */
  public void randomDelay() {
    int min = 1000;
    int max = 2000;
    try {
      TimeUnit.MILLISECONDS.sleep(
        ThreadLocalRandom.current().nextInt(min, max)
      );
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Waits a short random amount of time.
   */
  public void applyBehaviourDelay() {
    if (delay != null) {
      // Random Delay For Concurrency Avoidance
      randomDelay();
      // System.out.println("Behaviour applied, WAITING: " + delay + "millis");
      try {
        TimeUnit.MILLISECONDS.sleep(delay);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Disconnects and closes connection.
   */
  public void disconnect() throws IOException {
    this.in.close();
    this.out.close();
    this.clientSocket.close();
  }

  /**
   * Continually send server a message to let it know your alive.
   */
  Runnable startHeartBeat = new Runnable() {
    public void run() {
      try {
        send(name + ";hello");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  };

  /**
   * Acceptor
   * Print current name, action, message, state and timestamp.
   */
  public void trace(String action, String msg) {
    Date date = new Date();
    String color = ANSI_CYAN;
    String arrow = " --> ";
    if (action == "recv") {
      arrow = " <-- ";
      color = ANSI_YELLOW;
    }
    System.out.println(
      " ~A-" +
      name +
      color +
      arrow +
      action +
      ": " +
      msg +
      " |State: " +
      state +
      " |t=" +
      new Timestamp(date.getTime()) +
      ANSI_RESET
    );
  }

  /**
   * Proposer
   * Print current name, action, message, state, leader status and timestamp.
   */
  public void trace(String action, String msg, Boolean isLeader) {
    Date date = new Date();
    String color = ANSI_CYAN;
    String arrow = " --> ";
    if (action == "recv") {
      arrow = " <-- ";
      color = ANSI_YELLOW;
    }
    System.out.println(
      ANSI_PURPLE +
      " ~P-" +
      name +
      ANSI_RESET +
      color +
      arrow +
      action +
      ": " +
      msg +
      " |State: " +
      state +
      " |isLeader: " +
      isLeader +
      " |t=" +
      new Timestamp(date.getTime()) +
      ANSI_RESET
    );
  }

  /**
   * Send a request for a unique ID.
   */
  public void getID() {
    try {
      out.println(name + ";ID;" + ID.toString());
      String msg = in.readLine();
      ID = Integer.parseInt(msg);
      trace("send", msg);
    } catch (NumberFormatException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Send a request for the current number of connections.
   */
  public void getMajority() {
    out.println("CONNECTIONS");
  }

  /**
   * Send a request for the current leader status.
   */
  public void checkLeader() {
    try {
      send("IS LEADER?");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Set delay based on behaviour.
   */
  public void setBehaviour(String newBehaviour) {
    delay = behaviour.responseTime.get(newBehaviour);
  }

  /**
   * Placeholder.
   */
  public void start() {
    System.out.println("I don't have a role... I'm just a Member...");
  }

  public static void main(String[] args) throws IOException {
    Member m = null;
    String name = "";
    String profile = "immediate";
    Boolean goesOffline = false;

    /**
     * Takes in a name, a behaviour and if goes offline status.
     * ex~1: ./Member M1
     * ex~2: ./Member M1 immediate
     * ex~3: ./Member M1 small offline
     *
     * No arguments will result in immediate response time.
     */
    if (args.length > 0) {
      name = args[0];
      if (args.length > 1) {
        profile = args[1];
      }
      if (args.length > 2) {
        if (args[2].equals("offline")) goesOffline = true;
      }

      if (
        args[0].equals("M1") || args[0].equals("M2") || args[0].equals("M3")
      ) {
        if (goesOffline == true) {
          m = new Proposer(name, goesOffline);
        } else {
          m = new Proposer(name, goesOffline);
        }
        m.setBehaviour(profile);
      } else {
        m = new Acceptor(name);
        m.setBehaviour(profile);
      }
      m.start();
    } else {
      System.out.println("Please Input Name.");
    }

    System.exit(0);
  }
}

/**
 * Class for assigning delay in response times.
 * Options: immediate, small, large, none.
 */
class Behaviour {

  HashMap<String, Integer> responseTime = new HashMap<String, Integer>();

  Behaviour() {
    responseTime.put("immediate", 0); // immediate response
    responseTime.put("small", 3000); // small delay
    responseTime.put("large", 10000); // large delay
    responseTime.put("none", null); // no response
  }
}
