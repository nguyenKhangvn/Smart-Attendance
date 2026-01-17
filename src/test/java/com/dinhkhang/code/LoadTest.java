package com.dinhkhang.code;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load Test để kiểm tra khả năng chịu tải của hệ thống
 * Test mô phỏng: 50-100 sinh viên quét QR code đồng thời
 * 
 * Test này giúp đánh giá:
 * - Throughput: Số requests/giây hệ thống xử lý được
 * - Response Time: Thời gian phản hồi trung bình/min/max
 * - Concurrency: Khả năng xử lý đồng thời nhiều requests
 */
public class LoadTest {

    private static final int NUM_STUDENTS_50 = 50;
    private static final int NUM_STUDENTS_100 = 100;

    @Test
    public void testConcurrent50Students() throws InterruptedException, ExecutionException {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║      LOAD TEST: 50 Students Concurrent Check-in            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        runConcurrentTest(NUM_STUDENTS_50);
    }

    @Test
    public void testConcurrent100Students() throws InterruptedException, ExecutionException {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║      LOAD TEST: 100 Students Concurrent Check-in           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        runConcurrentTest(NUM_STUDENTS_100);
    }

    @Test
    public void testSequential100Students() throws Exception {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║      BASELINE: 100 Students Sequential Check-in            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        runSequentialTest(NUM_STUDENTS_100);
    }

    private void runConcurrentTest(int numStudents) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(numStudents);
        List<Future<TaskResult>> futures = new ArrayList<>();
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();
        
        long testStartTime = System.currentTimeMillis();

        // Submit concurrent tasks (mô phỏng check-in)
        for (int i = 0; i < numStudents; i++) {
            final int studentId = i;
            
            Future<TaskResult> future = executorService.submit(() -> {
                long startTime = System.currentTimeMillis();
                
                try {
                    // Mô phỏng xử lý check-in (validation + database save)
                    simulateCheckInProcess(studentId);
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    responseTimes.add(responseTime);
                    successCount.incrementAndGet();
                    
                    return new TaskResult(true, responseTime, null);
                    
                } catch (Exception e) {
                    long responseTime = System.currentTimeMillis() - startTime;
                    responseTimes.add(responseTime);
                    failureCount.incrementAndGet();
                    
                    return new TaskResult(false, responseTime, e.getMessage());
                }
            });
            
            futures.add(future);
        }

        // Wait for all tasks to complete
        for (Future<TaskResult> future : futures) {
            future.get(); // Block until task completes
        }
        
        long testEndTime = System.currentTimeMillis();
        long totalTestTime = testEndTime - testStartTime;

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // Print results
        printTestResults("CONCURRENT", numStudents, totalTestTime, 
                        successCount.get(), failureCount.get(), responseTimes);
    }

    private void runSequentialTest(int numStudents) throws Exception {
        int successCount = 0;
        int failureCount = 0;
        List<Long> responseTimes = new ArrayList<>();
        
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < numStudents; i++) {
            long startTime = System.currentTimeMillis();
            
            try {
                simulateCheckInProcess(i);
                
                long responseTime = System.currentTimeMillis() - startTime;
                responseTimes.add(responseTime);
                successCount++;
                
            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                responseTimes.add(responseTime);
                failureCount++;
            }
        }

        long testEndTime = System.currentTimeMillis();
        long totalTime = testEndTime - testStartTime;

        printTestResults("SEQUENTIAL", numStudents, totalTime, 
                        successCount, failureCount, responseTimes);
    }

    /**
     * Mô phỏng quy trình check-in:
     * - Validation QR code (10-30ms)
     * - GPS validation (20-50ms)
     * - Face recognition (100-300ms)
     * - Database save (30-100ms)
     */
    private void simulateCheckInProcess(int studentId) throws Exception {
        // 1. QR Code validation
        Thread.sleep(ThreadLocalRandom.current().nextInt(10, 31));
        
        // 2. GPS validation
        Thread.sleep(ThreadLocalRandom.current().nextInt(20, 51));
        
        // 3. Face recognition (expensive operation)
        Thread.sleep(ThreadLocalRandom.current().nextInt(100, 301));
        
        // 4. Database save
        Thread.sleep(ThreadLocalRandom.current().nextInt(30, 101));
        
        // Mô phỏng 5% lỗi rate (realistic scenario)
        if (ThreadLocalRandom.current().nextInt(100) < 5) {
            throw new Exception("Simulated error for student " + studentId);
        }
    }

    private void printTestResults(String testType, int numStudents, long totalTime, 
                                  int success, int failure, List<Long> responseTimes) {
        System.out.println("\n┌─────────────── Test Results ───────────────┐");
        System.out.println("│ Test Type       : " + testType);
        System.out.println("│ Total Students  : " + numStudents);
        System.out.println("│ Successful      : " + success);
        System.out.println("│ Failed          : " + failure);
        System.out.println("│ Success Rate    : " + String.format("%.2f%%", (success * 100.0 / numStudents)));
        System.out.println("│ Total Time      : " + totalTime + " ms");
        System.out.println("└────────────────────────────────────────────┘");
        
        if (!responseTimes.isEmpty()) {
            long avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).sum() / responseTimes.size();
            long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
            long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
            double throughput = numStudents * 1000.0 / totalTime;
            
            System.out.println("\n┌────────── Response Time Statistics ────────┐");
            System.out.println("│ Average         : " + avgResponseTime + " ms");
            System.out.println("│ Min             : " + minResponseTime + " ms");
            System.out.println("│ Max             : " + maxResponseTime + " ms");
            System.out.println("│ Throughput      : " + String.format("%.2f requests/sec", throughput));
            System.out.println("└────────────────────────────────────────────┘");
        }

        // Performance evaluation
        System.out.println("\n┌────────── Performance Assessment ──────────┐");
        double successRate = (success * 100.0 / numStudents);
        long avgTime = responseTimes.stream().mapToLong(Long::longValue).sum() / Math.max(1, responseTimes.size());
        
        String assessment;
        if (successRate >= 95 && avgTime < 500) {
            assessment = "✓ EXCELLENT - Ready for production";
        } else if (successRate >= 90 && avgTime < 1000) {
            assessment = "⚠ GOOD - Minor optimizations needed";
        } else if (successRate >= 80 && avgTime < 2000) {
            assessment = "⚠ FAIR - Performance tuning required";
        } else {
            assessment = "✗ POOR - Major improvements needed";
        }
        
        System.out.println("│ " + assessment);
        System.out.println("└────────────────────────────────────────────┘\n");
    }

    private static class TaskResult {
        boolean success;
        long responseTime;
        String errorMessage;

        public TaskResult(boolean success, long responseTime, String errorMessage) {
            this.success = success;
            this.responseTime = responseTime;
            this.errorMessage = errorMessage;
        }
    }
}
