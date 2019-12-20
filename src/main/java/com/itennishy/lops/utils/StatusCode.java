package com.itennishy.lops.utils;

public enum StatusCode {

    //自定义的状态码
    STATUS_OK(200,"执行操作正常"),
    STATUS_ERROR(50001,"执行操作返回错误"),
    STATUS_NOFUND_CONF(50002,"配置文件存在问题，请核实查看"),
    STATUS_PARAMS_ERROR(50003,"参数配置错误，请核实查看"),
    ;

    //错误码
    public Integer code;
    //提示信息
    public String message;

    //构造函数
    StatusCode(Integer code,String message){
        this.code = code;
        this.message = message;
    }

    //获取状态码
    public Integer getCode(){
        return code;
    }

    //获取提示信息
    public String getMessage(){
        return message;
    }
}
