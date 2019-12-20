package com.itennishy.lops.controller;

import com.itennishy.lops.service.NetworkConfigService;
import com.itennishy.lops.utils.StatusCode;
import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.service.ExeCmdService;
import com.itennishy.lops.utils.JsonData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@RestController
@RequestMapping("/exec")
public class ExeCmdController {

    @Autowired
    private ExeCmdService exeCmdService;

    @Autowired
    private NetworkConfigService networkConfigService;

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
        return exeCmdService.ExeCmd(ip, user, pwd, cmd);
    }

    /**
     * http://127.0.0.1:8081/exec/config?conf=hosts.conf&cmds=free -m
     *
     * @param conf
     * @return
     */
    @RequestMapping("/config")
    public JsonData ExeCmds(String conf, @RequestParam("cmds") Object cmds) {
        return exeCmdService.ExeCmds(conf, cmds);
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
        return exeCmdService.ExeCmds(conf, "dd if=/dev/zero of=/dev/sda bs=1k count=1 && echo 'Clear MBR OK' || echo 'Clear MBR failed'");
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
        return JsonData.BuildRequest(resultMap, StatusCode.STATUS_OK);
    }

    /**
     * 修改密码
     *
     * @param conf
     * @return
     */
    @RequestMapping("/changepwd")
    public JsonData changePwd(String conf){
        return exeCmdService.ExeCmds("change_pwd.conf", "echo ${3} | passwd --stdin root");
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

    @RequestMapping("changeip")
    public JsonData changeIp(String conf){
        return networkConfigService.ChangeIp(conf);
    }

}
