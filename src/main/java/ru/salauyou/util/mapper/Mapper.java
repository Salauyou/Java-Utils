package ru.salauyou.util.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Throwing analogue of {@link java.util.function.Function} which 
 * may be useful if one needs to apply alternative mappings or return
 * default result if exception is thrown or <tt>null</tt> is returned
 * 
 * @author Salauyou
 *
 * @param <S> source type
 * @param <D> destination type
 */
@FunctionalInterface
public interface Mapper<S, D> {

    
    /**
     * @see java.util.function.Function#apply(Object)
     */
    D apply(S s) throws Exception;
    
    
    
    // ----------------- combiner/converter methods ------------------- //
    
    /**
     * Converts this mapper to a mapper which first applies this mapper 
     * to input, then applies <tt>then</tt> mapper to the result
     * 
     * @see java.util.function.Function#andThen(Function)
     */
    public default <R> Mapper<S, R> then(Mapper<? super D, ? extends R> then) {
        Objects.requireNonNull(then);
        return s -> then.apply(apply(s));
    }
    
    
    /**
     * Converts this mapper to a mapper which applies <tt>alt</tt> mapper 
     * if this mapper produces <tt>null</tt> or throws an exception
     */
    public default Mapper<S, D> or(Mapper<? super S, ? extends D> alt) {
        Objects.requireNonNull(alt);
        return s -> {
            try {
                D res = apply(s);
                return res == null ? alt.apply(s) : res;
            } catch (Exception e) {
                return alt.apply(s);
            }
        };
    }
    
    
    /**
     * Converts this mapper to a mapper which retuns a value provided
     * by given <tt>altSupplier</tt> if this mapper produces <tt>null</tt> 
     * or throws an exception
     */
    public default Mapper<S, D> or(Supplier<? extends D> altSupplier) {
        Objects.requireNonNull(altSupplier);
        return s -> {
            try { 
                D res = apply(s); 
                return res == null ? altSupplier.get() : res;
            } catch (Exception e) {
                return altSupplier.get();
            }
        };
    }
    
    
    /**
     * Converts this mapper to a mapper which retuns given <tt>defaulValue</tt>
     * if mapping produces <tt>null</tt> or throws an exception
     */
    public default Mapper<S, D> orDefault(D defaultValue) {
        return s -> {
            try { 
                D res = apply(s); 
                return res == null ? defaultValue : res;
            } catch (Exception e) { 
                return defaultValue; 
            }
        };
    }
    
    
    
    // ----------------- converter methods ------------------- //
    
    /**
     * Returns a mapper itself with adjusted type bounds
     */
    static public <S, D> Mapper<S, D> of(Mapper<? super S, ? extends D> mapper) {
        Objects.requireNonNull(mapper);
        return s -> mapper.apply(s);
    }
    
    
    /**
     * Converts mapper to function which silently returns <tt>null</tt> 
     * if <tt>apply()</tt> throws an exception
     */
    default Function<S, D> toFunction() {
        return s -> {
            try {
                return apply(s);
            } catch (Exception e) {
                return null;
            }
        };
    }
    
    
    
    // -------------------- static converters --------------------- //
    
    
    /**
     * Maps each item from source array using given <tt>itemMapper</tt> 
     * and collects produced items to <tt>ArrayList</tt>, ignoring items for 
     * which the mapper produces <tt>null</tt> or throws an exception
     */
    static public <I, D> List<D> mapEach(I[] source, 
                            Mapper<? super I, ? extends D> itemMapper) {
        return mapEach(source, itemMapper, ArrayList::new);
    }
    
    
    /**
     * Maps each item from source iterable using given <tt>itemMapper</tt> 
     * and collects produced items to <tt>ArrayList</tt>, ignoring items for 
     * which the mapper produces <tt>null</tt> or throws an exception
     */
    static public <I, D> List<D> mapEach(Iterable<? extends I> source, 
                            Mapper<? super I, ? extends D> itemMapper) {
        return mapEach(source, itemMapper, ArrayList::new);
    }
    
    
    /**
     * Maps each item from source array using given <tt>itemMapper</tt> 
     * and collects produced items to a collection, ignoring items for 
     * which mapper produces <tt>null</tt> or throws an exception
     */
    static public <I, D, C extends Collection<D>> C mapEach(
                            I[] source, 
                            Mapper<? super I, ? extends D> itemMapper,
                            Supplier<C> collectionFactory) {
            
        return Stream.of(source)
                  .map(itemMapper.toFunction())
                  .filter(i -> i != null)
                  .collect(Collectors.toCollection(collectionFactory));
    }

    
    /**
     * Maps each item from source iterable using given <tt>itemMapper</tt> 
     * and collects produced items to a collection, ignoring items for 
     * which mapper produces <tt>null</tt> or throws an exception
     */
    static public <I, D, C extends Collection<D>> C mapEach(
                            Iterable<? extends I> source, 
                            Mapper<? super I, ? extends D> itemMapper, 
                            Supplier<C> collectionFactory) {
        
        return StreamSupport.stream(source.spliterator(), false)
                  .map(itemMapper.toFunction())
                  .filter(i -> i != null)
                  .collect(Collectors.toCollection(collectionFactory));
    }
    
    
    /**
     * @see java.util.function.Function#identity()
     */
    static public <S> Mapper<S, S> identity() {
        return s -> s;
    }
    
}
