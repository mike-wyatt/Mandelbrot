public class MandelbrotArgs {

    //
    // Parse command line parameters and return to the main application
    //

    // Indicates if there were errors in the parsing process. Do not continue
    // processing if set to true
    boolean parseErrors = false;
    //
    // Plain text explanation of the first parse error detected. Only to be used if
    // parseErrors is set to true
    //
    String errorMsg = null;

    //
    // Flag to indicate if debug logging should be printed or not.
    //
    boolean debug = false;
    //
    // Indicates if the provided arguments suggest the user should see the usage information
    //
    boolean printHelp = false;

    //
    // Output file relative path
    //
    String outputFile = null;

    //
    // Output file format. For the moment PNG is the only supported type
    //
    String outputFormat = "PNG";

    //
    // Viewport boundaries within the Mandelbrot plane
    // Technically X represents the real component of the complex plane, while Y represents the
    // imaginary component.
    //
    double minViewportX = -2.0;
    double maxViewportY = 1.125;
    double maxViewportX = 1.0;
    double minViewportY = -1.125;

    //
    // Output image resolution.
    //
    int xResolution = 1024;
    int yResolution = 768;

    //
    // Anti-Aliasing
    //
    int aaCycles = 1;

    //
    // Threads
    //
    int numThreads = 1;

    private MandelbrotArgs() {
    }

    public String printUsage() {
        return
                """
                        Usage: java Mandelbrot [-hv] -o <outputFileName> [-f <outputfileFormat][-vp <ulx> <uly> <lrx> <lry>] [-r <x> <y>]
                        -h, --help\t\t\tDisplay this information and exit
                        -v, --verbose\t\t\tPrint debugging information
                        -o <outputFileName>\t\tRelative path and file name of the output image. Must be writable
                        -f <outputFileFormat>\t\tFormat of the output image. May be PNG (Default) only
                        -vp <ulx> <uly> <lrx> <lry>\tViewport coordinates, i.e. the X and Y locations of the Upper Left and
                        \t\t\t\t\tLower Right corners of the image in the Mandelbrot plane. Accepts decimal values
                        \t\t\t\t\tDefault values show the whole set (-2.0 1.125 1.0 - 1.125)
                        -r <x> <y>\t\t\tResolution of the output image as integer values X and Y. Defaults to 1024 x 768.
                        -aa <x>\t\t\tAnti-aliasing cycles. Positive Integer 1 (default) to as many as you like
                        -t <threads>\t\t\tNumber of threads to use. Positive Integer 1 (Default) to 256.
                        """;
    }

    static public MandelbrotArgs parseArgs(String[] args) {

        MandelbrotArgs result = new MandelbrotArgs();

        if( args == null || args.length == 0 ) {
            result.parseErrors = true;
            result.errorMsg = "No arguments provided";
            result.printHelp = true;
            return result;
        }

        for( int i = 0; i < args.length && result.parseErrors == false; i++ ) {
            String switchName = safeGetArg(args, i);
            if( switchName == null ) {
                // Should never happen!
                result.parseErrors = true;
                result.errorMsg = "Missing argument " + i;
                return result;
            }

            if( switchName.compareToIgnoreCase("-h") == 0 ) {
                result.printHelp = true;
            } else if( switchName.compareToIgnoreCase("--help") == 0 ) {
                result.printHelp = true;
            } else if( switchName.compareToIgnoreCase("-v") == 0 ) {
                result.debug = true;
            } else if( switchName.compareToIgnoreCase("-verbose") == 0 ) {
                result.debug = true;
            } else if( switchName.compareToIgnoreCase("-o") == 0 ) {
                String outputFileName = safeGetArg(args, ++i);
                result.outputFile = outputFileName;
            } else if( switchName.compareToIgnoreCase( "-f") == 0 ) {
                String outputFileFormat = safeGetArg(args, ++i);
                result.outputFormat = outputFileFormat;
            } else if( switchName.compareToIgnoreCase( "-vp") == 0 ) {
                String vpUlXStr = safeGetArg(args, ++i);

                if( vpUlXStr == null ) {
                    result.parseErrors = true;
                    result.errorMsg = "Missing upper left X co-ordinate for the viewport.";
                } else {
                    try {
                        result.minViewportX = Double.parseDouble(vpUlXStr);
                    } catch ( NumberFormatException x ) {
                        result.parseErrors = true;
                        result.errorMsg = "Upper left X co-ordinate for the viewport is not a valid decimal number.";
                    }
                }

                String vpUlYStr = safeGetArg(args, ++i);

                if( vpUlYStr == null ) {
                    result.parseErrors = true;
                    result.errorMsg = "Missing upper left Y co-ordinate for the viewport.";
                } else {
                    try {
                        result.maxViewportY = Double.parseDouble(vpUlYStr);
                    } catch ( NumberFormatException x ) {
                        result.parseErrors = true;
                        result.errorMsg = "Upper left Y co-ordinate for the viewport is not a valid decimal number.";
                    }
                }

                String vpLRXStr = safeGetArg(args, ++i);

                if( vpLRXStr == null ) {
                    result.parseErrors = true;
                    result.errorMsg = "Missing lower right X co-ordinate for the viewport.";
                } else {
                    try {
                        result.maxViewportX = Double.parseDouble(vpLRXStr);
                    } catch ( NumberFormatException x ) {
                        result.parseErrors = true;
                        result.errorMsg = "Lower right X co-ordinate for the viewport is not a valid decimal number.";
                    }
                }

                String vpLRYStr = safeGetArg(args, ++i);

                if( vpLRYStr == null ) {
                    result.parseErrors = true;
                    result.errorMsg = "Missing lower right Y co-ordinate for the viewport.";
                } else {
                    try {
                        result.minViewportY = Double.parseDouble(vpLRYStr);
                    } catch ( NumberFormatException x ) {
                        result.parseErrors = true;
                        result.errorMsg = "Lower right Y co-ordinate for the viewport is not a valid decimal number.";
                    }
                }
            } else if( switchName.compareToIgnoreCase( "-r") == 0 ) {
                String resXStr = safeGetArg(args, ++i);

                if (resXStr == null) {
                    result.parseErrors = true;
                    result.errorMsg = "Missing X resolution for output image.";
                } else {
                    try {
                        result.xResolution = Integer.parseInt(resXStr);
                    } catch (NumberFormatException x) {
                        result.parseErrors = true;
                        result.errorMsg = "X resolution for output image is not a valid integer number.";
                    }
                }

                String resYStr = safeGetArg(args, ++i);

                if (resYStr == null) {
                    result.parseErrors = true;
                    result.errorMsg = "Missing Y resolution for output image.";
                } else {
                    try {
                        result.yResolution = Integer.parseInt(resYStr);
                    } catch (NumberFormatException x) {
                        result.parseErrors = true;
                        result.errorMsg = "Y resolution for output image is not a valid integer number.";
                    }
                }
            } else if( switchName.compareToIgnoreCase( "-aa") == 0 ) {
                String aaStr = safeGetArg(args, ++i);

                if (aaStr == null) {
                    result.parseErrors = true;
                    result.errorMsg = "Missing Anti-Alias value.";
                } else {
                    try {
                        result.aaCycles = Integer.parseInt(aaStr);
                        // TODO: Check for positive integers
                    } catch (NumberFormatException x) {
                        result.parseErrors = true;
                        result.errorMsg = "Anti-alias is not a valid integer number.";
                    }
                }
            } else if( switchName.compareToIgnoreCase( "-t") == 0 ) {
                String tStr = safeGetArg(args, ++i);

                if (tStr == null) {
                    result.parseErrors = true;
                    result.errorMsg = "Missing Thread Count value.";
                } else {
                    try {
                        result.numThreads = Integer.parseInt(tStr);
                        // TODO: Check for integers [1 .. 256]
                    } catch (NumberFormatException x) {
                        result.parseErrors = true;
                        result.errorMsg = "Thread Count is not a valid integer number.";
                    }
                }
            } else {
                result.parseErrors = true;
                result.errorMsg = "Unrecognised parameter " + switchName;
                result.printHelp = true;
            }

        }

        if( result.outputFile == null ) {
            result.parseErrors = true;
            result.errorMsg = "No output file provided.";
        }

        return result;
    }

    static private String safeGetArg(String[] args, int index ) {
        if( args.length <= index ) {
            return null;
        } else {
            return args[ index ];
        }
    }
}
