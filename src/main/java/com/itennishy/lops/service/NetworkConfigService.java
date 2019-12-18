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
    public void SetNetwork(JSchExecutor jSchUtil, String interfaceName, String ipAddr, String ipNetmask) throws Exception {
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
            log.warn("----配置接口ip地址成功");
        } else {
            log.error("不存在该网络接口配置文件:" + netfile);
        }
    }

    /**
     * 配置双网卡绑定
     *
     * @param jSchUtil
     * @param bondx
     * @param bondy
     * @param bondxy
     * @param ipAddr
     * @param ipNetmask
     * @param ipGateway
     * @param mode
     * @throws Exception
     */
    public void SetBonding(JSchExecutor jSchUtil, String bondx, String bondy, String bondxy, String ipAddr, String ipNetmask, String ipGateway, String mode) throws Exception {
        String net1file = "/etc/sysconfig/network-scripts/ifcfg-" + bondx;
        String net2file = "/etc/sysconfig/network-scripts/ifcfg-" + bondy;
        if (jSchUtil.isLinkExist(net1file) && jSchUtil.isLinkExist(net2file)) {
            jSchUtil.execCmd("(\n" +
                    "cat << EOF\n" +
                    "DEVICE=" + bondxy + "\n" +
                    "NAME=" + bondxy + "\n" +
                    "TYPE=Bond\n" +
                    "ONBOOT=yes\n" +
                    "BOOTPROTO=static\n" +
                    "BONDING_MASTER=yes\n" +
                    "BONDING_OPTS=\"mode=" + mode + " miimon=50 updelay=0 downdelay=0\"\n" +
                    "IPADDR=" + ipAddr + "\n" +
                    "GATEWAY=" + ipGateway + "\n" +
                    "NETMASK=" + ipNetmask + "\n" +
                    "EOF\n" +
                    ") > /etc/sysconfig/network-scripts/ifcfg-" + bondxy + "\n");

            jSchUtil.execCmd("cp " + net1file + " /tmp/ifcfg-" + bondx + "-bak" + System.currentTimeMillis());
            jSchUtil.execCmd("cp " + net2file + " /tmp/ifcfg-" + bondy + "-bak" + System.currentTimeMillis());
            jSchUtil.execCmd("(\n" +
                    "cat << EOF\n" +
                    "alias " + bondxy + " bonding\n" +
                    "options " + bondxy + " miimon=50  mode=" + mode + "\n" +
                    "EOF\n" +
                    ") >> /etc/modprobe.d/dist.conf");
            jSchUtil.execCmd("sed -i '/^MASTER/d' /etc/sysconfig/network-scripts/ifcfg-" + bondx);
            jSchUtil.execCmd("sed -i '/^SLAVE/d' /etc/sysconfig/network-scripts/ifcfg-" + bondx);
            jSchUtil.execCmd("sed -i '/^BOOTPROTO/d' /etc/sysconfig/network-scripts/ifcfg-" + bondx);
            jSchUtil.execCmd("sed -i '/^ONBOOT/d' /etc/sysconfig/network-scripts/ifcfg-" + bondx);
            jSchUtil.execCmd("sed -i '/^IPADDR/d' /etc/sysconfig/network-scripts/ifcfg-" + bondx);
            jSchUtil.execCmd("sed -i '/^NETMASK/d' /etc/sysconfig/network-scripts/ifcfg-" + bondx);
            jSchUtil.execCmd("sed -i '/^GATEWAY/d' /etc/sysconfig/network-scripts/ifcfg-" + bondx);

            jSchUtil.execCmd("sed -i '/^MASTER/d' /etc/sysconfig/network-scripts/ifcfg-" + bondy);
            jSchUtil.execCmd("sed -i '/^SLAVE/d' /etc/sysconfig/network-scripts/ifcfg-" + bondy);
            jSchUtil.execCmd("sed -i '/^BOOTPROTO/d' /etc/sysconfig/network-scripts/ifcfg-" + bondy);
            jSchUtil.execCmd("sed -i '/^ONBOOT/d' /etc/sysconfig/network-scripts/ifcfg-" + bondy);
            jSchUtil.execCmd("sed -i '/^IPADDR/d' /etc/sysconfig/network-scripts/ifcfg-" + bondy);
            jSchUtil.execCmd("sed -i '/^NETMASK/d' /etc/sysconfig/network-scripts/ifcfg-" + bondy);
            jSchUtil.execCmd("sed -i '/^GATEWAY/d' /etc/sysconfig/network-scripts/ifcfg-" + bondy);

            jSchUtil.execCmd("(\n" +
                    "cat << EOF\n" +
                    "BOOTPROTO=none\n" +
                    "ONBOOT=yes\n" +
                    "MASTER=" + bondxy + "\n" +
                    "SLAVE=yes\n" +
                    "EOF\n" +
                    ") >> /etc/sysconfig/network-scripts/ifcfg-" + bondx + "\n");
            jSchUtil.execCmd("(\n" +
                    "cat << EOF\n" +
                    "BOOTPROTO=none\n" +
                    "ONBOOT=yes\n" +
                    "MASTER=" + bondxy + "\n" +
                    "SLAVE=yes\n" +
                    "EOF\n" +
                    ") >> /etc/sysconfig/network-scripts/ifcfg-" + bondy + "\n");
            log.warn("----双网卡绑定配置成功");
        } else {
            log.error("不存在网络接口配置文件!");
        }
    }
}
