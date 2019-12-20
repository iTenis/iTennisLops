package com.itennishy.lops.controller;

import com.itennishy.lops.utils.StatusCode;
import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.service.UpDownFileService;
import com.itennishy.lops.utils.FileUtils;
import com.itennishy.lops.utils.JsonData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/updown")
public class UpDownFileController {

    @Autowired
    private UpDownFileService upDownFileService;

    /**
     * 上传文件
     * <p>
     * http://127.0.0.1:8081/updown/define?ip=192.168.0.160&user=root&pwd=123456&remote=/tmp/boot&local=/Users/apple/AllWorkCodeStation/JavaCodeProject/iTennisLops/boot&mode=upload
     *
     * @param ip
     * @param user
     * @param pwd
     * @param remote
     * @param local
     * @return
     */
    @RequestMapping("/define")
    public JsonData upDownLoadFile(String ip, String user, String pwd, @RequestParam("remote") String remote, @RequestParam("local") String local, String mode) {
        if ("upload".equals(mode) || "download".equals(mode)) {
            log.info("您选择的文件模式为:" + mode);
        } else {
            return JsonData.BuildRequest(StatusCode.STATUS_PARAMS_ERROR);
        }
        JSchExecutor jSchUtil = new JSchExecutor();
        try {
            jSchUtil = new JSchExecutor(user, pwd, ip);
            jSchUtil.connect();
            if ("upload".equals(mode)) {
                upDownFileService.upLoadFile(jSchUtil, remote, local);
            } else if ("download".equals(mode)) {
                upDownFileService.downLoadFile(jSchUtil, remote, local);
            } else {
                log.error("选择的模式有问题");
            }
            return JsonData.BuildRequest(StatusCode.STATUS_OK);
        } catch (Exception e) {
            return JsonData.BuildRequest(e.getMessage(), StatusCode.STATUS_ERROR);
        } finally {
            jSchUtil.disconnect();
        }

    }


    /**
     * 通过配置文件批量上传和下载文件
     * http://127.0.0.1:8081/updown/config?remote=/tmp/boot&local=/Users/apple/AllWorkCodeStation/JavaCodeProject/iTennisLops/boot&conf=hosts.conf&mode=upload
     *
     * @param remote
     * @param local
     * @param conf
     * @param mode   upload或者download
     * @return
     */
    @RequestMapping("/config")
    public JsonData upDownLoadFile(@RequestParam("remote") String remote, @RequestParam("local") String local, String conf, String mode) {
        Vector<String> vector = new Vector<>();
        try {
            if ("upload".equals(mode) || "download".equals(mode)) {
                log.info("您选择的文件模式为:" + mode);
            } else {
                return JsonData.BuildRequest(StatusCode.STATUS_PARAMS_ERROR);
            }
            List<String[]> contents = new FileUtils().getConfigContent(conf);
            AtomicInteger t = new AtomicInteger();
            List<FutureTask<String>> futureTasks = new ArrayList<>();
            for (String[] content : contents) {
                t.set(new FileUtils().getFlag(conf, content));

                futureTasks.add(new FutureTask<>(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        JSchExecutor jSchUtil = new JSchExecutor();
                        try {
                            t.set(1);
                            jSchUtil = new JSchExecutor(content[2 - t.get()], content[3 - t.get()], content[0]);
                            jSchUtil.connect();
                            if ("upload".equals(mode)) {
                                upDownFileService.upLoadFile(jSchUtil, remote, local);
                            } else if ("download".equals(mode)) {
                                upDownFileService.downLoadFile(jSchUtil, remote, local);
                            } else {
                                log.error("选择的模式有问题");
                            }
                            return content[0] + ":success";
                        } catch (Exception e) {
                            log.error("上传文件出现异常:", e);
                        } finally {
                            jSchUtil.disconnect();
                        }
                        return content[0] + ":fail";
                    }
                }));
            }
            if (contents.size() == 0) {
                return JsonData.BuildRequest(StatusCode.STATUS_NOFUND_CONF);
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
            return JsonData.BuildRequest(e.getMessage(), StatusCode.STATUS_ERROR);
        }
        return JsonData.BuildRequest(vector, StatusCode.STATUS_OK);
    }
}
