package com.itennishy.lops.service;

import com.itennishy.lops.domain.iTennisConfig;
import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class PxeServerConfigService {

    @Autowired
    private iTennisConfig iTennisConfig;

    @Autowired
    private NetworkConfigService networkConfigService;

    @Autowired
    private YumConfigService yumConfigService;

    @Autowired
    private InstallPackagesService installPackagesService;

    private ConcurrentLinkedQueue<String> linkedQueue = new ConcurrentLinkedQueue<>();

    public void ConfigPxeServer(JSchExecutor jSchUtil) throws Exception {
        AtomicBoolean flag = new AtomicBoolean(true);
        new Thread(() -> {
            try {
                linkedQueue.add("配置网络接口ip");
                networkConfigService.SetNetwork(jSchUtil, iTennisConfig.getPxeServer().get("interface"), iTennisConfig.getPxeServer().get("ip"), iTennisConfig.getPxeServer().get("netmask"));

                linkedQueue.add("配置yum源");
                yumConfigService.setMountISOWithConfig(jSchUtil, iTennisConfig.getPxeServer().get("iso"), "/media");
                yumConfigService.setLocalYumReposity(jSchUtil, "/media");

                linkedQueue.add("安装缺失包");
                installPackagesService.BeginInstallPackages(jSchUtil);

                String osversionPath = iTennisConfig.getInstallOS().get("os") + "/" + iTennisConfig.getInstallOS().get("version");
                String serverip = iTennisConfig.getDhcpServer().get("ip");
                String serveros = iTennisConfig.getInstallOS().get("os");
                String serverversion = iTennisConfig.getInstallOS().get("version");

                String currentPath = new FileUtils().getJarPath();

                linkedQueue.add("配置TFTP服务");
                jSchUtil.execCmd("sed -i '/disable/s/yes/no/' /etc/xinetd.d/tftp");
                linkedQueue.add("配置DHCP服务");
                jSchUtil.execCmd("cp -rvf /etc/dhcp/dhcpd.conf /tmp/dhcpd.conf.bak");
                jSchUtil.execCmd("(\n" +
                        "cat << EOF\n" +
                        "option space PXE;\n" +
                        "option PXE.mtftp-ip    code 1 = ip-address;\n" +
                        "option PXE.mtftp-cport code 2 = unsigned integer 16;\n" +
                        "option PXE.mtftp-sport code 3 = unsigned integer 16;\n" +
                        "option PXE.mtftp-tmout code 4 = unsigned integer 8;\n" +
                        "option PXE.mtftp-delay code 5 = unsigned integer 8;\n" +
                        "option client-system-arch code 93 = unsigned integer 16;\n" +
                        "#option domain-name-servers 8.8.8.8, " + serverip + ";\n" +
                        "allow booting;\n" +
                        "allow bootp;\n" +
                        "default-lease-time 600;\n" +
                        "max-lease-time 7200;\n" +
                        "#ddns-update-style interim;\n" +
                        "subnet " + iTennisConfig.getDhcpServer().get("subnet") + " netmask " + iTennisConfig.getDhcpServer().get("netmask") + " {\n" +
                        "        range " + iTennisConfig.getDhcpServer().get("start") + " " + iTennisConfig.getDhcpServer().get("end") + ";\n" +
                        "        option routers " + serverip + ";\n" +
                        "        class \"pxeclients\" {\n" +
                        "        match if substring (option vendor-class-identifier, 0, 9) = \"PXEClient\";\n" +
                        "        next-server " + serverip + ";\n" +
                        "        if option client-system-arch = 00:07 or option client-system-arch = 00:09 {\n" +
                        "         filename \"uefi/shim.efi\";\n" +
                        "         #filename \"efi/BOOTX64.efi\";\n" +
                        "        } else {\n" +
                        "         filename \"pxelinux/pxelinux.0\";\n" +
                        "        }\n" +
                        "   }\n" +
                        "}\n" +
                        "EOF\n" +
                        ") > /etc/dhcp/dhcpd.conf\n");

                jSchUtil.execCmd("mkdir -p /var/lib/tftpboot/pxelinux/pxelinux.cfg");
                jSchUtil.execCmd("mkdir -p /var/lib/tftpboot/uefi");
                jSchUtil.execCmd("mkdir -p /var/www/html/{kickstarts,os,driver}");

                jSchUtil.execCmd("mkdir -p /var/www/html/os/" + osversionPath);
                jSchUtil.execCmd("mkdir -p /var/lib/tftpboot/images/" + osversionPath);

//        jSchUtil.execCmd("cp -rvf " + currentPath + "/boot/pxelinux /var/lib/tftpboot/");
//        jSchUtil.execCmd("cp -rvf " + currentPath + "/boot/uefi /var/lib/tftpboot/");
//        jSchUtil.execCmd("cp -rvf " + currentPath + "/boot/efi /var/lib/tftpboot/");
//        jSchUtil.execCmd("cp -rvf " + currentPath + "/boot/kickstarts /var/www/html/");

                linkedQueue.add("分发系统引导文件");
                jSchUtil.upLoadFile(currentPath + "/boot/pxelinux", "/var/lib/tftpboot/pxelinux");
                jSchUtil.upLoadFile(currentPath + "/boot/uefi", "/var/lib/tftpboot/uefi");
                jSchUtil.upLoadFile(currentPath + "/boot/efi", "/var/lib/tftpboot/efi");

                linkedQueue.add("配置默认引导MENU");
                jSchUtil.execCmd("(\n" +
                        "cat << EOF\n" +
                        "default menu.c32\n" +
                        "prompt 1\n" +
                        "timeout 15\n" +
                        "menu title Install Linux by PXE\n" +
                        "label autolinux\n" +
                        "\tmenu label Auto ^Install " + serveros + "/" + " Linux\n" +
                        "\tkernel ../images/" + osversionPath + "/vmlinuz\n" +
                        "\tappend initrd=../images/" + osversionPath + "/initrd.img ip=dhcp repo=http://" + serverip + "/os/" + osversionPath + " ks=http://" + serverip + "/kickstarts/" + serveros + "_" + serverversion + "_ks.cfg ksdevice=" + iTennisConfig.getDhcpServer().get("interface") + "\n" +
                        "label manuallinux\n" +
                        "      menu label Manual ^Install " + serveros + " Linux\n" +
                        "      kernel ../images/" + osversionPath + "/vmlinuz\n" +
                        "      append initrd=../images/" + osversionPath + "/initrd.img ip=dhcp inst.repo=http://" + serverip + "/os/" + osversionPath + "\n" +
                        "label local\n" +
                        "  menu label Boot from ^local drive\n" +
                        "  localboot 0xffff\n" +
                        "EOF\n" +
                        ") > /var/lib/tftpboot/pxelinux/pxelinux.cfg/default");

                linkedQueue.add("配置EFI引导MENU");
                jSchUtil.execCmd("(\n" +
                        "cat << EOF\n" +
                        "default=0\n" +
                        "splashimage=(nd)/splash.xpm.gz\n" +
                        "timeout=5\n" +
                        "title " + serveros + " UEFI\n" +
                        "\troot (nd)\n" +
                        "        kernel /../images/" + osversionPath + "/vmlinuz ip=dhcp ks=http://" + serverip + "/kickstarts/" + serveros + "_" + serverversion + "_ks.cfg repo=http://" + serverip + "/os/" + osversionPath + " ksdevice=" + iTennisConfig.getDhcpServer().get("interface") + "\n" +
                        "        initrd /../images/" + osversionPath + "/initrd.img\n" +
                        "EOF\n" +
                        ") > /var/lib/tftpboot/efi/efidefault");

                linkedQueue.add("配置UEFI引导MENU");
                jSchUtil.execCmd("(\n" +
                        "cat << EOF\n" +
                        "set timeout=5\n" +
                        "menuentry 'Auto Install " + serveros + " Min UEFI' {\n" +
                        "        linuxefi /uefi/../images/" + osversionPath + "/vmlinuz ip=dhcp inst.repo=http://" + serverip + "/os/" + osversionPath + " inst.ks=http://" + serverip + "/kickstarts/" + serveros + "_" + serverversion + "_ks.cfg ksdevice=" + iTennisConfig.getDhcpServer().get("interface") + "\n" +
                        "        initrdefi /uefi/../images/" + osversionPath + "/initrd.img\n" +
                        "}\n" +
                        "menuentry 'Manual Install " + serveros + " Min UEFI' {\n" +
                        "        linuxefi /uefi/../images/" + osversionPath + "/vmlinuz ip=dhcp inst.repo=http://" + serverip + "/os/" + osversionPath + " ksdevice=" + iTennisConfig.getDhcpServer().get("interface") + "\n" +
                        "        initrdefi /uefi/../images/" + osversionPath + "/initrd.img\n" +
                        "}\n" +
                        "EOF\n" +
                        ") > /var/lib/tftpboot/uefi/grub.cfg");

                linkedQueue.add("配置ks文件");
                jSchUtil.execCmd("(\n" +
                        "cat << EOF\n" +
                        "firewall --disabled\n" +
                        "install\n" +
                        "url --url=\"http://" + serverip + "/os/" + osversionPath + "\"\n" +
                        "rootpw --plaintext " + iTennisConfig.getInstallOS().get("password") + "\n" +
                        "auth  --useshadow  --passalgo=sha512\n" +
                        "#graphical\n" +
                        "text\n" +
                        "keyboard us\n" +
                        "lang en_US.UTF-8\n" +
                        "selinux --disabled\n" +
                        "skipx\n" +
                        "logging --level=info\n" +
                        "reboot\n" +
                        "timezone --utc Asia/Shanghai\n" +
                        "network  --bootproto=dhcp --device=eth0 --onboot=on\n" +
                        "bootloader --location=mbr\n" +
                        "zerombr\n" +
                        "clearpart --all --initlabel\n" +
                        "#part / --fstype=\"ext4\" --grow --ondisk=sda --size=1\n" +
                        "#part /tmp --fstype=\"ext4\" --ondisk=sda --size=20480\n" +
                        "#part swap --fstype=\"swap\" --size=500\n" +
                        "part /boot/efi --fstype=\"efi\" --ondisk=sda --size=300\n" +
                        "EOF\n" +
                        ") > /var/www/html/kickstarts/" + serveros + "_" + serverversion + "_ks.cfg");

                List<Map<String, String>> parts = iTennisConfig.getPartitions();
                for (Map<String, String> part : parts) {
                    String line = "part " + part.get("path") + " --fstype=\"" + part.get("fstype") + "\" --ondisk=" + part.get("ondisk") + " --size=" + part.get("size");
                    if ("1".equals(part.get("size"))) {
                        line = line + (" --grow");
                    }
                    line = line + "\n";
                    jSchUtil.execCmd("echo \"" + line.replaceAll("\r|\n", "") + "\" >> /var/www/html/kickstarts/" + serveros + "_" + serverversion + "_ks.cfg");
                }

                jSchUtil.execCmd("(\n" +
                        "cat << EOF\n" +
                        "%packages --ignoremissing\n" +
                        "@base\n" +
                        "@core\n" +
                        "@fonts\n" +
                        "@debugging\n" +
                        "@development\n" +
                        "gcc\n" +
                        "make\n" +
                        "tree\n" +
                        "nmap\n" +
                        "sysstat\n" +
                        "telnet\n" +
                        "python\n" +
                        "%end\n" +
                        "#%addon com_redhat_kdump --disable --reserve-mb='auto'\n" +
                        "#%end\n" +
                        "%post\n" +
                        "sed -i \"/UseDNS/d\" /etc/ssh/sshd_config\n" +
                        "echo 'UseDNS no' >> /etc/ssh/sshd_config\n" +
                        "echo 0> /etc/resolv.conf\n" +
                        "service iptables stop\n" +
                        "chkconfig iptables off\n" +
                        "%end\n" +
                        "EOF\n" +
                        ") >> /var/www/html/kickstarts/" + serveros + "_" + serverversion + "_ks.cfg");

                if (!"".equals(iTennisConfig.getInstallOS().get("driver"))) {
                    linkedQueue.add("配置pxe过程中的引导驱动");
                    jSchUtil.execCmd("cp -rvf " + iTennisConfig.getInstallOS().get("driver") + " /var/www/html/driver/");
                    jSchUtil.execCmd("sed -i \"10a\\driverdisk --source=http://" + serverip + "/driver/" + new File(iTennisConfig.getInstallOS().get("driver")).getName() + "\" /var/www/html/kickstarts/" + serveros + "_" + serverversion + "_ks.cfg");
                }

                linkedQueue.add("配置子系统镜像");
                yumConfigService.setMountISOWithConfig(jSchUtil, iTennisConfig.getInstallOS().get("iso"), "/var/www/html/os/" + osversionPath);

                linkedQueue.add("配置子系统内核");
                jSchUtil.execCmd("cp -rvf /var/www/html/os/" + osversionPath + "/isolinux/{initrd.img,vmlinuz} /var/lib/tftpboot/images/" + osversionPath);

                linkedQueue.add("关闭系统安全设置");
                jSchUtil.execCmd("sed -i \"s/SELINUX=.*$/SELINUX=disabled/\" /etc/selinux/config");
                jSchUtil.execCmd("setenforce 0");
                if (iTennisConfig.getPxeServer().get("version").startsWith("6")) {
                    jSchUtil.execCmd("sed -i \"s/uefi\\/shim.efi/efi\\/BOOTX64.efi/g\" /etc/dhcp/dhcpd.conf");
                    jSchUtil.execCmd("service iptables stop");
                    jSchUtil.execCmd("chkconfig iptables off");
                } else {
                    jSchUtil.execCmd("sed -i \"s/efi\\/BOOTX64.efi/uefi\\/shim.efi/g\" /etc/dhcp/dhcpd.conf");
                    jSchUtil.execCmd("sed -i \"s/#%addon/%addon/g\" /var/www/html/kickstarts/" + serveros + "_" + serverversion + "_ks.cfg");
                    jSchUtil.execCmd("sed -i \"s/#%end/%end/g\" /var/www/html/kickstarts/" + serveros + "_" + serverversion + "_ks.cfg");
                    jSchUtil.execCmd("service firewalld stop");
                    jSchUtil.execCmd("chkconfig firewalld off");
                }

                linkedQueue.add("重启pxe需要的服务");
                jSchUtil.execCmd("chkconfig tftp on");
                jSchUtil.execCmd("service tftp restart");
                jSchUtil.execCmd("chkconfig dhcpd on");
                jSchUtil.execCmd("service dhcpd restart");
                jSchUtil.execCmd("chkconfig xinetd on");
                jSchUtil.execCmd("service xinetd restart");
                jSchUtil.execCmd("chkconfig httpd on");
                jSchUtil.execCmd("service httpd restart");
                jSchUtil.execCmd("service network restart &> /dev/null &");
                linkedQueue.add("pxe服务器配置完成");

            } catch (Exception e) {
                log.error("PXE服务配置失败:", e);
            } finally {
                flag.set(false);
            }
        }).start();
        int next = 1;
        while (flag.get()) {
            String txt = linkedQueue.poll();
            if (txt != null) {
                log.info("STEP" + next + ":" + txt);
                next++;
            }
        }
    }
}
