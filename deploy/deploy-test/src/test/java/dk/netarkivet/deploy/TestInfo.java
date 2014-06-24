package dk.netarkivet.deploy;

import java.io.File;

public class TestInfo {
    // directories
    public static final File DATA_DIR 
    = new File("tests/dk/netarkivet/deploy/data");
    public static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    public static final File WORKING_DIR = new File(DATA_DIR, "working");
    public static final File TMPDIR = new File(WORKING_DIR, "tmpdir");
    public static final File TARGETDIR = new File(WORKING_DIR, "target");
    public static final File SINGLE_TARGET_DIR = new File(
	    WORKING_DIR, "single_target");
    public static final File DATABASE_TARGET_DIR = new File(
	    WORKING_DIR, "database_target");
    public static final File TEST_TARGET_DIR = new File(
	    WORKING_DIR, "test_target");
    
    // argument files
    public static final File IT_CONF_FILE  = new File(
	    WORKING_DIR, "deploy_config.xml");
    public static final File IT_CONF_SINGLE_FILE = new File(
	    WORKING_DIR, "deploy_single_config.xml");
    public static final File IT_CONF_DATABASE_FILE = new File(
	    WORKING_DIR, "deploy_database_config.xml");
    public static final File FILE_NETATCHIVE_SUITE = new File(
            WORKING_DIR, "null.zip");
    public static final File FILE_SECURITY_POLICY = new File(
	    WORKING_DIR, "security.policy");
    public static final File FILE_LOG_PROP = new File(
	    WORKING_DIR, "log.prop");
    public static final File FILE_DATABASE = new File(
	    WORKING_DIR, "database.jar");
    public static final File FILE_BP_DATABASE = new File(
            WORKING_DIR, "bpdb.jar");
    public static final File EXTERNALS_DIR = new File(WORKING_DIR, "externals");

    // arguments
    public static final String ARGUMENT_CONFIG_FILE = 
	Constants.ARG_INIT_ARG + Constants.ARG_CONFIG_FILE;
    public static final String ARGUMENT_NETARCHIVE_SUITE_FILE = 
	Constants.ARG_INIT_ARG + Constants.ARG_NETARCHIVE_SUITE_FILE;
    public static final String ARGUMENT_SECURITY_FILE = 
	Constants.ARG_INIT_ARG + Constants.ARG_SECURITY_FILE;
    public static final String ARGUMENT_LOG_PROPERTY_FILE = 
	Constants.ARG_INIT_ARG + Constants.ARG_LOG_PROPERTY_FILE;
    public static final String ARGUMENT_OUTPUT_DIRECTORY = 
	Constants.ARG_INIT_ARG + Constants.ARG_OUTPUT_DIRECTORY;
    public static final String ARGUMENT_HARVEST_DATABASE_FILE = 
	Constants.ARG_INIT_ARG + Constants.ARG_DATABASE_FILE;
    public static final String ARGUMENT_ARCHIVE_DATABASE_FILE = 
        Constants.ARG_INIT_ARG + Constants.ARG_ARC_DB;
    public static final String ARGUMENT_TEST = 
	Constants.ARG_INIT_ARG + Constants.ARG_TEST;
    public static final String ARGUMENT_EVALUATE = 
	Constants.ARG_INIT_ARG + Constants.ARG_EVALUATE;
    public static final String ARGUMENT_JAR_FOLDER = 
        Constants.ARG_INIT_ARG + Constants.ARG_JAR_FOLDER;
    public static final String ARGUMENT_TEST_ARG = "1000,1005,test,test@kb.dk";
}
