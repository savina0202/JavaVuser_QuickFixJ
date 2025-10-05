/*
 * Performance test script 
 * Description: 
 * This script is a demo for how to use loadrunner Java Vuser to send Fix message from quickfix initiator to acceptor
 * 
 *                     
 */

import lrapi.lr;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.ExecutionReport;
import java.time.LocalDateTime;
import java.util.Date;
import org.slf4j.LoggerFactory;

public class Actions 
{
    private SocketInitiator initiator;
    private SessionID sessionId;
    private boolean loggedOn = false;	
    private volatile boolean executionReportReceived = false;
    private volatile boolean fromAppCalled = false;

	public int init() throws Throwable {
        
		lr.log_message("=== FIX Client Initialization ==="); 
				
        try {
			
			//Creat folders
			java.io.File storeDir = new java.io.File("store");
			java.io.File logDir = new java.io.File("log");
			if(!storeDir.exists()) storeDir.mkdirs();
			if(!logDir.exists())  logDir.mkdirs();    			

            String configString =
                "[DEFAULT]\n" +
                "ConnectionType=initiator\n" +
                "ReconnectInterval=60\n" +
                "SenderCompID=LOADRUNNER_CLIENT\n" +
                "TargetCompID=FIX_SERVER\n" +
            	"LogLevel=WARN\n" +
            	"FileStorePath=store\n" +
            	"FileLogPath=log\n" +
            	"ResetOnLogon=Y\n" +   //Reset msgSeqNum when logon
            	"ResetOnLogout=Y\n" +  //Reset msgSeqNum when logout
            	"ResetOnDisconnect=Y\n" +  //Reset msgSeqNum when disconnect
            	"LogLevel=WARN\n" +
                "[SESSION]\n" +
                "BeginString=FIX.4.4\n" +
                "DataDictionary=config/FIX44.xml\n" + 
                "HeartBtInt=60\n" +
                "SocketConnectHost=127.0.0.1\n" +
                "SocketConnectPort=9000\n" +
                "StartTime=00:00:00\n" +
                "EndTime=00:00:00\n" +
            	"MinaLogLevel=WARN";
            
        

            SessionSettings settings = new SessionSettings(new java.io.ByteArrayInputStream(configString.getBytes()));
            
			//2.
			lr.log_message("Starting Fix connection 127.0.0.1:9000 ...");
            FixApplication application = new FixApplication();
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            LogFactory logFactory = new FileLogFactory(settings);
            MessageFactory messageFactory = new DefaultMessageFactory();

            // 3. 
            lr.log_message("Creating SocketInitiator with application...");
            initiator = new SocketInitiator(application, storeFactory, settings, logFactory, messageFactory);
            
            lr.log_message("Starting FIX initiator...");
            initiator.start();

	        for (int i = 0; i < 15; i++) {

	            java.util.Iterator<SessionID> sessions = initiator.getSessions().iterator();
	            if (sessions.hasNext()) {
	                SessionID sid = sessions.next();
	                Session session = Session.lookupSession(sid);
	                if (session != null && session.isLoggedOn()) {
	                    sessionId = sid;
	                    lr.log_message("FIX login successful! sessionId:"+session);
	                    loggedOn = true;
	                    return lr.PASS;
	                }
	            }
				Thread.sleep(1000);
	            lr.log_message("Waiting for login... " + i + "s");
        	}

            
            lr.error_message("FIX login timeout");
            return lr.FAIL;

        } catch (Exception e) {
            lr.error_message("FIX session exception: " + e.getMessage());
            e.printStackTrace();
            return lr.FAIL;
        }

    }


	//Generate order id
	private int orderCounter = 0;
	
	private String generateOrderId() {
	        orderCounter++;
	        String orderid= "LR_VU" + lr.get_vuser_id() + 
	               "_" + orderCounter + 
	               "_" + System.currentTimeMillis();
	        //lr.log_message("orderid=" + orderid);
	        return orderid;
	}

	public int action() throws Throwable {
		
		//Check if session is created 
        if (!loggedOn || sessionId == null) {
            lr.error_message("FIX session is not ready. Cannot send order.");
            lr.error_message("loggedOn=" + loggedOn + ", sessionId=" + sessionId);
            return lr.FAIL;
        }		
        
        try {

			// Create a newOrderSingle
			NewOrderSingle order = new NewOrderSingle();
	
			String orderid = generateOrderId();
			ClOrdID clOrdID = new ClOrdID(orderid);
			order.setField(clOrdID);
			
			//Other tag:values
			order.setField(new Side(Side.BUY));
			order.setField(new Symbol("AAPL"));
			order.setField(new OrderQty(100));
			order.setField(new Price(150.99));
			order.setField(new OrdType(OrdType.LIMIT));
			order.setField(new Account("TEST_ACCOUNT"));
			order.setField(new TimeInForce(TimeInForce.DAY));
			order.setField(new HandlInst('1'));
			order.setField(new TransactTime(LocalDateTime.now()));
				
			lr.log_message("Order created: "+ clOrdID.getValue());
			
            // Send order to server
            lr.log_message("Sending order to FIX server and start to measure the transaction ... \n");
            
            // Starting a transaction
            lr.start_transaction("NewOrderSingle");

            boolean sent = Session.sendToTarget(order, sessionId);
            if (!sent) {
                lr.error_message("Failed to send order");
                lr.end_transaction("NewOrderSingle", lr.FAIL);
                return lr.FAIL;
            }
            lr.log_message("Order ClOrdID: " + order.getClOrdID().getValue() +" sent successfully ...");

            // Waiting for executionReport
//            Thread.sleep(3000); 

//			for (int i = 0; i < 50; i++) { // 5 seconds
//	            Thread.sleep(100);
//	            
//	            if (fromAppCalled) {
//	                lr.log_message("fromApp was called! Message is being processed");
//	                Thread.sleep(2000);
//	                break;
//	            }
//	            
//	            if (i % 10 == 0) {
//	                lr.log_message("Waiting for fromApp... " + (i/10) + "s");
//	            }
//	        }
//	        
//	        if (!fromAppCalled) {
//	            lr.error_message("fromApp was NEVER called - check session state and message flow");
//	        }

            lr.end_transaction("NewOrderSingle", lr.PASS);
            return lr.PASS;

        } catch (Exception e) {
            lr.error_message("Action error: " + e.getMessage());
            e.printStackTrace();
            lr.end_transaction("NewOrderSingle", lr.FAIL);
            return lr.FAIL;
        }
        
	}//end of action


