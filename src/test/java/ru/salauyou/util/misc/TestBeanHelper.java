package ru.salauyou.util.misc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import helpers.Model.Bank;
import helpers.Model.ForeignBank;
import helpers.Model.LocalBank;
import helpers.Model.Payment;
import helpers.Model.Subject;

public class TestBeanHelper {

  @Test
  public void testCloneSimpleBean() {
    
    Bank bankFrom = new LocalBank();
    bankFrom.setId(1L);
    bankFrom.setName("Alfabank");
    
    Subject payer = new Subject();
    payer.setBank(bankFrom);
    payer.setId("payer");
    payer.setName("Alice");
    
    Bank bankTo = new ForeignBank();
    bankTo.setId(2L);
    bankTo.setName("Citibank");
    
    Subject receiver = new Subject();
    receiver.setBank(bankTo);
    receiver.setId("receiver");
    receiver.setName("Alice");
    
    Payment pay = new Payment(100L);
    pay.setAmount(new BigDecimal("123.45"));
    pay.setPayer(payer);
    pay.setReceiver(receiver);
    pay.setTimestamp(Instant.now());
    
    Payment copy = BeanHelper.cloneOf(pay);
    
    assertNotSame(pay, copy);
    assertNotSame(pay.getPayer(), copy.getPayer());
    assertNotSame(pay.getReceiver(), copy.getReceiver());
    assertNotSame(pay.getPayer().getBank(), copy.getPayer().getBank());
    assertNotSame(pay.getReceiver().getBank(), copy.getReceiver().getBank());
    
    assertEquals(pay.getAmount(), copy.getAmount());
    assertEquals(pay.getId(), copy.getId());
    assertEquals(pay.getTimestamp(), copy.getTimestamp());
    
    Subject cPayer = copy.getPayer();
    Subject cReceiver = copy.getReceiver();
    Bank cBankFrom = cPayer.getBank();
    Bank cBankTo = cReceiver.getBank();
    
    assertEquals(payer.getId(), cPayer.getId());
    assertEquals(payer.getName(), cPayer.getName());
    assertEquals(bankFrom.getId(), cBankFrom.getId());
    assertEquals(bankFrom.getName(), cBankFrom.getName());
    assertSame(bankFrom.getClass(), cBankFrom.getClass());
    
    assertEquals(receiver.getId(), cReceiver.getId());
    assertEquals(receiver.getName(), cReceiver.getName());
    assertEquals(bankTo.getId(), cBankTo.getId());
    assertEquals(bankTo.getName(), cBankTo.getName());
    assertSame(bankTo.getClass(), cBankTo.getClass());
  }
  

  @Test
  public void testCloneBeanWithArray() {
    NonTrivialBean b = new NonTrivialBean();
    b.setName("onlyInts");
    b.setId(1L);
    b.setInts(new int[]{ 1, 2, 3 });
    
    NonTrivialBean copy = BeanHelper.cloneOf(b);
    assertNotSame(b, copy);
    assertNotSame(b.getInts(), copy.getInts());
    assertEquals(1L, copy.getId());
    assertEquals("onlyInts", copy.getName());
    assertArrayEquals(new int[]{ 1, 2, 3 }, copy.getInts());
    
    b = new NonTrivialBean();
    b.setObjects(new NonTrivialBean[]{ b, null, b }); // circular dependency
    
    copy = BeanHelper.cloneOf(b);
    assertNotSame(b, copy);
    assertNotSame(b.getObjects(), copy.getObjects());
    assertEquals(3, copy.getObjects().length);
    assertSame(copy.getObjects()[0], copy);
    assertNull(copy.getObjects()[1]);
    assertSame(copy.getObjects()[2], copy);
    assertEquals(0L, copy.getId());
    assertNull(copy.getName());
  }
  
  
  @Test
  public void testCloneBeanWithCollection() {
    NonTrivialBean b = new NonTrivialBean();
    b.setList(Collections.emptyList());
    
    NonTrivialBean copy = BeanHelper.cloneOf(b);
    assertNotSame(b, copy);
    assertNotNull(copy.getList());
    assertEquals(0, copy.getList().size());
    
    b = new NonTrivialBean();
    b.setList(new ArrayList<>());
    b.getList().addAll(Arrays.asList(null, b, null)); // circular dependency
    
    copy = BeanHelper.cloneOf(b);
    assertNotSame(b, copy);
    assertNotSame(b.getList(), copy.getList());
    assertEquals(3, copy.getList().size());
    assertNull(copy.getList().get(0));
    assertSame(copy, copy.getList().get(1));
    assertNull(copy.getList().get(2));
  }
  
  
  @Test
  public void testCloneBeanWithNoSetterCollection() {
    NonTrivialBean b = new NonTrivialBean();
    b.setName("b");
    NonTrivialBean b1 = new NonTrivialBean();
    b1.setName("b1");
    NonTrivialBean b2 = new NonTrivialBean();
    b2.setName("b2");
    b.getNoSetterList().addAll(Arrays.asList(b, b1, b2, null)); // circular dependency
    
    NonTrivialBean copy = BeanHelper.cloneOf(b);
    assertNotSame(b, copy);
    assertNotSame(b.getNoSetterList(), copy.getNoSetterList());
    assertEquals(4, copy.getNoSetterList().size());
    assertSame(copy, copy.getNoSetterList().get(0));
    assertNotSame(b1, copy.getNoSetterList().get(1));
    assertNotSame(b2, copy.getNoSetterList().get(2));
    assertNull(copy.getNoSetterList().get(3));
    assertEquals("b", copy.getName());
    assertEquals("b1", copy.getNoSetterList().get(1).getName());
    assertEquals("b2", copy.getNoSetterList().get(2).getName());
  }
  
  
  @Test
  public void testCloneBeanWithMap() {}
  
  
  

  
  static class NonTrivialBean {
    
    long id;
    String name;
    Map<String, NonTrivialBean> map;
    List<NonTrivialBean> list;
    List<NonTrivialBean> noSetterList;
    NonTrivialBean[] objects;
    int[] ints;
    Date date;
    
    public long getId() {
      return id;
    }
    
    public void setId(long id) {
      this.id = id;
    }
    
    public String getName() {
      return name;
    }
    
    
    public void setName(String name) {
      this.name = name;
    }
    
    public Map<String, NonTrivialBean> getMap() {
      return map;
    }
    
    public void setMap(Map<String, NonTrivialBean> map) {
      this.map = map;
    }
    
    public List<NonTrivialBean> getList() {
      return list;
    }
    
    public void setList(List<NonTrivialBean> list) {
      this.list = list;
    }
    
    public List<NonTrivialBean> getNoSetterList() {
      if (noSetterList == null) {
        noSetterList = new ArrayList<>();
      }
      return noSetterList;
    }
    
    public NonTrivialBean[] getObjects() {
      return objects;
    }
    
    public void setObjects(NonTrivialBean[] objects) {
      this.objects = objects;
    }
    
    public int[] getInts() {
      return ints;
    }
    
    public void setInts(int[] ints) {
      this.ints = ints;
    }
    
    public Date getDate() {
      return date;
    }
    
    public void setDate(Date date) {
      this.date = date;
    }
  }
  
  
}
