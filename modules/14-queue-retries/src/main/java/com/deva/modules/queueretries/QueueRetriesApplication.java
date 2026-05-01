package com.deva.modules.queueretries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class QueueRetriesApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueueRetriesApplication.class, args);
    }
}

record WorkMessage(String messageId, String mode, int receiveCount) {
    WorkMessage received() {
        return new WorkMessage(messageId, mode, receiveCount + 1);
    }
}

@Service
class LabQueue {
    private static final int MAX_RECEIVES = 3;
    private final Deque<WorkMessage> queue = new ArrayDeque<>();
    private final List<WorkMessage> dlq = new ArrayList<>();

    synchronized WorkMessage send(String mode) {
        WorkMessage message = new WorkMessage("msg-" + UUID.randomUUID(), mode, 0);
        queue.addLast(message);
        return message;
    }

    synchronized Map<String, Object> processOne() {
        WorkMessage message = queue.pollFirst();
        if (message == null) {
            return Map.of("processed", false, "reason", "queue-empty");
        }

        WorkMessage received = message.received();
        if ("poison".equals(received.mode()) || ("retry".equals(received.mode()) && received.receiveCount() < 3)) {
            if (received.receiveCount() >= MAX_RECEIVES) {
                dlq.add(received);
                return Map.of("processed", false, "messageId", received.messageId(), "result", "sent-to-dlq");
            }
            queue.addLast(received);
            return Map.of("processed", false, "messageId", received.messageId(), "result", "retry-scheduled",
                    "receiveCount", received.receiveCount());
        }

        return Map.of("processed", true, "messageId", received.messageId(), "receiveCount", received.receiveCount());
    }

    synchronized Map<String, Object> stats() {
        return Map.of("visible", queue.size(), "dlq", dlq.size());
    }
}

@RestController
class QueueController {
    private final LabQueue queue;

    QueueController(LabQueue queue) {
        this.queue = queue;
    }

    @PostMapping("/api/messages")
    WorkMessage send(@RequestParam(defaultValue = "normal") String mode) {
        return queue.send(mode);
    }

    @PostMapping("/api/worker/process-one")
    Map<String, Object> processOne() {
        return queue.processOne();
    }

    @GetMapping("/api/queue/stats")
    Map<String, Object> stats() {
        return queue.stats();
    }
}