	public int end() throws Throwable {
		try {
	            if (initiator != null) {
	                initiator.stop();
	                lr.log_message("FIX connection stopped");
	            }
        } catch (Exception e) {
            lr.error_message("Stop error: " + e.getMessage());
        }
        return lr.PASS; 
	}


	// Fix Application
    private class FixApplication extends MessageCracker implements Application {
		
        @Override
        public void onCreate(SessionID sessionId) {
        	lr.log_message("FIX Session created: " + sessionId);
        }
        
        @Override
        public void onLogon(SessionID sessionId) {
            lr.log_message("FIX Login successful: " + sessionId);
            loggedOn = true;
        }
        
        @Override
        public void onLogout(SessionID sessionId) {
            lr.log_message("FIX Logout: " + sessionId);
            loggedOn = false;
        }
        
        @Override
        public void toAdmin(Message message, SessionID sessionId) {
        	// Admin message processing here
        }
        
        @Override
        public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        	// Admin message processing here: 
        	try {
	        	MsgType msgType = new MsgType();
	        	message.getHeader().getField(msgType);
	        	
	        	lr.log_message("Received Admin message: " + msgType.getValue());
	        	
	        	if ("A".equals(msgType.getValue())){ //Logon
	        		lr.log_message("Received Logon response from server");
	        		loggedOn = true; //Set logon flag
	        		lr.log_message("Received Admin: " + msgType.getValue());
	        		
	        	} else if("5".equals(msgType.getValue())){  //Logout
	        		
	        		lr.log_message("Received Logout request");
	        		loggedOn = false; //Set logon flag
	        		
	        	} else if ("3".equals(msgType.getValue())){ //Reject
	        		lr.log_message("Received Reject message");
	        		
	        	}
	        	
	        	
        	} catch(Exception e){
        		//
        		lr.error_message("FromAdmin error: " + e.getMessage());
        	}
        }
        
        
        @Override
        public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        	lr.log_message("Sending: " + message.toString());
        }
        
        

        
        @Override
        public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
            // Core:Receving message
            try{
            	fromAppCalled = true;
            	
				lr.log_message("fromApp IS BEING CALLED!");
				        
		        MsgType msgType = new MsgType();
		        message.getHeader().getField(msgType);
		        lr.log_message("Message type: " + msgType.getValue());
		        lr.log_message("Full message: " + message.toString());
		        
		        lr.log_message("Calling crack()...");            	
				            	
	            crack(message, sessionId);
	            
	            lr.log_message("crack() completed");
	            
            } catch (Exception e){
            	lr.error_message("fromApp error: " + e.getMessage());
            }
        }


        public void onMessage(ExecutionReport executionReport, SessionID sessionId) throws FieldNotFound {
            // Core: Processing executionReport msgType=8
            
            try{
            lr.log_message("=== ExecutionReport Received ===");
            
            lr.end_transaction("NewOrderSingle", lr.PASS);
            
            ClOrdID clOrdID = new ClOrdID();
            executionReport.getField(clOrdID);
            
            OrdStatus ordStatus = new OrdStatus();
            executionReport.getField(ordStatus);
            
			ExecType execType = new ExecType();
	        executionReport.getField(execType);
	        
	        LeavesQty leavesQty = new LeavesQty();
	        executionReport.getField(leavesQty);
	        
	        CumQty cumQty = new CumQty();
	        executionReport.getField(cumQty);
	        
	        lr.log_message("ExecutionReport - ClOrdID: " + clOrdID.getValue() + 
	                      ", Status: " + ordStatus.getValue() +
	                      ", ExecType: " + execType.getValue() +
	                      ", LeavesQty: " + leavesQty.getValue() +
	                      ", CumQty: " + cumQty.getValue()); 
	        
            } catch (Exception e){
            	
		        lr.error_message("ExecutionReport error: " + e.getMessage());
		        lr.end_transaction("NewOrderSingle", lr.FAIL);            	
		        
            }
            
        }
    } 
}
