package demo.util;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public class MyObservableOnSubscribe implements ObservableOnSubscribe<Integer> {
    @Override
    public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
        emitter.onNext(1);
        emitter.onNext(2);
        emitter.onNext(3);
    }
}
