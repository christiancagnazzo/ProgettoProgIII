package client.write;

import common.request.Request;
import javafx.beans.property.SimpleStringProperty;
import common.mail.MailWrapper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Pattern;

public class WriteModel {
    private final String email_account;
    private SimpleStringProperty recipients = new SimpleStringProperty();
    private SimpleStringProperty object = new SimpleStringProperty();
    private SimpleStringProperty text = new SimpleStringProperty();
    private SimpleStringProperty msg = new SimpleStringProperty();

    public WriteModel(String email_account){
        this.email_account = email_account;
    }

    protected SimpleStringProperty getRecipientsProperty(){ return recipients;}
    protected SimpleStringProperty getObjectProperty(){ return object; }
    protected SimpleStringProperty getMsgProperty() { return msg; }
    protected SimpleStringProperty getTextProperty() { return text; }

    /* -------------------- Method to try to connect with the server --------------------------------------- */
    private Socket tryConnect() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            Socket s = new Socket(host, 8189);
            msg.set("");
            return s;
        } catch (IOException e) {
            msg.set("Server not active");
        }
        return null;
    }
    /* ----------------------------------------------------------------------------------------------------- */

    /* ----------------------------- Method the send mail -------------------------------------------------- */
    protected void sendMail(){
        if ("".equals(recipients.getValue()))
            msg.setValue("Insert a recipient");
        else if (!checkEmailAddress(recipients.getValue())){
            msg.setValue("Enter valid email addresses");
        }
        else {
            msg.setValue("");
            Socket s = tryConnect();
            if (s != null) {
                try {
                    ObjectOutputStream outStream = new ObjectOutputStream(s.getOutputStream());

                    outStream.writeObject(email_account); // send to server name account
                    outStream.writeObject(Request.SEND); // send to server request type

                    ObjectInputStream inStream = new ObjectInputStream(s.getInputStream());

                    // mail to send to the server
                    MailWrapper mail = new MailWrapper(
                            email_account,
                            recipients.getValue(),
                            object.getValue(),
                            text.getValue(),
                            new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime()));

                    outStream.writeObject(mail); // send

                    // message from the server indicating whether the mail has been sent or not
                    String message = inStream.readObject().toString();
                    msg.set(message);

                    if (message.equals("Email/s sent")) {
                        recipients.setValue("");
                        object.setValue("");
                        text.setValue("");
                    }
                } catch (IOException | ClassNotFoundException e) {
                    msg.set(e.getMessage());
                }
            }
        }
    }
    /* ----------------------------------------------------------------------------------------------------- */


    // private method to check email address
    private Boolean checkEmailAddress(String recipients){
        String recipients_string = recipients.replaceAll("\\s+","");
        String[] recipients_array = recipients_string.split(","); // individual recipients

        for (String s : recipients_array) {
            if (!Pattern.matches("[a-zA-Z0-9._%-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}", s)
                || (email_account.equals(s)))
                return false;
        }
        return true;
    }
}
