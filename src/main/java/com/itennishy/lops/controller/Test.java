package com.itennishy.lops.controller;

import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.service.PxeServerConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class Test {

    @Autowired
    private com.itennishy.lops.domain.iTennisConfig iTennisConfig;

    @RequestMapping("/test")
    public void test() {
        List<Map<String, String>> parts = iTennisConfig.getPartitions();
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> part : parts) {
            sb.append("part " + part.get("path") + " --fstype=\"" + part.get("fstype") + "\" --ondisk=" + part.get("ondisk") + " --size=" + part.get("size"));
            if ("1".equals(part.get("size"))) {
                sb.append(" --grow");
            }
            sb.append("\n");
        }
        System.out.println(sb);
//        System.out.println(installPackages.getPort());
//        JSchExecutor jSchUtil = new JSchExecutor("root", "19931103xhs-", "115.159.0.166");
//        new YumConfigService().setLocalYumReposity(jSchUtil,"/opt/xx.iso");

    }

    @Autowired
    private PxeServerConfigService pxeServerConfigService;

    @RequestMapping("/config/pxe")
    public void ConfigPxe() {
        JSchExecutor jSchUtil = new JSchExecutor(iTennisConfig.getPxeServer().get("user"), iTennisConfig.getPxeServer().get("password"), iTennisConfig.getPxeServer().get("ip"));
        pxeServerConfigService.ConfigPxeServer(jSchUtil);
    }
}
