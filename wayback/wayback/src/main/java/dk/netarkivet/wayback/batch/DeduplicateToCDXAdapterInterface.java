package dk.netarkivet.wayback.batch;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface describing a class which can be used to convert duplicate records
 * in a crawl log to wayback-compatible cdx records.
 */
public interface DeduplicateToCDXAdapterInterface {

    /**
     * Takes a deduplicate line from a crawl log and converts it to a line in a
     * cdx file suitable for searching in wayback. The target url in the line is
     * canonicalized by this method. The type of canonicalization is determined 
     * by the default canonicalizer from the wayback settings.xml file.
     * If the input String is not a crawl-log duplicate line, null is returned.
     * 
     * @param line a line from a crawl log
     * @return a line for a cdx file or null if the input is not a duplicate
     * line
     */
    String adaptLine(String line);

    /**
     * Scans an input stream from a crawl log and converts all lines containing
     * deduplicate information 
     * to cdx records which it outputs to an output stream.
     * @param is the input stream
     * @param os the output stream
     */
    void adaptStream(InputStream is, OutputStream os);

}
