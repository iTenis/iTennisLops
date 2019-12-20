package com.itennishy.lops.controller;

import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.service.UpDownFileService;
import com.itennishy.lops.utils.DeviceDiscoveryUtils;
import com.itennishy.lops.utils.FileUtils;
import com.itennishy.lops.utils.JsonData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/exec")
public class ExeCmdController {

    /**
     * 远程执行命令
     * http://127.0.0.1:8081/exec/define?ip=192.168.0.160&user=root&pwd=123456&cmd=free -m
     *
     * @param ip
     * @param user
     * @param pwd
     * @param cmd
     * @return
     */
    @RequestMapping("/define")
    public JsonData ExeCmd(String ip, String user, String pwd, @RequestParam("cmd") String cmd) {

        JSchExecutor jSchUtil = new JSchExecutor();
        try {
            jSchUtil = new JSchExecutor(user, pwd, ip);
            jSchUtil.connect();
            jSchUtil.execCmd(cmd);
            return JsonData.BuildSuccess(200, jSchUtil.getStandardOutput(), "发送请求成功");
        } catch (Exception e) {
            return JsonData.BuildError(50001, e.getMessage());
        } finally {
            jSchUtil.disconnect();
        }
    }

    /**
     * http://127.0.0.1:8081/exec/config?conf=hosts.conf&cmds=free -m
     *
     * @param conf
     * @return
     */
    @RequestMapping("/config")
    public JsonData ExeCmds(String conf, @RequestParam("cmds") Object cmds) {
        Vector<Map<String, Object>> vector = new Vector<>();
        try {
            List<String[]> contents = new FileUtils().getConfigContent(conf);
            AtomicInteger t = new AtomicInteger();
            List<FutureTask<Map<String, Object>>> futureTasks = new ArrayList<>();
            for (String[] content : contents) {
                t.set(new FileUtils().getFlag(conf,content));

                futureTasks.add(new FutureTask<>(new Callable<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> call() throws Exception {
                        Map<String, Object> result = new HashMap<>();
                        JSchExecutor jSchUtil = new JSchExecutor();
                        try {
                            if (new DeviceDiscoveryUtils().getOnlineDevice(content[0]) && new DeviceDiscoveryUtils().getOnlineDevice(content[1 - t.get()])) {
                                jSchUtil = new JSchExecutor(content[2 - t.get()], content[3 - t.get()], content[0]);
                                jSchUtil.connect();
                                if(LinkedList.class.isInstance(cmds)){
                                    LinkedList<String> linkedList = new LinkedList<>();
                                    String next = "";
                                    for (String cmd : (LinkedList<String>)cmds) {
                                        jSchUtil.execCmd(cmd);
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

                                if(String.class.isInstance(cmds)) {
                                    jSchUtil.execCmd((String) cmds);
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
            if(contents.size()==0){
                return JsonData.BuildSuccess("配置文件没有内容，请核实查看");
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
            return JsonData.BuildError(50001, e.getMessage());
        }
        return JsonData.BuildSuccess(vector);
    }

    /**
     * 配置时间时区
     * http://127.0.0.1:8081/exec/timezone?conf=hosts.conf&date=2019-12-20 17:48:00
     */
    @RequestMapping("/timezone")
    public JsonData setTimeZone(String conf, String date) {
        JsonData jsonData;
        if ("".equals(date)) {
            jsonData = ExeCmds(conf, "ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime;hwclock &> /dev/null;date -s \"`date '+%Y-%m-%d %H:%M:%S'`\" &> /dev/null");
        } else {
            jsonData = ExeCmds(conf, "ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime;hwclock &> /dev/null;date -s \"" + date + "\" &> /dev/null");
        }
        return jsonData;
    }

    @RequestMapping("/pwd")
    public JsonData setRootPwd(String conf) {
        JsonData jsonData = ExeCmds(conf, "echo $newpwd | passwd --stdin root");
        return jsonData;
    }


    /**
     * 清除MBR引导
     * 解决重装操作系统时进入旧系统问题,重启后操作系统将无法进入原系统
     * <p>
     * http://127.0.0.1:8081/mbr/clean/all?conf=hosts.conf
     */
    @RequestMapping("/mbr/clean/all")
    public JsonData ClearMBR(String conf) {
        JsonData jsonData = ExeCmds(conf, "dd if=/dev/zero of=/dev/sda bs=1k count=1 && echo 'Clear MBR OK' || echo 'Clear MBR failed'");
        return jsonData;
    }

    /**
     * 检查内存，cpu，磁盘信息等
     *
     * @param conf
     * @return
     */
    @RequestMapping("/checklld")
    public JsonData checkLLD(String conf) {
        LinkedList<String> sb = new LinkedList<>();
        sb.add("cat /proc/cpuinfo | grep 'processor'|sort -u|wc -l");
        sb.add("cat /proc/meminfo | grep MemTotal | awk '{print $2$3}'");
        sb.add("fdisk -l | grep 'Disk /dev/' | grep -iv 'Disk /dev/mapper'|wc -l");
        sb.add("fdisk -l | grep 'Disk /dev/' | grep -iv 'Disk /dev/mapper' | awk '{print $2$3$4}'");

        JsonData jsonData = ExeCmds(conf, sb);
        Vector<Map<String, LinkedList>> datas = (Vector<Map<String, LinkedList>>) jsonData.getData();
        Map<String, Map<String, String>> resultMap = new HashMap<>();

        datas.forEach((data) -> {
            data.forEach((key, value) -> {
                if (value != null) {
                    Map<String, String> subMap = new HashMap<>();
                    subMap.put("CPU", value.get(0).toString());
                    subMap.put("Memory", value.get(1).toString());
                    subMap.put("DiskNum", value.get(2).toString());
                    subMap.put("DiskInfo", value.get(3).toString().replaceAll(",, ", ","));
                    resultMap.put(key, subMap);
                } else {
                    resultMap.put(key, null);
                }
            });
        });

        return JsonData.BuildSuccess(resultMap);
    }

    @RequestMapping("/testTOP")
    public void test() throws Exception {
        JSchExecutor jSchUtil = new JSchExecutor("root", "123456", "192.168.0.102");
        jSchUtil.connect();
        int i = 0;
        while (i < 10) {
            jSchUtil.execCmd("top -b -c -n 1 | head -n 20");
            System.out.println(jSchUtil.getStandardOutput());
            i++;
            TimeUnit.SECONDS.sleep(1);
        }
        jSchUtil.disconnect();
    }

}
