package demo;

import demo.util.Util;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ThreadSwitchTest {

    public static void main(String[] args) {
        demo();
    }


    private static void demo() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                test2();//线程切换
                //test3();//线程切换
            }
        }).start();

        try {
            TimeUnit.HOURS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Util.waitOnObj();

    }

    /**
     * 线程切换
     */
    private static void test() {
        Observable
                .create(new MyObservableOnSubscribe())// 返回 ObservableCreate 对象
                .subscribeOn(Schedulers.newThread())//返回 ObservableSubscribeOn 对象
                .observeOn(Schedulers.io()) //返回 ObservableObserveOn对象
                .subscribe(new MyObserver<Integer>());
    }

    static int createData(){
        System.out.println("create:" + Thread.currentThread().getName());
        return 1;
    }

    /**
     * 线程切换
     */
    private static void test2() {
        Observable
                //.just(createData())
                .fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                System.out.println("create:" + Thread.currentThread().getName());
                return 1;
            }
        })
//                .create(new ObservableOnSubscribe<Integer>() {
//                    @Override
//                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
//                        System.out.println("create:" + Thread.currentThread().getName());
//                        emitter.onNext(1);
//                        emitter.onNext(2);
//                        emitter.onNext(3);
//                        emitter.onNext(4);
//                    }
//                })// 返回 ObservableCreate 对象
                .subscribeOn(Schedulers.single())//返回 ObservableSubscribeOn 对象
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Exception {
                        System.out.println("map-1:" + Thread.currentThread().getName());
                        return integer;
                    }
                })
                .observeOn(Schedulers.computation()) //返回 ObservableObserveOn对象
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Exception {
                        System.out.println("map-2:" + Thread.currentThread().getName());
                        return integer;
                    }
                })
                .subscribeOn(Schedulers.trampoline())//返回 ObservableSubscribeOn 对象
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Exception {
                        System.out.println("map-3:" + Thread.currentThread().getName());
                        return integer;
                    }
                })
                .subscribeOn(Schedulers.newThread())//返回 ObservableSubscribeOn 对象
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Exception {
                        System.out.println("map-4:" + Thread.currentThread().getName());
                        return integer;
                    }
                })
                .observeOn(Schedulers.io())//返回 ObservableSubscribeOn 对象
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        System.out.println("Consumer:" + Thread.currentThread().getName());
                    }
                });
    }

    /**
     * 线程切换
     */
    private static void test3() {
        Observable
                //.just(createData())
                .fromCallable(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        System.out.println("create:" + Thread.currentThread().getName());
                        return 1;
                    }
                })
//                .create(new ObservableOnSubscribe<Integer>() {
//                    @Override
//                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
//                        System.out.println("create:" + Thread.currentThread().getName());
//                        emitter.onNext(1);
//                        emitter.onNext(2);
//                        emitter.onNext(3);
//                        emitter.onNext(4);
//                    }
//                })// 返回 ObservableCreate 对象
                .subscribeOn(Schedulers.single())//返回 ObservableSubscribeOn 对象
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Exception {
                        System.out.println("map-1:" + Thread.currentThread().getName());
                        return integer;
                    }
                })
                .subscribeOn(Schedulers.computation()) //返回 ObservableObserveOn对象
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Exception {
                        System.out.println("map-2:" + Thread.currentThread().getName());
                        return integer;
                    }
                })
                .subscribeOn(Schedulers.trampoline())//返回 ObservableSubscribeOn 对象
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Exception {
                        System.out.println("map-3:" + Thread.currentThread().getName());
                        return integer;
                    }
                })
                .subscribeOn(Schedulers.newThread())//返回 ObservableSubscribeOn 对象
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer) throws Exception {
                        System.out.println("map-4:" + Thread.currentThread().getName());
                        return integer;
                    }
                })
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        System.out.println("Consumer:" + Thread.currentThread().getName());
                    }
                });
    }


    public static class MyObservableOnSubscribe implements ObservableOnSubscribe<Integer> {

        @Override
        public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        }
    }

    public static class MyObserver<T> implements Observer<T> {
        @Override
        public void onSubscribe(Disposable d) {
            System.out.println("onSubscribe ");
        }

        @Override
        public void onNext(T t) {
            System.out.println("onNext :" + t);
        }

        @Override
        public void onError(Throwable e) {
            System.out.println("onError:" + e.getMessage());
            Util.notifyObjAll();
        }

        @Override
        public void onComplete() {
            System.out.println("onComplete");
            Util.notifyObjAll();
        }
    }

}


