package demo.operator;

import demo.Util;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import java.util.concurrent.TimeUnit;

public class Operators {

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
}
