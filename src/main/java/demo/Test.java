package demo;

import demo.util.Util;
import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class Test {

    public static void main(String[] args) {
        demo();
    }


    private static void demo() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                test1(); //背压
            }
        }).start();

        Util.waitOnObj();

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
                        Util.notifyObjAll();
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                        Util.notifyObjAll();
                    }
                });

    }


}
