import org.apache.commons.numbers.complex.Complex;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.List;

public class MandelbrotGenerator {

    public static final int maxIterations = 255;

    protected static final double divergenceCeiling = 2.0;

    public class DataPoint {
        public double X;
        public double Y;
        public int rate;

        public DataPoint(double x, double y, int rate ) {
            X = x;
            Y = y;
            this.rate = rate;
        }
    }

    protected DataPoint calculatePoint(double X, double Y, double xIncrement, double yIncrement, int iterations) {

        //
        // X and Y are now the Upper-Left corner of the pixel we are rendering
        // For anti-aliasing level 0 this is all we render.
        // For each level, we subdivide this into 5 points and take the average
        //
        //    X,Y       X+x,Y
        //
        //       X+x/2,Y+y/2
        //
        //    X,Y+y     X+x,Y+y
        //
        // TODO: Cache previously calculated values in a HashMap or similar

        if( iterations == 0 ) {
            return calculatePoint(X, Y);
        } else if (iterations == 1) {
            DataPoint p1 = calculatePoint(X, Y);
            DataPoint p2 = calculatePoint(X + xIncrement, Y);
            DataPoint p3 = calculatePoint(X, Y + yIncrement);
            DataPoint p4 = calculatePoint(X + xIncrement, Y + yIncrement);
            DataPoint p5 = calculatePoint( X + xIncrement / 2.0, Y + yIncrement / 2.0);

            int avgRate = (p1.rate + p2.rate + p3.rate + p4.rate + p5.rate) / 5;

            return new DataPoint(X, Y, avgRate);
        } else {
            // Subdivide the space into 4 quadrants and recurse down
            double xIncrementHalf = xIncrement / 2.0;
            double X1 = X + xIncrementHalf;
            double yIncrementHalf = yIncrement / 2.0;
            double Y1 = Y + yIncrementHalf;

            DataPoint p1 = calculatePoint( X, Y, xIncrementHalf, yIncrementHalf, iterations - 1);
            DataPoint p2 = calculatePoint( X1, Y, xIncrementHalf, yIncrementHalf, iterations - 1);
            DataPoint p3 = calculatePoint( X, Y1, xIncrementHalf, yIncrementHalf, iterations - 1);
            DataPoint p4 = calculatePoint( X1, Y1, xIncrementHalf, yIncrementHalf, iterations - 1);

            int avgRate = (p1.rate + p2.rate + p3.rate + p4.rate) / 4;

            return new DataPoint(X, Y, avgRate);
        }
    }
    protected DataPoint calculatePoint(double X, double Y) {
        Complex c = Complex.ofCartesian(X, Y);
        Complex z = c;

        for( int i = 0; i < maxIterations; i++ ) {
            if( z.abs() >= divergenceCeiling ) {
                return new DataPoint(X, Y, i);
            }

            z = z.multiply( z ).add( c );
        }

        return new DataPoint(X, Y, maxIterations );
    }
}
