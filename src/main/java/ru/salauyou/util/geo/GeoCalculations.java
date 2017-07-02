package ru.salauyou.util.geo;

import ru.salauyou.util.geo.GeoProjections.Projection;

public class GeoCalculations {

    static public double distance(final Point p1, final Point p2){
        return Math.hypot(p1.getX() - p2.getX(), p1.getY() - p2.getY());
    }
    
    
    /**
     * Find intersection points of 2 circles with given 
     * center and radius. Supposed, that circles with 
     * 0.85d < (r1 + r2) < d (where d is distance between 
     * their centers) intersect in one point laying on 
     * line segment connecting centers 
     * 
     * @return array of 2 different points, or 2 same points, 
     *         or null, depending on how given circles intersect
     */
    static public Point[] find2CirclesIntersections(final Point c1, final double r1, 
                                                    final Point c2, final double r2) {
        
        if (r1 < 0 || r2 < 0)
            return null;
        
        Point[] result = null;
        
        if (r1 == 0 || r2 == 0){
            result = new Point[2];
            if (r1 == 0)
                result[0] = new Point(c1.getX(), c1.getY());
            else
                result[0] = new Point(c2.getX(), c2.getY());
             result[1] = result[0];
             return result;
        }
        
        double d = distance(c1, c2);
        double cosAlpha = 0.5 * (d*d - r2*r2 + r1*r1) / d / r1;
        double theta = Math.atan2(c2.getY() - c1.getY(), c2.getX() - c1.getX());

        if (cosAlpha < 1) {
            double alpha = Math.acos(cosAlpha);
            result = new Point[2];
            result[0] = new Point(c1.getX() + r1 * Math.cos(theta + alpha), c1.getY() + r1 * Math.sin(theta + alpha));
            result[1] = new Point(c1.getX() + r1 * Math.cos(theta - alpha), c1.getY() + r1 * Math.sin(theta - alpha));
        } else if ((r1 + r2) / d > 0.85) {
            result = new Point[2];
            result[0] = new Point(c1.getX() + (c2.getX() - c1.getX()) * r1 / d, c1.getY() + (c2.getY() - c1.getY()) * r1 / d);
            result[1] = result[0];
        }
        
        return result;
    }
    
    

    static public Point[] find3CirclesIntersections(Point c1, double r1, Point c2, double r2, Point c3, double r3){
        
        if (r1 < 0 || r2 < 0 || r3 < 0)
            return null;
        
        Point[] p12 = find2CirclesIntersections(c1, r1, c2, r2);
        Point[] p13 = find2CirclesIntersections(c1, r1, c3, r3);
        Point[] p23 = find2CirclesIntersections(c2, r2, c3, r3);
        
        if (p12 != null && p13 != null && p23 != null){
            Point p1 = p12[0];
            Point p2 = p13[0];
            double dmin = distance(p1, p2);
            if (distance(p12[0], p13[1]) < dmin){
                p1 = p12[0];
                p2 = p13[1];
                dmin = distance(p1, p2);
            }
            if (distance(p12[1], p13[0]) < dmin){
                p1 = p12[1];
                p2 = p13[0];
                dmin = distance(p1, p2);
            }
            if (distance(p12[1], p13[1]) < dmin){
                p1 = p12[1];
                p2 = p13[1];
                dmin = distance(p1, p2);
            }
            Point p4 = new Point((p1.getX() + p2.getX()) / 2d, (p1.getY() + p2.getY()) / 2d); // intermediate point 
            Point p3 = distance(p23[0], p4) < distance(p23[1], p4) ? p23[0] : p23[1];

            return new Point[]{p1, p3, p2}; // to make p1 belong to (c1,r1), p2 belong to (c2,r2), p3 belong to (c3,r3)
        
        } else {    
            return null;
        }
    }
    
    
    /**
     * Performs trilateration in geo coordinates.
     * @param c1
     * @param r1
     * @param c2
     * @param r2
     * @param c3
     * @param r3
     * @return
     */
    static public GeoPoint trilaterate(GeoPoint c1, double r1, GeoPoint c2, double r2, GeoPoint c3, double r3){
        
        if (r1 < 0 || r2 < 0 || r3 < 0)
            return null;
        
        // convert geo points to cartesian
        Projection proj = GeoProjections.newSimpleProjection((c1.getLon() + c2.getLon() + c3.getLon()) / 3d);
        Point cc1 = proj.convertGeoToPoint(c1);
        Point cc2 = proj.convertGeoToPoint(c2);
        Point cc3 = proj.convertGeoToPoint(c3);
        
        Point[] pts =  find3CirclesIntersections(cc1, r1, cc2, r2, cc3, r3);
        if (pts == null)
            return null;
        
        // adjust distances
        r1 = r1 * 2d - geoDistance(c1, proj.convertPointToGeo(pts[0]));
        r2 = r2 * 2d - geoDistance(c2, proj.convertPointToGeo(pts[1]));
        r3 = r3 * 2d - geoDistance(c3, proj.convertPointToGeo(pts[2]));
        pts = find3CirclesIntersections(cc1, r1, cc2, r2, cc3, r3);
        if (pts == null)
            return null;
        
        Point p = new Point((pts[0].getX() + pts[1].getX() + pts[2].getX()) / 3d, (pts[0].getY() + pts[1].getY() + pts[2].getY()) / 3d);
        double accuracy = Math.sqrt((distance(pts[0], p) + distance(pts[1], p) + distance(pts[2], p)) / 3d);
        return proj.convertPointToGeo(new Point(p.getX(), p.getY(), accuracy));
        
    }
    
    
    
