package demo.util;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class MyObserver<T> implements Observer<T> {
    String operator;

    public MyObserver(String tag) {
        this.operator = tag;
    }

    @Override
    public void onSubscribe(Disposable d) {
        System.out.println(operator + "-onSubscribe");
    }

    @Override
    public void onNext(T o) {
        System.out.println(operator + "-onNext:" + o);
    }

    @Override
    public void onError(Throwable e) {
        System.out.println(operator + "-onError:" + e.getMessage());
    }

    @Override
    public void onComplete() {
        System.out.println(operator + "-onComplete:");
    }
}