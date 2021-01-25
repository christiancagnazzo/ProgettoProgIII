package client.write;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import common.mail.Email;

public class WriteController {
    private WriteModel model;

    @FXML
    private TextField recipients;

    @FXML
    private TextField object;

    @FXML
    private TextArea text;

    @FXML
    private Label msg;

    public void initModel(WriteModel m, String user, String flag, Email selected_mail) {
        if(this.model != null){
            throw new IllegalStateException("Model can only be inizialized once");
        }
        this.model = m;

        // bind bidirectional because once the email is sent the model removes the written fields
        model.getRecipientsProperty().bindBidirectional(recipients.textProperty());
        model.getObjectProperty().bindBidirectional(object.textProperty());
        model.getTextProperty().bindBidirectional(text.textProperty());
        msg.textProperty().bind(m.getMsgProperty());


        switch (flag) {
            case "New":
                break;
            case "Reply":
                if (selected_mail != null){
                    recipients.textProperty().setValue(selected_mail.getSender());
                }
                break;
            case "Reply-all":
                if (selected_mail != null){
                    StringBuilder recipients_to = new StringBuilder();
                    recipients_to.append(selected_mail.getSender()).append(",");
                    String recipients_string = selected_mail.getRecipients().replaceAll("\\s+","");
                    String[] recipients_array = recipients_string.split(","); // individual recipients
                    for (String s : recipients_array) {
                        if (!s.equals(user))
                            recipients_to.append(s).append(",");
                    }
                    recipients_to.setLength(recipients_string.length()); // remove last ,
                    recipients.textProperty().setValue(recipients_to.toString());
                }
                break;
            case "Forward":
                if (selected_mail != null){
                    text.textProperty().setValue(selected_mail.getText());
                    object.textProperty().setValue(selected_mail.getObject());
                }
                break;
        }
    }

    @FXML
    public void Send(){
        model.sendMail();
    }
}

