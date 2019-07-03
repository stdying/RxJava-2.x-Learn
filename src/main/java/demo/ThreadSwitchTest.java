package demo;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ThreadSwitchTest {

    public static void main(String[] args) {
        demo();
    }


    private static void demo() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                test();//线程切换
            }
        }).start();

        Util.waitOnObj();

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


