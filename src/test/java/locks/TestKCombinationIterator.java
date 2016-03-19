package locks;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import ru.salauyou.util.misc.KCombinationIterator;

public class TestKCombinationIterator {

    @Test
    public void testKCombinationGeneration() {
        final List<String> source = asList("A", "B", "C", "D");
        final List<Collection<String>> res = new ArrayList<>();
        KCombinationIterator<String> ki;
        
        ki = new KCombinationIterator<>(source, 1);
        while (ki.hasNext() && res.add(ki.next()));
        assertEquals(asList(asList("A"), asList("B"), asList("C"), asList("D")), res);
        
        res.clear();
        ki = new KCombinationIterator<>(source, 2);
        while (ki.hasNext() && res.add(ki.next()));
        assertEquals(asList(asList("A", "B"), asList("A", "C"), asList("A", "D"), 
                            asList("B", "C"), asList("B", "D"), 
                            asList("C", "D")), 
                     res);
        
        res.clear();
        ki = new KCombinationIterator<>(source, 3);
        while (ki.hasNext() && res.add(ki.next()));
        assertEquals(asList(asList("A", "B", "C"), asList("A", "B", "D"), 
                            asList("A", "C", "D"), asList("B", "C", "D")), 
                     res);
    }
    
    
    
    
    
}
