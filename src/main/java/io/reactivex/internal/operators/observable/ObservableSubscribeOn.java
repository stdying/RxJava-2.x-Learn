/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 *
 * 通过subscribe向上订阅原始数据
 *
 * 通过 subscribe 实现向上订阅，传递订阅者，每传递一次，订阅者被包装一次
 *
 * 最后source数据在第一个subscribeOn的线程上发送数据
 *
 * 计算有多个subscribeOn，也只会在使用最靠近 source的线程
 *
 * @param <T>
 */
public final class ObservableSubscribeOn<T>
        extends AbstractObservableWithUpstream<T, T> {
    final Scheduler scheduler;

    /**
     * 使用 create创建时，source对象为 {@link ObservableCreate}
     */
    public ObservableSubscribeOn(ObservableSource<T> source, Scheduler scheduler) {
        super(source);
        this.scheduler = scheduler;
    }


    /**
     * 在ObservableObserveOn对象中生成存放数据的队列queue
     *
     * 启动异步任务，开始发送原始数据
     *
     * @param s
     */
    @Override
    public void subscribeActual(final Observer<? super T> s) {
        /**
         创建在用于 SubscribeOnObserver 的观察者，
         s在这里是 {@link ObservableObserveOn.ObserveOnObserver} 对象
         */
        final SubscribeOnObserver<T> parent = new SubscribeOnObserver<T>(s);
        /**
         *  调用 {@link ObservableObserveOn.ObserveOnObserver#onSubscribe(Disposable)}
         *  生成用于存放数据的缓存队列
         */

        s.onSubscribe(parent);

        /**
         * scheduler 中直接执行异步任务，即发送原始数据
         */
        parent.setDisposable(scheduler.scheduleDirect(new SubscribeTask(parent)));
    }

    static final class SubscribeOnObserver<T>
            extends AtomicReference<Disposable>
            implements Observer<T>, Disposable {

        private static final long serialVersionUID = 8094547886072529208L;

        /**
         * 在demo中对应 {@link ObservableObserveOn.ObserveOnObserver}
         */
        final Observer<? super T> actual;

        final AtomicReference<Disposable> s;

        SubscribeOnObserver(Observer<? super T> actual) {
            this.actual = actual;
            this.s = new AtomicReference<Disposable>();
        }

        @Override
        public void onSubscribe(Disposable s) {
            DisposableHelper.setOnce(this.s, s);
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(s);
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        void setDisposable(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }
    }

    final class SubscribeTask implements Runnable {
        private final SubscribeOnObserver<T> parent;

        SubscribeTask(SubscribeOnObserver<T> parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            /**
             * source 为 {@link ObservableCreate}对象实例，
             * 内部调用{@link ObservableCreate#subscribeActual(Observer)} 的方法
             * 数据会发送给观察者 parent即{@link SubscribeOnObserver},
             *
             * 数据到SubscribeOnObserver，会将数据发送给它观察者actual，即{@link ObservableObserveOn.ObserveOnObserver}
             * 在onNext方法中数据进入 {@link ObservableObserveOn.ObserveOnObserver#queue} 队列中
             */
            source.subscribe(parent);
        }
    }
}
