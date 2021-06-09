package com.yeesotr.auto.view.controller;

import com.yeesotr.auto.android.model.Device;
import com.yeesotr.auto.lua.LogUtils;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;

public class ConsoleViewController implements Initializable, DeviceOperator, LogUtils.LogDisplay {
    public ScrollPane scrollPane;
    public TextFlow textFlow;
    private Font font ;
    private Transition down ;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        font = Font.loadFont(getClass().getResourceAsStream("/fonts/SF-Mono-Regular.otf"), 14);
        this.down = new Transition() {
                    {
                        setCycleDuration(Duration.INDEFINITE);
                    }
                    @Override
                    protected void interpolate(double v) {

                        scrollPane.setVvalue(scrollPane.getVvalue()+0.01);
                        if(scrollPane.getVvalue() >= 1){
                            down.stop();
                        }

                    }
                };
        textFlow.heightProperty().addListener((observable, oldValue, newValue) -> {

            down.play();
//            scrollPane.setVvalue(1.0);
        });
    }

    @Override
    public void setDevice(Device device) {
        textFlow.getChildren().clear();

        // read info from file ;
        // 获取当前测试日志的文件
        File file = device.getCurrentLog();
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Text text = new Text(line+ "\n");
                text.setFill(Color.WHITE);
                text.setFont(font);
                textFlow.getChildren().add(
                        text
                );
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void info(String time, String tag, String msg) {
        Platform.runLater(()->{
            String line = time + LogUtils.SEPARATOR + tag   + LogUtils.SEPARATOR + msg + "\n";
            Text text = new Text(line);
            text.setFill(Color.RED);
            text.setFont(font);
            textFlow.getChildren().add(
                    text
            );
        });
    }
}
