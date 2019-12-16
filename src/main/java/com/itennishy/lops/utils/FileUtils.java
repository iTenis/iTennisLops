package com.itennishy.lops.utils;

import org.springframework.boot.system.ApplicationHome;

import java.io.File;

public class FileUtils {

    public String getJarPath() {
        ApplicationHome h = new ApplicationHome(getClass());
        File jarF = h.getSource();
        return jarF.getParentFile().toString();
    }
}
