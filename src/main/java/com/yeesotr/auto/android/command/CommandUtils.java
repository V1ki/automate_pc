package com.yeesotr.auto.android.command;

import com.yeesotr.auto.env.Environment;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class CommandUtils implements Commander{


    private final BufferedWriter stdin;
    private Process p = null;
    private final List<CommanderOutputCallback> callbackList = new ArrayList<>();

    public CommandUtils() {

        // init shell
        ProcessBuilder builder = new ProcessBuilder("C:/Windows/System32/cmd.exe");
        builder.redirectErrorStream(true);

        try {
            p = builder.start();
        } catch (IOException e) {
            log.warn("ProgressBuild init failed",e);
        }
        // get stdin of shell
        stdin = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

        // execute commands

        // write stdout of shell (=output of all commands)
        Scanner scanner = new Scanner(p.getInputStream());
        Thread mThread = new Thread(()->{
            while (scanner.hasNextLine()) {
                String newLine = scanner.nextLine();

                callbackList.forEach(callback -> callback.onNewline(newLine));

            }
            log.info("Thread stop!");
        });
        mThread.start();
    }

    public CommandUtils close(){
        log.info("before scanner quit!");
        try {
            p.destroy();
        }catch (Exception e){
            log.warn(e.getMessage());
        }
        return this;
    }

    @Override
    public void addOutputCallback(CommanderOutputCallback callback) {
        callbackList.add(callback);
    }

    @Override
    public void removeOutputCallback(CommanderOutputCallback callback) {
        callbackList.remove(callback);
    }

    public CommandUtils executeCommand(String command) {
        try {
            log.debug(command);
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
        log.debug(command);
        Runtime run = Runtime.getRuntime();
        Process pr = run.exec(command);
        try {
            pr.waitFor(milliseconds,TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));

        return buf.lines().collect(Collectors.joining("\n"));
    }

    public static String execCommandSync(int milliseconds, String... command) throws IOException {
        Runtime run = Runtime.getRuntime();
        Process pr = run.exec(command);
        try {
            pr.waitFor(milliseconds,TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));

        return buf.lines().collect(Collectors.joining("\n"));
    }



    public static String execCommandSync(String command) throws IOException {
        return execCommandSync(command,500);
    }

    public static void rebootDevice(String serial) {
        String command = Environment.ADB + " -s " + serial + " reboot" ;

        try {
            execCommandSync(command) ;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static int findAvailablePort(int startPort) throws Exception{
        String command = "netstat -an " ;
        String result = execCommandSync(command) ;
//        log.info("command:{} result:{} -",command,result.trim());

        String concatSymbol = Environment.isMacos() ? "." : ":" ;
        while (result.contains(concatSymbol+startPort)){
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

    public static void execCommandAsync(String command) {

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
            Process pr;
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
