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

import io.reactivex.*;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.observers.BasicIntQueueDisposable;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.schedulers.TrampolineScheduler;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableObserveOn<T>
        extends AbstractObservableWithUpstream<T, T> {
    final Scheduler scheduler;
    final boolean delayError;
    final int bufferSize;

    /**
     * @param source ObservableSubscribeOn对象
     */
    public ObservableObserveOn(ObservableSource<T> source, Scheduler scheduler,
                               boolean delayError, int bufferSize) {
        super(source);
        this.scheduler = scheduler;
        this.delayError = delayError;
        this.bufferSize = bufferSize;
    }

    /**
     * 观察者订阅调用该方法,
     * observer 正常来说时是最终的消费者 {@link demo.ThreadSwitchTest.MyObserver}
     * source 为 {@link ObservableSubscribeOn}
     */
    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        if (scheduler instanceof TrampolineScheduler) {
            source.subscribe(observer);
        } else {
            //获取一个工作执行者
            Scheduler.Worker w = scheduler.createWorker();

            /**
             * source订阅，这里source是{@link ObservableSubscribeOn}对象，
             *调用subscribe-->subscribeActual
             *后面会调用{@link ObservableSubscribeOn}的{@link ObservableSubscribeOn#subscribeActual}方法
             */
            source.subscribe(new ObserveOnObserver<T>(observer, w, delayError, bufferSize));
        }
    }

    static final class ObserveOnObserver<T>
            extends BasicIntQueueDisposable<T>
            implements Observer<T>, Runnable {

        private static final long serialVersionUID = 6576896619930983584L;

        /**
         * 根据demo，这里actual是最终的消费者,对应demo中{@link demo.ThreadSwitchTest.MyObserver}
         */
        final Observer<? super T> actual;
        final Scheduler.Worker worker;
        final boolean delayError;
        final int bufferSize;

        SimpleQueue<T> queue; //可扩展的队列，接受生产者产生的数据

        Disposable s;

        Throwable error;
        volatile boolean done;

        volatile boolean cancelled;

        int sourceMode;

        boolean outputFused;

        /**
         * @param actual 根据demo，这里actual是最终的消费者,对应demo中{@link demo.ThreadSwitchTest.MyObserver}
         */
        ObserveOnObserver(Observer<? super T> actual, Scheduler.Worker worker,
                          boolean delayError, int bufferSize) {
            this.actual = actual;
            this.worker = worker;
            this.delayError = delayError;
            this.bufferSize = bufferSize;
        }

        /**
         * s 根据demo，s是SubscribeOnObserver对象
         * <p>
         * 主要生成缓存队列queue，用于保存生成的数据
         */
        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                //第一次进入不是QueueDisposable
                if (s instanceof QueueDisposable) {
                    @SuppressWarnings("unchecked")
                    QueueDisposable<T> qd = (QueueDisposable<T>) s;

                    int m = qd.requestFusion(QueueDisposable.ANY | QueueDisposable.BOUNDARY);

                    if (m == QueueDisposable.SYNC) {
                        sourceMode = m;
                        queue = qd;
                        done = true;
                        actual.onSubscribe(this);
                        schedule();
                        return;
                    }
                    if (m == QueueDisposable.ASYNC) {
                        sourceMode = m;
                        queue = qd;
                        actual.onSubscribe(this);
                        return;
                    }
                }

                /*
                 *单个生产者消费者，在消费者速度生产者速度时，开辟新的数组容数据
                 */
                queue = new SpscLinkedArrayQueue<T>(bufferSize);

                //调用MyObserver的onSubscribe
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            if (sourceMode != QueueDisposable.ASYNC) {
                //将数据存入缓存池中
                queue.offer(t);
            }

            //判断是否启动异步任务消费数据
            schedule();
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            error = t;
            done = true;
            schedule();
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            schedule();
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                s.dispose();
                worker.dispose();
                if (getAndIncrement() == 0) {
                    queue.clear();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void schedule() {
            /**
             * 判断是否在worker中异步消费数据
             */
            if (getAndIncrement() == 0) {
                /**
                 * 执行 run方法，里面for循环，查询 {@link queue}中是否存在数据，
                 * 如果存在，获取数据执行根据 {@link demo.ThreadSwitchTest.MyObserver} 中 OnXX方法
                 */
                worker.schedule(this);
            }
        }

        void drainNormal() {
            int missed = 1;

            final SimpleQueue<T> q = queue;
            final Observer<? super T> a = actual;

            for (; ; ) {
                if (checkTerminated(done, q.isEmpty(), a)) {
                    return;
                }

                for (; ; ) {
                    boolean d = done;
                    T v;

                    try {
                        //没有数据返回null
                        v = q.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        s.dispose();
                        q.clear();
                        a.onError(ex);
                        worker.dispose();
                        return;
                    }
                    boolean empty = v == null;

                    if (checkTerminated(d, empty, a)) {
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(v);
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        void drainFused() {
            int missed = 1;

            for (; ; ) {
                if (cancelled) {
                    return;
                }

                boolean d = done;
                Throwable ex = error;

                if (!delayError && d && ex != null) {
                    actual.onError(error);
                    worker.dispose();
                    return;
                }

                actual.onNext(null);

                if (d) {
                    ex = error;
                    if (ex != null) {
                        actual.onError(ex);
                    } else {
                        actual.onComplete();
                    }
                    worker.dispose();
                    return;
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public void run() {
            if (outputFused) {
                drainFused();
            } else {
                drainNormal();
            }
        }

        boolean checkTerminated(boolean d, boolean empty, Observer<? super T> a) {
            if (cancelled) {
                queue.clear();
                return true;
            }
            if (d) {
                Throwable e = error;
                if (delayError) {
                    if (empty) {
                        if (e != null) {
                            a.onError(e);
                        } else {
                            a.onComplete();
                        }
                        worker.dispose();
                        return true;
                    }
                } else {
                    if (e != null) {
                        queue.clear();
                        a.onError(e);
                        worker.dispose();
                        return true;
                    } else if (empty) {
                        a.onComplete();
                        worker.dispose();
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & ASYNC) != 0) {
                outputFused = true;
                return ASYNC;
            }
            return NONE;
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            return queue.poll();
        }

        @Override
        public void clear() {
            queue.clear();
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }
}
