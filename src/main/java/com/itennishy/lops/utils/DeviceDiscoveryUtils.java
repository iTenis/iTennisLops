package com.itennishy.lops.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeviceDiscoveryUtils {

    List<String> iplists = new ArrayList<>();
    ExecutorService pool = Executors.newFixedThreadPool(10);

    public void worker(long i) {
        pool.execute(() -> {
            try {
                String ip = IPUtil.longToIP(i);
                InetAddress addip = InetAddress.getByName(ip);
                boolean status = addip.isReachable(100);
                System.out.println("IP地址为:" + ip + "\t\t设备名称为: " + addip.getHostName() + "\t\t是否可用: " + (status ? "可用" : "不可用"));
                if (status) {
                    iplists.add(ip);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public List<String> getOnlineDevices(String ip1, String ip2) {
        long begin = IPUtil.ipToLong(ip1);
        long end = IPUtil.ipToLong(ip2);
        for (long i = begin; i < end; i++) {
            worker(i);
        }
        pool.shutdown();
        return iplists;
    }

    public boolean getOnlineDevice(String ip) throws Exception {
        InetAddress addip = InetAddress.getByName(ip);
        boolean status = addip.isReachable(100);
        System.out.println("IP地址为:" + ip + "\t\t设备名称为: " + addip.getHostName() + "\t\t是否可用: " + (status ? "可用" : "不可用"));
        return status;
    }

    public static void main(String[] args) throws Exception {
        new DeviceDiscoveryUtils().getOnlineDevice("192.168.0.100");
    }
}


