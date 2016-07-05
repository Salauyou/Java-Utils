package ru.salauyou.util.misc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.math.BigDecimal;
import java.time.Instant;

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
  public void testCloneBeanWithArray() {}
  
  
  @Test
  public void testCloneBeanWithMap() {}
  
  
  @Test
  public void testCloneBeanWithCollection() {}
  
  
  @Test
  public void testCloneBeanWithNoSetterCollection() {}
  
}
