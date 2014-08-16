package ru.salauyou.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <p>Iterator that generates k-length subsets (subcollections) of given collection.
 * E. g. for source {A, B, C} and k = 2 it will sequentially return {A, B}, {A, C} and {B, C}.
 * The total number of such subsets is determined by binomial coefficient C(n, k).</p> 
 * <p>By default, resulting collection is {@code ArrayList<T>}, so order of elements in resulting
 * collections is kept as in the source collection. In special cases, when you need another implementation 
 * (e. g. {@code TreeSet<T>}) to present results, pass an empty object of needed type to constructor. 
 * This object will be cleaned and filled on every {@code next()} invocation.</p>
 */
public class KSubsetIterator<T> implements Iterator<Collection<T>> {

	final private Collection<T> resultCollection;
	final private List<T> source;
	final int k, n;
	private int[] subset; // index subset based on which next() subset will be generated
	private boolean started = false;
	private boolean hasNext = false;
	
	
	/**
	 * Default constructor
	 * 
	 * @param source collection, which data will be used to generate subsets. On creation, source data
	 * are copied into internal collection, so their further changes won't effect the iterator behavior
	 * @param k
	 * @throws IllegalArgumentException if {@code k < 1} or {@code k > source.size()}
	 */
	public KSubsetIterator(Collection<T> source, int k){
		this(source, null, k);
	}
	
	
	/**
	 * Constructor that determines special type of collection for results
	 * 
	 * @param source	collection, which data will be used to generate subsets. On creation, source data
	 * are copied into internal collection, so their further changes won't effect the iterator behavior
	 * @param resultCollection	object which will be used to return results on every {@code next()} invocation 
	 * @param k	
	 * @throws IllegalArgumentException if {@code k < 1} or {@code k > source.size()}
	 */
	public KSubsetIterator(Collection<T> source, Collection<T> resultCollection, int k) {
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

	
	@Override
	public Collection<T> next() {
		if (!hasNext)
			throw new NoSuchElementException("No more subsets can be generated");
		Collection<T> result;
		if (resultCollection != null){
			resultCollection.clear();
			result = resultCollection;
		} else
			result = new ArrayList<T>(k);
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
