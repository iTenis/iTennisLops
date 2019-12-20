package com.itennishy.lops.controller;

import com.itennishy.lops.utils.StatusCode;
import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.service.UpDownFileService;
import com.itennishy.lops.utils.FileUtils;
import com.itennishy.lops.utils.JsonData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
@Api(value = "文件上传下载类",tags = "文件操作类")
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
    @ApiOperation(value = "文件上传下载", notes = "根据指定地址上传下载")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "ip", value = "ip地址", dataType = "String", required = true),
            @ApiImplicitParam(name = "user", value = "用户名", dataType = "String", required = true),
            @ApiImplicitParam(name = "pwd", value = "密码", dataType = "String", required = true),
            @ApiImplicitParam(name = "remote", value = "远程路径", dataType = "String", required = true),
            @ApiImplicitParam(name = "local", value = "本地路径", dataType = "String", required = true),
    })
    @RequestMapping(value = "/define", method = RequestMethod.GET)
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
    @ApiOperation(value = "配置RAID", notes = "根据配置文件批量配置RAID")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "remote", value = "远程路径", dataType = "String", required = true),
            @ApiImplicitParam(name = "local", value = "本地路径", dataType = "String", required = true),
            @ApiImplicitParam(name = "conf", value = "配置文件名", dataType = "String", required = true),
            @ApiImplicitParam(name = "mode", value = "上传或者下载", dataType = "String", required = true),
    })
    @RequestMapping(value = "/config", method = RequestMethod.GET)
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
