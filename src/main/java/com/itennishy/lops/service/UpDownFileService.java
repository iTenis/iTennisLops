package com.itennishy.lops.service;

import com.itennishy.lops.executor.JSchExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UpDownFileService {

    public void upLoadFile(JSchExecutor jSchUtil, String remote, String local) throws Exception {
        jSchUtil.upLoadFile(local, remote, 0);
    }

    public void downLoadFile(JSchExecutor jSchUtil, String remote, String local) throws Exception {
        jSchUtil.downloadDirAndFile(remote, local);
    }
}
