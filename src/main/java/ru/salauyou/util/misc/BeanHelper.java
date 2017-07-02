package ru.salauyou.util.misc;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
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
   * This method tries to create a new bean of the same runtime class (or, if
   * runtime class is detected as proxy or has no accessible no-arg constructor,
   * is used the closest non-proxy superclass with accessible no-arg
   * constructor), then take each property by accessible {@code getXxx()}
   * getter, clone it if necessary, and set to a target bean using corresponding
   * {@code setXxx()} setter, or, if a property is of collection type and does
   * not have direct setter, using {@code getXxx().addAll()}.
   * <p>
   * Primitive types and wrappers, enum types, {@link java.lang.String} and
   * {@link java.time} immutable instances are copied directly.
   * {@link java.util.Date} instances are cloned, arrays are cloned,
   * {@link java.util.Collection} and {@link java.util.Map} types are cloned by
   * instantiating a collection/map of the same type via no-arg constructor (or,
   * if such constructor is not available, it is looked-up among superclasses;
   * finally one of empty {@link java.util.ArrayList}, {@link java.util.HashSet}
   * or {@link java.util.HashMap} is created) and filling it using
   * {@code add()/put()}. Other types are cloned recursively as beans appliying
   * the same rules to their properties.
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
      Year.class, YearMonth.class, ZoneOffset.class,
      Collections.emptyList().getClass(), Collections.emptySet().getClass(),
      Collections.emptyMap().getClass()));

  final static Set<String> SKIPPED_PROPS = new HashSet<>(Arrays.asList("class"));

  
  /** Static cache where property descriptors are kept for classes */
  final static Map<Class<?>, PropertyDescriptor[]> PDS_CACHE = new ConcurrentHashMap<>();

  
  @SuppressWarnings("unchecked")
  static private <E> E cloneInternally(E source, Map<Object, Object> visited) {
    if (source == null) {
      return null;
    }
    Class<?> clazz = source.getClass();
    if (clazz.isEnum() || IMMUTABLES.contains(clazz)) {
      return source;
    } 
    if (clazz == Date.class) {
      return (E) ((Date) source).clone();
    } 
    if (visited.containsKey(source)) {
      return (E) visited.get(source);
    }
    if (clazz.isArray()) {
      final int size = Array.getLength(source);
      Object copy = Array.newInstance(clazz.getComponentType(), size);
      visited.put(source, copy);
      for (int i = 0; i < size; i++) {
        Object o = cloneInternally(Array.get(source, i), visited);
        Array.set(copy, i, o);
      }
      return (E) copy;
    } 
    try {
      Object copy = tryInstantiate(clazz);
      clazz = copy.getClass();
      visited.put(source, copy);
      if (Collection.class.isAssignableFrom(clazz)) {
        Collection<Object> c = (Collection<Object>) copy;
        ((Collection<?>) source).forEach(e 
            -> c.add(cloneInternally(e, visited)));
      } else if (Map.class.isAssignableFrom(clazz)) {
        Map<Object, Object> m = (Map<Object, Object>) copy;
        ((Map<?, ?>) source).forEach((k, v) 
            -> m.put(cloneInternally(k, visited), cloneInternally(v, visited)));
      } else {
        PropertyDescriptor[] pds = PDS_CACHE.get(clazz);
        if (pds == null) {
          pds = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
          PDS_CACHE.put(clazz, pds);
        }
        for (PropertyDescriptor d : pds) {
          try {
            if (SKIPPED_PROPS.contains(d.getName())) {
              continue;
            }
            Object c;
            Object o = d.getReadMethod().invoke(source);
            if (o == null || (c = cloneInternally(o, visited)) == null) {
              continue;
            }
            if (d.getWriteMethod() != null) {
              d.getWriteMethod().invoke(copy, c);
            } else if (Collection.class.isAssignableFrom(d.getPropertyType())) {
              Collection<Object> coll 
                  = (Collection<Object>) d.getReadMethod().invoke(copy);
              coll.addAll((Collection<?>) c);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      return (E) copy;
    } catch (ReflectiveOperationException | IntrospectionException e) {
      e.printStackTrace();
      return null;
    }
  }

  
  static Object tryInstantiate(final Class<?> clazz) throws InstantiationException {
    Class<?> cl = clazz;
    while (cl != Object.class) {
      if (!isProxy(cl)) {
        try {
          return cl.newInstance();
        } catch (ReflectiveOperationException goNext) {}
      }
      cl = cl.getSuperclass();
    }
    if (Set.class.isAssignableFrom(clazz)) {
      return new HashSet<>();
    } else if (Map.class.isAssignableFrom(clazz)) {
      return new HashMap<>();
    } else if (Collection.class.isAssignableFrom(clazz)) {
      return new ArrayList<>();
    }
    throw new InstantiationException(String.format(
        "Cannot find no-arg constructor in class %s or any its superclasses", 
        clazz.getCanonicalName()));
  }
  
  
  static final List<Method> PROXY_TESTERS = new ArrayList<>();
  static Class<?> SPRING_PROXY = null;
  
  static {
    for (String method : Arrays.asList(
          "org.springframework.util.ClassUtils#isCglibProxyClass",
          "java.lang.reflect.Proxy#isProxyClass",
          "javassist.util.proxy.ProxyFactory#isProxyClass",
          "net.sf.cglib.proxy.Proxy#isProxyClass")) {
      String[] ms = method.split("#");
      try {
        PROXY_TESTERS.add(Class.forName(ms[0]).getMethod(ms[1], Class.class));
      } catch (ReflectiveOperationException | SecurityException ignored) {}
      try {
        SPRING_PROXY = Class.forName("org.springframework.aop.SpringProxy");
      } catch (ReflectiveOperationException | SecurityException ignored) {}
    }
  }
  
  
  static final Set<Class<?>> NOT_PROXY 
      = Collections.newSetFromMap(new ConcurrentHashMap<>());
  
  
  static boolean isProxy(Class<?> clazz) {
    if (NOT_PROXY.contains(clazz)) {
      return false;
    }
    for (Method m : PROXY_TESTERS) {
      try {
        if ((boolean) m.invoke(null, clazz)) {
          return true;
        }
      } catch (Exception e) {}
    }
    if (SPRING_PROXY != null && SPRING_PROXY.isAssignableFrom(clazz)) {
      return true;
    }
    NOT_PROXY.add(clazz);
    return false;
  }

  
  
  /**
   * Resolves actual type arguments that {@code target} 
   * type and its supertypes define when they implement/extend
   * {@code source} generic type. 
   * <p>
   * Due to Java reflection restrictions, this method cannot 
   * resolve types for classes that directly extend generic 
   * class, e.g. {@code new ArrayList<String>().getClass()}
   * 
   * @param target target class
   * @param source generic class or interface
   * 
   * @return resolved arguments in order as defined in 
   *     {@code source} generic class. Unresolved arguments 
   *     are returned as nulls.
   */
  static List<Class<?>> resolveTypeArguments(
      Class<?> target, Class<?> source) {
   
    if (!source.isAssignableFrom(target)) {  
      throw new IllegalArgumentException(
          "Source is not supertype of target");
    }
    
    // source is not a generic type
    if (source.getTypeParameters().length == 0) {
      return Collections.emptyList();
    }
    
    // Build inheritance chain from `source` to 
    // `target` -- in order to handle type parameters 
    // defined in `source` subtypes
    Type type = target;
    List<ParameterizedType> types = new ArrayList<>();
    next: while (type != null) {
      if (type instanceof ParameterizedType) {
        ParameterizedType t = (ParameterizedType) type;
        types.add(t);
      }
      Class<?> cl = adjustClass(type);
      if (cl == source) {
        break;
      }
      for (Type t : cl.getGenericInterfaces()) {
        if (source.isAssignableFrom(adjustClass(t))) {
          type = t;
          continue next;
        }
      } 
      type = cl.getGenericSuperclass();
    }
    Collections.reverse(types);
    
    // Perform argument resolution
    Type[] resolved = null;
    
    // index tracking current positions 
    // of type variables in result array
    Map<String, Integer> varIndex = new HashMap<>();
    
    for (Type t : types) {
      Type[] args = ((ParameterizedType) t).getActualTypeArguments();
      
      // initialize result array and index
      if (resolved == null) {
        resolved = args;
        for (int i = 0; i < args.length; ++i) {
          if (args[i] instanceof TypeVariable) {
            varIndex.put(((TypeVariable<?>) args[i]).getName(), i);
          }
        }
        continue;
      }
      
      // process next subtype
      TypeVariable<?>[] vars = adjustClass(t).getTypeParameters();
      for (int i = 0; i < args.length; ++i) {
        t = args[i];
        Integer pos = varIndex.remove(vars[i].getName());
        
        // variable declared in `source`?
        if (pos != null) {          
          resolved[pos] = t;
          if (t instanceof TypeVariable) {
            varIndex.put(((TypeVariable<?>) t).getName(), pos);
          }
        }
      }
    }
    
    // Prepare result
    if (resolved == null) {
      return Collections.nCopies(
          source.getTypeParameters().length, null);
    }
    List<Class<?>> result = new ArrayList<>();
    for (Type t : resolved) {
      result.add(t instanceof Class ? (Class<?>) t : null);
    }
    return result;
  }
  
  
  static Class<?> adjustClass(Type t) {
    if (t instanceof ParameterizedType) {
      t = ((ParameterizedType) t).getRawType();
    } 
    return (Class<?>) t;
  }
  
  
  private BeanHelper() {};

}
