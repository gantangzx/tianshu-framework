package com.gantang.tianshu.mq.trace;

/**
 * 可抛出受检异常的任务，用于消费侧模板包装。
 *
 * @author gantang
 */
@FunctionalInterface
public interface CheckedRunnable {

    /**
     * 执行任务。
     *
     * @throws Exception 执行异常
     */
    void run() throws Exception;
}
