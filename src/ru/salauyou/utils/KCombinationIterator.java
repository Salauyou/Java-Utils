package ru.salauyou.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <p>Iterator that generates all <a href="http://en.wikipedia.org/wiki/Combination">k-combinations</a> 
 * from a given {@code Collection<T>} of n entries. E. g. for source [foo, bar, baz] and k = 2 it will sequentially 
 * return [foo, bar], [foo, baz] and [bar, baz]. The total number of combinations is determined by binomial 
 * coefficient C(n, k).</p> 
 * <p>Results are returned as {@code Collection<T>}, implemented using {@code ArrayList<T>} by default. 
 * If another implementation (e. g. {@code TreeSet<T>}) is required, use 3-parameter constructor.</p>
 * <p>To iterate over k-combinations inside a for-each loop, use static {@code decorateForEach()} decorator.</p>
 */
public class KCombinationIterator<T> implements Iterator<Collection<T>> {

	final private Collection<T> resultCollection;
	final private List<T> source;
	final int k, n;
	final private int[] subset; // indexes of combination's elements in source list
	private boolean started = false;
	private boolean hasNext = false;
	
	/**
	 * Default constructor
	 * 
	 * @param source	collection, which data will be used to generate k-combinations. On creation, source data
	 * are copied into internal collection, so their further changes won't effect the iterator behavior
	 * @param k
	 * @throws IllegalArgumentException if {@code k < 1} or {@code k > source.size()}
	 */
	public KCombinationIterator(Collection<T> source, int k){
		this(source, k, new ArrayList<T>());
	}
	
	
	/**
	 * Constructor that determines special type of collection for results
	 * 
	 * @param source	collection, which data will be used to generate k-combinations. On creation, source data
	 * are copied into internal collection, so their further changes won't effect the iterator behavior
	 * @param k	
	 * @param resultCollection		object that will be used to return results invocation 
	 * @throws IllegalArgumentException if {@code k < 1} or {@code k > source.size()}
	 */
	public KCombinationIterator(Collection<T> source, int k, Collection<T> resultCollection) {
		if (k < 1)
			throw new IllegalArgumentException("k cannot be less than 1");
		if (k > source.size())
			throw new IllegalArgumentException("k cannot be greater than size of source collection");
		
		this.source = new ArrayList<T>(source);
		this.k = k;
		this.n = source.size();
		this.resultCollection = resultCollection;
		subset = new int[k];
	}
	

	@Override
	public boolean hasNext() {
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

	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<T> next() {
		if (!hasNext)
			throw new NoSuchElementException("No more subsets can be generated");
		Collection<T> result;
		try {
			result = resultCollection.getClass().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		for (int i = 0; i < k; i++)
			result.add(source.get(subset[i]));
		return result;
	}
	
	
	/**
	 * Not supported
	 * 
	 * @throws UnsupportedOperationException 
	 */
	@Override
	public void remove(){
		throw new UnsupportedOperationException("remove() is not supported");
	}
	
	//=======================================================================================================//
	
	/**
	 * <p>Decorator which allows to iterate over all k-combinations of given collection using for-each loop</p>
	 * <p>For example:</p>
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
	static public <T> Iterable<Collection<T>> decorateForEach(final Collection<T> source, final int k){
		return new Iterable<Collection<T>>(){
			@Override
			public Iterator<Collection<T>> iterator() {
				return new KCombinationIterator<T>(source, k);
			}
		};
	}
}
