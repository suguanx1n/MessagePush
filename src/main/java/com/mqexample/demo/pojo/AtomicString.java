package com.mqexample.demo.pojo;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author sgx
 * @Date 2024/3/16 21:54
 * @Description: 封装一个原子字符串类
 */
@Slf4j
public class AtomicString {
    private AtomicReference<String> atomicString = new AtomicReference<>("initial");

    public String getAtomicString() {
        return atomicString.get();
    }

    public void setAtomicString(String newValue) {
        atomicString.set(newValue);
    }

    public boolean compareAndSetAtomicString(String expect, String update) {
        return atomicString.compareAndSet(expect, update);
    }
}
