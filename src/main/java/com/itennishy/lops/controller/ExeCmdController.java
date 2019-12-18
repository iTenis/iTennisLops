package com.itennishy.lops.controller;

import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.utils.FileUtils;
import com.itennishy.lops.utils.JsonData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/exec")
public class ExeCmdController {

    /**
     * 远程执行命令
     * http://127.0.0.1:8081/exec/define?ip=192.168.0.160&user=root&pwd=123456&cmd=free -m
     *
     * @param ip
     * @param user
     * @param pwd
     * @param cmd
     * @return
     */
    @RequestMapping("/define")
    public JsonData ExeCmd(String ip, String user, String pwd, @RequestParam("cmd") String cmd) {

        JSchExecutor jSchUtil = new JSchExecutor();
        try {
            jSchUtil = new JSchExecutor(user, pwd, ip);
            jSchUtil.connect();
            jSchUtil.execCmd(cmd);
            return JsonData.BuildSuccess(200, jSchUtil.getStandardOutput(), "发送请求成功");
        } catch (Exception e) {
            return JsonData.BuildError(50001, e.getMessage());
        } finally {
            jSchUtil.disconnect();
        }
    }

    /**
     * http://127.0.0.1:8081/exec/config?conf=hosts.conf&cmd=free -m
     *
     * @param conf
     * @param cmd
     * @return
     */
    @RequestMapping("/config")
    public JsonData ExeCmds(String conf, @RequestParam("cmd") String cmd) {
        Vector<String> vector = new Vector<>();
        try {
            List<String[]> contents = new FileUtils().getConfigContent(conf);
            AtomicInteger t = new AtomicInteger();
            List<FutureTask<String>> futureTasks = new ArrayList<>();
            for (String[] content : contents) {
                if (content.length == 3) {
                    t.set(1);
                } else if (content.length == 4) {
                    t.set(0);
                } else {
                    log.error("配置文件内容有问题");
                }

                futureTasks.add(new FutureTask<>(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        JSchExecutor jSchUtil = new JSchExecutor();
                        try {
                            t.set(1);
                            jSchUtil = new JSchExecutor(content[2 - t.get()], content[3 - t.get()], content[0]);
                            jSchUtil.connect();
                            jSchUtil.execCmd(cmd);
                            return content[0] + ":" + jSchUtil.getStandardOutput();
                        } catch (Exception e) {
                            log.error("上传文件出现异常:", e);
                        } finally {
                            jSchUtil.disconnect();
                        }
                        return content[0] + ":fail";
                    }
                }));
            }

            ExecutorService executorService = Executors.newFixedThreadPool(contents.size());
            for (FutureTask<String> futureTask : futureTasks) {
                executorService.submit(futureTask);
            }
            executorService.shutdown();
            for (int i = 0; i < contents.size(); i++) {
                try {
                    String flag = futureTasks.get(i).get();
                    vector.add(flag);
                    System.out.println(flag);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            return JsonData.BuildError(50001, e.getMessage());
        }
        return JsonData.BuildSuccess(vector);
    }

    /**
     * 配置时间时区
     * http://127.0.0.1:8081/exec/timezone?conf=hosts.conf&date=2019-12-20 17:48:00
     */
    @RequestMapping("/timezone")
    public JsonData setTimeZone(String conf, String date) {
        JsonData jsonData;
        if ("".equals(date)) {
            jsonData = ExeCmds(conf, "ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime;hwclock &> /dev/null;date -s \"`date '+%Y-%m-%d %H:%M:%S'`\" &> /dev/null");
        } else {
            jsonData = ExeCmds(conf, "ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime;hwclock &> /dev/null;date -s \"" + date + "\" &> /dev/null");
        }
        return jsonData;
    }

    /**
     * 清除MBR引导
     * 解决重装操作系统时进入旧系统问题,重启后操作系统将无法进入原系统
     * http://127.0.0.1:8081/mbr/clean/all?conf=hosts.conf
     */
    @RequestMapping("/mbr/clean/all")
    public JsonData ClearMBR(String conf) {
        JsonData jsonData = ExeCmds(conf, "dd if=/dev/zero of=/dev/sda bs=1k count=1 && echo 'Clear MBR OK' || echo 'Clear MBR failed'");
        return jsonData;
    }

}
