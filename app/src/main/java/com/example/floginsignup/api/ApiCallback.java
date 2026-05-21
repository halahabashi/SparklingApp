package com.example.floginsignup.api;

public interface ApiCallback<T> {
    void onSuccess(T data);
    void onError(Throwable error);
}
