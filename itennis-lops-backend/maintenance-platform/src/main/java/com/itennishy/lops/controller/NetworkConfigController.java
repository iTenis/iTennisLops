package com.itennishy.lops.controller;

import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.service.NetworkConfigService;
import com.itennishy.lops.utils.DeviceDiscoveryUtils;
import com.itennishy.lops.utils.FileUtils;
import com.itennishy.lops.utils.JsonData;
import com.itennishy.lops.utils.StatusCode;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

@Slf4j
@Api(value = "网络配置接口", tags = "网络配置接口")
@RestController
@RequestMapping("/net")
public class NetworkConfigController {

    @Autowired
    private NetworkConfigService networkConfigService;

    @RequestMapping(value = "/config", method = RequestMethod.GET)
    public JsonData setNetPLConfig(String conf) {
        Vector<Map<String, Object>> vector = new Vector<>();
        try {
            List<String[]> contents = new FileUtils().getConfigContent(conf);
            List<FutureTask<Map<String, Object>>> futureTasks = new ArrayList<>();
            for (String[] content : contents) {

                futureTasks.add(new FutureTask<>(new Callable<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> call() throws Exception {
                        Map<String, Object> result = new HashMap<>();
                        JSchExecutor jSchUtil = new JSchExecutor();
                        try {
                            if (new DeviceDiscoveryUtils().getOnlineDevice(content[0])) {
                                jSchUtil = new JSchExecutor(content[1], content[2], content[0]);
                                jSchUtil.connect();
                                int netCount = (content.length - 3) / 7;
                                for (int i = 0; i < netCount; i++) {
                                    String bondx = content[i * 7 + 3 + 6];
                                    String bondy = content[i * 7 + 3 + 7];
                                    String bondxy = content[i * 7 + 3 + 1];
                                    String ipAddr = content[i * 7 + 3 + 3];
                                    String ipNetmask = content[i * 7 + 3 + 4];
                                    String ipGateway = content[i * 7 + 3 + 5];
                                    String mode = content[i * 7 + 3 + 2];
                                    networkConfigService.SetBonding(jSchUtil, bondx, bondy, bondxy, ipAddr, ipNetmask, ipGateway, mode);
                                    result.put(content[0], jSchUtil.getStandardOutput().toString());
                                }
                            } else {
                                result.put(content[0], null);
                            }
                        } catch (Exception e) {
                            log.error("执行命令出现异常:", e);
                            result.put(content[0], null);
                        } finally {
                            jSchUtil.disconnect();
                        }
                        return result;
                    }
                }));
            }
            if (contents.size() == 0) {
                return JsonData.BuildRequest(StatusCode.STATUS_NOFUND_CONF);
            }
            ExecutorService executorService = Executors.newFixedThreadPool(contents.size());
            for (FutureTask<Map<String, Object>> futureTask : futureTasks) {
                executorService.submit(futureTask);
            }
            executorService.shutdown();
            for (int i = 0; i < contents.size(); i++) {
                try {
                    Map<String, Object> flag = futureTasks.get(i).get();
                    vector.add(flag);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            return JsonData.BuildRequest(StatusCode.STATUS_ERROR);
        }
        return JsonData.BuildRequest(vector, StatusCode.STATUS_OK);
    }

    @RequestMapping(value = "/config", method = RequestMethod.POST)
    public JsonData setNetPLConfig(@RequestBody Map<String, Object> data) {
        try {
            new FileUtils().getContent2File(data.get("conf").toString(), data);
        } catch (Exception e) {
            return JsonData.BuildRequest(StatusCode.STATUS_NOFUND_CONF);
        }
        return setNetPLConfig(data.get("conf").toString());
    }
}
