package com.itennishy.lops.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.system.ApplicationHome;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileUtils {

    /**
     * 获取jar包所在路径
     *
     * @return
     */
    public String getJarPath() {
        ApplicationHome h = new ApplicationHome(getClass());
        File jarF = h.getSource();
        return jarF.getParentFile().toString();
    }

    /**
     * 获取jar包所在路径
     *
     * @return
     */
    public String getPath() {
        String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        if (System.getProperty("os.name").contains("dows")) {
            path = path.substring(1, path.length());
        }
        if (path.contains("jar")) {
            path = path.substring(0, path.lastIndexOf("."));
            return path.substring(0, path.lastIndexOf("/"));
        }
        return path.replace("target/classes/", "");
    }

    /**
     * 读取jar路径下的conf下的配置信息，屏蔽以#开头的行，每行内容以空格或者制表符进行分割
     *
     * @param filename
     * @return
     * @throws Exception
     */
    public List<String[]> getConfigContent(String filename) throws Exception {
        List<String[]> contents = new ArrayList<>();
        File file = new File(new FileUtils().getPath() + "conf" + File.separator + filename);
        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    contents.add(line.split("\t| "));
                }
            }
            return contents;
        } else {
            log.error("----文件不存在:" + filename);
        }
        return contents;
    }

    public int getFlag(String conf, String[] content) {
        int t = 0;
        if ("hosts.conf".equals(conf)) {
            if (content.length == 3) {
                t = 1;
            } else if (content.length == 4) {
                t = 0;
            } else {
                log.error("配置文件内容有问题");
            }
        } else {
            t = 1;
        }
        return t;
    }

}
