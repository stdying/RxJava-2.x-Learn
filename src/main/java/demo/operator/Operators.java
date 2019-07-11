package demo.operator;

import demo.util.MyObserver;
import demo.util.Util;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Operators {

    static void map() {
        Observable
                .create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        emitter.onNext(1);
                        emitter.onNext(2);
                        emitter.onComplete();
                    }
                })
                .map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer integer) throws Exception {
                        return String.valueOf(integer + 1);
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        System.out.println("map-accept:" + s);
                    }
                });
    }

    /**
     * 当第二个Observable发送数据时，第一个Observable停止发送数据
     */
    static void takeUtil() {
        Observable.interval(0, 1, TimeUnit.SECONDS)
                .takeUntil(Observable.timer(10, TimeUnit.SECONDS))
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Long aLong) {
                        System.out.println("take util:" + aLong);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        System.out.println("take util onComplete");
                        Util.notifyObjAll();
                    }
                });
    }

    /**
     * onError失败后，重新订阅所有数据源
     */
    static void retry() {

        Observable
                .create(new ObservableOnSubscribe<Integer>() {
                    int retry = 0;

                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        emitter.onNext(1);
                        emitter.onNext(2);
                        if (++retry != 3)
                            emitter.onError(new Exception("Exception"));
                        emitter.onNext(3);
                        emitter.onNext(4);
                        emitter.onNext(5);
                        emitter.onComplete();
                    }
                })
                .retry()
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        System.out.println("retry onnext:" + integer);
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("retry onComplete");
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("retry onComplete");
                        Util.notifyObjAll();
                    }
                });
    }

    static void retryWhen() {
        Observable
                .create(new ObservableOnSubscribe<Integer>() {
                    int retry = 0;

                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        emitter.onNext(1);
                        emitter.onNext(2);
                        if (++retry != 2) {
                            emitter.onError(new Exception("error"));
                            System.out.println("emitter:" + retry);
                        }
                        emitter.onComplete();

                    }
                })
                /**
                 *当onError触发，内部实现重试逻辑，
                 * 返回Observable发送相同的数据
                 */
                .retryWhen(new Function<Observable<Throwable>, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Observable<Throwable> throwableObservable) throws Exception {
                        return throwableObservable
                                .zipWith(Observable.range(1, 3), new BiFunction<Throwable, Integer, Integer>() {
                                    @Override
                                    public Integer apply(Throwable throwable, Integer integer) throws Exception {
                                        return integer;
                                    }
                                })
                                .flatMap(new Function<Integer, ObservableSource<?>>() {
                                    @Override
                                    public ObservableSource<?> apply(Integer integer) throws Exception {
                                        System.out.println("delay retry by " + integer + " second(s)");
                                        return Observable.timer(integer, TimeUnit.SECONDS);
                                    }
                                });

                    }
                })
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        System.out.println("retryWhen:" + integer);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        System.out.println("retryWhen onComplete");
                        Util.notifyObjAll();
                    }
                });
    }

    static void retryWhen2() {
        Observable.timer(1, TimeUnit.SECONDS)
                .doOnSubscribe(s -> System.out.println("subscribing"))
                .map(v -> {
                    throw new RuntimeException();
                })
                .retryWhen(errors -> {
                    AtomicInteger counter = new AtomicInteger();
                    return errors
                            .takeWhile(e -> counter.getAndIncrement() != 3)
                            .flatMap(e -> {
                                System.out.println("delay retry by " + counter.get() + " second(s)");
                                return Observable.timer(counter.get(), TimeUnit.SECONDS);
                            });
                })
                .subscribe(System.out::println, System.out::println, () -> {
                    //Util.notifyObjAll();
                });
    }

}



