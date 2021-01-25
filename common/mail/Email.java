package common.mail;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/* Class used by the client to store (observable) emails. */

public class Email {
    private final int id;
    private final StringProperty sender = new SimpleStringProperty();
    private final StringProperty recipients = new SimpleStringProperty();
    private final StringProperty object = new SimpleStringProperty();
    private final StringProperty text = new SimpleStringProperty();
    private final StringProperty date = new SimpleStringProperty();

    public Email(int id, String sender, String recipients, String object, String text, String date) {
        this.id = id;
        this.sender.setValue(sender);
        this.recipients.setValue(recipients);
        this.object.setValue(object);
        this.text.setValue(text);
        this.date.setValue(date);
    }

    public int getId(){ return id;}

    public String getSender() {
        return sender.get();
    }
    public StringProperty getSenderProperty(){
        return this.sender;
    }

    public StringProperty getRecipientsProperty(){ return this.recipients; }
    public String getRecipients(){ return recipients.get(); }

    public String getObject() {
        return object.get();
    }
    public StringProperty getObjectProperty(){
        return this.object;
    }

    public StringProperty getTextProperty(){
        return this.text;
    }
    public String getText() { return text.get(); }

    public String getDate() {
        return date.get();
    }

    @Override
    public String toString() {
        return  "FROM: "+ getSender()+ "\t\t\t\t\t\t\t" + getDate()+ "\nOBJECT: " + getObject();
    }
}