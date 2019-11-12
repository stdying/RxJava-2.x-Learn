package demo.operator;

import demo.util.Util;

import java.util.concurrent.TimeUnit;

/**
 * 操作符
 */
public class OperatorLearn {
    public static void main(String[] args) throws InterruptedException {
        //Operators.takeUtil();
       // Operators.retry();

        //Operators.retryWhen();
        //Operators.retryWhen2();

        //Operators.map();
        Operators.publish();

        //Operators.hotColdObservable();
        //Operators.hotColdObservableRefCount();
        //FilterOperator.debounce();

        TimeUnit.HOURS.sleep(1);

    }


}
