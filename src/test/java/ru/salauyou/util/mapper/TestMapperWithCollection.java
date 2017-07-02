package ru.salauyou.util.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ru.salauyou.util.mapper.Annotations.ApplyIf;
import ru.salauyou.util.mapper.Annotations.MapTo;

public class TestMapperWithCollection {

  static class RootBean {
    String name;
    CollectionBean collection;
    
    public String getName() {
      return name;
    }
    
    public void setName(String name) {
      this.name = name;
    }

    public CollectionBean getCollection() {
      return collection;
    }

    public void setCollection(CollectionBean collection) {
      this.collection = collection;
    }
  }
  
  
  static abstract class CollectionBean {
    public abstract Collection<String> getItems();
  }
  
  
  static class SimpleCollectionBean extends CollectionBean {
    Collection<String> items;
    
    @Override
    public Collection<String> getItems() {
      return items;
    }

    public void setItems(Collection<String> items) {
      this.items = items;
    }
  }
  
  
  static class NoSetterCollectionBean extends CollectionBean {
    Set<String> items;
    
    @Override
    public Collection<String> getItems() {
      if (items == null) {
        items = new HashSet<>();
      }
      return items;
    }
  }
  
  
  // --------------- mapper --------------- //
 
  static final Mapper<String, String> UPPER_CASE = String::toUpperCase;
  
  
  // List structure:
  // 0 - name
  // 1 - collection type (SIMPLE, NO_SETTER)
  // 2...n - items
  
  static final Mapper<List<String>, RootBean> MAPPER 
      = new EntityMapper<List<String>, RootBean>() {
    
    @MapTo()
    public void mapRoot() {
      map("name").from(s -> s.get(0));
      getTypeFor("collection").from(s -> {
        String type = s.get(1);
        if (type.equals("SIMPLE")) {
          return SimpleCollectionBean.class;
        } else if (type.equals("NO_SETTER")) {
          return NoSetterCollectionBean.class;
        } else {
          return null;
        }
      });
    }
    
    @MapTo("collection")
    @ApplyIf({ SimpleCollectionBean.class, NoSetterCollectionBean.class }) 
    public void mapCollection() {
      map("items").from(s -> Mapper.mapEach(s.subList(2, s.size()), UPPER_CASE));
    }
  };
  
  
  // ----------- test -------------- //
  
  @Test
  public void testSimpleCollection() {
    List<String> sample = Arrays.asList("sample-1", "SIMPLE", "one", "two");
    RootBean b = MAPPER.toFunction().apply(sample);
    assertEquals("sample-1", b.getName());
    assertSame(SimpleCollectionBean.class, b.getCollection().getClass());
    assertEquals(2, b.getCollection().getItems().size());
    assertEquals(Arrays.asList("ONE", "TWO"), b.getCollection().getItems());
    
    sample = Arrays.asList("sample-2", "NO_SETTER", "one", "two", "three");
    b = MAPPER.toFunction().apply(sample);
    assertEquals("sample-2", b.getName());
    assertSame(NoSetterCollectionBean.class, b.getCollection().getClass());
    assertEquals(3, b.getCollection().getItems().size());
    assertEquals(new HashSet<>(Arrays.asList("ONE", "TWO", "THREE")), b.getCollection().getItems());
    
    sample = Arrays.asList("sample-3", "UNKNOWN", "1");
    b = MAPPER.toFunction().apply(sample);
    assertEquals("sample-3", b.getName());
    assertNull(b.getCollection());
  }
  
}
