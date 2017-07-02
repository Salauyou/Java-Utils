package ru.salauyou.util.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import helpers.Model.Bank;
import helpers.Model.ForeignBank;
import helpers.Model.LocalBank;
import helpers.Model.Subject;
import ru.salauyou.util.mapper.Annotations.ApplyIf;
import ru.salauyou.util.mapper.Annotations.MapTo;


public class TestMapperConditionalTypes {

  static class SimpleMapper extends EntityMapper<List<String>, Subject> {

    // List structure:
    // 0 id
    // 1 name
    // 2 bank.id
    // 3 bank.name
    // 4 bank identifier ("LOCAL", "FOREIGN")

    static final Map<String, Class<?>> BANK_TYPES = new HashMap<>();
    
    static {
      BANK_TYPES.put("LOCAL", LocalBank.class);
      BANK_TYPES.put("FOREIGN", ForeignBank.class);
    }
    
    
    @MapTo()
    public void mapRoot() {
      map("id").from(s -> s.get(0));
      map("name").from(s -> s.get(1));
      getTypeFor("bank").from(s -> BANK_TYPES.getOrDefault(s.get(4), Bank.class));
    }
    
    @MapTo()
    @ApplyIf(Bank.class) // never called, as Subject cannot be of type Bank
    public void mapRootNeverCalled() {
      map("id").from(s -> "fakeId");
      map("name").from(s -> "fakeName");
    }

    @MapTo("bank")
    @ApplyIf({ LocalBank.class, ForeignBank.class }) // apply to both bank types
    public void mapLocalOrForeignBank() {
      map("id", take(s -> s.get(2)).then(Long::parseLong));
    }

    @MapTo("bank")
    @ApplyIf(LocalBank.class) // apply only to LocalBank
    public void mapLocalBank() {
      map("name", take(s -> s.get(3)).then(name -> "LOCAL " + name));
    }

    @MapTo("bank")
    @ApplyIf(ForeignBank.class) // apply only to ForeignBank
    public void mapForeignBank() {
      map("name", take(s -> s.get(3)).then(name -> "FOREIGN " + name));
    }
  }


  @Test
  public void testSimpleMapper() {
    SimpleMapper sm = new SimpleMapper();
    List<String> partsLocal = Arrays.asList("id-1", "Sasha", "10", "Alfabank", "LOCAL");
    List<String> partsForeign = Arrays.asList("id-2", "Dasha", "20", "Citibank", "FOREIGN");
    List<String> partsUndef = Arrays.asList("id-3", "Lesha", "100", "Inkombank", "");

    Subject s = sm.apply(partsLocal);
    assertEquals("id-1", s.getId());
    assertEquals("Sasha", s.getName());
    assertSame(LocalBank.class, s.getBank().getClass());
    assertEquals((Long) 10L, s.getBank().getId());
    assertEquals("LOCAL Alfabank", s.getBank().getName());
    
    s = sm.apply(partsForeign);
    assertEquals("id-2", s.getId());
    assertEquals("Dasha", s.getName());
    assertSame(ForeignBank.class, s.getBank().getClass());
    assertEquals((Long) 20L, s.getBank().getId());
    assertEquals("FOREIGN Citibank", s.getBank().getName());
    
    s = sm.apply(partsUndef);
    assertEquals("id-3", s.getId());
    assertEquals("Lesha", s.getName());
    assertNull(s.getBank());
  }

}
