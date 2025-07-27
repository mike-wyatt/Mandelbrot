import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.imaging.formats.png.PngWriter;
import org.apache.commons.imaging.formats.png.PngImagingParameters;
import org.apache.commons.imaging.palette.PaletteFactory;

public class Mandelbrot {

    public static void main(String[] args) {

        MandelbrotArgs mArgs = MandelbrotArgs.parseArgs(args);

        if(mArgs.parseErrors) {
            if(mArgs.debug) {
                System.err.println("Unable to parse argument string [" + String.join(" ", args) + "]");
            }
            System.err.println(mArgs.errorMsg);
            if(mArgs.printHelp) {
                System.out.println(mArgs.printUsage());
            }
            return;
        }

        //
        // Very naive iterative approach
        //
        double xPxIncrement = 1.0 / (double) mArgs.xResolution;
        double xIncrement = (mArgs.maxViewportX - mArgs.minViewportX) * xPxIncrement;
        double yPxIncrement = 1.0 / (double) mArgs.yResolution;
        double yIncrement = -((mArgs.maxViewportY - mArgs.minViewportY) * yPxIncrement);

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
        int[] colours = new int[256];
        for( int i = 0; i < 255; i++) {
            Color c = new Color(i/2,i/2,i);
            colours[i] = c.getRGB();
        }
        Color c = new Color(0,0,0);
        colours[255] = c.getRGB();

        //
        // Now generate
        //
        CountDownLatch latch = new CountDownLatch(mArgs.numThreads);
        int xPortion = mArgs.xResolution / mArgs.numThreads;

        long startTimeNano = System.nanoTime();

        for( int t = 0; t < mArgs.numThreads; t++ ) {
            int ulX = t * xPortion;
            int xRes = ulX + xPortion;
            if( xRes > mArgs.xResolution ) { xRes = mArgs.xResolution; }
            double minViewportX = mArgs.minViewportX + (t * xPortion * xIncrement);

            Calculator calc = new Calculator(img, ulX, 0, xRes, mArgs.yResolution,
                    minViewportX, mArgs.maxViewportY, xIncrement, yIncrement, mArgs.aaCycles, colours, latch );

            new Thread(calc).start();
        }


        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTimeNano = System.nanoTime();
        long timeMs = (endTimeNano - startTimeNano)/ 1000000;
        System.out.println("Calculated image in [" + timeMs + "] ms");

        try {
            PngWriter png = new PngWriter();
            png.writeImage(img, outputF, pngImagingParameters, new PaletteFactory());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    static class Calculator implements Runnable {

        private final BufferedImage img;
        private final int ulX;
        private final int ulY;
        private final int resX;
        private final int resY;
        private final double imgUlX;
        private final double imgUlY;
        private final double xIncrement;
        private final double yIncrement;
        private final int aaCycles;
        private final int colours[];
        private CountDownLatch latch;

        Calculator(BufferedImage img, int ulX, int ulY, int resX, int resY, double imgUlX, double imgUlY,
                   double xIncrement, double yIncrement, int aaCycles, int[] colours, CountDownLatch latch) {
            this.img = img;
            this.ulX = ulX;
            this.ulY = ulY;
            this.resX = resX;
            this.resY = resY;
            this.imgUlX = imgUlX;
            this.imgUlY = imgUlY;
            this.xIncrement = xIncrement;
            this.yIncrement = yIncrement;
            this.aaCycles = aaCycles;
            this.colours = colours;
            this.latch = latch;
        }

        @Override
        public void run() {
            //
            // Weird. The M1 has 4 "economy" and 4 "performance" cores. The first thread always seems to
            // be tied to an economy core, as it takes considerably longer than subsequent threads, even with
            // the priority increased from 5 to 10. Look into if there is a way to make this change.
            //
            Thread.currentThread().setPriority(10);
            System.out.println("Starting generation from [" + ulX + "," + ulY + "] in thread [" + Thread.currentThread().threadId() + "] with priority [" + Thread.currentThread().getPriority() + "]");

            long nanoStart = System.nanoTime();

            MandelbrotGenerator gen = new MandelbrotGenerator();
            int portionWidth = resX - ulX;
            int portionHeight = resY - ulY;

            int[] result = new int[portionWidth * portionHeight];

            int cacheHint = MandelbrotGenerator.CACHE_HINT_TOP_ROW;
            for( int y = ulY; y < resY; y++ ) {
                if( y == resY - 1 ) {
                    cacheHint = MandelbrotGenerator.CACHE_HINT_LAST_ROW;
                }

                for (int x = ulX; x < resX; x++) {
                    int portionX = x - ulX;
                    int portionY = y - ulY;

                    if( x == resX - 1 ) {
                        cacheHint |= MandelbrotGenerator.CACHE_HINT_LAST_COLUMN;
                    } else if( portionX == 0 ) {
                        cacheHint |= MandelbrotGenerator.CACHE_HINT_FIRST_COLUMN;
                    }

                    double X = imgUlX + ((double) portionX * xIncrement);
                    double Y = imgUlY + ((double) portionY * yIncrement);      // yIncrement is negative

                    MandelbrotGenerator.DataPoint p = gen.calculatePoint(X, Y, xIncrement, yIncrement, aaCycles, cacheHint);


                    int offsetY = portionY * portionWidth;
                    int offset = portionX + offsetY;

                    result[ offset ] = colours[p.rate];

                    // DEBUG:
                    // Real screen X
                    // Portion array X
                    // Image space X
                    // Real screen Y
                    // Portion array Y
                    // Image space Y
                    // Offset into portion array
                    // result value
//                    System.out.println(Thread.currentThread().threadId() + "," + x + "," + portionX + "," + X + "," +
//                            y + "," + portionY + "," + Y + "," + offset + "," + p.rate);

                    cacheHint &= ~MandelbrotGenerator.CACHE_HINT_FIRST_COLUMN;
                }
                cacheHint = 0;
            }

            long nanoEndGen = System.nanoTime();
            long nanoEndLock = 0;
            synchronized (img) {
                nanoEndLock = System.nanoTime();
                img.setRGB(ulX, ulY, portionWidth, portionHeight, result, 0, portionWidth  );
            }

            long nanoEnd = System.nanoTime();

            long genMs = (nanoEndGen - nanoStart) / 1000000;
            long lockMs = (nanoEndLock - nanoEndGen ) / 1000000;
            long imgSetMs = (nanoEnd - nanoEndLock) / 1000000;

            // TODO: PICKUP HERE
            // Why 2x or even 3x difference between compute times in identical threads?
            // Is the 1st section that takes the longest BUT not as long as to do the whole picture

            System.out.println("Finished generation in thread [" + Thread.currentThread().threadId() + "] - Generation [" +
                    genMs + "] ms - locking - [" + lockMs + "] ms - imgSet [" + imgSetMs + "] ms");


            // System.out.println(gen.getStats());

            latch.countDown();
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
