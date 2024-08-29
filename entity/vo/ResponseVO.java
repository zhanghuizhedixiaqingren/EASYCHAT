package com.easychat.entity.vo;

public class ResponseVO<T> {
    // 响应状态，例如："success" 或 "error"
    private String status;

    // 响应码，通常用于表示HTTP状态码或自定义的状态码
    private Integer code;

    // 响应信息，可能包含错误描述或成功消息
    private String info;

    // 泛型数据，可以是任何类型
    private T data;

    // 无参构造方法
    public ResponseVO() {
    }

    public String getStatus() {
        return status;
    }

    public Integer getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public T getData() {
        return data;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public void setData(T data) {
        this.data = data;
    }
}
