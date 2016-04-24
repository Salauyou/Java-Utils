package ru.salauyou.util.mapper;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import ru.salauyou.util.mapper.Annotations.MapTo;



/**
 * 
 * @author Salauyou
 *
 * @param <S> document type
 * @param <D> root entity bean type
 */
public abstract class EntityMapper<S, D> implements Mapper<S, D> {

    static final Logger log = Logger.getLogger("entity-mapper");
    
    String currentPrefix;
    Class<?> rootEntity;
    
    
    @PostConstruct
    public final void init() {
        @SuppressWarnings("rawtypes")
        Class<? extends EntityMapper> actual = this.getClass();
        try {
            ParameterizedType pt = (ParameterizedType) actual.getGenericSuperclass();
            rootEntity = (Class<?>) pt.getActualTypeArguments()[1];
            log.info("Root entity is " + rootEntity.getName());
        } catch (ClassCastException e) {
            throw new IllegalStateException(
                "Cannot recover class of entity in mapper " + actual.getName());
        }
        for (Method m : actual.getMethods()) {
            MapTo mapTo = m.getAnnotation(MapTo.class);
            if (mapTo == null)
                continue;
            currentPrefix = mapTo.value();
            try {
                m.setAccessible(true);
                m.invoke(this);
                m.setAccessible(false);
            } catch (RuntimeException | ReflectiveOperationException e) {
                e.printStackTrace();
            } 
        }
    }
    
    
    @Override
    public final D apply(S document) {
        if (rootEntity == null) {
            synchronized (this) {
                init();
            }
        }
        // TODO: implement
        return null;
    }
    
    
    final Map<String, Mapper<S, Object>> extractors = new HashMap<>();
    
    
    @SuppressWarnings("unchecked")
    final <T> void setMapping(String property, Mapper<? super S, ? extends T> e) {
        Mapper<S, Object> old = extractors.put(property, (Mapper<S, Object>) e);
        log.info((old == null ? "Mapped" : "Remapped") + " property: " + property);
    }
    
    
    @SuppressWarnings("unchecked")
    final <T> Mapper<S, T> getMapping(String property) {
        return (Mapper<S, T>) extractors.get(property);
    }
    
    
    @SuppressWarnings("rawtypes")
    final void setTypeMapping(String property,  Mapper<? super S, Class> e) {
        log.info("Mapped type for " + property);
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
            Mapping<R> m = new Mapping<>(property);
            EntityMapper.this.setMapping(property, mapper);
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
