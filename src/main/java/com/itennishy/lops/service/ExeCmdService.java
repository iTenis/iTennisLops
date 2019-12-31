package com.itennishy.lops.service;

import com.itennishy.lops.utils.StatusCode;
import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.utils.DeviceDiscoveryUtils;
import com.itennishy.lops.utils.FileUtils;
import com.itennishy.lops.utils.JsonData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ExeCmdService {

    /**
     * 通过指定ip来执行命令
     *
     * @param ip   IP地址
     * @param user 用户名
     * @param pwd  密码
     * @param cmd  命令
     * @return
     */

    public JsonData ExeCmd(String ip, String user, String pwd, @RequestParam("cmd") String cmd) {
        JSchExecutor jSchUtil = new JSchExecutor();
        try {
            jSchUtil = new JSchExecutor(user, pwd, ip);
            jSchUtil.connect();
            jSchUtil.execCmd(cmd);
            return JsonData.BuildRequest(jSchUtil.getStandardOutput(), StatusCode.STATUS_OK);
        } catch (Exception e) {
            return JsonData.BuildRequest(jSchUtil.getStandardOutput(), StatusCode.STATUS_ERROR);
        } finally {
            jSchUtil.disconnect();
        }
    }

    /**
     * 通过配置文件批量执行一条或者多条命令
     *
     * @param conf 指定配置文件名称
     * @param cmds 可以是String类型（可以是一条命令，也可以将多条命令组合成一条命令执行）
     *             或者是LinkedList类型(保证命令执行的顺序，这样返回结果就有顺序)
     * @return
     */
    public JsonData ExeCmds(String conf, @RequestParam("cmds") Object cmds) {
        Vector<Map<String, Object>> vector = new Vector<>();
        try {
            List<String[]> contents = new FileUtils().getConfigContent(conf);
            AtomicInteger t = new AtomicInteger();
            List<FutureTask<Map<String, Object>>> futureTasks = new ArrayList<>();
            for (String[] content : contents) {
                t.set(new FileUtils().getFlag(conf, content));

                futureTasks.add(new FutureTask<>(new Callable<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> call() throws Exception {
                        Map<String, Object> result = new HashMap<>();
                        JSchExecutor jSchUtil = new JSchExecutor();
                        try {
                            if (new DeviceDiscoveryUtils().getOnlineDevice(content[0]) && new DeviceDiscoveryUtils().getOnlineDevice(content[1 - t.get()])) {
                                jSchUtil = new JSchExecutor(content[2 - t.get()], content[3 - t.get()], content[0]);
                                jSchUtil.connect();
                                // 如果传递进来的命令是一个LinkedList类型
                                if (LinkedList.class.isInstance(cmds)) {
                                    LinkedList<String> linkedList = new LinkedList<>();
                                    String next = "";
                                    for (String cmd : (LinkedList<String>) cmds) {
                                        String finalcmd = (String) cmds;
                                        for (int i = 3; i < content.length; i++) {
                                            finalcmd = finalcmd.replaceFirst("\\$\\{" + i + "}", content[i]);
                                        }
                                        jSchUtil.execCmd(finalcmd);
                                        String tstring = jSchUtil.getStandardOutput().toString();
                                        Matcher matcher = Pattern.compile("^\\[(.*?)]$").matcher(tstring);
                                        while (matcher.find()) {
                                            tstring = matcher.group(1);
                                        }
                                        if ("".equals(next)) {
                                            linkedList.add(tstring.trim());
                                        } else {
                                            String x = tstring.substring(next.length());
                                            if (x.startsWith(","))
                                                x = x.substring(1);
                                            if (x.endsWith(","))
                                                x = x.substring(0, x.length() - 1);
                                            linkedList.add(x.trim());

                                        }
                                        next = tstring;
                                    }
                                    result.put(content[0], linkedList);
                                }

                                // 如果传递进来的命令是一个String类型，一般用于执行单个命令，当然也可以执行多个命令
                                if (String.class.isInstance(cmds)) {
                                    String finalcmd = (String) cmds;
                                    for (int i = 3; i < content.length; i++) {
                                        finalcmd = finalcmd.replaceFirst("\\$\\{" + i + "}", content[i]);
                                    }
                                    jSchUtil.execCmd(finalcmd);
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
}
