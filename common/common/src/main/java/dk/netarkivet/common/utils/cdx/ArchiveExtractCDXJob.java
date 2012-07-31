package dk.netarkivet.common.utils.cdx;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.io.arc.ARCRecord;
import org.jwat.common.ByteCountingPushBackInputStream;
import org.jwat.common.ContentType;
import org.jwat.common.HttpHeader;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.archive.ArchiveBatchJob;
import dk.netarkivet.common.utils.archive.ArchiveHeaderBase;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;
import dk.netarkivet.common.utils.batch.ArchiveBatchFilter;

/** Batch job that extracts information to create a CDX file.
 *
 * A CDX file contains sorted lines of metadata from the ARC/WARC files, with
 * each line followed by the file and offset the record was found at, and
 * optionally a checksum.
 * The timeout of this job is 7 days.
 * See http://www.archive.org/web/researcher/cdx_file_format.php
 */
public class ArchiveExtractCDXJob extends ArchiveBatchJob {

	/**
	 * UID.
	 */
	private static final long serialVersionUID = -5136098924912556708L;

	/** An encoding for the standard included metadata fields without
     * checksum.*/
    private static final String[] STD_FIELDS_EXCL_CHECKSUM = {
            "A", "e", "b", "m", "n", "g", "v"
        };

    /** An encoding for the standard included metadata fields with checksum. */
    private static final String[] STD_FIELDS_INCL_CHECKSUM = {
            "A", "e", "b", "m", "n", "g", "v", "c"
        };

    /** The fields to be included in CDX output. */
    private String[] fields;

    /** True if we put an MD5 in each CDX line as well. */
    private boolean includeChecksum;

    /**
     * Logger for this class.
     */
    private static final Log log = LogFactory.getLog(ArchiveExtractCDXJob.class.getName());

    /**
     * Constructs a new job for extracting CDX indexes.
     * @param includeChecksum If true, an MD5 checksum is also
     * written for each record. If false, it is not.
     */
    public ArchiveExtractCDXJob(boolean includeChecksum) {
        this.fields = includeChecksum ? STD_FIELDS_INCL_CHECKSUM
                                      : STD_FIELDS_EXCL_CHECKSUM;
        this.includeChecksum = includeChecksum;
        batchJobTimeout = 7*Constants.ONE_DAY_IN_MILLIES;
    }

    /**
     * Equivalent to ExtractCDXJob(true).
     */
    public ArchiveExtractCDXJob() {
        this(true);
    }

    /** Filter out the filedesc: headers.
     * @see dk.netarkivet.common.utils.arc.ARCBatchJob#getFilter()
     * @return The filter that defines what ARC records are wanted
     * in the output CDX file.
     */
    @Override
    public ArchiveBatchFilter getFilter() {
        //Per default we want to index all records except ARC file headers:
        //return ArchiveBatchFilter.EXCLUDE_FILE_HEADERS;
        return ArchiveBatchFilter.NO_FILTER;

    }

    /**
     * Initialize any data needed (none).
     * @see dk.netarkivet.common.utils.arc.ARCBatchJob#initialize(OutputStream)
     */
    @Override
    public void initialize(OutputStream os) {
    }

    /** Process this entry, reading metadata into the output stream.
     * @see dk.netarkivet.common.utils.arc.ARCBatchJob#processRecord(
     * ARCRecord, OutputStream)
     * @throws IOFailure on trouble reading arc record data
     */
    @Override
    public void processRecord(ArchiveRecordBase record, OutputStream os) {
        log.trace("Processing ARCRecord with offset: "
                + record.getHeader().getOffset());
        /*
        * Fields are stored in a map so that it's easy
        * to pull them out when looking at the
        * fieldarray.
        */
        ArchiveHeaderBase header = record.getHeader();
        Map<String, String> fieldsread = new HashMap<String,String>();
        fieldsread.put("A", header.getUrl());
        fieldsread.put("e", header.getIp());
        fieldsread.put("b", header.getDate());
        fieldsread.put("n", Long.toString(header.getLength()));
        fieldsread.put("g", record.getHeader().getArchiveFile().getName());
        fieldsread.put("v", Long.toString(record.getHeader().getOffset())); 

        String mimeType = header.getMimetype();
        String msgType;
        ContentType contentType = ContentType.parseContentType(mimeType);
        boolean bResponse = false;
        if (contentType != null) {
        	if ("application".equals(contentType.contentType)
        			&& "http".equals(contentType.mediaType)) {
        		msgType = contentType.getParameter("msgtype");
        		if ("response".equals(msgType)) {
        			bResponse = true;
        		} else if ("request".equals(msgType)) {
        		}
        	}
        	mimeType = contentType.toStringShort();
        }
        ByteCountingPushBackInputStream pbin = new ByteCountingPushBackInputStream(record.getInputStream(), 8192);
        HttpHeader httpResponse = null;
        if (bResponse) {
            try {
    			httpResponse = HttpHeader.processPayload(HttpHeader.HT_RESPONSE, 
    			        pbin, header.getLength(), null);
    			if (httpResponse != null && httpResponse.contentType != null) {
    				contentType = ContentType.parseContentType(httpResponse.contentType);
    		        if (contentType != null) {
    		        	mimeType = contentType.toStringShort();
    		        }
    			}
    		} catch (IOException e) {
                throw new IOFailure("Error reading httpresponse header", e);
    		}
        }
        fieldsread.put("m", mimeType);

        /* Only include checksum if necessary: */
        if (includeChecksum) {
            // To avoid taking all of the record into an array, we
            // slurp it directly from the ARCRecord.  This leaves the
            // sar in an inconsistent state, so it must not be used
            // afterwards.
            //InputStream instream = sar; //Note: ARCRecord extends InputStream
            //fieldsread.put("c", MD5.generateMD5(instream));
            fieldsread.put("c", MD5.generateMD5(pbin));
        }

        if (httpResponse != null) {
        	try {
				httpResponse.close();
			} catch (IOException e) {
                throw new IOFailure("Error closing httpresponse header", e);
			}
        }

        printFields(fieldsread, os);
    }

    /** End of the batch job.
     * @see dk.netarkivet.common.utils.arc.ARCBatchJob#finish(OutputStream)
     */
    @Override
    public void finish(OutputStream os) {
    }

    /** Print the values found for a set of fields.  Prints the '-'
     * character for any null values.
     *
     * @param fieldsread A hashtable of values indexed by field letters
     * @param outstream The outputstream to write the values to 
     */
    private void printFields(Map<String, String> fieldsread, OutputStream outstream) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < fields.length; i++) {
            Object o = fieldsread.get(fields[i]);
            sb.append((i > 0) ? " " : "");
            sb.append((o == null) ? "-" : o.toString());
        }
        sb.append("\n");
        try {
            outstream.write(sb.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            throw new IOFailure("Error writing CDX line '"
                    + sb + "' to batch outstream", e);
        }
    }

    /**
     * @return Humanly readable description of this instance.
     */
    public String toString() {
        return getClass().getName() + ", with Filter: " + getFilter()
                + ", include checksum = " + includeChecksum;
    }

}
