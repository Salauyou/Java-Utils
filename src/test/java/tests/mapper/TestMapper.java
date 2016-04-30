package tests.mapper;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ru.salauyou.util.mapper.Annotations.MapTo;
import ru.salauyou.util.mapper.Annotations.PostMapping;
import ru.salauyou.util.mapper.EntityMapper;
import tests.Model.Payment;

public class TestMapper {

    
    
    static class SimpleMapper extends EntityMapper<List<String>, Payment> {
    
        static final DateTimeFormatter DF 
                    = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        //  List structure:
        //  0 payer.id
        //  1 payer.name
        //  2 payer.bank.id
        //  3 payer.bank.name
        //  4 receiver.id
        //  5 receiver.name
        //  6 id
        //  7 amount
        //    8 timestamp
        
        @MapTo()
        public void mapPayment() {
            map("receiver.id").from(s -> s.get(4));
            map("receiver.name").from(s -> s.get(5));
            map("id").from(take(s -> s.get(6)).then(Long::parseLong));
            map("amount").from(take(s -> s.get(7)).then(BigDecimal::new).orDefault(BigDecimal.ZERO));
            map("timestamp").from(take(s -> s.get(8)).then(Instant::parse));
        }
        
        @MapTo("payer")
        public void mapPayer() {
            map("id",   s -> s.get(0));
            map("name", s -> s.get(1));
        }
        
        @MapTo("payer.bank")
        public void mapPayerBank() {
            map("id",   take(s -> s.get(2)).then(Long::parseLong));
            map("name", s -> s.get(3));
        }

        @PostMapping() 
        public void copyPayerBankToReceiver(Payment pay) {
            pay.getReceiver().setBank(pay.getPayer().getBank());
        }
    }
    
    
    
    @Test
    public void testSimpleMapper() {
        SimpleMapper sm = new SimpleMapper();
        List<String> parts 
            = Arrays.asList("payer-1", "Sasha", "1000", "Bank", "receiver-1", "Dasha", 
                            "1", "10.55", "2016-04-30T12:00:00.00Z");    
        
        Payment p = sm.apply(parts);
        
        assertEquals("payer-1", p.getPayer().getId());
        assertEquals("Sasha",   p.getPayer().getName());
        
        assertEquals("Bank", p.getPayer().getBank().getName());
        assertEquals(Long.valueOf(1000), p.getPayer().getBank().getId());
        assertSame(p.getPayer().getBank(), p.getReceiver().getBank());
        
        assertEquals(Long.valueOf(1), p.getId());
        assertEquals(new BigDecimal("10.55"), p.getAmount());
        assertEquals(LocalDateTime.of(2016, Month.APRIL, 30, 12, 0, 0), 
                     LocalDateTime.ofInstant(p.getTimestamp(), ZoneId.of("UTC")));

        // alter some properties
        parts.set(8, "WRONG DATETIME");  // invalid
        parts.set(7, "WRONG AMOUNT");    // invalid
        parts.set(6, "2");               // valid
        
        p = sm.apply(parts);
        assertNull(p.getTimestamp());                     // null
        assertEquals(BigDecimal.ZERO, p.getAmount());     // default value
        assertEquals(Long.valueOf(2), p.getId());         // correct value
        assertEquals("Sasha", p.getPayer().getName());    
        assertEquals("Dasha", p.getReceiver().getName());
        
    }
    
}
