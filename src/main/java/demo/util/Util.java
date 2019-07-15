package demo.util;

import java.util.concurrent.TimeUnit;

public class Util {

    private static final Object obj = new Object();


    public static void notifyObjAll() {
        synchronized (obj) {
            obj.notifyAll();
        }
    }

    public static void waitOnObj() {
        synchronized (obj) {
            try {
                obj.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void waitHours(){
        try {
            TimeUnit.HOURS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
