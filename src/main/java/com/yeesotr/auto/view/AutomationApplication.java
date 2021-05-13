package com.yeesotr.auto.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;

public class AutomationApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("自动化测试");
        try {
            Parent root = FXMLLoader.load(getClass()
                    .getResource("/Main.fxml"));

            JMetro metro = new JMetro(Style.LIGHT) ;
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            metro.setScene(scene);
            metro.setParent(root);
        }catch (Exception e){
            e.printStackTrace();
        }



        primaryStage.setOnCloseRequest(e ->{
            System.exit(0);
        });


        primaryStage.show();
    }
}
