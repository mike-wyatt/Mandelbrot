import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.imaging.PixelDensity;
import org.apache.commons.imaging.formats.png.PngWriter;
import org.apache.commons.imaging.formats.png.PngImagingParameters;
import org.apache.commons.imaging.palette.PaletteFactory;

public class Mandelbrot {

    public static void main( String args[]) {

        MandelbrotArgs mArgs = MandelbrotArgs.parseArgs(args);

        if(mArgs.parseErrors) {
            if(mArgs.debug) {
                System.err.println("Unable to parse argument string [" + Arrays.stream(args).collect(Collectors.joining(" ")) + "]");
            }
            System.err.println(mArgs.errorMsg);
            if(mArgs.printHelp) {
                System.out.println(mArgs.printUsage());
            }
            return;
        }

        MandelbrotGenerator gen = new MandelbrotGenerator();

        //
        // Very naive iterative approach
        //
        double xPxIncrement = 1.0 / (double) mArgs.xResolution;
        double xIncrement = (mArgs.maxViewportX - mArgs.minViewportX) * xPxIncrement;
        double yPxIncrement = 1.0 / (double) mArgs.yResolution;
        double yIncrement = (mArgs.maxViewportY - mArgs.minViewportY) * yPxIncrement;

        //
        // Output image
        //
        FileOutputStream outputF = setupOutputFile(mArgs.outputFile);
        if( outputF == null ) {
            return;
        }

        PngImagingParameters pngImagingParameters = new PngImagingParameters();
        pngImagingParameters.setForceTrueColor(true);

        BufferedImage img = new BufferedImage(mArgs.xResolution, mArgs.yResolution, BufferedImage.TYPE_INT_ARGB);

        // Precalculate colours
        int colours[] = new int[256];
        for( int i = 0; i < 255; i++) {
            Color c = new Color(i/2,i/2,i);
            colours[i] = c.getRGB();
        }
        Color c = new Color(0,0,0);
        colours[255] = c.getRGB();

        //
        // Now generate
        //
        long startTimeNano = System.nanoTime();
        int cacheHint = MandelbrotGenerator.CACHE_HINT_TOP_ROW;
        for( int y = 0; y < mArgs.yResolution; y++ ) {
            if( y == mArgs.yResolution - 1 ) {
                cacheHint = MandelbrotGenerator.CACHE_HINT_LAST_ROW;
            }

            for (int x = 0; x < mArgs.xResolution; x++) {
                if( x == mArgs.xResolution - 1 ) {
                    cacheHint |= MandelbrotGenerator.CACHE_HINT_LAST_COLUMN;
                }

                double X = mArgs.minViewportX + ((double) x * xIncrement);
                double Y = mArgs.maxViewportY - ((double) y * yIncrement);      // Invert Y from world to camera co-ords

                MandelbrotGenerator.DataPoint p = gen.calculatePoint(X, Y, xIncrement, yIncrement, mArgs.aaCycles, cacheHint);
                img.setRGB(x,y,colours[p.rate]);
            }
            cacheHint = 0;
        }
        long endTimeNano = System.nanoTime();
        long timeMs = (endTimeNano - startTimeNano)/ 1000000;
        System.out.println("Calculated image in [" + timeMs + "] ms");
        System.out.println(gen.getStats());

        try {
            PngWriter png = new PngWriter();
            png.writeImage(img, outputF, pngImagingParameters, new PaletteFactory());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    static FileOutputStream setupOutputFile(String filename) {
        try {
            File f = new File(filename);
            if( f.exists() && f.canWrite() ) {
                return new FileOutputStream(f);
            } else {
                if( f.createNewFile() ) {
                    if( f.canWrite()) {
                        return new FileOutputStream(f);
                    } else {
                        System.err.println("Created a file, but now can't write to it!");
                        return null;
                    }
                }
            }
        } catch (IOException ex ) {
            ex.printStackTrace();
            return null;
        }
        return null;
    }
}
