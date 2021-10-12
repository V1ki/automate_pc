package com.yeesotr.auto.android;

import com.yeesotr.auto.android.model.Device;
import com.yeesotr.auto.appium.Appium;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import javafx.application.Platform;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbServices;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 *
 * 用于管理设备列表，以及一些相关的内容
 * @author bugs.wan
 * @since 1.0.3
 * @version 1.0.3
 */
@Slf4j
public class DeviceManager {


    public interface DeviceChangeListener {
        void onDeviceAttached(Device device);
        void onDeviceDetached(Device device);
    }


    private Disposable usbDisposable;
    private List<Device> deviceLst = new ArrayList<>();
    @Setter
    private DeviceChangeListener listener ;

    public DeviceManager(){
        //  start listen usb
        listenUsbEvent();
        //  add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            log.info("shutdown!!");
            usbDisposable.dispose();
        }));
    }


    /**
     * 调用USB 服务来监听usb的插拔事件
     */
    private void listenUsbEvent() {
        try {
            UsbServices services = UsbHostManager.getUsbServices();

            usbDisposable = Observable.create(emitter -> {
                services.addUsbServicesListener(new UsbServicesListener() {
                    @Override
                    public void usbDeviceAttached(UsbServicesEvent event) {
                        if (!emitter.isDisposed()) {
                            //发送消息
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            emitter.onNext(event);
                        }
                    }
                    @Override
                    public void usbDeviceDetached(UsbServicesEvent event) {
                        if (!emitter.isDisposed()) {
                            //发送消息
                            emitter.onNext(event);
                        }
                    }
                });

            }).throttleLast(2, TimeUnit.SECONDS)
                    .subscribe((arg) -> refreshDeviceList());
        } catch (UsbException e) {
            e.printStackTrace();
        }

    }

    /**
     * 调用 adb devices -l 来检查目前的设备列表。
     */
    private void refreshDeviceList() {
        List<Device> deviceList = Device.getConnectedDevices();
        log.info("deviceList:{}", deviceList);
        List<Device> tmpResult = new ArrayList<>(deviceLst);
        List<Device> needRemoveDeviceList = new ArrayList<>() ;

        // 需要添加的设备,没有选中, 不需要处理.
        deviceList.forEach(d -> {
            if (!deviceLst.contains(d)) {
                tmpResult.add(d);
            }
        });
        // 已经断开了的设备,需要对其相应的服务进行关闭.
        deviceLst.forEach(d -> {
            if (!deviceList.contains(d)) {
                tmpResult.remove(d);
                needRemoveDeviceList.add(d) ;
            }
        });
        deviceLst.removeAll(needRemoveDeviceList) ;
        for(Device detachDevice: needRemoveDeviceList){
            if(listener != null) listener.onDeviceDetached(detachDevice);
        }
        needRemoveDeviceList.stream().map(Device::getAppium).filter(Objects::nonNull).forEach(Appium::stop);

        for (Device attachedDevice : tmpResult) {
            if (!deviceLst.contains(attachedDevice)) {
                deviceLst.add(attachedDevice);
                if(listener != null) listener.onDeviceAttached(attachedDevice);
            }
        }
        log.info("refreshDeviceList");

    }

    public void refresh(){
        refreshDeviceList();
    }


    public void runDeviceTask(Device device, String luaPath){
        // 更新设备的状态
        Globals globals = device.getGlobals();
        LuaValue chunk = globals.loadfile(luaPath);
        log.info("chunk:{}",chunk);
        LuaValue result = chunk.call(LuaValue.valueOf(luaPath));
        log.info("result:{}", result);
    }


    public static void main(String[] args) {
        DeviceManager deviceManager = new DeviceManager();
        deviceManager.setListener(new DeviceChangeListener() {
            @Override
            public void onDeviceAttached(Device device) {
                log.info("onDeviceAttached:{}!",device);
                try {
                    device.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDeviceDetached(Device device) {
                log.info("onDeviceDetached:{}!",device);
            }
        });
        Scanner scanner = new Scanner(System.in);
        String nextLine = "";
        while (true){
            nextLine = scanner.nextLine();
            if ("Hello".equalsIgnoreCase(nextLine)){
                System.exit(0);
            }
        }

    }

}
