package ru.salauyou.util.mapper;

import static java.util.Collections.singleton;
import static ru.salauyou.util.misc.ExceptionHelper.buildExceptionMessage;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author Salauyou
 */
final class BeanMapper<S, D> implements Mapper<S, D> {

  final static Log log = LogFactory.getLog(BeanMapper.class);

  /** Mapper to discover instance type */
  @SuppressWarnings("rawtypes")
  Mapper<? super S, Class> typeMapper;

  /** Root type (common superclass) for this mapper */
  final Class<? extends D> rootType;

  /** Property mappers for concrete types */
  final Map<Class<?>, List<PropMapper>> propMappers = new IdentityHashMap<>();

  /** Callbacks to be invoked after mapping */
  // TODO: change to Map<Class<?>, List<Method>> to separate by types
  final List<Method> postMethods = new ArrayList<>();

  /** Reference to EntityMapper who holds this mapper 
   * (to call @PostMapping methods on it) */
  final EntityMapper<?, ?> entityMapper;


  @Override
  @SuppressWarnings("unchecked")
  public D apply(S s) throws Exception {
    D res = null;
    // construct destination object
    try {
      res = typeMapper == null
          ? rootType.newInstance()
          : (D) typeMapper.apply(s).newInstance();
    } catch (RuntimeException | ReflectiveOperationException e) {
      log.warn("Failed to create a bean instance");
      log.warn(buildExceptionMessage(e));
      return null;
    }
    Class<?> cl = res.getClass();
    boolean changed = false;
    // fill up properties
    for (Entry<Class<?>, List<PropMapper>> pms : propMappers.entrySet()) {
      if (!pms.getKey().isAssignableFrom(cl)) {
        continue;
      }
      for (PropMapper pm : pms.getValue()) {
        String prop = pm.prop;
        try {
          Object p = pm.mapper.apply(s);
          if (p != null) {
            pm.setter.invoke(res, p);
            changed = true;
          } else {
            reportNullMapping(cl, prop, pm.source);
          }
        } catch (NullPointerException e) {
          reportNullMapping(cl, prop, pm.source);
        } catch (NoSuchMethodError | Exception e) {
          log.warn(String.format("%s.%s mapping failed: %s",
              cl.getSimpleName(), prop, buildExceptionMessage(e)));
          log.warn("→  " + pm.source);
        }
      }
    }
    if (!changed) {
      return null;
    }
    for (Method m : postMethods) {
      changed = true;
      try {
        m.setAccessible(true);
        m.invoke(entityMapper, res);
      } catch (Exception e) {
        log.warn("Exception when calling @PostMapping method " + m.getName());
        log.warn("→  " + buildExceptionMessage(e));
      }
    }
    return res;
  }


