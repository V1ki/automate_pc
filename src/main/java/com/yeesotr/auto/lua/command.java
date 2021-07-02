package com.yeesotr.auto.lua;

import com.yeesotr.auto.android.Automation;
import com.yeesotr.auto.android.model.Device;
import io.appium.java_client.MobileElement;
import javafx.application.Platform;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.DataInputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Sample library that can be called via luaj's require() implementation.
 * <p>
 * This library, when loaded, creates a lua package called "com.yeesotr.auto.lua.hyperbolic"
 * which has two functions, "sinh()" and "cosh()".
 * <p>
 * Because the class is in the default Java package, it can be called using
 * lua code such as:
 *
 * <pre> {@code
 * require 'com.yeesotr.auto.lua.hyperbolic'
 * print('sinh',  com.yeesotr.auto.lua.hyperbolic.sinh)
 * print('sinh(1.0)',  com.yeesotr.auto.lua.hyperbolic.sinh(1.0))
 * }</pre>
 * <p>
 * When require() loads the code, two things happen: 1) the public constructor
 * is called to construct a library instance, and 2) the instance is invoked
 * as a java call with no arguments.  This invocation should be used to initialize
 * the library, and add any values to globals that are desired.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class command extends TwoArgFunction {

    private Automation automation;

    /**
     * Public constructor.  To be loaded via require(), the library class
     * must have a public constructor.
     */
    public command() {
    }

    public command(Automation automation) {
        this.automation = automation;
    }

    /**
     * The implementation of the TwoArgFunction interface.
     * This will be called once when the library is loaded via require().
     *
     * @param modname LuaString containing the name used in the call to require().
     * @param env     LuaValue containing the environment for this function.
     * @return Value that will be returned in the require() call.  In this case,
     * it is the library itself.
     */
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaValue library = tableOf();
        Device device = Optional.ofNullable(this.automation).map(Automation::getDevice).orElse(null) ;
        log.info(" -- command init");
        library.set("iozone", new iozone(this.automation));
        library.set("pull", new pull(device));
        library.set("shell", new shell(device));
        env.set("command", library);

        env.set("log", new log(device));
        env.get("package").get("loaded").set("command", library);
        return library;
    }

    /* Each library function is coded as a specific LibFunction based on the
     * arguments it expects and returns.  By using OneArgFunction, rather than
     * LibFunction directly, the number of arguments supplied will be coerced
     * to match what the implementation expects.  */

    /**
     * Mathematical sinh function provided as a OneArgFunction.
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    static class iozone extends OneArgFunction {
        private Automation automation;
        private final Object lock = new Object();


        public iozone(Automation automation) {
            this.automation = automation;
        }

        public LuaValue call(LuaValue x) {
            String iozoneOption = x.tojstring();
            log.info("iozoneOption:{}", iozoneOption);

            automation.terminateApp("com.yeestor.iozone");

            automation.pullToForeground("com.yeestor.iozone", ".MainActivity");
            MobileElement iozoneOptionText = automation.waitForPresenceMS(2000, "com.yeestor.iozone:id/iozoneOptionText");
            while (iozoneOptionText == null) {
                //
                automation.dismiss();
                iozoneOptionText = automation.waitForPresenceMS(2000, "com.yeestor.iozone:id/iozoneOptionText");

                log.info(" can not found OptionText");
                try {
                    Thread.sleep(1000 * 60 );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            iozoneOptionText.clear();
            iozoneOptionText.setValue(iozoneOption);

            Device currentDevice = automation.getDevice();

            StringBuffer sb = new StringBuffer();

            currentDevice.startIozoneLog();
            new Thread(() -> {
                Socket socket = null;
                DataInputStream dis;
                try {
                    socket = new Socket("localhost", currentDevice.getForwardPort());
                    log.debug("建立新连接:" + socket);
                    dis = new DataInputStream(socket.getInputStream());

                    while (true) {
                        String s = dis.readUTF();
                        sb.append(s);
                        if (s.contains("iozone test complete")) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.info(currentDevice.execAdb("forward --list"));

                    try {
                        socket = new Socket("localhost", currentDevice.getForwardPort());
                        log.debug("建立新连接:" + socket);
                        dis = new DataInputStream(socket.getInputStream());

                        while (true) {
                            String s = dis.readUTF();
                            sb.append(s);
                            if (s.contains("iozone test complete")) {
                                break;
                            }

                        }
                    }
                    catch (Exception e1){
                        e1.printStackTrace();
                    }

                } finally {
                    try {
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                synchronized (lock) {
                    lock.notifyAll();
                }
                log.info("iozone log over!");
            }).start();

            MobileElement startBtn = automation.waitForPresence(1, "com.yeestor.iozone:id/startBtn");
            startBtn.click();

            // 这个等待时间可能会很久....
            try {
                synchronized (lock) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 等待时间太久了,导致出现了newCommandTimeOut的问题,所以,这个地方需要检测一下session的状态,如果session 的状态不对的话,就需要重新连接.

            if (!automation.checkStatus()) {
                // driver 异常,需要重新连接Appium.
                automation.reconnect();
            }


            automation.terminateApp("com.yeestor.iozone");
            log.info("--- iozone return");
            return LuaValue.valueOf(sb.toString());
        }
    }

    static class log extends TwoArgFunction {

        private Device device;

        public log(Device device) {
            this.device = device;
        }

        @Override
        public LuaValue call(LuaValue tag, LuaValue msg) {
            log.info("tag:{} msg: {}", tag, msg);

            LogUtils.writeLog(device, tag.tojstring(), msg.toString());

            return LuaValue.valueOf("");
        }
    }

    static class pull extends TwoArgFunction {

        private Device device;

        public pull(Device device) {
            this.device = device;
        }


        @Override
        public LuaValue call(LuaValue arg1, LuaValue arg2) {
            String deviceFileLoc = arg1.tojstring() ;
            String saveLoc = arg2.tojstring() ;

            log.info("deviceFileLoc:{} - saveLoc:{}", deviceFileLoc,saveLoc);
            if(device != null) {
                device.execAdb(" pull "+ deviceFileLoc + " "+saveLoc) ;
            }
            return LuaValue.valueOf("");
        }
    }

    static class shell extends OneArgFunction {

        private Device device;

        public shell(Device device) {
            this.device = device;
        }


        @Override
        public LuaValue call(LuaValue arg) {
            String shell = arg.tojstring() ;
            log.info("run shell:{} with device:{}", shell, Optional.ofNullable(device).map(Device::getSerial).orElse(null));

            if(device != null) {
               String result = device.execAdbShell(shell) ;
                log.info("shell:{} - result:{}", shell,result);
                return LuaValue.valueOf(result);
            }
            return LuaValue.valueOf("");
        }
    }
}