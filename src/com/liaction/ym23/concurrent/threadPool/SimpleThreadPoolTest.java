package com.liaction.ym23.concurrent.threadPool;

import java.util.Optional;
import java.util.stream.IntStream;

public class SimpleThreadPoolTest {

    public static void main(String[] args) throws InterruptedException {
        SimpleThreadPool simpleThreadPool = new SimpleThreadPool();
        IntStream.rangeClosed(0,40).forEach(index->{
            simpleThreadPool.addTask(()->{
                Optional.of("The task " + index + " start." + " service thread: " + Thread.currentThread()).ifPresent(System.out::println);
                try {
                    if (index == 23){
                        while (true){}
                    }else {
                        Thread.sleep(1_000L);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Optional.of("The task " + index + " end." + " service thread: " + Thread.currentThread()).ifPresent(System.out::println);
            });
        });
        simpleThreadPool.start();
        try {
            Thread.sleep(1_000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        simpleThreadPool.shutDown(5_000L);
        IntStream.rangeClosed(0,23).forEach(index->{
            simpleThreadPool.addTask(()->{
                Optional.of("The task " + index + " start." + " service thread: " + Thread.currentThread()).ifPresent(System.out::println);
                try {
                    Thread.sleep(1_000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Optional.of("The task " + index + " end." + " service thread: " + Thread.currentThread()).ifPresent(System.out::println);
            });
        });
    }

}
