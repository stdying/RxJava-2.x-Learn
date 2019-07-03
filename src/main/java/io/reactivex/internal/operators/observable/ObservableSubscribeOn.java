/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

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
     * s demo中对应 {@link ObservableObserveOn.ObserveOnObserver}对象
     */
    @Override
    public void subscribeActual(final Observer<? super T> s) {
        //创建在用于 SubscribeOnObserver 的观察者，s在这里是 ObserveOnObserver 对象
        final SubscribeOnObserver<T> parent = new SubscribeOnObserver<T>(s);
        /**
         *  调用 {@link ObservableObserveOn.ObserveOnObserver#onSubscribe}
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
             * 内部调用{@link ObservableCreate#subscribeActual} 的方法
             *
             * 数据最终会发送到 {@link ObservableObserveOn.ObserveOnObserver#queue}的中
             */
            source.subscribe(parent);
        }
    }
}
