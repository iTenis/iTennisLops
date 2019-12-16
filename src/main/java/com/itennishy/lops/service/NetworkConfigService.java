package com.itennishy.lops.service;

import com.itennishy.lops.executor.JSchExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NetworkConfigService {

    /**
     * 配置网络接口ip地址
     *
     * @param jSchUtil
     * @param interfaceName
     * @param ipAddr
     * @param ipNetmask
     */
    public void SetNetwork(JSchExecutor jSchUtil, String interfaceName, String ipAddr, String ipNetmask) {
        try {
            jSchUtil.connect();
            String netfile = "/etc/sysconfig/network-scripts/ifcfg-" + interfaceName;
            if (jSchUtil.isLinkExist(netfile)) {
                jSchUtil.execCmd("cp -rvf " + netfile + " /tmp/ifcfg-" + interfaceName + "_" + System.currentTimeMillis());
                jSchUtil.execCmd("sed -i '/^ONBOOT/d' " + netfile);
                jSchUtil.execCmd("sed -i '/^IPADDR/d' " + netfile);
                jSchUtil.execCmd("sed -i '/^NETMASK/d' " + netfile);
                jSchUtil.execCmd("sed -i '/^BOOTPROTO/d' " + netfile);
                jSchUtil.execCmd("sed -i '/^GATEWAY/d' " + netfile);
                jSchUtil.execCmd("echo \"ONBOOT=yes\" >> " + netfile);
                jSchUtil.execCmd("echo \"BOOTPROTO=static\" >> " + netfile);
                jSchUtil.execCmd("echo \"IPADDR=" + ipAddr + "\" >> " + netfile);
                jSchUtil.execCmd("echo \"NETMASK=" + ipNetmask + "\" >> " + netfile);
                log.info("配置接口ip地址成功!");
            } else {
                log.error("不存在该网络接口配置文件:" + netfile);
            }

            System.out.println(jSchUtil.getStandardOutput());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jSchUtil.disconnect();
        }
    }

    public static void main(String[] args) {
        JSchExecutor jSchUtil = new JSchExecutor("root", "19931103xhs-", "115.159.0.166");
        new NetworkConfigService().SetNetwork(jSchUtil, "eth2", "12.0.0.1", "255.255.255.0");
    }
}
