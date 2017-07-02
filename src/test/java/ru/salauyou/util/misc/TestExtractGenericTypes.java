package ru.salauyou.util.misc;



import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class TestExtractGenericTypes {

  Class<?> target;
  List<Class<?>> exp;
  
  interface Single<E> {}
  static class SingleImpl<E> implements Single<E> {}
  interface NestedSingle<E> extends Single<E> {}
  static class AbstractNestedSingle<E> implements NestedSingle<E> {}
  static class NestedSingleString extends AbstractNestedSingle<String> {}
  
  @Test
  public void testSingleParameter() {

    // cannot extract if instantiated directly 
    // with type parameters
    target = new SingleImpl<String>().getClass();
    exp = Collections.singletonList(null);
    assertEquals(exp, BeanHelper.resolveTypeArguments(target, Single.class));
    
    // but can extract from anonymous class!
    exp = Collections.singletonList(String.class);
    target = new Single<String>() { }.getClass();
    assertEquals(exp, BeanHelper.resolveTypeArguments(target, Single.class));
    
    // also when actual parameters are in nested types
    target = NestedSingleString.class;
    // resolve against base interface
    assertEquals(exp, BeanHelper.resolveTypeArguments(target, Single.class));
    // resolve against superclass
    assertEquals(exp, BeanHelper.resolveTypeArguments(target, AbstractNestedSingle.class));
  }
  
  
  
  interface Double<A, B> {}
  interface NestedDouble<A> extends Double<A, Long> {}
  interface NestedDouble2<B> extends Double<String, B> {}
  
  static class Double1 implements NestedDouble<String> {}
  static class Double11 extends Double1 {}
  
  static class Double2 implements Double<String, Long> {}
  static class Double21 extends Double2 implements NestedDouble2<Long> {}
  
  interface Triple<A, B, C> {}
  interface TripleB<C1, A1> extends Triple<A1, Long, C1> {}             // A <-> C reordered
  static class TripleBc<D2, A2, E2> implements TripleB<Integer, A2> {}  // additional parameters
  static class TripleAbc extends TripleBc<Void, String, Void> {}
  
  
  @Test
  public void testMultipleParameters() {
     
    exp = Arrays.asList(String.class, Long.class);
    
    // resolve in anonymous class
    target = new Double<String, Long>() {}.getClass();
    assertEquals(exp, BeanHelper.resolveTypeArguments(target, Double.class));
    target = new NestedDouble<String>() {}.getClass();
    assertEquals(exp, BeanHelper.resolveTypeArguments(target, Double.class));
    target = new NestedDouble2<Long>() {}.getClass();
    assertEquals(exp, BeanHelper.resolveTypeArguments(target, Double.class));
    
    // parameters in nested types
    List<Class<?>> targets = Arrays.asList(
        Double1.class, Double11.class, Double2.class, Double21.class);
    for (Class<?> t : targets) {
      assertEquals(exp, BeanHelper.resolveTypeArguments(t, Double.class));
    }
    
    // resolve against single-parameter type
    // implementing two-parameter interface
    exp = Collections.singletonList(String.class);
    targets = Arrays.asList(Double1.class, Double11.class);
    for (Class<?> t : targets) {
      assertEquals(exp, BeanHelper.resolveTypeArguments(t, NestedDouble.class));
    }
    exp = Collections.singletonList(Long.class);
    target = Double21.class;
    assertEquals(exp, BeanHelper.resolveTypeArguments(target, NestedDouble2.class));
    
    // three-parameter type, mixed order + additional parameters
    exp = Arrays.asList(String.class, Long.class, Integer.class);
    target = TripleAbc.class;
    assertEquals(exp, BeanHelper.resolveTypeArguments(target, Triple.class));
    
    exp = Arrays.asList(Integer.class, String.class);
    assertEquals(exp, BeanHelper.resolveTypeArguments(target, TripleB.class));
  }
  
  
  @Test
  @SuppressWarnings("rawtypes")
  public void testSpecialCases() {
    
    // raw type
    exp = Arrays.asList(null, null, null);
    target = new Triple() {}.getClass();
    assertEquals(exp, BeanHelper.resolveTypeArguments(target, Triple.class));
    
    // partially raw
    exp = Arrays.asList(null, Long.class, Integer.class);
    target = TripleBc.class;
    assertEquals(exp, BeanHelper.resolveTypeArguments(target, Triple.class));
    
    // non-generic supertype
    exp = Collections.emptyList();
    assertEquals(exp, BeanHelper.resolveTypeArguments(target, Object.class));
  }
  
  
}