    public static double geoDistance(GeoPoint gp1, GeoPoint gp2) {
            // Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
            // using the "Inverse Formula" (section 4)

            int MAXITERS = 20;
            // Convert lat/long to radians
            double lat1 = gp1.getLat() * Math.PI / 180.0;
            double lat2 = gp2.getLat() * Math.PI / 180.0;
            double lon1 = gp1.getLon() * Math.PI / 180.0;
            double lon2 = gp2.getLon() * Math.PI / 180.0;

            double a = 6378.137; // WGS84 major axis
            double b = 6356.7523142; // WGS84 semi-major axis
            double f = (a - b) / a;
            double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

            double L = lon2 - lon1;
            double A = 0.0;
            double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
            double U2 = Math.atan((1.0 - f) * Math.tan(lat2));

            double cosU1 = Math.cos(U1);
            double cosU2 = Math.cos(U2);
            double sinU1 = Math.sin(U1);
            double sinU2 = Math.sin(U2);
            double cosU1cosU2 = cosU1 * cosU2;
            double sinU1sinU2 = sinU1 * sinU2;

            double sigma = 0.0;
            double deltaSigma = 0.0;
            double cosSqAlpha = 0.0;
            double cos2SM = 0.0;
            double cosSigma = 0.0;
            double sinSigma = 0.0;
            double cosLambda = 0.0;
            double sinLambda = 0.0;

            double lambda = L; // initial guess
            for (int iter = 0; iter < MAXITERS; iter++) {
                double lambdaOrig = lambda;
                cosLambda = Math.cos(lambda);
                sinLambda = Math.sin(lambda);
                double t1 = cosU2 * sinLambda;
                double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
                double sinSqSigma = t1 * t1 + t2 * t2; // (14)
                sinSigma = Math.sqrt(sinSqSigma);
                cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
                sigma = Math.atan2(sinSigma, cosSigma); // (16)
                double sinAlpha = (sinSigma == 0) ? 0.0 :
                    cosU1cosU2 * sinLambda / sinSigma; // (17)
                cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
                cos2SM = (cosSqAlpha == 0) ? 0.0 :
                    cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)

                double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
                A = 1 + (uSquared / 16384.0) * // (3)
                    (4096.0 + uSquared *
                     (-768 + uSquared * (320.0 - 175.0 * uSquared)));
                double B = (uSquared / 1024.0) * // (4)
                    (256.0 + uSquared *
                     (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
                double C = (f / 16.0) *
                    cosSqAlpha *
                    (4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
                double cos2SMSq = cos2SM * cos2SM;
                deltaSigma = B * sinSigma * // (6)
                    (cos2SM + (B / 4.0) *
                     (cosSigma * (-1.0 + 2.0 * cos2SMSq) -
                      (B / 6.0) * cos2SM *
                      (-3.0 + 4.0 * sinSigma * sinSigma) *
                      (-3.0 + 4.0 * cos2SMSq)));

                lambda = L +
                    (1.0 - C) * f * sinAlpha *
                    (sigma + C * sinSigma *
                     (cos2SM + C * cosSigma *
                      (-1.0 + 2.0 * cos2SM * cos2SM))); // (11)

                double delta = (lambda - lambdaOrig) / lambda;
                if (Math.abs(delta) < 1.0e-12) {
                    break;
                }
            }

            return b * A * (sigma - deltaSigma);
        }
    
    
    
}
