package ru.salauyou.util.mapper;

import static ru.salauyou.util.misc.ExceptionHelper.buildExceptionMessage;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.salauyou.util.mapper.Annotations.ApplyIf;
import ru.salauyou.util.mapper.Annotations.MapTo;
import ru.salauyou.util.mapper.Annotations.PostMapping;


/**
 * 
 * @author Salauyou
 *
 * @param <S> document type
 * @param <D> root entity bean type
 */
public abstract class EntityMapper<S, D> implements Mapper<S, D> {

    static final Log log = LogFactory.getLog(EntityMapper.class);
    
    String currentPrefix;
    final Map<String, Class<?>> currentTypes = new HashMap<>();
    
    Class<? extends D> rootEntity;
    BeanMapper<S, ? extends D> mapper;
    
    volatile boolean initialized = false;
    
            
    
    @PostConstruct
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final void init() {
        initialized = false;
        Class<? extends EntityMapper> actual = this.getClass();
        try {
            ParameterizedType pt = (ParameterizedType) actual.getGenericSuperclass();
            rootEntity = (Class<? extends D>) pt.getActualTypeArguments()[1];
            mapper = new BeanMapper<>(rootEntity, this);
            log.info("Root entity is " + rootEntity.getName());
        } catch (ClassCastException e) {
            throw new IllegalStateException(
                "Cannot recover class of entity in mapper " + actual.getName());
        }
        
        // scan for annotated methods
        for (Method m : actual.getMethods()) {
            // assign mappers
            MapTo mapTo = m.getAnnotation(MapTo.class);
            if (mapTo != null) {
                currentPrefix = mapTo.value();
                currentTypes.clear();
                ApplyIf ai = m.getAnnotation(ApplyIf.class);
                if (ai != null)
                    currentTypes.put(currentPrefix, ai.value());
                try {
                    m.setAccessible(true);
                    m.invoke(this);
                } catch (RuntimeException | ReflectiveOperationException e) {
                    e.printStackTrace();
                }
            }
            // assign post mapping callback
            PostMapping postMap = m.getAnnotation(PostMapping.class);
            if (postMap != null) {
                for (String prop : postMap.value()) {
                    try {
                        mapper.addPostMappingMethod(prop, m);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        initialized = true;
    }
    
    
    
    @Override
    public final D apply(S source) {
        long ts = 0;
        D res = null;
        if (!initialized) {
            synchronized (this) {
                if (!initialized) 
                    init();
            }
        }
        if (log.isDebugEnabled())
            ts = System.nanoTime();
        try {
            res = mapper.apply(source);
        } catch (Exception e) {
            log.warn("Failed to map the source");
            log.warn(buildExceptionMessage(e));
        }
        if (log.isDebugEnabled()) {
            long te = System.nanoTime();
            log.debug(String.format("Mapped %s, took %.2f ms",
                        rootEntity.getSimpleName(), (double) (te - ts) / 1E6));
        }
        return res;
    }
    
    
    
    final Map<String, Mapper<S, Object>> extractors = new HashMap<>();
    
    
    @SuppressWarnings("unchecked")
    final <T> void setMapping(String property, Mapper<? super S, ? extends T> e, String source) {
        try {
            mapper.addMapperForProperty(property, e, currentTypes, source);
            if (e instanceof EntityMapper)
                ((EntityMapper<?,?>) e).init();
            Mapper<S, Object> old = extractors.put(property, (Mapper<S, Object>) e);
            log.info(String.format("%s property '%s'", 
                        (old == null ? "Mapped" : "Remapped"), property));
        } catch (Exception ex) {
            log.warn(buildExceptionMessage(ex));
            log.warn("→  " + source);
        }
    }
    

    
    @SuppressWarnings("rawtypes")
    final void setTypeMapping(String property,  Mapper<? super S, Class> e) {
        try {
            mapper.addTypeMapper(property, e, currentTypes);
            log.info("Mapped type for " + property);
        } catch (NoSuchFieldException ex) {
            log.warn(buildExceptionMessage(ex));
        }
    }
    
     
    
    public final <T> Mapping<T> map(String property) {
        return new Mapping<>(currentPrefix.isEmpty() 
                ? property 
                : (currentPrefix + "." + property));
    }
    
    
    public final <T> Mapping<T> map(String property, Mapper<? super S, ? extends T> extractor) {
        return this.<T>map(property).from(extractor);
    }
    
    
    public final TypeMapping getTypeFor(String property) {
        return new TypeMapping(property);
    }
    
    
    @SuppressWarnings("rawtypes")
    public final void getTypeFor(String property, Mapper<? super S, Class> typeMapper) {
        this.getTypeFor(property).from(typeMapper);
    }
    
    
    
    // ------------------ mapping classes ------------------- //
    
    public final class Mapping<T> {
        
        final String property;
        
        private Mapping(String property) {
            this.property = property;
        }
        
        
        /**
         * Mapper which will be used to produce actual value for mapping
         */
        public <R> Mapping<R> from(Mapper<? super S, ? extends R> mapper) {
            String source = "";
            Iterator<StackTraceElement> i 
                    = Arrays.asList(Thread.currentThread().getStackTrace()).iterator();
            while (i.hasNext() && !i.next().getMethodName().equals("from"));
            if (i.hasNext()) {
                StackTraceElement e = i.next();
                if (e.getMethodName().equals("map") 
                    && e.getClassName().contains("EntityMapper"))
                    e = i.next();
                source = String.format("at %s.%s(%s:%s)", 
                            e.getClassName(), e.getMethodName(), 
                            e.getFileName(), e.getLineNumber());
            }
            Mapping<R> m = new Mapping<>(property);
            EntityMapper.this.setMapping(property, mapper, source);
            return m;
        } 
    }
        
    
    
    public final class TypeMapping {
        
        final String property;
        
        
        TypeMapping(String property) {
            this.property = property;
        }
        
        @SuppressWarnings("rawtypes")
        public TypeMapping from(Mapper<? super S, Class> typeMapper) {
            EntityMapper.this.setTypeMapping(this.property, typeMapper);
            return this;
        }

    }

   
    
    public <T> Mapper<S, T> take(Mapper<? super S, ? extends T> mapper) {
        return Mapper.of(mapper);
    }
    

}
