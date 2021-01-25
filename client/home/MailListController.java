package client.home;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import common.mail.Email;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import client.write.WriteController;
import client.write.WriteModel;

import java.io.IOException;

public class MailListController {
    @FXML
    private ListView<Email> mailList;

    @FXML
    private Label email_account;

    @FXML
    private Label msg;


    private HomeModel model;

    public void initModel(HomeModel m, Stage stage){
        if(this.model != null){
            throw new IllegalStateException("Model can only be inizialized once");
        }
        this.model = m;

        // set account
        email_account.textProperty().bind(model.getEmailAccountProperty());

        // bind model list and list view
        mailList.setItems(model.getMailListProperty());
        model.currentMailProperty().bind(mailList.getSelectionModel().selectedItemProperty());

        // bind label error
        msg.textProperty().bind(m.getMsgProperty());

        // listener alert new common.mail
        model.getNotifyProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "You have new email/s!");
                alert.initOwner(stage);
                alert.setTitle("MESSAGE");
                alert.setHeaderText(email_account.getText());
                alert.show();
            }
        });

    }

    @FXML
    private void refresh(){
        model.askNews();
    }

    @FXML
    private void writeNewMail(ActionEvent event) throws IOException {
        BorderPane root = new BorderPane();

        String flag = ((Button)event.getSource()).getText(); // button type
        WriteModel writeModel = new WriteModel(email_account.getText());

        FXMLLoader newWriteLoader = new FXMLLoader(getClass().getClassLoader().getResource("client/write/writeEmail.fxml"));
        root.setCenter(newWriteLoader.load());
        WriteController writecontroller = newWriteLoader.getController();
        writecontroller.initModel(writeModel, email_account.getText(), flag, model.getCurrentMail());

        Stage stage = new Stage();
        stage.setTitle("New Email");
        stage.setResizable(false);
        stage.setWidth(730);
        stage.setHeight(730);
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    private void delete(){
        model.deleteMail();
        mailList.getSelectionModel().clearSelection();
    }
}
