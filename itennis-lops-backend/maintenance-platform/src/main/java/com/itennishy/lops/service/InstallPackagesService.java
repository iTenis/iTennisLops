package com.itennishy.lops.service;

import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InstallPackagesService {

    public void BeginInstallPackages(JSchExecutor jSchUtil) {
        try {
            FileUtils fileUtils = new FileUtils();
            for (String aPackage : fileUtils.getPxeConfigYmlWithpackages()) {
                int status = jSchUtil.execCmd("yum list installed | grep " + aPackage);
                if (status != 0) {
                    log.warn("----安装缺失软件:" + aPackage);
                    jSchUtil.execCmd("yum install -y " + aPackage);
                } else {
                    log.warn("----软件已经安装过:" + aPackage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
