package ru.salauyou.util.misc;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;


/**
 * @author Salauyou
 */
public class BeanHelper {

    /**
     * Returns a deep clone of given bean.
     * <p>
     * Method tries to create a new bean of the same type using
     * default no-arg constructor, then copy all properties accessible by
     * public <tt>getXxx()</tt> methods from a given source bean to a target bean 
     * using corresponding <tt>setXxx()</tt> methods. Primitive types and wrappers,
     * enum types, <tt>java.lang.String</tt> and <tt>java.util.time.*</tt> 
     * immutable types are copied directly. <tt>java.util.Date</tt> is cloned.
     * <tt>Collection</tt> and <tt>Map</tt> types are cloned: values from 
     * source properties are copied to a newly instantiated collection/map 
     * using <tt>add()/put()</tt>. Other types are cloned as beans using default 
     * constructor and <tt>getXxx()/setXxx()</tt>. The same rules are applied 
     * to nested properties.
     */
    public static <E> E cloneOf(E bean) {
        return cloneInternally(bean, new IdentityHashMap<>());
    }
    
    
    final static Set<Class<?>> SCALARS 
        = new HashSet<>(Arrays.asList(Integer.class, Long.class, Double.class, Float.class,
                                      Byte.class, Character.class, Short.class, Boolean.class,
                                      BigDecimal.class, BigInteger.class, String.class,  
                                      LocalDate.class, LocalTime.class, LocalDateTime.class,
                                      Instant.class, ZonedDateTime.class));

    final static Set<String> SKIPPED_PROPS = new HashSet<>(Arrays.asList("class"));


    @SuppressWarnings("unchecked")
    static private <E> E cloneInternally(E source, Map<Object, Object> visited) {
        if (source == null)
            return null;
        Class<?> clazz = source.getClass();
        if (clazz == String.class)
            return (E) ((String) source).intern();
        if (clazz.isEnum() || SCALARS.contains(clazz))
            return source;
        if (clazz == Date.class)
            return (E) new Date(((Date) source).getTime());
        if (visited.containsKey(source))
            return (E) visited.get(source);
        try {
            E copy = (E) clazz.newInstance();
            visited.put(source, copy);
            PropertyDescriptor[] pds = Introspector.getBeanInfo(source.getClass())
                                                   .getPropertyDescriptors(); 
            for (PropertyDescriptor d : pds) {
                try {
                    if (SKIPPED_PROPS.contains(d.getName()))
                        continue;
                    Object o = d.getReadMethod().invoke(source);
                    if (o == null)
                        continue;
                    Object c = null;
                    if (o instanceof Collection) {
                        final Collection<Object> k = (Collection<Object>) o.getClass().newInstance();
                        ((Collection<?>) o).forEach(e -> k.add(cloneInternally(e, visited)));
                        c = k;
                    } else if (o instanceof Map) {
                        final Map<Object, Object> m = (Map<Object, Object>) o.getClass().newInstance();
                        ((Map<?,?>) o).forEach((k, v) 
                                -> m.put(cloneInternally(k, visited), cloneInternally(v, visited)));
                        c = m;
                    } else {
                        c = cloneInternally(o, visited);
                    }
                    if (c != null)
                        d.getWriteMethod().invoke(copy, c);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return copy;

        } catch (ReflectiveOperationException | IntrospectionException e) {
            e.printStackTrace();
            return null;
        }
    }

    
    private BeanHelper() {};
    
}
