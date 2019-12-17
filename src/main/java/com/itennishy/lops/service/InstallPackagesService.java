package com.itennishy.lops.service;

import com.itennishy.lops.domain.iTennisConfig;
import com.itennishy.lops.executor.JSchExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InstallPackagesService {

    @Autowired
    private iTennisConfig iTennisConfig;

    public void BeginInstallPackages(JSchExecutor jSchUtil) {
        try {
            for (String aPackage : iTennisConfig.getPackages()) {
                int status = jSchUtil.execCmd("yum list installed | grep " + aPackage);
                if (status != 0) {
                    log.info("正在安装:" + aPackage);
                    jSchUtil.execCmd("yum install -y " + aPackage);
                } else {
                    log.info("软件已经安装过:" + aPackage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
