import org.apache.commons.numbers.complex.Complex;

import java.util.HashMap;

public class MandelbrotGenerator {

    public static final int maxIterations = 255;

    protected static final double divergenceCeiling = 2.0;

    protected static HashMap<String, DataPoint> pointCache = new HashMap<String, DataPoint>();

    protected long pointsCalculated = 0, cacheHits = 0, cacheRemoves = 0, cacheMisses = 0, cacheSkips = 0, cachePuts = 0;


    //
    // Caching Hint bitmasks
    //
    public static final int CACHE_HINT_SKIP = 0x0001;
    public static final int CACHE_HINT_REMOVE = 0x0002;
    public static final int CACHE_HINT_STORE = 0x0004;
    public static final int CACHE_HINT_TOP_ROW = 0x0008;
    public static final int CACHE_HINT_LAST_ROW = 0x0010;
    public static final int CACHE_HINT_LAST_COLUMN = 0x0020;

    public class DataPoint {
        public double X;
        public double Y;
        public int rate;

        public DataPoint(double x, double y, int rate ) {
            X = x;
            Y = y;
            this.rate = rate;
        }

        public String strHashCode() {
            return DataPoint.strHashCode(X, Y);
        }

        public static String strHashCode( double X, double Y) {
            return Double.hashCode(X) + "-" + Double.hashCode(Y);
        }
    }

    protected DataPoint calculatePoint(double X, double Y, double xIncrement, double yIncrement, int iterations, int cacheHint) {

        //
        // X and Y are now the Upper-Left corner of the pixel we are rendering
        // For anti-aliasing level 0 this is all we render.
        // At the lowest level, we subdivide this into 5 points and take the average
        //
        //    X,Y       X+x,Y
        //
        //       X+x/2,Y+y/2
        //
        //    X,Y+y     X+x,Y+y
        //
        // At higher levels we subdivide into quadrants and recurse
        //


        if( iterations == 0 ) {
            //
            // Ignore the cache all together when AA is disabled
            //
            return calculatePoint(X, Y, CACHE_HINT_SKIP);
        } else if (iterations == 1) {
            //
            // p1 is the top-left corner. This should be skipped if we're on the first row,
            // or removed from the cache in subsequent rows.
            //
            DataPoint p1 = calculatePoint(X, Y,
                    ((cacheHint & CACHE_HINT_TOP_ROW) != 0 ) ?
                    CACHE_HINT_SKIP : CACHE_HINT_REMOVE);

            //
            // p2 is the top-right corner. This should be skipped if we're on the first row,
            // and should also be skipped if we're on the last column. If neither of these,
            // it might already be in the cache, or it might be needed later, so store it
            //
            DataPoint p2 = calculatePoint(X + xIncrement, Y,
                    ((cacheHint & (CACHE_HINT_TOP_ROW | CACHE_HINT_LAST_COLUMN)) != 0) ?
                            CACHE_HINT_SKIP : CACHE_HINT_STORE);

            //
            // p3 is the bottom-left corner. This should be skipped if we're on the last row,
            // and should be stored otherwise.
            //
            DataPoint p3 = calculatePoint(X, Y + yIncrement,
                    ((cacheHint & CACHE_HINT_LAST_ROW) != 0) ?
                            CACHE_HINT_SKIP : CACHE_HINT_STORE );

            //
            // p4 is the bottom-right corner. This should be skipped if we're on the last row,
            // or if we're on the last column. If neither of these it should be stored.
            //
            DataPoint p4 = calculatePoint(X + xIncrement, Y + yIncrement,
                    ((cacheHint & (CACHE_HINT_LAST_ROW | CACHE_HINT_LAST_COLUMN)) != 0) ?
                            CACHE_HINT_SKIP : CACHE_HINT_STORE);
            //
            // p5 is in the centre of the pixel, and will never be needed in the cache
            //
            DataPoint p5 = calculatePoint( X + xIncrement / 2.0, Y + yIncrement / 2.0, CACHE_HINT_SKIP);

            int avgRate = (p1.rate + p2.rate + p3.rate + p4.rate + p5.rate) / 5;

            return new DataPoint(X, Y, avgRate);
        } else {
            // Subdivide the space into 4 quadrants and recurse down
            double xIncrementHalf = xIncrement / 2.0;
            double X1 = X + xIncrementHalf;
            double yIncrementHalf = yIncrement / 2.0;
            double Y1 = Y + yIncrementHalf;

            //
            // p1 is the top-left quadrant. Persist the TOP_ROW if set, but not LAST_COLUMN
            //
            DataPoint p1 = calculatePoint( X, Y, xIncrementHalf, yIncrementHalf, iterations - 1,
                    cacheHint & CACHE_HINT_TOP_ROW);
            //
            // p2 is the top-right quadrant. Persist the TOP_ROW and LAST_COLUMN if set
            //
            DataPoint p2 = calculatePoint( X1, Y, xIncrementHalf, yIncrementHalf, iterations - 1,
                    cacheHint & (CACHE_HINT_TOP_ROW | CACHE_HINT_LAST_COLUMN));
            //
            // p3 is the bottom-left quadrant. Persist the LAST_ROW if set
            //
            DataPoint p3 = calculatePoint( X, Y1, xIncrementHalf, yIncrementHalf, iterations - 1,
                cacheHint & (CACHE_HINT_LAST_ROW));
            //
            // p4 is the bottom-right quadrant. Persist LAST_ROW and LAST_COLUMN if set
            //
            DataPoint p4 = calculatePoint( X1, Y1, xIncrementHalf, yIncrementHalf, iterations - 1,
                    cacheHint & (CACHE_HINT_LAST_ROW | CACHE_HINT_LAST_COLUMN));

            int avgRate = (p1.rate + p2.rate + p3.rate + p4.rate) / 4;

            return new DataPoint(X, Y, avgRate);
        }
    }

    protected DataPoint calculatePoint(double X, double Y, int cacheHint) {

        String cacheKey = DataPoint.strHashCode(X, Y);
        if((cacheHint & CACHE_HINT_SKIP) == 0 ) {
            if (pointCache.containsKey(cacheKey)) {
                if ((cacheHint & CACHE_HINT_REMOVE) != 0) {
                    cacheRemoves++;
                    return pointCache.remove(cacheKey);
                } else {
                    cacheHits++;
                    return pointCache.get(cacheKey);
                }
            } else {
                cacheMisses++;
            }
        }

        Complex c = Complex.ofCartesian(X, Y);
        Complex z = c;

        for( int i = 0; i < maxIterations; i++ ) {
            if( z.abs() >= divergenceCeiling ) {
                DataPoint result = new DataPoint(X, Y, i);
                pointsCalculated++;
                if((cacheHint & CACHE_HINT_STORE) != 0) {
                    cachePuts++;
                    pointCache.put( cacheKey, result);
                } else {
                    cacheSkips++;
                }
                return result;
            }

            z = z.multiply( z ).add( c );
        }

        DataPoint result = new DataPoint(X, Y, maxIterations );
        pointsCalculated++;
        if((cacheHint & CACHE_HINT_STORE) != 0) {
            cachePuts++;
            pointCache.put( cacheKey, result);
        } else {
            cacheSkips++;
        }
        return result;
    }

    public String getStats() {
        return "pointsCalculated: [" + pointsCalculated + "] cacheHits: [" + cacheHits + "] cacheRemoves: ["
                + cacheRemoves + "] cacheMisses: [" + cacheMisses + "] cacheSkips: [" + cacheSkips +
                "] cachePuts: [" + cachePuts + "] Final cache size: [" + pointCache.size() + "]";

    }
}
