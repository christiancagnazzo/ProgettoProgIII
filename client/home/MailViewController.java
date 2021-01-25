package client.home;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class MailViewController {
    @FXML
    private TextArea sender;
    @FXML
    private TextArea recipients;
    @FXML
    private TextArea object;
    @FXML
    private TextArea text;

    private HomeModel model;

    public void initModel(HomeModel m){
        if(this.model != null){
            throw new IllegalStateException("Model can only be inizialized once");
        }
        this.model = m;

        // The selected email is displayed in the box
        model.currentMailProperty().addListener((observable,oldValue,newValue) -> {
            if(oldValue != null){
                text.textProperty().unbind();
                sender.textProperty().unbind();
                object.textProperty().unbind();
                recipients.textProperty().unbind();
            }
            if(newValue == null){
                text.setText("");
                sender.setText("");
                object.setText("");
                recipients.setText("");
            }else{
                text.textProperty().bind(newValue.getTextProperty());
                sender.textProperty().bind(newValue.getSenderProperty());
                object.textProperty().bind(newValue.getObjectProperty());
                recipients.textProperty().bind(newValue.getRecipientsProperty());
            }
        });
    }
}
