package ru.salauyou.utils;

/**
 * Immutable point in Cartesian coordinate system with x, y and accuracy
 */
public class Point {
	
	private final double x;
	private final double y;
	private final double accuracy;
	
	public Point(double x, double y){
		this(x, y, -1);
	}

	public Point(double x, double y, double accuracy){
		this.x = x;
		this.y = y;
		this.accuracy = accuracy;
	}
	
	public double getX(){
		return x;
	}
	
	public double getY(){
		return y;
	}
	
	/**
	 * @return	accuracy of the point. If < 0, accuracy is not defined
	 */
	public double getAccuracy(){
		return accuracy;
	}
	
	@Override
	public String toString(){
		return "x = " + x + "; y = " + y + (accuracy < 0 ? "" : ("; accuracy = " + accuracy));
	}
	
	@Override
	public boolean equals(Object o){
		return (o != null && o instanceof Point 
				&& ((Point)o).getX() == this.x && ((Point)o).getY() == this.y && ((Point)o).getAccuracy() == this.accuracy);
	}
	
}
