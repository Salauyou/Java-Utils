package locks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatsBuilder<T extends Comparable<T>> {

    private List<T> values = new ArrayList<>();
    
    
    public synchronized StatsBuilder<T> clear() {
        values.clear();
        return this;
    }
    
    
    public synchronized StatsBuilder<T> put(T v) {
        values.add(v);
        return this;
    }
    
    
    public synchronized List<T> getPercentiles(double[] percentiles) throws IllegalArgumentException {
        
        if (values.size() == 0)
            return Collections.emptyList();
        
        Collections.sort(values);
        List<T> result = new ArrayList<>(percentiles.length);
        for (int i = 0; i < percentiles.length; i++) {
            if (percentiles[i] < 0)
                throw new IllegalArgumentException("Percentile cannot be less than 0");
            int idx = (int) Math.round(values.size() * percentiles[i] / 100d) - 1;
            idx = Math.min(idx, values.size() - 1);
            idx = Math.max(idx, 0);
            result.add(values.get(idx));
        } 
        return result;
    }
    
    
    public synchronized String percentilesToString(double[] percentiles) {
        
        StringBuilder sb = new StringBuilder();
        List<T> percentileValues = this.getPercentiles(percentiles);
        
        if (percentileValues.size() == 0)
            return "";
        
        for (int i = 0; i < percentiles.length; i++)
            sb.append(percentiles[i]).append("%\t").append(percentileValues.get(i)).append("\n");
        return sb.toString().substring(0, sb.length() - 1);
    }
    
    
    
    public synchronized int getCount() {
        return values.size();
    }
    
    
}
