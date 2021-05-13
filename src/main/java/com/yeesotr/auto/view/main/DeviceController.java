package com.yeesotr.auto.view.main;

import com.yeesotr.auto.android.command.ADBCommand;
import com.yeesotr.auto.android.model.Device;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class DeviceController implements Initializable {
    public ImageView imageView;

    private Device device;
    private ADBCommand command ;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Platform.runLater(()->{
            try {




                new Thread(){
                    @Override
                    public void run() {
                        while (true){
                            try {
                                long startTime = System.currentTimeMillis() ;
                                File file= command.screenshot();

                                FileInputStream fileInputStream = new FileInputStream(file);
                                imageView.setImage(new Image(fileInputStream));

                                fileInputStream.close();
                                log.info("screenshot --spend time:{}", System.currentTimeMillis()- startTime);

                                Thread.sleep( startTime <1000 ? 1000 - startTime : 10 );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.start();


            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }


    public void setDevice(Device device) {
        this.device = device;
        command = new ADBCommand(device) ;
    }
}
