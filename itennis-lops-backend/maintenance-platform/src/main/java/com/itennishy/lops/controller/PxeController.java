package com.itennishy.lops.controller;

import com.alibaba.fastjson.JSONArray;
import com.itennishy.lops.utils.FileUtils;
import com.itennishy.lops.utils.StatusCode;
import com.itennishy.lops.executor.JSchExecutor;
import com.itennishy.lops.service.PxeServerConfigService;
import com.itennishy.lops.utils.JsonData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

@Api(value = "PXE配置类", tags = "PXE配置类")
@RestController
@RequestMapping("/pxe")
public class PxeController {


    @Autowired
    private PxeServerConfigService pxeServerConfigService;

    @ApiOperation(value = "本地安装PXE服务", notes = "本地安装PXE服务")
    @RequestMapping(value = "/local", method = RequestMethod.GET)
    public JsonData setPxeLocalServer() {
        JSchExecutor jSchUtil = new JSchExecutor();
        try {
            jSchUtil = new JSchExecutor(new FileUtils().getPxeConfigYmlWithpxeServer().get("user"), new FileUtils().getPxeConfigYmlWithpxeServer().get("password"), "127.0.0.1");
            jSchUtil.connect();
            int status = pxeServerConfigService.ConfigPxeServer(jSchUtil);
            if (status < 0) {
                return JsonData.BuildRequest(status, StatusCode.STATUS_ERROR);
            } else {
                return JsonData.BuildRequest(StatusCode.STATUS_OK);
            }
        } catch (Exception e) {
            return JsonData.BuildRequest(e.getMessage(), StatusCode.STATUS_ERROR);
        } finally {
            jSchUtil.disconnect();
        }
    }

    @ApiOperation(value = "远程安装PXE服务", notes = "根据指定地址远程安装PXE服务")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "ip", value = "ip地址", dataType = "String", required = true),
            @ApiImplicitParam(name = "user", value = "用户名", dataType = "String", required = true),
            @ApiImplicitParam(name = "pwd", value = "密码", dataType = "String", required = true),
    })
    @RequestMapping(value = "/remote", method = RequestMethod.GET)
    public JsonData setPxeRemoteServer(String ip, String user, String pwd) {
        JSchExecutor jSchUtil = new JSchExecutor();
        try {
            jSchUtil = new JSchExecutor(user, pwd, ip);
            jSchUtil.connect();
            int status = pxeServerConfigService.ConfigPxeServer(jSchUtil);
            if (status < 0) {
                return JsonData.BuildRequest(status, StatusCode.STATUS_ERROR);
            } else {
                return JsonData.BuildRequest(StatusCode.STATUS_OK);
            }
        } catch (Exception e) {
            return JsonData.BuildRequest(e.getMessage(), StatusCode.STATUS_ERROR);
        } finally {
            jSchUtil.disconnect();
        }
    }

    @RequestMapping(value = "/remote", method = RequestMethod.POST)
    public JsonData setPxeRemoteServer(@RequestBody Map<String, Object> data) throws IOException {
        JSONArray arrays = JSONArray.parseArray(data.get("data").toString());
        if (arrays.size() != 1) {
            return JsonData.BuildRequest(StatusCode.STATUS_NOFUND_CONF);
        }
        for (Object array : arrays) {
            String[] objects = JSONArray.parseArray(array.toString()).toArray(new String[0]);
            new FileUtils().getContent2File(data.get("conf").toString(), data);
            BufferedWriter writer = new BufferedWriter(new FileWriter(new FileUtils().getPath() + "conf" + File.separator + "pxe.yml", false));
            writer.write(data.get("highparams").toString());
            writer.flush();
            writer.close();
            return setPxeRemoteServer(objects[1], objects[2], objects[0]);
        }
        return JsonData.BuildRequest(StatusCode.STATUS_ERROR);
    }
}
