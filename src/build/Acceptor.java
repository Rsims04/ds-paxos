package build;

/**
 * Acceptor.java
 *
 */

import java.io.IOException;
import java.net.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Acceptor extends Member {

  // LEADER
  Boolean isLeader = false;
  Boolean leaderExists = false;

  Integer connections = 0;
  Integer majority = 0;

  // PREPARE
  Integer cnt = 1;
  Double max_ID = 0.0;
  String proposedValue;

  // ACCEPT
  Boolean proposalAccepted = false;
  String acceptedValue;
  String acceptedID;
  String acceptValue;

  // CONSENSUS
  Boolean consensusReached = false;

  // MESSAGING
  String currentMsg = "";

  ArrayList<String> receivedPromises2 = new ArrayList<String>();

  Queue<String> msgs = new ArrayDeque<String>();

  /**
   * Initiate Acceptor with:
   * Name, Unique ID and State
   */
  Acceptor(String name) {
    this.name = name;
    this.ID = ThreadLocalRandom.current().nextInt(4, 9 + 1);
    this.state = "LEADER";
  }

  /**
   * Send a promise for a proposal.
   * If a proposal is already accepted sends that accepted ID.
   */
  public void PROMISE() {
    if (proposalAccepted) {
      try {
        currentMsg =
          name + ";PROMISE;" + ID + ";" + max_ID + ";" + acceptedValue;
        send(currentMsg);
        trace("send", currentMsg);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      try {
        currentMsg = name + ";PROMISE;" + max_ID;
        send(currentMsg);
        trace("send", currentMsg);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Sends an Accept message for a received proposal.
   * If the current max_ID is equal to its own.
   */
  public void ACCEPT() {
    if (acceptValue != null) {
      String split[] = acceptValue.split(";");

      Double acceptID = Double.parseDouble(split[2]);
      String VALUE = split[3];

      if (acceptID.equals(max_ID)) {
        proposalAccepted = true;
        acceptedID = acceptID.toString();
        acceptedValue = VALUE;
        try {
          currentMsg = name + ";ACCEPT;" + acceptID + ";" + VALUE;
          send(currentMsg);
          trace("send", currentMsg);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Sets the current connection size and majority of those connections.
   */
  public void setConnectionsAndMajority(String msg) {
    Integer connections = Integer.parseInt(msg.split(";")[1]);
    this.connections = connections;
    this.majority = (connections / 2) + 1;
  }

  /**
   * The greater of the members ID and the received prepare ID,
   * will become the new proposed value.
   */
  public void handlePrepare(String msg) {
    String otherValue = msg.split(";")[0];
    Double otherID = Double.parseDouble(msg.split(";")[2]);
    if (otherID > max_ID) {
      max_ID = otherID;
      proposedValue = otherValue;
    }
  }

  /**
   * Store a value for comparison in Accept.
   */
  public void handlePropose(String msg) {
    acceptValue = msg;
  }

  /**
   * The main listening loop.
   * Exit on Consensus Reached.
   */
  public void start() {
    try {
      connect("localhost", 4567);
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Start Heartbeat
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    executor.scheduleAtFixedRate(startHeartBeat, 0, 3, TimeUnit.SECONDS);

    getID();

    System.out.println("Hi I'm " + name + ", an Acceptor! My ID is: " + ID);

    Integer retryCount = 0;

    while (!consensusReached) {
      String line;

      try {
        while ((line = in.readLine()) != null) {
          msgs.add(line);

          // Get and Set current connections.
          if (line.contains("CONNECTIONS")) {
            setConnectionsAndMajority(line);
            continue;
          } else {
            trace("recv", line);
          }

          if (line.equals("EXIT")) {
            break;
          }

          // FIND LEADER
          if (!leaderExists) {
            if (line.contains("LEADER FOUND")) {
              leaderExists = true;
              if (state == "LEADER") {
                state = "PROMISE";
              }

              continue;
            }
            checkLeader();
          }

          if (line.equals("NO LEADER")) {
            leaderExists = false;
          }

          if (leaderExists) {
            // PREPARE
            if (line.contains("PREPARE")) {
              handlePrepare(line);
              if (state == "PROMISE") {
                PROMISE();
                state = "ACCEPT";
              }
              continue;
            }

            // ACCEPT
            if (line.contains("PROPOSE")) {
              handlePropose(line);
              ACCEPT();
              continue;
            }
          }

          // ACKNOWLEDGE CONSENSUS
          if (line.equals("CONSENSUS")) {
            consensusReached = true;
            currentMsg = name + ";ACKCONSENSUS;" + acceptedValue;
            sendWithoutDelay(currentMsg);
            trace("send", currentMsg);
            continue;
          }
        }
      } catch (SocketTimeoutException e) {
        // Retry on readline() timeout.
        try {
          if (retryCount >= 3) {
            checkLeader();
            retryCount = 0;
          }
          if (!currentMsg.equals("")) {
            System.out.println(
              name + " -TIMEOUT- retrying send..." + currentMsg
            );
            send(currentMsg);
            trace("send", currentMsg);
          }
          retryCount++;
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    // Let server know you are done.
    try {
      sendMsg("DONE");
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    // Print out result.
    System.out.println(name + " --> Consensus Reached: " + acceptedValue);
    try {
      disconnect();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
