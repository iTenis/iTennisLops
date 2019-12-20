package com.itennishy.lops.controller;

import com.itennishy.lops.utils.StatusCode;
import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.utils.DeviceDiscoveryUtils;
import com.itennishy.lops.utils.FileUtils;
import com.itennishy.lops.utils.JsonData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/raid")
public class RaidController {

    /**
     * 批量做Raid
     *
     * @param conf
     * @return
     */
    @RequestMapping("/config")
    public JsonData setRaid(String conf) {
        Vector<Map<String, String>> vector = new Vector<>();
        try {
            List<String[]> contents = new FileUtils().getConfigContent(conf);
            List<FutureTask<Map<String, String>>> futureTasks = new ArrayList<>();
            for (String[] content : contents) {
                futureTasks.add(new FutureTask<>(new Callable<Map<String, String>>() {
                    @Override
                    public Map<String, String> call() throws Exception {
                        Map<String, String> result = new HashMap<>();
                        JSchExecutor jSchUtil = new JSchExecutor();
                        try {
                            if (new DeviceDiscoveryUtils().getOnlineDevice(content[0])) {
                                jSchUtil = new JSchExecutor(content[1], content[2], content[0]);
                                jSchUtil.connect();
                                String path = new FileUtils().getPath();
                                jSchUtil.upLoadFile(path + "boot" + File.separator + "tools" + File.separator + "megacli", "/tmp/itennis_tmp", 0);
                                jSchUtil.execCmd("rpm -ivh /tmp/itennis_tmp/megacli/Lib_Utils-1.00-09.noarch.rpm &> /dev/null");
                                jSchUtil.execCmd("rpm -ivh /tmp/itennis_tmp/megacli/MegaCli-8.07.10-1.noarch.rpm &> /dev/null");
                                jSchUtil.execCmd("/opt/MegaRAID/MegaCli/MegaCli64 -PDList -aALL | grep Adapter | awk -F '#' '{print $2}'");
                                Matcher matcherAdapter = Pattern.compile("^\\[(.*?)]$").matcher(jSchUtil.getStandardOutput().toString());
                                String strAdapter = "";
                                while (matcherAdapter.find()) {
                                    strAdapter = matcherAdapter.group(1);
                                }

                                jSchUtil.execCmd("/opt/MegaRAID/MegaCli/MegaCli64 -PDList -aAll| grep -Ei \"(Enclosure Device|Slot Number)\"|awk -F ':' '{print $2}'");
                                Matcher matcher = Pattern.compile("^\\[(.*?)]$").matcher(jSchUtil.getStandardOutput().toString());
                                String strPDList = "";
                                while (matcher.find()) {
                                    strPDList = matcher.group(1);
                                }
                                String pdlist = strPDList.substring(strAdapter.length());

                                String[] makeRaidInfos = content[3].split(",");
                                for (String makeRaidInfo : makeRaidInfos) {
                                    String[] rinfo = makeRaidInfo.split(":");
                                    int mode = Integer.valueOf(rinfo[0]);
                                    int numPerGroup = Integer.valueOf(rinfo[1]);
                                    int group = Integer.valueOf(rinfo[2]);
                                    for (int i = 0; i < group; i++) {

                                    }

                                    System.out.println(mode + " " + numPerGroup + " " + group);
                                    //暂时无法测试需要raidinfo测试出来，循环生成1：2，3：4...
                                    jSchUtil.execCmd("/opt/MegaRAID/MegaCli/MegaCli64 -CfgLdADD -r" + mode + " [${raidinfo:1}] WB Direct -a" + strAdapter);
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
            for (FutureTask<Map<String, String>> futureTask : futureTasks) {
                executorService.submit(futureTask);
            }
            executorService.shutdown();
            for (int i = 0; i < contents.size(); i++) {
                try {
                    Map<String, String> flag = futureTasks.get(i).get();
                    vector.add(flag);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            return JsonData.BuildRequest(e.getMessage(), StatusCode.STATUS_ERROR);
        }


        return JsonData.BuildRequest(vector,StatusCode.STATUS_OK);
    }
}
