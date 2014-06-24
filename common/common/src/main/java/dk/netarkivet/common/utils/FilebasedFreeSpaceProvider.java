
package dk.netarkivet.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * File Free Space Provider returns the number of bytes free out of a file.
 */

public class FilebasedFreeSpaceProvider implements FreeSpaceProvider {
    
    /** The error logger we notify about error messages on. */
    private Log log = LogFactory.getLog(getClass());

    /** The default place in classpath where the settings file can be found. */
    private static String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/common/utils/FilebasedFreeSpaceProvider.xml";

    /*
     * The static initialiser is called when the class is loaded.
     * It will add default values for all settings defined in this class, by
     * loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(
                DEFAULT_SETTINGS_CLASSPATH
        );
    }

    /** 
     * <b>settings.common.freespaceprovider.file</b>: <br>
     * The setting for filename of the free space information.
     */
    public static final String FREESPACEPROVIDER_DIR_SETTING
            = "settings.common.freespaceprovider.dir";
    
    /** The filename for reading out the free space infomation. */
    private static final String FREESPACEPROVIDER_DIR = Settings.get(
            FREESPACEPROVIDER_DIR_SETTING);
    
    /**
     * Returns the number of bytes free which is read out of a file
     * containing the bytes free information. This file is located 
     * in the FREESPACEPROVIDER_DIR and has the name as parameter f.
     * Will return 0 on any IO- or Format-Exceptions.
     *
     * @param f a given file
     * @return the number of bytes free.
     */
    public long getBytesFree(File f) {
        ArgumentNotValid.checkNotNull(f, "File f");

        BufferedReader reader = null;
        String content;
        long bytes = 0;
        File bytesFreeFile = new File(FREESPACEPROVIDER_DIR, f.getName());

        try {
           reader = new BufferedReader(new FileReader(bytesFreeFile));
           content = reader.readLine();     // only read first line
           bytes = Long.parseLong(content);
        } catch (Exception e) {
            log.warn("Exception while reading " 
                    + bytesFreeFile.getAbsolutePath() 
                    + ". The value 0 returned.");
            return 0;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("Unable to close FileReader");
                }
            }
        }
        
        return bytes;
    }
}
