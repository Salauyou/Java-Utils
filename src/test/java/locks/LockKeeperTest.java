package locks;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import org.junit.Test;

import locks.Model.Bank;
import locks.Model.Payment;
import locks.Model.Subject;
import ru.salauyou.util.concurrent.LockKeeper;
import ru.salauyou.util.misc.StatsBuilder;


public class LockKeeperTest {

    
    static final int PAYERS = 1000;
    static final int RECEIVERS = 2000;
    static final int BANKS = 50;
    static final int PAYMENTS = 5000;
    static final int THREADS = 50;
    
    static final double[] timeLimits = new double[]{6, 12, 15, 20, 25};
    
    @Test(timeout = 80000)
    public void testLocks() {
        
        LockKeeper lockKeeper = new LockKeeper(10, Bank.class, Subject.class, Payment.class);
        
        Random rnd = new Random();
        StatsBuilder<Integer> sb = new StatsBuilder<>();
        
        List<Bank> banks = new ArrayList<>(BANKS);
        List<Subject> payers = new ArrayList<>(PAYERS);
        List<Subject> receivers = new ArrayList<>(RECEIVERS);
        List<Payment> payments = new ArrayList<>(PAYMENTS);
        
        for (int i = 0; i < BANKS; i++) 
            banks.add(generateBank(rnd));
        for (int i = 0; i < PAYERS; i++) 
            payers.add(generateSubject(rnd, banks));
        for (int i = 0; i < RECEIVERS; i++) 
            receivers.add(generateSubject(rnd, banks));
        for (int i = 0; i < PAYMENTS; i++) 
            payments.add(generatePayment(rnd, payers, receivers));
        
        List<Future<Void>> tasks = new ArrayList<>();
        ExecutorService es = Executors.newFixedThreadPool(THREADS);
            
        for (int i = 0; i < 5; i++) {
            final int ii = i;
            payments.forEach(p -> {
                tasks.add(es.submit(() -> {
                    long timeStart = System.nanoTime();
                    Lock lock = lockKeeper.lockAndGet(
                            rnd.nextDouble() < (ii * 0.2d) ? LockKeeper.LockType.WRITE : LockKeeper.LockType.READ,
                            p, 
                            p.getPayer(), 
                            p.getReceiver(), 
                            p.getPayer().getBank(), 
                            p.getReceiver().getBank()
                            );
                    sb.put((int) ((System.nanoTime() - timeStart) / 1000000));
                    if (rnd.nextDouble() < 0.001) {
                        System.out.println("Locks acquired for: " + p);
                    }
                    Thread.sleep(50);
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
            
            double time = (System.currentTimeMillis() - timeStart) / 1000d;
            System.out.println(String.format("Executed in %s s.", time));
            System.out.println(sb.percentilesToString(new double[] { 0, 25, 50, 75, 85, 90, 95, 99, 100 }));
            assertTrue(time < timeLimits[i]);
        }
        es.shutdownNow();
    }
    
    
    
    //========================================================================================= //
    
    
    static Subject generateSubject(Random rnd, List<Bank> banks) {
        return new Subject(Helper.generateString(rnd, 2) + "-" + Helper.generateStringNumeric(rnd, 5))
                .setBank(banks.get(rnd.nextInt(banks.size())))
                .setName(Helper.generateName(rnd, 2));
    }
    
    
    static Bank generateBank(Random rnd) {
        return new Bank((long) rnd.nextInt(1000000000))
                .setName(Helper.generateName(rnd, 1) + " Bank");
    
    }
    
    
    static Payment generatePayment(Random rnd, List<Subject> payers, List<Subject> receivers) {
        return new Payment(rnd.nextLong()).setPayer(payers.get(rnd.nextInt(payers.size())))
                .setReceiver(receivers.get(rnd.nextInt(receivers.size())))
                .setTimeStamp(Instant.now())
                .setAmount(BigDecimal.valueOf(rnd.nextInt(10000)).scaleByPowerOfTen(-2));
    }

}
