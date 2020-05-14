package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author gao
 * @create 2020-04-23 17:38
 */
@RestController
@RequestMapping("admin/product/test")
public class TestController {

    @Autowired
    private TestService testService;

    @GetMapping("testLock")
    public Result test(){
        testService.testLock();
        return Result.ok();
    }

    //读锁
    @GetMapping("read")
    public Result readLock(){
        String msg = testService.readLock();
        return Result.ok(msg);
    }

    @GetMapping("write")
    public Result write(){
        String msg = testService.writeLock();
        return Result.ok(msg);
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        //并行化
        //创建一个线程池
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(50, 500, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
        CompletableFuture<String> futureA = CompletableFuture.supplyAsync(() -> "hello");

        CompletableFuture<Void> futureB = futureA.thenAcceptAsync(s -> {
            //线程睡一会
            delaySec(3);
            //输出
            printCurrTime(s + " 第一个线程");
        }, threadPoolExecutor);

        CompletableFuture<Void> futurec = futureA.thenAcceptAsync(s -> {
            //线程睡一会
            delaySec(1);
            //输出
            printCurrTime(s + " 第二个线程");
        }, threadPoolExecutor);

        //支持返回值
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                System.out.println(Thread.currentThread().getName() + "---线程名字");
//                //int i = 1/0;
//                return 1024;
//            }
//        }).thenApply(new Function<Integer, Integer>() {
//            @Override
//            public Integer apply(Integer o) {
//                System.out.println("串行化"+o);
//                return o*2;
//            }
//        }).whenCompleteAsync(new BiConsumer<Integer, Throwable>() {
//            @Override
//            public void accept(Integer o, Throwable throwable) {
//                System.out.println("o执行结果" + o + "===" + o.toString());
//                System.out.println("throwable" + throwable);
//            }
//        }).exceptionally(new Function<Throwable, Integer>() {
//            @Override
//            public Integer apply(Throwable throwable) {
//                System.out.println("throwable异常" + throwable);
//                return 666;
//            }
//        });
//        System.out.println(future.get());
    }

    private static void printCurrTime(String s) {
        System.out.println(s);
    }

    private static void delaySec(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
