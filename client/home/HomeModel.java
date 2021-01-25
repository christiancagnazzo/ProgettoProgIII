package client.home;


import common.request.Request;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import common.mail.Email;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import common.mail.MailWrapper;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class HomeModel {
    private ObservableList<Email> mailList = FXCollections.observableList(new ArrayList<>());
    private ObjectProperty<Email> currentMail = new SimpleObjectProperty<>();
    private SimpleStringProperty email_account = new SimpleStringProperty();
    private SimpleStringProperty msg = new SimpleStringProperty();
    private SimpleBooleanProperty notify = new SimpleBooleanProperty();
    private AtomicBoolean new_mail = new AtomicBoolean();
    private boolean init_mailbox = false;
    private boolean disconnected_server_after_init = false;
    private static final int NUM_THREAD = 3;
    ExecutorService exec = Executors.newFixedThreadPool(NUM_THREAD); // pool

    public HomeModel(String email_account) {
        this.email_account.set(email_account);
        new_mail.set(false);
        Timer timer = new Timer(true); // Thread Daemon
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Platform.runLater because in javaFX
                // only the FX thread can modify UI elements
                Platform.runLater(() -> {
                    if (!init_mailbox) { // only first one
                        getInitialMailbox();
                    }
                    else {
                        if (disconnected_server_after_init){
                            getInitialMailbox();
                        } else
                            askNews();
                    }
                });
            }
        }, 0, 10000);
    }


    /* ------------------- Method to try to connect with the server --------------------------------------- */
    private Socket tryConnect() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            Socket s = new Socket(host, 8189);
            msg.setValue("");
            return s;
        } catch (IOException e) {
            msg.set("Server not active. Click REFRESH to try a reconnect");
        }
        return null;
    }
    /* ----------------------------------------------------------------------------------------------------- */


    /* ----------------   Method to request the initial mailbox --------------------------------------------- */
    private void getInitialMailbox(){
        Socket socket = tryConnect();
        if (socket != null) {
            exec.execute(new InitialMailbox(socket));
        }
    }
    /* ----------------------------------------------------------------------------------------------------- */


    /* ----------------------- Method to ask server if there are new mails --------------------------------- */
    protected void askNews() {
        if (!init_mailbox || disconnected_server_after_init)
            getInitialMailbox();
        else {
            Socket socket = tryConnect();
            if (socket != null) {
                exec.execute(new AskNews(socket));
            } else {
                if (init_mailbox)
                    disconnected_server_after_init = true;
            }
        }
    }

    /* ----------------------------------------------------------------------------------------------------- */


    /* ----------------------- Method to take by the server new mails -------------------------------------- */
    private void takeNewMails(){
        if (new_mail.get()){ // only if new mail
            Socket socket = tryConnect();

            if (socket != null){
                exec.execute(new TakeNewMails(socket));
            }
        }
    }
    /* ----------------------------------------------------------------------------------------------------- */


    /* -------------------------------------- Method to update mail list ----------------------------------- */
    private synchronized void updateMailList(List<MailWrapper> mail_box){
        if (disconnected_server_after_init)
            mailList.clear();
        mail_box.forEach((mail -> mailList.add(0,
                new Email(
                        mail.getId(),
                        mail.getSender(),
                        mail.getRecipients(),
                        mail.getObject(),
                        mail.getText(),
                        mail.getDate()))));
        notify.setValue(false);
        disconnected_server_after_init = false;
    }
    /* ----------------------------------------------------------------------------------------------------- */


    /* ---------------- Method to remove a mail from the mail list (and from the server) ------------------- */
    protected synchronized void deleteMail(){
        Socket socket = tryConnect();
        if (socket != null)
            exec.execute(new DeleteMail(socket, getCurrentMail().getId()));
        // remove mail from list
        mailList.remove(getCurrentMail());
    }
    /* ----------------------------------------------------------------------------------------------------- */


    protected ObservableList<Email> getMailListProperty() { return mailList; }

    protected Email getCurrentMail() { return currentMail.get(); }

    protected ObjectProperty<Email> currentMailProperty() { return currentMail; }

    protected SimpleStringProperty getEmailAccountProperty() { return email_account; }

    protected SimpleStringProperty getMsgProperty() { return msg; }

    protected SimpleBooleanProperty getNotifyProperty() {return notify;}



    /* RUNNABLE TASK REQUEST TO SERVER */

    /* ------------------------------- INITIAL MAILBOX ---------------------------------------  */
    @SuppressWarnings("unchecked")
    private class InitialMailbox implements Runnable {
        Socket socket;

        private InitialMailbox(Socket s) {
            socket = s;
        }

        @Override
        public void run() {
            try {
                try {
                    ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());

                    outStream.writeObject(email_account.getValue()); // send to server name account
                    outStream.writeObject(Request.INITIAL_MAILBOX); // send to server request type

                    ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

                    List<MailWrapper> mail_list = (List<MailWrapper>) inStream.readObject();

                    if (mail_list != null) {
                        Platform.runLater(() -> updateMailList(mail_list));
                        init_mailbox = true;
                    }

                } catch (ClassNotFoundException | IOException e) {
                    Platform.runLater(()-> msg.setValue(e.getMessage()));
                } finally {
                    socket.close();
                }
            } catch (IOException e) {
                Platform.runLater(()-> msg.setValue(e.getMessage()));
            }

        }
    }
    /* -------------------------------------------------------------------------------------  */


    /* ------------------------------- ASK NEWS --------------------------------------------  */
    private class AskNews implements Runnable {
        Socket socket;

        private AskNews(Socket s){ socket = s;}

        @Override
        public void run() {
            try {
                try {
                    ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());

                    outStream.writeObject(email_account.getValue()); // send to server name account
                    outStream.writeObject(Request.CHECK_NEW); // send to server request type

                    ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

                    new_mail.set((Boolean) inStream.readObject());
                    Platform.runLater(() -> notify.set(new_mail.get()));
                    takeNewMails();

                } catch (IOException | ClassNotFoundException e) {
                    Platform.runLater(()-> msg.setValue(e.getMessage()));
                } finally {
                    socket.close();
                }
            } catch (IOException e){ Platform.runLater(()-> msg.setValue(e.getMessage()));}
        }
    }
    /* -------------------------------------------------------------------------------------  */


    /* ------------------------------- TAKE NEW MAILS --------------------------------------  */
    @SuppressWarnings("unchecked")
    private class TakeNewMails implements Runnable {
        Socket socket;

        private TakeNewMails(Socket s){ socket = s;}

        @Override
        public void run() {
                try {
                    try {
                        ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());

                        outStream.writeObject(email_account.getValue()); // send to server name account
                        outStream.writeObject(Request.GET_NEW_MAIL); // send to server request type

                        ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

                        List<MailWrapper>  mail_list = (List<MailWrapper>) inStream.readObject();

                        Platform.runLater(()-> updateMailList(mail_list));
                        new_mail.set(false);

                    } catch (ClassNotFoundException | IOException e) {
                        Platform.runLater(()-> msg.setValue(e.getMessage()));
                    } finally {
                        socket.close();
                    }
                } catch (IOException e){Platform.runLater(()-> msg.setValue(e.getMessage()));}
        }
    }
    /* -------------------------------------------------------------------------------------  */


    /* ------------------------------- DELETE MAIL -----------------------------------------  */
    private class DeleteMail implements Runnable{
        Socket socket;
        int mail_id;

        private DeleteMail(Socket s, int mail_id){
            socket = s;
            this.mail_id = mail_id;
        }

        @Override
        public void run() {
            try {
                try {
                    ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());

                    outStream.writeObject(email_account.getValue()); // send to server name account
                    outStream.writeObject(Request.DELETE); // send to server request type

                    // send to server mail id to delete
                    outStream.writeObject(Integer.toString(mail_id));
                } catch (IOException e) {
                    Platform.runLater(()-> msg.setValue(e.getMessage()));
                } finally {
                    socket.close();
                }
            } catch (IOException e){Platform.runLater(()-> msg.setValue(e.getMessage()));}

        }
    }
    /* -------------------------------------------------------------------------------------  */
}
