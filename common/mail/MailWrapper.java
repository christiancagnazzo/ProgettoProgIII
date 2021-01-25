package common.mail;

import java.io.Serializable;

/* Class used to represent a mail in the exchange of messages between client and server */

public class MailWrapper implements Serializable {
    private int id;
    private final String sender;
    private final String recipients;
    private final String object;
    private final String text;
    private final String date;

    public MailWrapper(int id, String sender, String recipients, String object, String text, String date) {
        this.id = id;
        this.sender = sender;
        this.recipients = recipients;
        this.object = object;
        this.text = text;
        this.date = date;
    }

    public MailWrapper(String sender, String recipients, String object, String text, String date) {
        this.sender = sender;
        this.recipients = recipients;
        this.object = object;
        this.text = text;
        this.date = date;
    }

    public int getId(){ return id; }
    public void setId(int i){ id = i; }

    public String getSender(){
        return sender;
    }

    public String getRecipients() {
        return recipients;
    }

    public String getObject() {
        return object;
    }

    public String getText() {
        return text;
    }

    public String getDate(){
        return date;
    }
}

