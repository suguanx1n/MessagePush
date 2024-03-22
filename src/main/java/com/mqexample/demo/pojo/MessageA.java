package com.mqexample.demo.pojo;


import lombok.Data;

import java.io.Serializable;

/**
 * @Author sgx
 * @Date 2024/2/29 15:10
 * @Description:
 */
@Data
public class MessageA implements Serializable {

    private String msg;

    private String msgId;

    private String sendTime;

    public MessageA() {
    }

    public MessageA(String msg, String msgId, String sendTime) {
        this.msg = msg;
        this.msgId = msgId;
        this.sendTime = sendTime;
    }
}
