package com.kcs.zolang.utility;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
@Component
public class JobQueueUtility {

    private final ConcurrentHashMap<Long, LinkedBlockingQueue<Runnable>> userQueues = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public void addJob(Long userId, Runnable job) {
        userQueues.computeIfAbsent(userId, id -> new LinkedBlockingQueue<>()).offer(() -> {
            try {
                job.run();
            } finally {
                processNextJob(userId);
            }
        });
        processNextJob(userId);
    }

    private synchronized void processNextJob(Long userId) {
        LinkedBlockingQueue<Runnable> queue = userQueues.get(userId);
        if (queue != null && !queue.isEmpty()) {
            Runnable nextJob = queue.poll();
            if (nextJob != null) {
                executorService.submit(nextJob);
            }
        }
    }
}

