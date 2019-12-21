package com.itennishy.lops.controller;

import com.itennishy.lops.service.NetworkConfigService;
import com.itennishy.lops.utils.StatusCode;
import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.service.ExeCmdService;
import com.itennishy.lops.utils.JsonData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Api(value = "常用命令执行接口", tags = "常用接口类")
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
    @ApiOperation(value = "远程执行命令", notes = "通过指定地址直接执行命令")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "ip", value = "ip地址", dataType = "String", required = true),
            @ApiImplicitParam(name = "user", value = "用户名", dataType = "String", required = true),
            @ApiImplicitParam(name = "pwd", value = "密码", dataType = "String", required = true),
            @ApiImplicitParam(name = "cmd", value = "执行命令", dataType = "String", required = true)

    })
    @RequestMapping(value = "/define", method = RequestMethod.GET)
    public JsonData ExeCmd(String ip, String user, String pwd, @RequestParam("cmd") String cmd) {
        return exeCmdService.ExeCmd(ip, user, pwd, cmd);
    }

    /**
     * http://127.0.0.1:8081/exec/config?conf=hosts.conf&cmds=free -m
     *
     * @param conf
     * @return
     */
    @ApiOperation(value = "远程执行命令", notes = "通过配置文件批量执行命令")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "conf", value = "配置文件名", dataType = "String", required = true),
            @ApiImplicitParam(name = "cmds", value = "执行命令", dataType = "Object", required = true)

    })
    @RequestMapping(value = "/config", method = RequestMethod.GET)
    public JsonData ExeCmds(String conf, @RequestParam("cmds") Object cmds) {
        return exeCmdService.ExeCmds(conf, cmds);
    }

    /**
     * 配置时间时区
     * http://127.0.0.1:8081/exec/timezone?conf=hosts.conf&date=2019-12-20 17:48:00
     */
    @ApiOperation(value = "配置时间时区", notes = "通过配置文件批量配置时间时区")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "conf", value = "配置文件名", dataType = "String", required = true),
            @ApiImplicitParam(name = "date", value = "时间", dataType = "String", required = true)

    })
    @RequestMapping(value = "/timezone", method = RequestMethod.GET)
    public JsonData setTimeZone(String conf, String date) {
        JsonData jsonData;
        if ("".equals(date)) {
            jsonData = ExeCmds(conf, "ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime;hwclock &> /dev/null;date -s \"`date '+%Y-%m-%d %H:%M:%S'`\" &> /dev/null");
        } else {
            jsonData = ExeCmds(conf, "ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime;hwclock &> /dev/null;date -s \"" + date + "\" &> /dev/null");
        }
        return jsonData;
    }


    /**
     * 清除MBR引导
     * 解决重装操作系统时进入旧系统问题,重启后操作系统将无法进入原系统
     * <p>
     * http://127.0.0.1:8081/mbr/clean/all?conf=hosts.conf
     */
    @ApiOperation(value = "清除MBR引导", notes = "解决重装操作系统时进入旧系统问题,重启后操作系统将无法进入原系统")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "conf", value = "配置文件名", dataType = "String", required = true),
    })
    @RequestMapping(value = "/mbr/clean/all", method = RequestMethod.GET)
    public JsonData ClearMBR(String conf) {
        return exeCmdService.ExeCmds(conf, "dd if=/dev/zero of=/dev/sda bs=1k count=1 && echo 'Clear MBR OK' || echo 'Clear MBR failed'");
    }

    /**
     * 检查内存，cpu，磁盘信息等
     *
     * @param conf
     * @return
     */
    @ApiOperation(value = "检查设备信息", notes = "检查内存，cpu，磁盘信息等")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "conf", value = "配置文件名", dataType = "String", required = true),
    })
    @RequestMapping(value = "/checklld", method = RequestMethod.GET)
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
    @ApiOperation(value = "修改密码", notes = "修改linux用户密码")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "conf", value = "配置文件名", dataType = "String", required = true),
    })
    @RequestMapping(value = "/change/pwd", method = RequestMethod.GET)
    public JsonData changePwd(String conf) {
        return exeCmdService.ExeCmds(conf, "echo ${3} | passwd --stdin root");
    }

    /**
     * 根据配置文件批量修改ip地址
     *
     * @param conf
     * @return
     */
    @ApiOperation(value = "修改ip地址", notes = "根据配置文件批量修改ip地址")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "conf", value = "配置文件名", dataType = "String", required = true),
    })
    @RequestMapping(value = "/change/ip", method = RequestMethod.GET)
    public JsonData changeIp(String conf) {
        return networkConfigService.ChangeIp(conf);
    }

    /**
     * 修改文件内容，全匹配
     *
     * @param conf
     * @param oldv
     * @param newv
     * @param addrv
     * @return
     */
    @ApiOperation(value = "修改文件内容", notes = "修改文件内容，全匹配")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "conf", value = "配置文件名", dataType = "String", required = true),
            @ApiImplicitParam(name = "oldv", value = "旧值", dataType = "String", required = true),
            @ApiImplicitParam(name = "newv", value = "新值", dataType = "String", required = true),
            @ApiImplicitParam(name = "addrv", value = "文件地址", dataType = "String", required = true),
    })
    @RequestMapping(value = "/change/txt", method = RequestMethod.GET)
    public JsonData ModifyConfig(String conf, String oldv, String newv, String addrv) {
        String cmd = "sed -i 's/" + oldv + "/" + newv + "/g' " + addrv;
        exeCmdService.ExeCmds(conf, cmd);
        return JsonData.BuildRequest();
    }

    @ApiOperation(value = "查找文件", notes = "根据用户权限和用户查找文件")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "conf", value = "配置文件名", dataType = "String", required = true),
            @ApiImplicitParam(name = "dir", value = "查找指定路径", dataType = "String", required = true),
            @ApiImplicitParam(name = "perm", value = "权限", dataType = "String", required = true),
            @ApiImplicitParam(name = "user", value = "用户", dataType = "String", required = true),
    })
    @RequestMapping(value = "/find", method = RequestMethod.GET)
    public JsonData FindPermOrUserFile(String conf, String dir, String perm, String user) {
        String cmd = "find " + dir;
        if (!"".equals(perm)) {
            if (perm.startsWith("!")) {
                cmd = cmd + " ! -perm " + perm.substring(1);
            } else {
                cmd = cmd + " -perm " + perm;
            }
        }
        if (!"".equals(user)) {
            if (user.startsWith("!")) {
                cmd = cmd + " ! -user " + user.substring(1);
            } else {
                cmd = cmd + " -user " + user;
            }
        }
        return exeCmdService.ExeCmds(conf, cmd);
    }

    @ApiOperation(value = "免密配置", notes = "配置该服务器到配置文件中服务器的免密配置")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "conf", value = "配置文件名", dataType = "String", required = true),
            @ApiImplicitParam(name = "ip", value = "ip地址", dataType = "String", required = true),
            @ApiImplicitParam(name = "user", value = "用户名", dataType = "String", required = true),
            @ApiImplicitParam(name = "pwd", value = "密码", dataType = "String", required = true),
    })
    @RequestMapping(value = "/config/nopwd", method = RequestMethod.GET)
    public JsonData SetNoPwdLogin(String conf, String ip, String user, String pwd) {
        JSchExecutor jSchExecutor = new JSchExecutor(user, pwd, ip);
        try {
            jSchExecutor.connect();
            jSchExecutor.execCmd("rm -rf ~/.ssh | ssh-keygen -t rsa -N '' -f ~/.ssh/id_rsa -q");
            jSchExecutor.execCmd("cat ~/.ssh/id_rsa.pub");
            Matcher matcher = Pattern.compile("^\\[(.*?)]$").matcher(jSchExecutor.getStandardOutput().toString());
            String rsa = "";
            while (matcher.find()) {
                rsa = matcher.group(1);
            }
            exeCmdService.ExeCmds(conf, "echo \"" + rsa + "\" >> ~/.ssh/authorized_keys");
        } catch (Exception e) {
            return JsonData.BuildRequest(StatusCode.STATUS_ERROR);
        } finally {
            jSchExecutor.disconnect();
        }
        return JsonData.BuildRequest();
    }

    @ApiOperation(value = "免密配置", notes = "配置文件中服务器之间免密登录")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "conf", value = "配置文件名", dataType = "String", required = true),
    })
    @RequestMapping(value = "/config/nopwd/all", method = RequestMethod.GET)
    public JsonData SetNoPwdLogin(String conf) {
        JsonData result;
        try {
            result = exeCmdService.ExeCmds(conf, "rm -rf ~/.ssh | ssh-keygen -t rsa -N '' -f ~/.ssh/id_rsa -q|echo ok");
            JsonData jsonData = exeCmdService.ExeCmds(conf, "cat ~/.ssh/id_rsa.pub");
            List<Map<String, String>> list = (List<Map<String, String>>) jsonData.getData();
            StringBuilder sb = new StringBuilder();
            for (Map<String, String> s : list) {
                String t = (String) s.values().toArray()[0];
                String rsa = "";
                if (t != null && !"[]".equals(t)) {
                    Matcher matcher = Pattern.compile("^\\[(.*?)]$").matcher(t);
                    while (matcher.find()) {
                        rsa = matcher.group(1);
                    }
                    sb.append(rsa + "\n");
                }
            }
            exeCmdService.ExeCmds(conf, "echo \"" + sb.toString() + "\" >> ~/.ssh/authorized_keys");
        } catch (Exception e) {
            e.printStackTrace();
            return JsonData.BuildRequest(e.getMessage(), StatusCode.STATUS_ERROR);
        }
        return JsonData.BuildRequest(result, StatusCode.STATUS_OK);
    }

    @RequestMapping(value = "/testTOP", method = RequestMethod.GET)
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
