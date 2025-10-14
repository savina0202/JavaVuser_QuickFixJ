# Performance test a trading system POC using Loadrunner  

The POC is to prove that the performance test for a trading system can be done using Loadrunner Java Vuser protocol. The script uses QuickFIX/J library to simulate FIX protocal order flow between a client and a server (accepter).

In anohter words, it allows Loadrunner to behave like a FIX trading client that logs on to a FIX gateway and sends NewOrderSingle message, measuring latency and validating ExecutionReport response - just like a real trading front-end would do.


## 🎯 Overview
This POC implements a Java-based LoadRunner virtual user script that:

* Establishes FIX 4.4 connections to the trading servers
* Generates and sends realistic FIX orders (NewOrderSingle)
* Measures transaction performance and response times
* Simulates real-world trading load for performance testing  

## 🏗️ Architecture

`LoadRunner Virtual User → FIX 4.4 Protocol → Trading System QuickFix/J (127.0.0.1:9000)`

## ✨ Step by Step Breakdown
### 1. Initialization (`init()` method)
* The script builds an in-memory FIX session configuration dynamically
* Defines FIX version (FIX.4.4), CompIDs, heartbeat interval, connection type, and reset flags
* Creates store and log directories for QuickFIX/J’s persistence 
* Initializes QuickFIX components:
  FixApplication -> the application logic (handles callbacks for logon, logout message)
  FileStoreFactory, FileLogFactory and DefaultMesssageFactory -> message persistence and logging
* Starts the **SocketInitiator**, connecting to the FIX server (127.0.0.1:9000)
* Waits up to 15 seconds for the Logon (35=A) handshake to complete
* Marks the session as loggedOn = true once connected successfully
✅ Purpose: Establish a FIX connection between LoadRunner and the trading system
### 2.Action (`action()` method)
* Validates that the FIX session is active.
* Builds a **NewOrderSingle (35=D)** message:
  * Sets core tags like `ClOrdID`, `Symbol`, `Side`, `OrderQty`, `Price`, `OrdType`, `Account`, and `TimeInForce`
* Starts a Loadrunner transaction Timer named `NewOrderSingle` to measure latency  
* Sends the order using:
```
Session.sendToTarget(order, sessionId);
```
* Waits for an ExecutionReport (35=8) response in the `fromApp()` callback
* Ends the transaction with `lr.end_transaction("NewOrderSingle", lr.PASS)` once the response is received.
✅ Purpose: Simulate sending an order, measure end-to-end latency

### Teardown (`end()` method)
* Gracefully stops the QuickFIX initiator
* Logs "FIX connection steopped"
✅ Purpose: Clearly close the session and resoruces

## 🛠️ Technical Details

* FIX Version: 4.4
* Transport: Socket-based MINA network
* Message Type: NewOrderSingle, ExecutionReport, Logon, logout
* Session: Initiator mode with heartbeat support
* Dynamically generates values: 
  * `ClOrdID`: LR_VU + lr.get_vuser_id() +"_" + orderCounter + "_" + System.currentTimeMillis();
* Log: Capture the FIX conversitions between the Initiator and the Accepptor 

## FIX Conversations 

Sending the order from Loadrunner to Trading system
```
8=FIX.4.4|9=182|35=D|34=2|49=LOADRUNNER_CLIENT|52=20251005-05:20:22.659|56=FIX_SERVER|1=TEST_ACCOUNT|11=LR_VU-1_1_1759641622388|21=1|38=100|40=2|44=150.99|54=1|55=AAPL|59=0|60=20251005-01:20:22.601|10=163|
```

Trading sytem response back the ExecutionReport
```
8=FIX.4.4|9=208|35=8|34=2|49=FIX_SERVER|52=20251005-05:20:22.707|56=LOADRUNNER_CLIENT|6=150.5|11=LR_VU-1_1_1759641622388|14=100|17=EXEC_1759641622707|31=150.5|32=100|37=SRV_1759641622707|38=100|39=2|54=1|55=AAPL|150=2|151=0|10=044|

```
Session Established (Logon)
```
8=FIX.4.4|9=88|35=A|34=1|49=LOADRUNNER_CLIENT|52=20251005-05:20:20.922|56=FIX_SERVER|98=0|108=60|141=Y|10=096|
8=FIX.4.4|9=88|35=A|34=1|49=FIX_SERVER|52=20251005-05:20:21.031|56=LOADRUNNER_CLIENT|98=0|108=60|141=Y|10=088|
```

Session Stopped (Logout)
```
8=FIX.4.4|9=70|35=5|34=4|49=LOADRUNNER_CLIENT|52=20251005-05:20:28.906|56=FIX_SERVER|10=015|
8=FIX.4.4|9=70|35=5|34=4|49=FIX_SERVER|52=20251005-05:20:28.906|56=LOADRUNNER_CLIENT|10=015|
```





## Deployment



To deploy this project: 

* Install Opentext | Virtual User Generator CE 25.3
* Clone QuickFix/J 2.2.1 (org.quickfixj-2.2.1-bin.zip) and extract the zip file on your local windows machine
``` bash
https://sourceforge.net/projects/quickfixj/
```
On Loadrunner side (Client):
```
1.Open VU Gen, Clicking on New Script and Solution from File
   Select "Java Vuser" protocol from Create a New Script window
   Enter your Script Name and specify the location

2.Configure the Java version of Loadrunner. Since QuickFix/J is pertty old, it uses jdk 1.8, Loadrunner needs to have this version JDK configured 

<image>

3.Configure the following Classpath Entries of quickfixj under Runtime Settings. 
  Note: these .jar files can be found from org.quickfixj-2.2.1-bin.zip
<image>

4. Copy my actions.java, then overwrite the existing empty Actions Script
5. Make sure log folders are created as per the configuration on the script:

  "FileStorePath=store\n" +
  "FileLogPath=log\n" +
```

On Server Side:

```
1. On fixserver.bat, configure the following path as per your machine : 
  set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202
  set QUICKFIXJ_HOME=E:\Loadrunner_basic\scripts\JavaVuser_QuickFixJ\JavaVuser_QuickFixJ
  set LIB_DIR=%QUICKFIXJ_HOME%\org.quickfixj-2.2.1-bin\lib
  set CONFIG_FILE=fixserver.cfg
  set STORE_DIR=server_store
  set LOG_DIR=server_log
```
```
2. Make sure fixserver.cfg is configured properly
[DEFAULT] 
ConnectionType=acceptor 
SenderCompID=FIX_SERVER 
TargetCompID=LOADRUNNER_CLIENT 
FileStorePath=server_store
FileLogPath=server_log
ResetOnLogon=Y
ResetOnLogout=Y
ResetOnDisconnect=Y
 
[SESSION] 
BeginString=FIX.4.4 
HeartBtInt=30 
SocketAcceptPort=9000 
StartTime=00:00:00 
EndTime=00:00:00 
```

```
3. Make sure the log folders are created with the same name on the fixserver.cfg 
FileStorePath=server_store
FileLogPath=server_log
```

## Run it
  * Run the fixserver.bat
  * Run the Loadrunner script using Replay and monitor the Replay output










## Demo

Insert gif or link to demo

