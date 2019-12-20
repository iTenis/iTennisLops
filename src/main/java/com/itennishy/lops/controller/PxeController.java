package com.itennishy.lops.controller;

import com.itennishy.lops.domain.iTennisConfig;
import com.itennishy.lops.utils.StatusCode;
import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.service.PxeServerConfigService;
import com.itennishy.lops.utils.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pxe")
public class PxeController {

    @Autowired
    private iTennisConfig iTennisConfig;

    @Autowired
    private PxeServerConfigService pxeServerConfigService;

    @RequestMapping("/local")
    public JsonData setPxeLocalServer() {
        JSchExecutor jSchUtil = new JSchExecutor();
        try {
            jSchUtil = new JSchExecutor(iTennisConfig.getPxeServer().get("user"), iTennisConfig.getPxeServer().get("password"), "127.0.0.1");
            jSchUtil.connect();
            int status = pxeServerConfigService.ConfigPxeServer(jSchUtil);
            if (status < 0) {
                return JsonData.BuildRequest(status, StatusCode.STATUS_ERROR);
            } else {
                return JsonData.BuildRequest(StatusCode.STATUS_OK);
            }
        } catch (Exception e) {
            return JsonData.BuildRequest(e.getMessage(),StatusCode.STATUS_ERROR);
        } finally {
            jSchUtil.disconnect();
        }
    }

    @RequestMapping("/remote")
    public JsonData setPxeRemoteServer(String ip, String user, String pwd) {
        JSchExecutor jSchUtil = new JSchExecutor();
        try {
            jSchUtil = new JSchExecutor(user, pwd, ip);
            jSchUtil.connect();
            int status = pxeServerConfigService.ConfigPxeServer(jSchUtil);
            if (status < 0) {
                return JsonData.BuildRequest(status, StatusCode.STATUS_ERROR);
            } else {
                return JsonData.BuildRequest(StatusCode.STATUS_OK);
            }
        } catch (Exception e) {
            return JsonData.BuildRequest(e.getMessage(),StatusCode.STATUS_ERROR);
        } finally {
            jSchUtil.disconnect();
        }
    }
}
