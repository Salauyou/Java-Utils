package ru.salauyou.utils;

public final class GeoProjections {

    private final static double LAT_LENGTH = 111.3; // default length of 1 degree of latitude
    private final static double A_COEF = 6378.137;
    private final static double E_2_COEF = (Math.pow(A_COEF, 2) - Math.pow(6356.752, 2)) / Math.pow(A_COEF, 2);
    
    /**
     * Interface containing methods of converting geo point to cartesian and vice versa
     */
    public interface Projection {
        Point convertGeoToPoint(GeoPoint gp);
        GeoPoint convertPointToGeo(Point p);
    }
    
    /**
     * Simple projection object. Area between 2 parallels and 2 meridians is projected in a trapezoid with bases parallel to x axis.
     * It doesn't work properly near +- 180 degree longitude and near poles.
     */
    private static class SimpleProjection implements Projection {

        private final double zeroLon;
        
        private SimpleProjection(double zeroLongitude){
            zeroLon = zeroLongitude;
        }
        
        @Override
        public Point convertGeoToPoint(GeoPoint gp) {
            return new Point((gp.getLon() - zeroLon) * lengthOfOneDegree(gp.getLat()), gp.getLat() * LAT_LENGTH, gp.getAccuracy());
        }

        @Override
        public GeoPoint convertPointToGeo(Point p) {
            double lat = p.getY() / LAT_LENGTH;
            return new GeoPoint(lat, p.getX() / lengthOfOneDegree(lat) + zeroLon, p.getAccuracy());
        }
        
        /**
         * Returns approximate length of 1 longitude degree 
         * 
         * @param lat    given latitude
         * @return    length in km
         */
        double lengthOfOneDegree(double lat){
            return Math.PI * A_COEF * Math.cos(Math.toRadians(lat)) / 180d 
                    / Math.sqrt(1d - E_2_COEF * Math.pow(Math.sin(Math.toRadians(lat)), 2));
        }
    }
    
    
    /**
     * Factory method that creates simple projection object
     * 
     * @param zeroLongitude    reference longitude
     * @return
     */
    public static Projection newSimpleProjection(final double zeroLongitude){
        return new SimpleProjection(zeroLongitude);
    }
    
}
