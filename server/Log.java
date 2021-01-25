package server;


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;

/* Class to represent server log messages */

public class Log {
    private ObservableList<String> log_list = FXCollections.observableList(new ArrayList<>());

    protected ObservableList<String> getLogProperty() {return log_list;}

    public void addLog(String event){
        Platform.runLater(() -> log_list.add(event));
    }

}
