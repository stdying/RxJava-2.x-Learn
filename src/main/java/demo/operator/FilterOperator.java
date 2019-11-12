package demo.operator;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.reactivestreams.Subscriber;

import java.util.concurrent.TimeUnit;

public class FilterOperator {
    public static void debounce() {
        Observable
                .create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        for (int i = 0; i < 11; i++) {
                            if (!emitter.isDisposed()) {
                                emitter.onNext(i);
                            }
                            /**
                             * 前面过滤掉，每次阈值250毫秒，当发送一个值，重新等待250毫秒
                             *
                             * 如果每次间隔时间都小于250毫秒，最后只发送最后数据
                             */
                            int sleep = 1;
                            if (i % 3 == 0) {
                                sleep = 3;
                            }
                            try {
                                TimeUnit.SECONDS.sleep(sleep);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        emitter.onComplete();
                    }
                })
                .debounce(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        System.out.println(integer);
                    }
                });
    }
}
