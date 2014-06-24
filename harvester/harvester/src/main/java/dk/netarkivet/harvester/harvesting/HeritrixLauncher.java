package dk.netarkivet.harvester.harvesting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;

/**
 * A HeritrixLauncher object wraps around an instance of the web crawler
 * Heritrix. The object is constructed with the necessary information to do a
 * crawl. The crawl is performed when doOneCrawl() is called. doOneCrawl()
 * monitors progress and returns when the crawl is finished or must be stopped
 * because it has stalled.
 */
public abstract class HeritrixLauncher {
    /** Class encapsulating placement of various files. */
    private HeritrixFiles files;

    /** the arguments passed to the HeritricController constructor. */
    private Object[] args;

    /**
     * The period to wait in seconds before checking if Heritrix has done
     * anything.
     */
    protected static final int CRAWL_CONTROL_WAIT_PERIOD 
        = Settings.getInt(HarvesterSettings.CRAWL_LOOP_WAIT_TIME);

    
    /** The class logger. */
    final Log log = LogFactory.getLog(getClass());
    
    
    /**
     * Private HeritrixLauncher constructor. Sets up the HeritrixLauncher from
     * the given order file and seedsfile.
     *
     * @param files Object encapsulating location of Heritrix crawldir and
     *              configuration files.
     *
     * @throws ArgumentNotValid If either seedsfile or orderfile does not
     *                          exist.
     */
    protected HeritrixLauncher(HeritrixFiles files) throws ArgumentNotValid {
        if (!files.getOrderXmlFile().isFile()) {
            throw new ArgumentNotValid(
                    "File '" + files.getOrderXmlFile().getName()
                    + "' must exist in order for Heritrix to run. "
                    + "This filepath does not refer to existing file: "
                    + files.getOrderXmlFile().getAbsolutePath());
        }
        if (!files.getSeedsTxtFile().isFile()) {
            throw new ArgumentNotValid(
                    "File '" + files.getSeedsTxtFile().getName()
                    + "' must exist in order for Heritrix to run. "
                    + "This filepath does not refer to existing file: "
                    + files.getSeedsTxtFile().getAbsolutePath());
        }
        this.files = files;
        this.args = new Object[]{files};
    }

    /**
     * Generic constructor to allow HeritrixLauncher to use any implementation
     * of HeritrixController.
     *
     * @param args the arguments to be passed to the constructor or non-static
     *             factory method of the HeritrixController class specified in
     *             settings
     */
    public HeritrixLauncher(Object... args) {
        this.args = args;
    }

    /**
     * Launches the crawl and monitors its progress.
     * @throws IOFailure
     */
    public abstract void doCrawl() throws IOFailure;

        
    /**
     * @return an instance of the wrapper class for Heritrix files.
     */
    protected HeritrixFiles getHeritrixFiles() {
        return files;
    }
    
    /**
     * @return the optional arguments used to initialize 
     * the chosen Heritrix controller implementation.
     */
    protected Object[] getControllerArguments() {
        return args;
    }
    
    public void setupOrderfile(HeritrixFiles files) {
        HeritrixTemplate.makeOrderfileReadyForHeritrix(files);
    }

}
