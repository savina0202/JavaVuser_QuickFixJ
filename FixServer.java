import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;

public class FixServer {
    public static void main(String[] args) throws ConfigError, Exception {
        System.out.println("=== Starting FIX Server ===");
        

        SessionSettings settings = new SessionSettings("fixserver.cfg");
        

        Application application = new FixServerApplication();
        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        quickfix.MessageFactory messageFactory = new DefaultMessageFactory();  


        SocketAcceptor acceptor = new SocketAcceptor(application, storeFactory, settings, logFactory, messageFactory);
        acceptor.start();
        
        System.out.println("FIX Server started successfully on port 9000");
        System.out.println("Waiting for LoadRunner clients to connect...");
        System.out.println("Press Ctrl+C to stop the server");
        

        System.in.read();
        
        acceptor.stop();
        System.out.println("FIX Server stopped");
    }
    
    static class FixServerApplication implements Application {
        @Override
        public void onCreate(SessionID sessionId) {
            System.out.println("Session created: " + sessionId);
        }
        
        @Override
        public void onLogon(SessionID sessionId) {
            System.out.println("Client logged in: " + sessionId);
        }
        
        @Override
        public void onLogout(SessionID sessionId) {
            System.out.println("Client logged out: " + sessionId);
        }
        
        @Override
        public void toAdmin(quickfix.Message message, SessionID sessionId) {  
            try {
                MsgType msgType = new MsgType();
                message.getHeader().getField(msgType);
                System.out.println("Sending Admin: " + msgType.getValue());
            } catch (Exception e) {
                
            }
        }
        
        @Override
        public void fromAdmin(quickfix.Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon { 
            try {
                MsgType msgType = new MsgType();
                message.getHeader().getField(msgType);
                System.out.println("Received Admin: " + msgType.getValue());
                

                if ("A".equals(msgType.getValue())) {
                    System.out.println("Received Logon response");
                }
            } catch (Exception e) {

            }
        }
        
        @Override
        public void toApp(quickfix.Message message, SessionID sessionId) throws DoNotSend {  
            System.out.println("Sending App: " + message.toString().replace((char)1, '|'));
        }
        
        @Override
        public void fromApp(quickfix.Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {  
            System.out.println("Received Order: " + message.toString().replace((char)1, '|'));
            
            try {

                ClOrdID clOrdID = new ClOrdID();
                message.getField(clOrdID);
                

                ExecutionReport executionReport = createExecutionReport(clOrdID.getValue());
                

                Session.sendToTarget(executionReport, sessionId);
                System.out.println("Sent ExecutionReport for: " + clOrdID.getValue());
                
            } catch (Exception e) {
                System.err.println("Error processing order: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        private ExecutionReport createExecutionReport(String clOrdID) throws Exception {
			System.out.print("From CreateExecutionReport: input clOrdID: " + clOrdID);
            ExecutionReport report = new ExecutionReport(
                new OrderID("SRV_" + System.currentTimeMillis()),
                new ExecID("EXEC_" + System.currentTimeMillis()),
                new ExecType(ExecType.FILL),
                new OrdStatus(OrdStatus.FILLED),
                new Side(Side.BUY),
                new LeavesQty(0),
                new CumQty(100),
                new AvgPx(150.50)
            );
            

            report.setField(new ClOrdID(clOrdID));
            report.setField(new Symbol("AAPL"));
            report.setField(new OrderQty(100));
            report.setField(new LastQty(100));
            report.setField(new LastPx(150.50));
            
            return report;
        }
    }
}