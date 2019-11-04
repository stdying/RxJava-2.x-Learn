package demo.create;

import demo.util.MyObserver;
import io.reactivex.Observable;

public class CreateTest {

    public static void main(String[] args) {
        repeat();
    }


    private static void repeat() {
        Observable.just(1).repeat(10).subscribe(new MyObserver<>("repeat"));
    }


}