  static void reportNullMapping(Class<?> cl, String prop, String source) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("%s.%s wasn't set: null", cl.getSimpleName(), prop));
      log.debug("→  " + source);
    }
  }


  BeanMapper(Class<? extends D> defaultType, EntityMapper<?, ?> entityMapper) {
    this.entityMapper = entityMapper;
    this.rootType = defaultType;
  }


  @SuppressWarnings("unchecked")
  void addMapperForProperty(String prop, Mapper<? super S, ?> mapper,
            Map<String, ? extends Collection<Class<?>>> typeFilters, String source) 
                throws NoSuchFieldException {
    validateProperty(prop);
    String first = splitProperty(prop)[0];
    String rest = splitProperty(prop)[1];
    Collection<Class<?>> types = typeFilters.get(""); 
    if (types == null) {
      types = singleton(rootType);
    }
    for (Class<?> type : types) {
      List<PropMapper> pms = propMappers.computeIfAbsent(type, c -> new ArrayList<>());
      if (rest == null) {  // simple property
        PropertyDescriptor pd = getPropDescriptor(type, first);
        pms.add(new PropMapper(first, mapper, source, pd.getWriteMethod()));
      } else {  // nested property
        PropMapper pm = findOrCreateMapper(type, first);
        ((BeanMapper<S, ?>) pm.mapper)
            .addMapperForProperty(rest, mapper, shiftTypeMap(first, typeFilters), source);
      }
    }
  }


  @SuppressWarnings({ "rawtypes", "unchecked" })
  void addTypeMapper(String prop, Mapper<? super S, Class> typeMapper,
              Map<String, ? extends Collection<Class<?>>> typeFilters) 
                  throws NoSuchFieldException {
    if (prop.isEmpty()) {
      this.typeMapper = typeMapper;
      return;
    }
    validateProperty(prop);
    String first = splitProperty(prop)[0];
    String rest = splitProperty(prop)[1];
    Collection<Class<?>> types = typeFilters.get("");
    if (types == null) {
      types = singleton(rootType);
    }
    for (Class<?> type : types) {
      PropMapper pm = findOrCreateMapper(type, first);
      ((BeanMapper<S, ?>) pm.mapper)
          .addTypeMapper(ifNull(rest, ""), typeMapper, shiftTypeMap(first, typeFilters));
    }
  }


  // TODO: add type filters (@ApplyIf)
  @SuppressWarnings("unchecked")
  void addPostMappingMethod(String prop, Method m) throws NoSuchFieldException {
    if (prop.isEmpty()) {
      this.postMethods.add(m);
      return;
    }
    validateProperty(prop);
    String first = splitProperty(prop)[0];
    String rest = splitProperty(prop)[1];
    PropMapper pm = findOrCreateMapper(rootType, first);
    ((BeanMapper<S, ?>) pm.mapper).addPostMappingMethod(ifNull(rest, ""), m);
  }


  PropMapper findOrCreateMapper(final Class<?> type, final String prop) 
        throws NoSuchFieldException {
    List<PropMapper> pms = propMappers.computeIfAbsent(type, c -> new ArrayList<>());
    Optional<PropMapper> existing = pms.stream()
        .filter(pm -> pm.prop.equals(prop))
        .findAny();
    if (existing.isPresent()) {
      return existing.get();
    } else {
      PropertyDescriptor pd = getPropDescriptor(type, prop);
      Mapper<? super S, ?> m = new BeanMapper<>(pd.getPropertyType(), entityMapper);
      PropMapper pm = new PropMapper(prop, m, "", pd.getWriteMethod());
      pms.add(pm);
      return pm;
    }
  }


  /**
   * Returns the property descriptor for property "prop" in bean class "type",
   * or throws if property doesn't exist or setter is unaccessible
   */
  PropertyDescriptor getPropDescriptor(final Class<?> clazz, final String prop)
        throws NoSuchFieldException {
    try {
      Optional<PropertyDescriptor> pd 
          = Stream.of(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
               .filter(d -> d.getName().equals(prop))
               .filter(d -> d.getWriteMethod() != null)
               .findAny();
      if (!pd.isPresent()) {
        throw new NoSuchFieldException(String.format("'%s' in %s", prop, clazz));
      }
      return pd.get();
    } catch (IntrospectionException e) {
      throw new RuntimeException("Cannot introspect class " + clazz, e);
    }

  }

  static final Pattern PROPERTY_NAME 
          = Pattern.compile("[a-z][_$0-9a-zA-Z]*(?:\\.[a-z][_$0-9a-zA-Z]*)*");


  static void validateProperty(String prop) throws InvalidPropertyNameException {
    if (prop == null || !PROPERTY_NAME.matcher(prop).matches()) {
      throw new InvalidPropertyNameException(String.valueOf(prop));
    }
  }


  static String[] splitProperty(String prop) {
    boolean simple = !prop.contains(".");
    return new String[] {
        simple ? prop : prop.substring(0, prop.indexOf('.')),
        simple ? null : prop.substring(prop.indexOf('.') + 1)
    };
  }


  static <T> Map<String, T> shiftTypeMap(String rootProp, Map<String, T> map) {
    if (map.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, T> shifted = new HashMap<>();
    for (Entry<String, T> e : map.entrySet()) {
      if (e.getKey().equals(rootProp)) {
        shifted.put("", e.getValue());
      } else {
        String[] parts = splitProperty(e.getKey());
        if (parts[1] != null)
          shifted.put(parts[1], e.getValue());
      }
    }
    return shifted;
  }


  static <V> V ifNull(V v, V ifNull) {
    return v == null ? ifNull : v;
  }

  
  class PropMapper {

    final Mapper<? super S, Object> mapper; // actual mapper
    final String prop;   // property
    final String source; // where this mapping came from
    final Method setter;

    @SuppressWarnings("unchecked")
    PropMapper(String prop, Mapper<? super S, ?> mapper,
            String source, Method setter) {
      this.mapper = (Mapper<? super S, Object>) mapper;
      this.prop = prop;
      this.source = source;
      this.setter = setter;
    }

    @Override
    public String toString() {
      return String.format("Mapper(%s.%s)",
          BeanMapper.this.rootType.getSimpleName(), prop);
    }
  }

  
  static class InvalidPropertyNameException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    InvalidPropertyNameException() {
      super();
    }

    InvalidPropertyNameException(String prop) {
      super(String.format("Invalid property: '%s'", prop));
    }
  }

}
