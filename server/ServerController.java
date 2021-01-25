package server;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import java.net.URL;
import java.util.ResourceBundle;

public class ServerController implements Initializable {
    private ServerModel model;

    @FXML
    private ListView<String> logArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (model != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }
        model = new ServerModel();

        logArea.setItems(model.logProperty());
        model.start(); // run thread server
    }
}
