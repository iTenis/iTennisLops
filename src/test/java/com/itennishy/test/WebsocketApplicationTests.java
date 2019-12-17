package com.itennishy.test;

import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.runMainLops;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = runMainLops.class)
public class WebsocketApplicationTests {

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


}
