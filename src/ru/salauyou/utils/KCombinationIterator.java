package ru.salauyou.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <p>Iterator that generates all <a href="http://en.wikipedia.org/wiki/Combination">k-combinations</a> 
 * from a given set (or list) of n entries. E. g. for source {A, B, C} and k = 2 it will sequentially return {A, B}, {A, C} and {B, C}.
 * The total number of combinations is determined by binomial coefficient C(n, k).</p> 
 * <p>Source set is presented as {@code Collection<T>}, result set is presented as {@code Collection<T>}.
 * By default, result object is implemented using {@code ArrayList<T>} and is reused on every {@code next()} invocation. 
 * If you need another implementation (e. g. {@code TreeSet<T>}) to present results, use 4-parameter constructor.
 */
public class KCombinationIterator<T> implements Iterator<Collection<T>> {

	final private Collection<T> resultCollection;
	final private List<T> source;
	final int k, n;
	final private int[] subset; // indexes of combination's elements in source list
	private boolean started = false;
	private boolean hasNext = false;
	final private boolean createNewInstances;
	
	/**
	 * Default constructor
	 * 
	 * @param source	collection, which data will be used to generate k-combinations. On creation, source data
	 * are copied into internal collection, so their further changes won't effect the iterator behavior
	 * @param k
	 * @throws IllegalArgumentException if {@code k < 1} or {@code k > source.size()}
	 */
	public KCombinationIterator(Collection<T> source, int k){
		this(source, k, new ArrayList<T>(), false);
	}
	
	
	/**
	 * Constructor that determines special type of collection for results
	 * 
	 * @param source	collection, which data will be used to generate k-combinations. On creation, source data
	 * are copied into internal collection, so their further changes won't effect the iterator behavior
	 * @param k	
	 * @param resultCollection		object that will be used to return results invocation 
	 * @param createNewInstances	if {@code true}, a new instance of result object will be created on every {@code next()} invocation.
	 * Otherwise, one object that was passed as {@code resultCollection} parameter will be reused.
	 * @throws IllegalArgumentException if {@code k < 1} or {@code k > source.size()}
	 */
	public KCombinationIterator(Collection<T> source,  int k, Collection<T> resultCollection, boolean createNewInstances) {
		if (k < 1)
			throw new IllegalArgumentException("k cannot be less than 1");
		if (k > source.size())
			throw new IllegalArgumentException("k cannot be greater than size of source collection");
		
		this.source = new ArrayList<T>(source);
		this.k = k;
		this.n = source.size();
		this.resultCollection = resultCollection;
		this.createNewInstances = createNewInstances;
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
		if (createNewInstances){
			try {
				result = resultCollection.getClass().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			result = resultCollection;
			result.clear();
		}
		for (int i = 0; i < k; i++)
			result.add(source.get(subset[i]));
		return result;
	}
	
	
	/**
	 * {@code remove()} operation is not supported - since there are no "undelying" collection to remove entries from
	 * 
	 * @throws UnsupportedOperationException 
	 */
	@Override
	public void remove(){
		throw new UnsupportedOperationException("remove() is not supported");
	}
}
