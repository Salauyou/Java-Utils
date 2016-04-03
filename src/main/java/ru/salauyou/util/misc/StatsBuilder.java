package ru.salauyou.util.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class StatsBuilder<V extends Comparable<? super V>> 
                    implements Collector<V, StatsBuilder<V>, StatsBuilder<V>> {

    final Queue<V> values = new ConcurrentLinkedQueue<>();
    final AtomicInteger items = new AtomicInteger();
    final int toSkip;
    
    
    public StatsBuilder() {
        this(0);
    }
    
    
    /**
     * Creates a new StatsBuilder that will ignore
     * N first items
     */
    public StatsBuilder(int skipFirst) {
        this.toSkip = skipFirst;
    }
    
    
    public StatsBuilder<V> put(V v) {
        int c = items.incrementAndGet();
        if (toSkip != 0 && c <= toSkip) 
            return this;
        values.add(v);
        return this;
    }
    
    
    public StatsBuilder<V> putAll(Iterable<? extends V> from) {
        for (V v : from)
            put(v);
        return this;
    }
    
    
    public StatsBuilder<V> putAll(StatsBuilder<? extends V> from) {
        for (V v : from.values)
            put(v);
        return this;
    }
    
    
    public List<? super V> getPercentiles(double[] percentiles) 
                                                   throws IllegalArgumentException {
        List<V> vs = new ArrayList<>(values);
        if (vs.size() == 0)
            return Collections.emptyList();
        Collections.sort(vs);
        List<V> result = new ArrayList<>(percentiles.length);
        for (int i = 0; i < percentiles.length; i++) {
            if (percentiles[i] < 0)
                throw new IllegalArgumentException("Percentile cannot be less than 0");
            int idx = (int) Math.round(vs.size() * percentiles[i] / 100d) - 1;
            idx = Math.min(idx, vs.size() - 1);
            idx = Math.max(idx, 0);
            result.add(vs.get(idx));
        } 
        return result;
    }
    
    
    public String percentilesToString(double[] percentiles) 
                                                   throws IllegalArgumentException {        
        StringBuilder sb = new StringBuilder();
        List<? super V> percentileValues = this.getPercentiles(percentiles);
        for (int i = 0; i < percentiles.length; i++) {
            sb.append(percentiles[i])
              .append("%\t")
              .append(percentileValues.get(i));
            if (i == percentiles.length - 1)
                return sb.toString();
            sb.append('\n');
        }
        return "";
    }
    
    
    /**
     * Number of counted items (skipped items not included)
     */
    public int getCount() {
        return values.size();
    }

    
    /**
     * Number of all items accepted by <tt>put()/putAll()</tt>, 
     * including skipped items
     */
    public int totalCount() {
        return items.get();
    }
    
    
    
    // ------------- Collector impl --------------- //
    
    @Override
    public Supplier<StatsBuilder<V>> supplier() {
        return StatsBuilder::new;
    }


    @Override
    public BiConsumer<StatsBuilder<V>, V> accumulator() {
        return StatsBuilder::put;
    }


    @Override
    public BinaryOperator<StatsBuilder<V>> combiner() {
        return StatsBuilder::putAll;
    }


    @Override
    public Function<StatsBuilder<V>, StatsBuilder<V>> finisher() {
        return Function.identity();
    }

    
    static final Set<Collector.Characteristics> CH
        = Collections.unmodifiableSet(EnumSet.of(
                        Collector.Characteristics.UNORDERED,
                        Collector.Characteristics.IDENTITY_FINISH,
                        Collector.Characteristics.CONCURRENT));

    @Override
    public Set<Collector.Characteristics> characteristics() {
        return CH;
    }
    
    
}