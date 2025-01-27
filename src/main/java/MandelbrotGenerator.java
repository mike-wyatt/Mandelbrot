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
