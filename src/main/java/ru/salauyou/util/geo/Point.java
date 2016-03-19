package ru.salauyou.util.geo;

/**
 * Immutable point in Cartesian coordinate system with x, y and accuracy
 */
public class Point {
    
    private final double x;
    private final double y;
    private final double accuracy;
    
    
    public Point(double x, double y) {
        this(x, y, -1);
    }

    
    public Point(double x, double y, double accuracy) {
        this.x = x;
        this.y = y;
        this.accuracy = accuracy;
    }
    

    public double getX() {
        return x;
    }
    
    
    public double getY() {
        return y;
    }
    
   
    public double getAccuracy() {
        return accuracy;
    }
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("x = %.4f; y = %.4f", x, y));
        if (accuracy >= 0)
            sb.append(String.format("; accuracy = %.4f", accuracy));
        return sb.toString();
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Point))
            return false;
        Point p = (Point) o;
        return this.x == p.x && this.y == p.y 
               && this.accuracy == p.accuracy;
    }
    
}
