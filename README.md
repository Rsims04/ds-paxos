# ds-paxos

A system implements the PAXOS protocol to acheive consensus among a council of members.

## Build

To build all files, type: `make`.<br />

## Start The Central Server

To start the server, use: `./Server`.<br />
This will start the server on default host/port 4567.<br />
The server will receive and forward messages amongst the members.<br/><br/>

## Run a Member

To run a Member, use: `./Member [Name] [Behaviour?] [goesOffline?]` <br/>
Name: choose M1, M2 or M3 for proposers, any for other (recommended M4 through to M9).<br/>
Behaviour: Will set the delay between sending messages.<br/>
Options: (default will be immediate)

- immediate
- small
- large
- none

example-1: `./Member M1` <br/>
example-2: `./Member M1 immediate` <br/>
example-3: `./Member M1 immediate offline` <br/><br/>

goesOffline: type `offline`, and the proposer will go offline after proposing.<br/>

## Failures

All members will retry on timeout if they do not receive a response. The timeout is set to 20 seconds, you can change the timeout in `Member.java`.

## Reset

Run `make clean`.

## Testing

### Test Case 1

Run `./PAXOS-CASE1`.<br>
This test involves all members having immediate response times.

### Test Case 2

Run `./PAXOS-CASE2`.<br>
This test involves all members having varying response times. M3 will not respond at all.
M

### Test Case 3

Run `./PAXOS-CASE3`.<br>
M3 will be the leader and will die on propose. This shows that the system can recover after losing a member,<br/>
and that M3 can still win the election even though it is not online and returning messages.

---
