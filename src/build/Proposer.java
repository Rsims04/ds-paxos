package build;

/**
 * Proposer.java
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

public class Proposer extends Member {

  // LEADER
  Boolean isLeader = false;
  Boolean leaderExists = false;

  // PREPARE
  Integer cnt = 1;
  Double max_ID = 0.0;

  // PROPOSE
  String proposedValue;
  Boolean goesOffline;

  // ACCEPT
  Boolean proposalAccepted = false;
  String acceptedValue;
  String acceptedID;
  String acceptValue;

  // CONSENSUS
  Boolean consensusReached = false;
  Boolean done = false;

  // MESSAGING
  String currentMsg = "";

  // CONNECTION
  Integer connections = 0;
  Integer majority = 0;
  Boolean online = true;
  String internetSpeed = "Fast";

  Queue<String> msgs = new ArrayDeque<String>();

  ArrayList<Integer> receivedVotes = new ArrayList<Integer>();
  ArrayList<String> receivedPromises = new ArrayList<String>();
  ArrayList<String> receivedAccepts = new ArrayList<String>();
  ArrayList<String> receivedConsensusAcknowledgements = new ArrayList<String>();

  /**
   * Initiate Proposer with:
   * Name, Unique ID, State, Proposed Value and Going Offline status
   */
  Proposer(String name, Boolean goesOffline) {
    this.name = name;
    this.ID = ThreadLocalRandom.current().nextInt(1, 3 + 1);
    this.state = "LEADER";
    this.proposedValue = name;
    this.goesOffline = goesOffline;
    if (goesOffline == true) {
      ID = 300;
    }
  }

  /**
   * !NOT IMPLEMENTED!
   * Sets Internet Speed to determine delays in message sending.
   */
  Runnable checkInternet = new Runnable() {
    public void run() {
      switch (name) {
        case "M1":
          internetSpeed = "Fast";
          break;
        case "M2":
          internetSpeed = "Slow";
          break;
        case "M3":
          // Average
          internetSpeed = "Dead";
          break;
        default:
          break;
      }

      switch (internetSpeed) {
        case "Fast":
          delay = 0;
          break;
        case "Average":
          delay = 3000;
        case "Slow":
          delay = 10000;
        case "Dead":
          delay = null;
        default:
          break;
      }
    }
  };

  /**
   * Sends a Vote For a New Leader.
   */
  public void voteLeader() {
    try {
      currentMsg = name + ";VOTELEADER;" + ID;
      sendWithoutDelay(currentMsg);
      trace("send", currentMsg, isLeader);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Sends a Prepare message,
   * With an ever-incrementing ID plus current time stamp.
   */
  public void PREPARE() {
    getMajority();
    String prepareID = (cnt++).toString() + "." + System.nanoTime();
    currentMsg = name + ";PREPARE;" + prepareID.toString();
    this.max_ID = Double.parseDouble(prepareID);

    try {
      send(currentMsg);
      trace("send", currentMsg, isLeader);
    } catch (IOException e) {
      e.printStackTrace();
    }
    out.flush();
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
        trace("send", currentMsg, isLeader);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      try {
        currentMsg = name + ";PROMISE;" + max_ID;
        send(currentMsg);
        trace("send", currentMsg, isLeader);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * The proposer now checks to see if it can use its proposal.
   * Or if it has to use the highest-numbered one it received from among all responses.
   * If the user is set to go offline it will happen here also.
   */
  public void PROPOSE() {
    getMajority();
    System.out.println("PROMISES: " + receivedPromises);

    if (receivedPromises.size() >= majority) {
      proposedValue = name;
      for (String promise : receivedPromises) {
        String split[] = promise.split(";");
        Double acceptedID = Double.parseDouble(promise.split(";")[2]);

        if (acceptedID > max_ID) {
          max_ID = acceptedID;
          proposedValue = split[3];
        }
      }
      getMajority();

      try {
        currentMsg = name + ";PROPOSE;" + max_ID + ";" + proposedValue;
        send(currentMsg);
        trace("send", currentMsg, isLeader);
        acceptedValue = proposedValue;
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else if (!receivedPromises.isEmpty()) {
      if (acceptedValue == null) {
        PREPARE();
      } else {
        try {
          currentMsg = name + ";PROPOSE;" + max_ID + ";" + acceptedValue;
          send(currentMsg);
          trace("send", currentMsg, isLeader);
        } catch (IOException e) {
          e.printStackTrace();
        }
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
          trace("send", currentMsg, isLeader);
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
   * The new leader will be based on which ID is greater.
   * If the member has the highest ID in the received votes,
   * It will send a request to be the leader.
   */
  public void bully(String line) {
    int other = Integer.parseInt(line.split(";")[2]);
    if (!receivedVotes.contains(other)) {
      receivedVotes.add(other);
    }
    Integer maxValue = ID;
    for (Integer otherID : receivedVotes) {
      if (otherID > ID) {
        maxValue = otherID;
      }
    }
    if (maxValue == ID) {
      try {
        isLeader = true;
        currentMsg = name + ";I AM THE LEADER;" + ID;
        send(currentMsg);
        trace("send", currentMsg, isLeader);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      this.isLeader = false;
    }
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
   * Store a received promise message in an array.
   */
  public void handlePromise(String msg) {
    if (!receivedPromises.contains(msg)) {
      receivedPromises.add(msg);
    }
  }

  /**
   * Store a value for comparison in Accept.
   */
  public void handlePropose(String msg) {
    acceptValue = msg;
  }

  /**
   * Stores received Accept messages,
   * If the messages received are greater than the majority,
   * Send Consensus for the accepted value.
   */
  public void handleAccept(String line) {
    // If set to go offline, die after proposal has been accepted.
    if (goesOffline == true) {
      this.delay = null;
      this.isLeader = false;
      consensusReached = true;
      done = true;
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    System.out.println("ACCEPTS: " + receivedAccepts);
    if (!receivedAccepts.contains(line)) {
      receivedAccepts.add(line);
    }
    Integer validAccepts = 0;
    for (String accept : receivedAccepts) {
      Double acceptID = Double.parseDouble(accept.split(";")[2]);
      if (acceptID.equals(max_ID)) {
        validAccepts++;
      }
    }
    if (validAccepts >= majority) {
      consensusReached = true;
      try {
        System.out.println(name + ": SENDING CONSENSUS. for: " + acceptedValue);
        currentMsg = "CONSENSUS";
        send(currentMsg);
        trace("send", currentMsg, isLeader);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Connect to the server, listen
   * Handle incoming messages.
   * Retry send after timeout.
   * Exits after Consensus is reached or member disconnects.
   */
  public void start() {
    try {
      connect("localhost", 4567);
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Get a new unique ID
    getID();

    // Start Heartbeat
    ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(
      1
    );
    heartbeatExecutor.scheduleAtFixedRate(
      startHeartBeat,
      0,
      3,
      TimeUnit.SECONDS
    );

    System.out.println("Hi I'm " + name + ", a Proposer! My ID is: " + ID);

    Integer retryCount = 0;
    Boolean msgSent = false;
    Boolean exiting = false;

    /**
     * The main listening loop.
     * Exit on Consensus Reached.
     * Leader Will exit last when connections are 1 or less.
     * Will re-elect a new leader, if a leader is not found.
     */
    while (!consensusReached) {
      String line;
      Integer receivedCount = 0;

      try {
        while ((line = in.readLine()) != null && !done) {
          if (
            isLeader &&
            state.equals("PROMISE") ||
            isLeader &&
            state.equals("ACCEPT")
          ) {
            state = "PROPOSE";
            PROPOSE();
          }
          if (isLeader && state.equals("PREPARE")) {
            PREPARE();
          }
          if (!isLeader && state.equals("PROPOSE")) {
            state = "PROMISE";
          }

          msgs.add(line);

          // Get and Set current connections.
          if (line.contains("CONNECTIONS")) {
            setConnectionsAndMajority(line);
          } else {
            trace("recv", line, isLeader);
          }

          if (line.equals("EXIT") || exiting && !line.contains("ACK")) {
            if (isLeader) {
              getMajority();
              if ((connections.equals(1))) {
                done = true;
                break;
              }
            } else {
              break;
            }
          }

          if (line.contains("LEADER FOUND")) {
            leaderExists = true;
            String[] split = line.split(";");
            String leaderName = split[0];
            if (leaderName.equals(name)) {
              isLeader = true;
            } else {
              bully(line);
            }
            continue;
          }

          // FIND LEADER
          if (!leaderExists) {
            if (line.contains("VOTELEADER")) {
              bully(line);
            }

            if (!msgSent) {
              System.out.println("VOTING NEW LEADER.");
              voteLeader();
              msgSent = true;
            }
          }

          if (leaderExists) {
            // PREPARE
            if (state.equals("LEADER")) {
              state = "PREPARE";
              PREPARE();
              if (isLeader) {
                state = "PROPOSE";
              } else {
                state = "PROMISE";
              }
            }

            // LEADER
            if (line.contains("LEADER")) {
              if (line.equals("NO LEADER")) {
                System.out.println("NO LEADER SECTION");
                leaderExists = false;
                msgSent = false;
                receivedVotes.clear();
              }
            }

            // PROMISE
            if (line.contains("PREPARE")) {
              handlePrepare(line);
              if (state == "PROMISE") {
                PROMISE();
                state = "ACCEPT";
              }
              continue;
            }

            // PROPOSE
            if (line.contains("PROMISE")) {
              handlePromise(line);
              receivedCount++;
              if (receivedCount >= majority) {
                receivedCount = 0;
                PROPOSE();
                continue;
              }
            }

            // HANDLE ACCEPT
            if (line.contains("ACCEPT")) {
              handleAccept(line);
              receivedCount++;

              if (receivedAccepts.size() >= majority) {
                consensusReached = true;
                System.out.println("ACCEPTS: " + receivedAccepts);
              }
              continue;
            }

            // ACCEPT
            if (line.contains("PROPOSE")) {
              handlePropose(line);
              ACCEPT();
              continue;
            }

            // ACKNOWLEDGE CONSENSUS
            if (line.equals("CONSENSUS")) {
              consensusReached = true;
              currentMsg = name + ";ACKCONSENSUS;" + acceptedValue;
              try {
                sendWithoutDelay(currentMsg);
              } catch (IOException e) {
                e.printStackTrace();
              }
              trace("send", currentMsg, isLeader);
              continue;
            }

            if (line.contains("ACKCONSENSUS")) {
              if (consensusReached && isLeader && !exiting) {
                if (!receivedConsensusAcknowledgements.contains(line)) {
                  receivedConsensusAcknowledgements.add(line);
                  System.out.println(
                    "ACKS: " + receivedConsensusAcknowledgements
                  );
                }
                if (
                  receivedConsensusAcknowledgements.size() >= connections - 1
                ) {
                  exiting = true;
                  try {
                    sendWithoutDelay("EXIT");
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                }
              }
            }
          }
        }
      } catch (SocketTimeoutException e) {
        // Retry on readline() timeout.
        try {
          if (retryCount >= 3) {
            checkLeader();
            retryCount = 0;
          } else {
            System.out.println(
              name + " -TIMEOUT- retrying send... " + currentMsg
            );
            send(currentMsg);
            trace("send", currentMsg, isLeader);
            retryCount++;
          }
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
      try {
        disconnect();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    // Print out result.
    if (goesOffline == true) {
      System.out.println(name + " --> Going Offline.");
    } else {
      System.out.println(name + " --> Consensus Reached: " + acceptedValue);
      if (isLeader) {
        System.out.println("Done.");
      }
    }
    try {
      disconnect();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
