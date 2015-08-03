/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.handler;

import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.channel.CloseReason;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public abstract class ChannelHandler {

    private static final ConcurrentMap<ChannelHandler, ExecutorService> THREAD_POOLS;

    private boolean __active_check = false;
    private boolean __inactive_check = false;

    static {
        THREAD_POOLS = new ConcurrentHashMap<>();
    }

    public String getHandlerName() {
        if (getClass().isAnonymousClass()) {
            return getClass().getSuperclass().getName();
        } else {
            return getClass().getName();
        }
    }

    public static final void handleChannel(ChannelHandler handler, Channel channel) {
        ExecutorService executor = THREAD_POOLS.get(handler);
        if (executor == null) {
            ExecutorService newExecutor = Executors.newCachedThreadPool(new HandlerThreadFactory(handler));
            executor = THREAD_POOLS.putIfAbsent(handler, newExecutor);
            if (executor == null) {
                executor = newExecutor;
            }
        }
        executor.execute(new ChannelHandlerThread(handler, channel));
    }

    /**
     *
     * @param channel
     * @throws java.lang.Exception
     */
    public void channelActive(Channel channel) throws Exception {
        this.__active_check = true;
    }

    /**
     *
     * @param channel
     * @param reason
     * @throws java.lang.Exception
     */
    public void channelInactive(Channel channel, CloseReason reason) throws Exception {
        this.__inactive_check = true;
    }

    /**
     *
     * @param channel
     * @param input
     * @throws java.lang.Exception
     */
    public abstract void channelRead(Channel channel, int input) throws Exception;

    private static class HandlerThreadFactory implements ThreadFactory {

        private static final ConcurrentMap<String, Future<AtomicInteger>> poolNumber = new ConcurrentHashMap<>();
        private final ThreadGroup group;
        private final AtomicInteger threadNumber;
        private final String namePrefix;

        public HandlerThreadFactory(ChannelHandler handler) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.threadNumber = new AtomicInteger(1);
            Future<AtomicInteger> future = poolNumber.get(handler.getHandlerName());
            if (future == null) {
                FutureTask<AtomicInteger> task = new FutureTask<>(new Callable<AtomicInteger>() {

                    @Override
                    public AtomicInteger call() throws Exception {
                        return new AtomicInteger(1);
                    }
                });
                future = poolNumber.putIfAbsent(handler.getHandlerName(), task);
                if (future == null) {
                    future = task;
                    task.run();
                }
            }
            try {
                this.namePrefix = handler.getHandlerName() + "-" + future.get().getAndIncrement() + "-thread-";
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(handler.getHandlerName() + " Thread Pool Open Error.");
            }
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + this.threadNumber.getAndIncrement());
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    public abstract void exceptionCaught(Channel channel, Throwable cause);

}
