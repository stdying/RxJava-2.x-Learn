package demo.backpressure;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class Test {
    private static final Object obj = new Object();

    public static void main(String[] args) {
        demo();
    }


    private static void demo() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                //test1(); //背压
                test2();//线程切换
            }
        }).start();

        waitOnObj();

    }

    /**
     * 线程切换
     */
    private static void test2() {
        Observable
                .create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                        emitter.onNext(1);
                        emitter.onNext(2);
                        emitter.onNext(3);
                        emitter.onComplete();
                    }
                })// 返回 ObservableCreate 对象
                .subscribeOn(Schedulers.newThread())//返回 ObservableSubscribeOn 对象
                .observeOn(Schedulers.io()) //返回 ObservableObserveOn对象
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        System.out.println("onSubscribe ");
                    }

                    @Override
                    public void onNext(Integer integer) {
                        System.out.println("onNext :" + integer);
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("onError:" + e.getMessage());
                        notifyObjAll();
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                        notifyObjAll();
                    }
                });
    }


    /**
     * 背压
     */
    private static void test1() {
        Flowable.
                create(new FlowableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                        for (int i = 0; i < 300; i++) {
                            System.out.println("requested: --------->" + emitter.requested());
                            emitter.onNext(i);
                            //TimeUnit.MILLISECONDS.sleep(20);
                            System.out.println("emitter: ==>" + i);
                        }
                        emitter.onComplete();
                    }
                }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(100);
                    }

                    @Override
                    public void onNext(Integer integer) {
                        System.out.println("onNext:" + integer);
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("onError:" + t.getMessage());
                        notifyObjAll();
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                        notifyObjAll();
                    }
                });

    }


    private static void notifyObjAll() {
        synchronized (obj) {
            try {
                obj.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void waitOnObj() {
        synchronized (obj) {
            try {
                obj.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
