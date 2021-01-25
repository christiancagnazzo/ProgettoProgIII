package server;

import common.request.Request;
import common.mail.MailWrapper;
import javafx.collections.ObservableList;
import server.mailbox.AllMailboxes;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;




public class ServerModel extends Thread{
    private static final int NUM_THREAD = 5;
    private Log log = new Log();
    private AllMailboxes all_mailboxes = new AllMailboxes(log);



    /* ------------------------------ LOOP SERVER THREAD --------------------------------------------------------- */
    public void run(){
        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREAD); // pool

        try {
            ServerSocket s = new ServerSocket(8189);
            log.addLog("Active server. On hold...");
            while (true) {
                Socket incoming = s.accept();
                ObjectOutputStream outStream = new ObjectOutputStream(incoming.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(incoming.getInputStream());
                String user = (String) inStream.readObject(); // wait for email account user
                String request = (String) inStream.readObject(); // wait for request type
                log.addLog("Client "+user+" connected. Request: "+request);
                switch (request) {
                    case Request.INITIAL_MAILBOX -> {
                        Runnable get_mailbox_init = new getMailbox(incoming, user, outStream);
                        exec.execute(get_mailbox_init);
                    }
                    case Request.CHECK_NEW -> {
                        Runnable news = new ifNewMail(incoming, user, outStream);
                        exec.execute(news);
                    }
                    case Request.GET_NEW_MAIL -> {
                        Runnable get_news = new getNewMail(incoming, user, outStream);
                        exec.execute(get_news);
                    }
                    case Request.SEND -> {
                        Runnable send_mail = new SendMail(incoming, user, outStream, inStream);
                        exec.execute(send_mail);
                    }
                    case Request.DELETE -> {
                        Runnable delete_mail = new deleteMail(incoming, user, inStream);
                        exec.execute(delete_mail);
                    }
                }
            }
        }
        catch (IOException | ClassNotFoundException e) { log.addLog("Error: "+e.getMessage()); }
    }

    protected ObservableList<String> logProperty() { return log.getLogProperty(); }

    /* ------------------------------------------------------------------------------------------------------------ */


    /* --------------------------------- INITIAL MAILBOX THREAD --------------------------------------------------- */
    /* ----------- Nested class representing the runnable of the thread that sends users their mailbox ------------ */

    private class getMailbox implements Runnable {
        private Socket incoming;
        private String user;
        private ObjectOutputStream out;

        private getMailbox(Socket incoming, String user, ObjectOutputStream outStream) {
            this.incoming = incoming;
            this.user = user;
            this.out = outStream;
        }

        @Override
        public void run() {
            all_mailboxes.initializeMailbox(user);
            try {
                try {
                    out.writeObject(all_mailboxes.getUserMailList(user)); // send mailbox to user
                } catch (IOException e) {
                    log.addLog(e.getMessage());
                } finally {
                    incoming.close();
                    log.addLog("Client "+user+" disconnected");
                }
            } catch (IOException e) { log.addLog(e.getMessage());}
        }
    }
    /* ------------------------------------------------------------------------------------------------------------ */


    /* ----------------------------------- NEW MAILS GET THREAD --------------------------------------------------- */
    /* ----------- Nested class representing the runnable of the thread that sends users their mailbox ------------ */

    private class getNewMail implements Runnable {
        private Socket incoming;
        private String user;
        private ObjectOutputStream out;

        private getNewMail(Socket incoming, String user, ObjectOutputStream outStream) {
            this.incoming = incoming;
            this.user = user;
            this.out = outStream;
        }

        @Override
        public void run() {
            try {
                try {
                    out.writeObject(all_mailboxes.getNewMails(user)); // send new mail to user
                } catch (IOException e) {
                    log.addLog(e.getMessage());
                } finally {
                    incoming.close();
                    log.addLog("Client "+user+" disconnected");
                }
            } catch (IOException e) { log.addLog(e.getMessage());}
        }
    }
    /* ------------------------------------------------------------------------------------------------------------ */


    /* --------------------------------- NEW MAILS CHECK THREAD --------------------------------------------------- */
    /* -------- Nested class representing the runnable of the thread that sends users if there are new mail ------- */

    class ifNewMail implements Runnable {
        private Socket incoming;
        private String user;
        private ObjectOutputStream out;


        private ifNewMail(Socket incoming, String user, ObjectOutputStream outStream) {
            this.incoming = incoming;
            this.user = user;
            this.out = outStream;
        }

        @Override
        public void run() {
            try {
                try {
                    out.writeObject(all_mailboxes.areNewMails(user)); // send if are new mail
                } catch (IOException e) {
                    log.addLog(e.getMessage());
                } finally {
                    incoming.close();
                    log.addLog("Client "+user+" disconnected");
                }
            } catch (IOException e) { log.addLog(e.getMessage());}
        }
    }
    /* ------------------------------------------------------------------------------------------------------------ */



    /* -------------------------------- SEND NEW MAIL THREAD ------------------------------------------------------ */
    /* ----------- Nested class representing the executable of the thread sending a new email --------------------- */

    private class SendMail implements Runnable {
        private Socket incoming;
        private String user;
        private ObjectOutputStream outStream;
        private ObjectInputStream inStream;


        private SendMail(Socket incoming, String user, ObjectOutputStream outStream, ObjectInputStream inStream) {
            this.incoming = incoming;
            this.outStream = outStream;
            this.inStream = inStream;
            this.user = user;
        }

        @Override
        public void run() {
            try {
                try {
                    MailWrapper mail_to_send = (MailWrapper) inStream.readObject(); // await the email to be sent
                    outStream.writeObject(all_mailboxes.sendMail(user, mail_to_send)); // write msg
                } catch (IOException | ClassNotFoundException e) {
                    log.addLog(e.getMessage());
                } finally {
                    incoming.close();
                    log.addLog("Client "+user+" disconnected");
                }
            } catch (IOException e) { log.addLog(e.getMessage());}
        }
    }

    /* ------------------------------------------------------------------------------------------------------------ */


    /* ----------------------------------- DELETE MAIL THREAD ----------------------------------------------------- */
    /* ------- Nested class representing the runnable of the thread that remove mail from the user mailbox -------- */

    private class deleteMail implements Runnable {
        private Socket incoming;
        private String user;
        private ObjectInputStream in;


        private deleteMail(Socket incoming, String user, ObjectInputStream inStream) {
            this.incoming = incoming;
            this.user = user;
            this.in = inStream;
        }

        @Override
        public void run() {
            try {
                try {
                    int mail_id = Integer.parseInt((String) in.readObject()); // wait for the mail id
                    all_mailboxes.deleteMail(user, mail_id);
                } catch (IOException | ClassNotFoundException e) {
                    log.addLog(e.getMessage());
                } finally {
                    incoming.close();
                    log.addLog("Client "+user+" disconnected");
                }
            } catch (IOException e) { log.addLog(e.getMessage());}

        }
    }
    /* ------------------------------------------------------------------------------------------------------------ */

}



