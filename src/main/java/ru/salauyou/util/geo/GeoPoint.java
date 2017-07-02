package ru.salauyou.util.geo;

/**
 * Immutable point in geo coordinates (latitude, longitude) with accuracy in km
 */
public final class GeoPoint {

    private final double lat;
    private final double lon;
    private final double accuracy;

    /**
     * Creates a new geo point without accuracy
     * @param lat
     * @param lon 
     */
    public GeoPoint(double lat, double lon) {
        this(lat, lon, -1);
    }
    
    /**
     * Creates a new geo point with specified accuracy
     * @param lat        latitude
     * @param lon        longitude
     * @param accuracy    accuracy in km
     */
    public GeoPoint(double lat, double lon, double accuracy) {
        this.lat = lat;
        this.lon = lon;
        this.accuracy = accuracy;
    }

    public double getLat(){
        return this.lat;
    }
    
    public double getLon(){
        return this.lon;
    }
    
    /**
     * @return accuracy in km. If < 0, accuracy is not defined
     */
    public double getAccuracy(){
        return this.accuracy;
    }
    
    @Override
    public String toString(){
        return "lat = " + this.lat + "; lon = " + this.lon + (this.accuracy < 0 ? "" : ("; accuracy = " + this.accuracy));
    }
    
    @Override
    public boolean equals(Object o){
        return (o != null && o instanceof GeoPoint 
                && ((GeoPoint)o).getLat() == this.lat && ((GeoPoint)o).getLon() == this.lon 
                && ((GeoPoint)o).getAccuracy() == this.accuracy);
    }
    

}
