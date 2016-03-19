package ru.salauyou.util.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * <p>Iterator that sequentially returns all k-subsets, or
 * <a href="http://en.wikipedia.org/wiki/Combination">k-combinations</a> 
 * from a given collection of N entries. 
 * E. g. for source [foo, bar, baz] and k = 2 it will sequentially 
 * return [foo, bar], [foo, baz] and [bar, baz]. The total number 
 * of combinations is determined by binomial coefficient C(n, k). 
 * <p>
 * Combinations are returned stored in {@link ArrayList} by default. 
 * If another implementation is required, use three-arg constructor.
 * <p>
 * To iterate over k-combinations inside a for-each loop, 
 * use {@link KCombinationIterator#decorateForEach(Collection, int)}
 * decorator.
 */
public class KCombinationIterator<T> implements Iterator<Collection<T>> {

    final private Supplier<? extends Collection<T>> resultColSuppl;
    
    final private List<T> source;
    final int k, n;
    final private int[] subset; // indexes of combination's elements in source list
    private boolean started = false;
    private boolean hasNext = false;
    private boolean hasNextCalled = false;
    
    
    /**
     * Default constructor
     * 
     * @param source collection used as a base of subsets (k-combinations) 
     * @throws IllegalArgumentException if {@code k < 1} 
     *         or {@code k > source.size()}
     */
    public KCombinationIterator(Collection<T> source, int k) 
                                               throws IllegalArgumentException {
        this(source, k, ArrayList::new);
    }
    
    
    /**
     * Constructor where additionally is determined type of colection, 
     * where k-combinations would be stored for each generated k-combination
     * 
     * @deprecated use {@link Supplier}-based constructor instead
     * 
     * @param source source collection used as a base of subsets (k-combinations) 
     * @throws IllegalArgumentException if {@code k < 1} 
     *         or {@code k > source.size()}
     */
    @SuppressWarnings({ "unchecked" })
    public KCombinationIterator(Collection<T> source, int k, Collection<T> resultCollection) 
                                                    throws IllegalArgumentException {   
        this(source, k, () -> {
            try {
                return resultCollection.getClass().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    
    /**
     * Constructor where additionally is determined supplier used to
     * generate an empty collection to store each generated k-combination
     * 
     * @param source source collection used as a base of subsets (k-combinations) 
     * @throws IllegalArgumentException if {@code k < 1} or {@code k > source.size()}
     */
    public KCombinationIterator(Collection<T> source, int k, 
                                Supplier<? extends Collection<T>> resultCollection) 
                                                    throws IllegalArgumentException {
        if (k < 1 || k > source.size())
            throw new IllegalArgumentException("k must be between 1 and source.size");
        this.source = new ArrayList<T>(source);
        this.k = k;
        this.n = source.size();
        this.resultColSuppl = resultCollection;
        this.subset = new int[k];
    }

    
    @Override
    public boolean hasNext() {
        // check if hasNext was called before to follow idempotency
        if (hasNextCalled)
            return hasNext;
        
        hasNextCalled = true;
        if (n == 0)
            return false;
        if (!started){
            // if not started, generate first subset
            for (int i = 0; (subset[i] = i) < k - 1; i++);
            started = true;
            hasNext = true;
            return true;
        } else {
            // check if more subsets can be generated
            int i;
            for (i = k - 1; i >= 0 && subset[i] == n - k + i; i--);
            if (i < 0){
                hasNext = false;
                return false;
            } else {
                subset[i]++;
                for (++i; i < k; i++)
                    subset[i] = subset[i - 1] + 1;
                hasNext = true;
                return true;
            }
        }
    }

    
    @Override
    public Collection<T> next() {
        hasNextCalled = false;
        if (!hasNext)
            throw new NoSuchElementException();
        Collection<T> result = resultColSuppl.get();
        for (int i = 0; i < k; i++)
            result.add(source.get(subset[i]));
        return result;
    }
    
    
    @Override
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
    
    
    
    //=======================================================================================================//
    
    /**
     * <p>Decorator which allows to iterate over all k-combinations in for-each fashion
     * <pre>
     * Collection&lt;String&gt; source = Arrays.asList(new String[]{"foo", "bar", "baz"});
     * for (Collection&lt;String&gt; c : KCombinationIterator.decorateForEach(source, 2))
     *     System.out.println(c);</pre>
     * <p>will produce:</p>
     * <pre>
     * [foo, bar]
     * [foo, baz]
     * [bar, baz]</pre>
     * 
     * @param source
     * @param k
     * @return
     */
    static public <T> Iterable<Collection<T>> 
                          decorateForEach(final Collection<T> source, final int k){
        return () -> new KCombinationIterator<T>(source, k);
    }
}
