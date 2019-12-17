package com.itennishy.lops.controller;

import com.itennishy.lops.domain.iTennisConfig;
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
        try {
            JSchExecutor jSchUtil = new JSchExecutor(iTennisConfig.getPxeServer().get("user"), iTennisConfig.getPxeServer().get("password"), "127.0.0.1");
            pxeServerConfigService.ConfigPxeServer(jSchUtil);
            return JsonData.BuildSuccess();
        } catch (Exception e) {
            return JsonData.BuildError(50001, e.getMessage());
        }
    }

    @RequestMapping("/remote")
    public JsonData setPxeRemoteServer(String ip, String user, String pwd) {
        try {
            JSchExecutor jSchUtil = new JSchExecutor(user, pwd, ip);
            pxeServerConfigService.ConfigPxeServer(jSchUtil);
            return JsonData.BuildSuccess();
        } catch (Exception e) {
            return JsonData.BuildError(50001, e.getMessage());
        }
    }
}
