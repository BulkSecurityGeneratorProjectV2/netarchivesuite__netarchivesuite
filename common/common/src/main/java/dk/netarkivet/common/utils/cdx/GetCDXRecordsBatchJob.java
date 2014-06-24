
package dk.netarkivet.common.utils.cdx;

import java.util.regex.Pattern;
import java.io.OutputStream;
import java.io.IOException;

import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Job to get cdx records out of metadata files.
 *
 */
@SuppressWarnings({ "serial"})
public class GetCDXRecordsBatchJob extends ARCBatchJob {
    
    /** The URL pattern used to retrieve the CDX-records. */ 
    private final Pattern URLMatcher;
    /** The MIME pattern used to retrieve the CDX-records. */
    private final Pattern mimeMatcher;

    /**
     * Constructor.
     */
    public GetCDXRecordsBatchJob() {
        URLMatcher = Pattern.compile(Constants.ALL_PATTERN);
        mimeMatcher = Pattern.compile(Constants.CDX_MIME_PATTERN);
        batchJobTimeout = 7 * Constants.ONE_DAY_IN_MILLIES;
    }

    /**
     * Initialize job. Does nothing
     * @param os The output stream (unused in this implementation)
     */
    public void initialize(OutputStream os) {
    }

    /**
     * Process a single ARCRecord if the record contains cdx.
     * @param sar The record we want to process
     * @param os The output stream to write the result to
     */
    public void processRecord(ARCRecord sar, OutputStream os) {
        if (URLMatcher.matcher(sar.getMetaData().getUrl()).matches()
            && mimeMatcher.matcher(sar.getMetaData().getMimetype()).matches()) {
            try {
                try {
                    byte[] buf = new byte[Constants.IO_BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = sar.read(buf)) != -1) {
                        os.write(buf, 0, bytesRead);
                    }
                } finally {
                    //TODO Should we close ARCRecord here???
                    //if (is != null) {
                    //    is.close();
                    //}
                }
            } catch (IOException e) {
                String message = "Error writing body of ARC entry '"
                                 + sar.getMetaData().getArcFile() + "' offset '"
                                 + sar.getMetaData().getOffset() + "'";
                throw new IOFailure(message, e);
            }
        }
    }

    /**
     * Finish job. Does nothing
     * @param os The Outputstream (unused in this implementation)
     */
    public void finish(OutputStream os) {
    }
}
