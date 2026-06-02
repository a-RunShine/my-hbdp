package com.hmdp.utils;

public interface ILock {
    public Boolean tryLock(Long timeOut);
    public void unlock();
}
