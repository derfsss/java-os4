package java8.java.util.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrencyTest {
    static int P, F;
    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    // Shared counter guarded by synchronized
    static final Object LOCK = new Object();
    static long sharedCounter = 0;
    static void bump(){ synchronized(LOCK){ sharedCounter++; } }

    public static void main(String[] args) {
        try {
            // --- Thread: start/join/getName/setPriority/isAlive ---
            final AtomicInteger ranFlag = new AtomicInteger(0);
            Thread t = new Thread(() -> ranFlag.set(42), "worker-A");
            t.setPriority(Thread.NORM_PRIORITY);
            ck("thread.getName", t.getName(), "worker-A");
            ck("thread.getPriority", t.getPriority(), Thread.NORM_PRIORITY);
            ck("thread.notAliveBeforeStart", t.isAlive(), false);
            t.start();
            t.join();
            ck("thread.notAliveAfterJoin", t.isAlive(), false);
            ck("thread.ranBody", ranFlag.get(), 42);

            // --- synchronized + shared counter across N threads (exact total) ---
            sharedCounter = 0;
            final int N = 8, ITERS = 1000;
            Thread[] ths = new Thread[N];
            for (int i = 0; i < N; i++) {
                ths[i] = new Thread(() -> { for (int k = 0; k < ITERS; k++) bump(); });
            }
            for (Thread th : ths) th.start();
            for (Thread th : ths) th.join();
            ck("synchronized.exactTotal", sharedCounter, (long)(N * ITERS));

            // --- ExecutorService: submit/Future.get, invokeAll, shutdown/awaitTermination ---
            ExecutorService pool = Executors.newFixedThreadPool(4);
            Future<Integer> fut = pool.submit(() -> 6 * 7);
            ck("executor.future.get", fut.get(), 42);

            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 1; i <= 5; i++) { final int v = i; tasks.add(() -> v * v); }
            List<Future<Integer>> results = pool.invokeAll(tasks);
            int sumSquares = 0;
            for (Future<Integer> rf : results) sumSquares += rf.get();
            ck("executor.invokeAll.sumSquares", sumSquares, 1+4+9+16+25);

            pool.shutdown();
            boolean term = pool.awaitTermination(5, TimeUnit.SECONDS);
            ck("executor.awaitTermination", term, true);
            ck("executor.isShutdown", pool.isShutdown(), true);

            // --- CountDownLatch ---
            ExecutorService p2 = Executors.newFixedThreadPool(3);
            final CountDownLatch latch = new CountDownLatch(3);
            final AtomicInteger latchHits = new AtomicInteger(0);
            for (int i = 0; i < 3; i++) {
                p2.submit(() -> { latchHits.incrementAndGet(); latch.countDown(); });
            }
            boolean latchDone = latch.await(5, TimeUnit.SECONDS);
            ck("countDownLatch.await", latchDone, true);
            ck("countDownLatch.count", latch.getCount(), 0L);
            ck("countDownLatch.hits", latchHits.get(), 3);

            // --- CyclicBarrier ---
            final int PARTIES = 4;
            final AtomicInteger barrierAction = new AtomicInteger(0);
            final CyclicBarrier barrier = new CyclicBarrier(PARTIES, () -> barrierAction.incrementAndGet());
            final AtomicInteger pastBarrier = new AtomicInteger(0);
            Thread[] bts = new Thread[PARTIES];
            for (int i = 0; i < PARTIES; i++) {
                bts[i] = new Thread(() -> {
                    try { barrier.await(5, TimeUnit.SECONDS); pastBarrier.incrementAndGet(); }
                    catch (Exception e) { /* fail via count */ }
                });
            }
            for (Thread bt : bts) bt.start();
            for (Thread bt : bts) bt.join();
            ck("cyclicBarrier.allPassed", pastBarrier.get(), PARTIES);
            ck("cyclicBarrier.actionRanOnce", barrierAction.get(), 1);

            // --- Semaphore: acquire/release ---
            Semaphore sem = new Semaphore(2);
            ck("semaphore.initialPermits", sem.availablePermits(), 2);
            sem.acquire();
            sem.acquire();
            ck("semaphore.afterTwoAcquire", sem.availablePermits(), 0);
            ck("semaphore.tryAcquireFails", sem.tryAcquire(), false);
            sem.release();
            ck("semaphore.afterRelease", sem.availablePermits(), 1);
            sem.release();

            // --- ConcurrentHashMap: compute/merge/putIfAbsent ---
            ConcurrentHashMap<String,Integer> chm = new ConcurrentHashMap<>();
            ck("chm.putIfAbsentNew", chm.putIfAbsent("a", 1), null);
            ck("chm.putIfAbsentExisting", chm.putIfAbsent("a", 99), 1);
            chm.compute("a", (k,v) -> v + 10);
            ck("chm.compute", chm.get("a"), 11);
            chm.merge("b", 5, Integer::sum);
            chm.merge("b", 7, Integer::sum);
            ck("chm.merge", chm.get("b"), 12);
            ck("chm.size", chm.size(), 2);

            // --- CopyOnWriteArrayList ---
            CopyOnWriteArrayList<String> cow = new CopyOnWriteArrayList<>();
            cow.add("x"); cow.add("y"); cow.addIfAbsent("x"); cow.addIfAbsent("z");
            ck("cow.contents", new ArrayList<>(cow), Arrays.asList("x","y","z"));
            ck("cow.size", cow.size(), 3);

            // --- Atomics ---
            AtomicInteger ai = new AtomicInteger(10);
            ck("atomicInt.incrementAndGet", ai.incrementAndGet(), 11);
            ck("atomicInt.cas.success", ai.compareAndSet(11, 20), true);
            ck("atomicInt.cas.value", ai.get(), 20);
            ck("atomicInt.cas.fail", ai.compareAndSet(11, 99), false);
            AtomicLong al = new AtomicLong(100L);
            ck("atomicLong.addAndGet", al.addAndGet(23L), 123L);
            AtomicReference<String> ar = new AtomicReference<>("old");
            ck("atomicRef.cas", ar.compareAndSet("old", "new"), true);
            ck("atomicRef.value", ar.get(), "new");

            // --- ReentrantLock: lock/unlock/tryLock ---
            ReentrantLock rl = new ReentrantLock();
            rl.lock();
            try {
                ck("reentrantLock.isHeld", rl.isHeldByCurrentThread(), true);
                ck("reentrantLock.tryReenter", rl.tryLock(), true);
                rl.unlock();
                ck("reentrantLock.holdCount", rl.getHoldCount(), 1);
            } finally { rl.unlock(); }
            ck("reentrantLock.released", rl.isLocked(), false);

            // --- ReentrantReadWriteLock ---
            ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
            rwl.readLock().lock();
            rwl.readLock().lock();
            ck("rwLock.readHoldCount", rwl.getReadLockCount(), 2);
            rwl.readLock().unlock();
            rwl.readLock().unlock();
            rwl.writeLock().lock();
            ck("rwLock.writeHeld", rwl.isWriteLockedByCurrentThread(), true);
            rwl.writeLock().unlock();
            ck("rwLock.writeReleased", rwl.isWriteLocked(), false);

            // --- CompletableFuture: supplyAsync/thenApply/thenCompose/thenCombine/join ---
            CompletableFuture<Integer> cf = CompletableFuture
                .supplyAsync(() -> 5)
                .thenApply(x -> x + 1);
            ck("completableFuture.thenApply", cf.join(), 6);

            CompletableFuture<Integer> composed = CompletableFuture
                .supplyAsync(() -> 4)
                .thenCompose(x -> CompletableFuture.supplyAsync(() -> x * 10));
            ck("completableFuture.thenCompose", composed.join(), 40);

            CompletableFuture<Integer> combined = CompletableFuture.supplyAsync(() -> 3)
                .thenCombine(CompletableFuture.supplyAsync(() -> 7), (a,b) -> a + b);
            ck("completableFuture.thenCombine", combined.join(), 10);

            // --- LinkedBlockingQueue: put/take ---
            LinkedBlockingQueue<Integer> q = new LinkedBlockingQueue<>();
            q.put(11); q.put(22);
            ck("blockingQueue.takeFirst", q.take(), 11);
            ck("blockingQueue.takeSecond", q.take(), 22);
            ck("blockingQueue.empty", q.size(), 0);

            p2.shutdown();
            p2.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println("ConcurrencyTest: "+P+"/"+(P+F)+" passed");
        System.out.println("ConcurrencyTest RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
