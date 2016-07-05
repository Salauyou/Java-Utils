package ru.salauyou.util.misc;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;



/**
 * @author Salauyou
 */
public class BeanHelper {

  /**
   * Creates a deep clone of given bean.
   * <p>
   * This method tries to create a new bean of the same type using default
   * no-arg constructor, then copy all properties accessible by public
   * {@code getXxx()} methods from a given source bean to a target bean using
   * corresponding {@code setXxx()} methods, or, in case of collections without
   * direct setter, using {@code getXxx().addAll()}.
   * <p>
   * Primitive types and wrappers, enum types, {@link java.lang.String} and
   * {@link java.time} immutable instances are copied directly.
   * {@link java.util.Date} instances and arrays are cloned.
   * {@link java.util.Collection} and {@link java.util.Map} types are cloned:
   * values from source properties are copied to a newly instantiated
   * collection/map using {@code add()/put()}. Other types are cloned
   * recursively as beans appliying the same rules for nested properties.
   */
  public static <E> E cloneOf(E bean) {
    return cloneInternally(bean, new IdentityHashMap<>());
  }

  /** Immutable types that are safe to copy by reference */
  final static Set<Class<?>> IMMUTABLES = new HashSet<>(Arrays.asList(
      Integer.class, Long.class, Double.class, Float.class, Byte.class,
      Character.class, Short.class, Boolean.class, BigDecimal.class,
      BigInteger.class, String.class, LocalDate.class, LocalTime.class,
      LocalDateTime.class, Instant.class, ZonedDateTime.class, Duration.class,
      MonthDay.class, OffsetDateTime.class, OffsetTime.class, Period.class,
      Year.class, YearMonth.class, ZoneOffset.class));

  final static Set<String> SKIPPED_PROPS = new HashSet<>(Arrays.asList("class"));

  /** Static cache where property descriptors are kept for classes */
  final static Map<Class<?>, PropertyDescriptor[]> PDS_CACHE = new ConcurrentHashMap<>();

  final static Set<Class<?>> CLONEABLES = new HashSet<>(Arrays.asList(
      Date.class, int[].class, long[].class, double[].class, float[].class, 
      byte[].class, char[].class, short[].class, boolean[].class));
  

  
  @SuppressWarnings("unchecked")
  static private <E> E cloneInternally(E source, Map<Object, Object> visited) {
    if (source == null)
      return null;
    Class<?> clazz = source.getClass();
    if (clazz.isEnum() || IMMUTABLES.contains(clazz))
      return source;
    if (clazz == Date.class) {
      return (E) ((Date) source).clone();
    }
    if (visited.containsKey(source)) {
      return (E) visited.get(source);
    }
    if (CLONEABLES.contains(clazz)) {
      try {
        E copy = (E) clazz.getMethod("clone").invoke(source);
        visited.put(source, copy);
        return copy;
      } catch (NoSuchMethodException nsm) {
        throw new AssertionError();
      } catch (ReflectiveOperationException e) {
        e.printStackTrace();
        return null;
      }
    }
    if (clazz.isArray()) {
      final Class<?> compCl = clazz.getComponentType();
      final Object[] srcArr = (Object[]) source;
      int size = srcArr.length;
      final Object[] copy = (Object[]) Array.newInstance(compCl, size);
      visited.put(source, copy);
      for (int i = 0; i < size; i++) {
        copy[i] = cloneInternally(srcArr[i], visited);
      }
    }
    try {
      final E copy = (E) clazz.newInstance();
      visited.put(source, copy);
      PropertyDescriptor[] pds = PDS_CACHE.get(clazz);
      if (pds == null) {
        pds = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
        PDS_CACHE.put(clazz, pds);
      }
      for (PropertyDescriptor d : pds) {
        try {
          if (SKIPPED_PROPS.contains(d.getName()))
            continue;
          Object o = d.getReadMethod().invoke(source);
          if (o == null) {
            continue;
          }
          Object c = null;
          if (o instanceof Collection) {
            final Collection<Object> k 
                = (Collection<Object>) o.getClass().newInstance();
            ((Collection<?>) o).forEach(e -> k.add(cloneInternally(e, visited)));
            c = k;
          } else if (o instanceof Map) {
            final Map<Object, Object> m 
                = (Map<Object, Object>) o.getClass().newInstance();
            ((Map<?, ?>) o).forEach((k, v) 
                -> m.put(cloneInternally(k, visited), cloneInternally(v, visited)));
            c = m;
          } else {
            c = cloneInternally(o, visited);
          }
          if (c != null) {
            if (d.getWriteMethod() != null) {
              d.getWriteMethod().invoke(copy, c);
            } else if (Collection.class.isAssignableFrom(d.getPropertyType())) {
              Collection<Object> coll 
                  = (Collection<Object>) d.getReadMethod().invoke(copy);
              coll.addAll((Collection<?>) c);
            }
          }
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
