package ru.salauyou.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import helpers.Helper;
import helpers.Model.Bank;
import helpers.Model.Payment;
import helpers.Model.Subj;
import helpers.Model.Subject;
import ru.salauyou.util.misc.StatsBuilder;



public class TestLockKeeperV2 {

    
    static final int PAYERS    = 1000;
    static final int RECEIVERS = 2000;
    static final int BANKS     = 50;
    static final int PAYMENTS  = 500000;
    static final int THREADS   = 10;
    
 
    
    @Test 
    public void testLocks() {
        
        LockKeeperV2 lockKeeper = new LockKeeperV2(8, Bank.class, Subj.class, Payment.class);
        
        Random rnd = new Random();
        
        List<Bank> banks = Stream.generate(() -> Helper.generateBank(rnd)).limit(BANKS)
                .collect(Collectors.toList());        
        List<Subject> payers = Stream.generate(() -> Helper.generateSubject(rnd, banks)).limit(PAYERS)
                .collect(Collectors.toList());        
        List<Subject> receivers = Stream.generate(() -> Helper.generateSubject(rnd, banks)).limit(RECEIVERS)
                .collect(Collectors.toList());        
        List<Payment> payments = Stream.generate(() -> Helper.generatePayment(rnd, payers, receivers)).limit(PAYMENTS)
                .collect(Collectors.toList());

        List<Future<Void>> tasks = new ArrayList<>();
        ExecutorService es = Executors.newFixedThreadPool(THREADS);
        
        for (int i = 0; i < 10; i++) {
            StatsBuilder<Integer> sb = new StatsBuilder<>();
            payments.forEach(p -> {
                tasks.add(es.submit(() -> {
                    long timeStart = System.nanoTime();
                    Lock lock = lockKeeper.lockAndGet(
                            LockKeeperV2.LockType.WRITE,
                            /*rnd.nextDouble() < (ii * 0.2d) 
                                    ? LockKeeper.LockType.WRITE 
                                    : LockKeeper.LockType.READ,*/
                            p, 
                            p.getPayer(), 
                            p.getReceiver(), 
                            p.getPayer().getBank(), 
                            p.getReceiver().getBank()
                            );
                    sb.put((int) ((System.nanoTime() - timeStart) / 1E6));
                    if (rnd.nextDouble() < 0.00001) 
                        System.out.println("Locks acquired for: " + p);
//                    Thread.sleep(50);
                    lock.unlock();
                    return null;
                }));
            });
            
            long timeStart = System.currentTimeMillis();
            for (Future<Void> task : tasks) {
                try {
                    task.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            tasks.clear();
            
            double time = (System.currentTimeMillis() - timeStart) / 1000d;
            System.out.println(String.format("Executed in %s s.", time));
            System.out.println(sb.percentilesToString(new double[] { 0, 25, 50, 75, 85, 90, 95, 99, 100 }));
        }
        es.shutdownNow();
    }


}
