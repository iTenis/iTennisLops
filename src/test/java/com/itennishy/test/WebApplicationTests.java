package com.itennishy.test;

import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.runMainLops;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = runMainLops.class)
public class WebApplicationTests {

    @Test
    public void contextLoads() throws Exception {

//        JSchExecutor jSchUtil = new JSchExecutor();
//        try {
//            jSchUtil = new JSchExecutor("root", "1234", "192.168.188.128");
//            jSchUtil.connect();
//            jSchUtil.upLoadFile("/Users/apple/AllWorkCodeStation/JavaCodeProject/iTennisLops/boot/efi/efidefault","/root/Desktop");
//            jSchUtil.downloadFile("/root/Desktop/boot/efi/efidefault", "/Users/apple/boot/efi/efidefault");
//            jSchUtil.downloadDirAndFile("/root/Desktop/boot", "/Users/apple/boot");
//            jSchUtil.downloadDirAndFile("/root/Desktop/boot/efi/efidefault", "/Users/apple/boot/efi/efidefault");
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            jSchUtil.disconnect();
//        }
    }

    private ConcurrentLinkedQueue<String> linkedQueue = new ConcurrentLinkedQueue<>();


    @Test
    public void testLinkRequest() {
//        AtomicBoolean flag = new AtomicBoolean(true);
//        new Thread(() -> {
//            for (int i = 1; i < 10; i++) {
//                linkedQueue.add("STEP" + i + ":正在执行。。。");
//                try {
//                    TimeUnit.SECONDS.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            flag.set(false);
//        }).start();
//        while (flag.get()) {
//            String txt = linkedQueue.poll();
//            if (txt != null)
//                System.out.println(txt);
//        }
//        System.out.println("结束");
    }

//    @Test
//    public void testRaid() throws Exception {
//        JSchExecutor jSchUtil = new JSchExecutor("root", "123456", "192.168.0.102");
//        jSchUtil.connect();
////        jSchUtil.execCmd("ls /");
//        new Thread(()->{
//            try {
//                System.out.println(jSchUtil.shellCmd("top"));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();
//
//
//    }

}
