package com.itennishy.lops.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JsonData implements Serializable {

    @Singular
    private Integer code; // 状态码 0 表示成功，1表示处理中，-1表示失败
    @Singular
    private Object data; // 数据
    @Singular
    private String msg;

    public static JsonData BuildSuccess(){
        return new JsonData(200,null,null);
    }

    public static JsonData BuildSuccess(Object data){
        return new JsonData(200,data,null);
    }

    public static JsonData BuildSuccess(Integer code, Object data, String msg){
        return new JsonData(code,data,msg);
    }

    public static JsonData BuildError(){
        return new JsonData(400,null,null);
    }
    public static JsonData BuildError(Integer code, String msg){
        return new JsonData(code,null,msg);
    }

    public static JsonData BuildError(Integer code, Object data, String msg){
        return new JsonData(code,data,msg);
    }

}
