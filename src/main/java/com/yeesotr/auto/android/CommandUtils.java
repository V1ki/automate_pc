package com.yeesotr.auto.android;

import com.yeesotr.auto.env.Environment;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class CommandUtils {


    private BufferedWriter stdin;
    private Process p = null;
    public CommandUtils() {

        // init shell
        ProcessBuilder builder = new ProcessBuilder("C:/Windows/System32/cmd.exe");
        builder.redirectErrorStream(true);

        try {
            p = builder.start();
        } catch (IOException e) {
            System.out.println(e);
        }
        // get stdin of shell
        stdin = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

        // execute commands

        // write stdout of shell (=output of all commands)
        Scanner scanner = new Scanner(p.getInputStream());
        Thread mThread = new Thread(()->{
            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }
            log.info("Thread stop!");
        });
        mThread.start();
    }

    public void close(){
        log.info("before scanner quit!");
        try {
            p.destroy();
        }catch (Exception e){
            log.warn(e.getMessage());
        }
    }

    public CommandUtils executeCommand(String command) {
        try {
            // single execution
            stdin.write(command);
            stdin.newLine();
            stdin.flush();
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
        return this ;
    }
    public static String execCommandSync(String command,int milliseconds) throws IOException {
        Runtime run = Runtime.getRuntime();
        Process pr = run.exec(command);
        try {
            pr.waitFor(milliseconds,TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String result = buf.lines().collect(Collectors.joining("\n"));

        log.info(" {} -- {} ",command, result);
        return result;
    }


    public static String execCommandSync(String command) throws IOException {
        return execCommandSync(command,500);
    }

    public static void rebootDevice(String serial) {
        String command = "cmd /c "+ Environment.ADB + " -s " + serial + " reboot" ;

        try {
            execCommandSync(command) ;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static int findAvailablePort(int startPort) throws Exception{
        String command = "cmd /c netstat -ano " ;
        String result = execCommandSync(command) ;
//        log.info("command:{} result:{} -",command,result.trim());

        while (result.contains(":"+startPort)){
            startPort++ ;
        }
        log.info("find available port:"+startPort);
        return startPort ;
//        if(result.trim().length() == 0){
//            return result ;
//        }
//
//        return findAvailablePort(startPort + 1) ;

    }

    public static void execCommandAsync(String command) throws Exception {

        new Thread(()-> {
            List<String> list = new ArrayList<>(Arrays.asList(
                    "C:\\Windows\\System32\\cmd.exe",
                    "/c", "set", "ANDROID_HOME=D:\\Android\\Sdk &"));
            list.addAll(Arrays.asList(command.split(" ")));
                ProcessBuilder builder = new ProcessBuilder(
                        list);
        Map<String, String> environment = builder.environment();
        environment.put("ANDROID_HOME", "D:\\Android\\Sdk");
//        builder.command("set","ANDROID_HOME=D:\\Android\\Sdk ;");
//        builder.command("echo", "%ANDROID_HOME% ;");
            Process pr = null;
            try {
                pr = builder.start();
                BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                String result = buf.lines().collect(Collectors.joining("\n"));
                log.info(result);
            } catch (IOException e) {
                e.printStackTrace();
            }

//        buf.close();

//        new Thread(() -> {
//            try {
//                BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
//                while (true) {
//                    String line = "";
//                    while ((line = buf.readLine()) != null) {
//                        System.out.println(line);
//                    }
//                }
//            }catch (Exception e){
//
//            }
//        }).start();
        }).start();
    }

}
