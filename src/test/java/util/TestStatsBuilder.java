package util;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.Test;

import ru.salauyou.util.misc.StatsBuilder;

public class TestStatsBuilder {
    
    static final double[] S_PERCENTILES = { 68.26, 95.44, 99.73 };
    static final double[] QUARTILES = { 25, 50, 75, 100 };
    
    static final Random R = new Random();
    
    
    @Test
    public void testStatsBuilderCollector() {
        StatsBuilder<Double> sb;
        
        // folded normal distribution, σ = 1
        sb = Stream.generate(R::nextGaussian).limit(10_000) 
                   .map(Math::abs)
                   .collect(StatsBuilder.collector());
        
        List<Double> prc = sb.getPercentiles(S_PERCENTILES);
        assertEquals(1d, prc.get(0), 0.02);     // ± 1σ
        assertEquals(2d, prc.get(1), 0.05);     // ± 2σ
        assertEquals(3d, prc.get(2), 0.1);      // ± 3σ
        
        // uniform distribution [0, 100)
        sb = Stream.generate(() -> R.nextDouble() * 100).limit(10_000)  
                   .collect(StatsBuilder.collector());
        
        prc = sb.getPercentiles(QUARTILES);
        assertEquals(25, prc.get(0), 1);
        assertEquals(50, prc.get(1), 1);
        assertEquals(75, prc.get(2), 1);
        assertEquals(100, prc.get(3), 1);
    }
    
    
    
}
