/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.webinterface;

/**
 * Harvester webinterface constants.
 */
public class Constants {

    /**
     * The constructor for this class. Making the constructor private prevents the class from being instantiated.
     */
    private Constants() {
    }

    /** Names of various parameters used in the webinterface. */
    public static final String JOBSTATUS_PARAM = HarvestStatusQuery.UI_FIELD.JOB_STATUS.name();

    public static final String JOBIDORDER_PARAM = "jobidorder";

    public static final String DOMAIN_SEARCH_PARAM = "domainName";

    // public static final String DOMAIN

    public static final String HARVEST_ID_PARAM = HarvestStatusQuery.UI_FIELD.HARVEST_ID.name();

    public static final String HARVEST_NUM_PARAM = HarvestStatusQuery.UI_FIELD.HARVEST_RUN.name();

    public static final String EDIT_CONFIG_PARAM = "editConfig";

    public static final String DEFAULT_PARAM = "default";

    public static final String EDIT_SEEDLIST_PARAM = "editSeedList";

    public static final String SEEDLIST_NAME_PARAM = "seedListName";

    public static final String CRAWLERTRAPS_PARAM = "crawlerTraps";

    public static final String SEED_LIST_PARAMETER = "seedList";

    public static final String INDEXLABEL_PARAM = "indexLabel";

    public static final String SCHEDULE_PARAM = "schedulename";

    public static final String HARVEST_PARAM = "harvestname";

    public static final String HARVEST_OLD_PARAM = "harvestoldname";

    public static final String HARVEST_ID = "harvestid";

    public static final String COLUMN_PARAM = "column";

    public static final String SORT_FIELD_PARAM = "sort_field";
    public static final String SORT_ORDER_PARAM = "sort_order";
    public static final String SORT_ORDER_ASC = "asc";
    public static final String SORT_ORDER_DESC = "desc";

    public static final String FROM_FILE_PARAM = "fromFile";

    public static final String DOMAINLIST_PARAM = "domainlist";

    public static final String OLDSNAPSHOT_PARAM = "old_snapshot_name";

    public static final String DOMAIN_OBJECTLIMIT_PARAM = "snapshot_object_limit";

    public static final String DOMAIN_BYTELIMIT_PARAM = "snapshot_byte_limit";

    public static final String JOB_TIMELIMIT_PARAM = "snapshot_time_limit";

    public static final String CREATENEW_PARAM = "createnew";

    public static final String UPDATE_PARAM = "update";
    
    public static final String ADD_SEEDS_PARAM = "addSeeds"; 

    public static final String ADDDOMAINS_PARAM = "addDomains";

    public static final String SAVE_PARAM = "save";

    public static final String COMMENTS_PARAM = "comments";

    public static final String AUDIENCE_PARAM = "audience";

    public static final String NEXTDATE_PARAM = "nextdate";

    public static final String NEXTDATE_SUBMIT = "setnextdate";

    public static final String DELETEDOMAIN_PARAM = "deletedomain";

    public static final String DELETECONFIG_PARAM = "deleteconfig";
    
    public static final String DELETEALL_CONFIGS_PARAM = "deleteallconfigs";

    public static final String EDITION_PARAM = "edition";

    public static final String UNKNOWN_DOMAINS_PARAM = "unknownDomains";

    public static final String DOMAIN_PARAM = "name";

    public static final String CONFIG_NAME_PARAM = "configName";
    
    public static final String CONFIG_OLDNAME_PARAM = "configOldName";

    public static final String ORDER_XML_NAME_PARAM = "order_xml";

    public static final String MAX_RATE_PARAM = "maxRate";

    public static final String MAX_OBJECTS_PARAM = "maxObjects";

    public static final String MAX_BYTES_PARAM = "maxBytes";

    public static final String FLIPACTIVE_PARAM = "flipactive";

    public static final String SHOW_INACTIVE_PARAM = "showInactive";

    public static final String SHOW_UNUSED_CONFIGURATIONS_PARAM = "showUnusedConfigurations";

    public static final String SHOW_UNUSED_SEEDS_PARAM = "showUnusedSeeds";

    public static final String SEEDLIST_LIST_PARAM = "seedListList";

    public static final String JOB_PARAM = "jobID";

    public static final String JOB_RESUBMIT_PARAM = "resubmit";

    public static final String JOB_REJECT_PARAM = "reject";

    public static final String JOB_UNREJECT_PARAM = "unreject";

    public static final String SEEDS_PARAM = "seeds";

    public static final String ORDER_TEMPLATE_PARAM = "orderTemplate";

    public static final String ALIAS_PARAM = "alias";

    public static final String RENEW_ALIAS_PARAM = "renewAlias";

    public static final String UPLOAD_FILE_PARAM = "upload_file";

    public static final String IS_NEWEST_FIRST = "is_newest_first";

    public static final String DESCENDING = "DESC";
    public static final String ASCENDING = "ASC";

    public static final String FALSE = "false";
    public static final String TRUE = "true";

    /**
     * Names of some parameters used in management of global crawler traps.
     */
    public static final String TRAP_ID = "trap_id";

    public static final String TRAP_ACTION = "trap_action";

    public static final String TRAP_CREATE = "trap_create";

    public static final String TRAP_DOWNLOAD = "trap_download";

    public static final String TRAP_ACTIVATE = "trap_activate";

    public static final String TRAP_DEACTIVATE = "trap_deactivate";

    public static final String TRAP_CONTENT_TYPE = "trap_content_type";

    public static final String TRAP_NAME = "trap_name";

    public static final String TRAP_IS_ACTIVE = "trap_is_active";

    public static final String TRAP_FILENAME = "trap_filename";

    public static final String TRAP_DESCRIPTION = "trap_description";

    /**
     * Names of the two directories for the sitesections belonging to the harvester package, and the directory belonging
     * to the viewerproxy package.
     */
    public static final String DEFINITIONS_SITESECTION_DIRNAME = "HarvestDefinition";

    public static final String HISTORY_SITESECTION_DIRNAME = "History";

    public static final String QA_SITESECTION_DIRNAME = "QA";

    /**
     * The maximum length of a seed before it is truncated before showing it.
     */
    public static final int MAX_SHOWN_SIZE_OF_URL = 40;

    /** Regexp for checking if a seed starts with a protocol. */
    public static final String PROTOCOL_REGEXP = "^[a-zA-Z]+:.*";

    /** Fields used in our calendar functionality. */
    public static final String END_TIME_FIELD = "endTimeField";

    public static final String HOW_OFTEN_FIELD = "howOftenField";

    /**
     * This constant is used as a prefix to identify a request parameter as a domain/configuration pair. Ie one sets
     * such a pair as DOMAIN_IDENTIFIER<domainname>=<configname>
     */
    public static final String DOMAIN_IDENTIFIER = "domain_config_pair_";

    /**
     * Extension used for XML files, including '.' separator.
     */
    public static final String XML_EXTENSION = ".xml";

    /** An edition that will never occur in existing DAO-controlled objects. */
    public static final long NO_EDITION = 1L;

    /**
     * String constant to denote: No next date.
     */
    public static final String NoNextDate = "-";

    /** The size of field for a domain name. */
    public static final int DOMAIN_NAME_FIELD_SIZE = 40;

    /**
     * The number of columns when showing the crawlertraps associated with a domain.
     */
    public static final int CRAWLERTRAPS_COLUMNS = 60;

    /**
     * The number of rows when showing the crawlertraps associated with a domain.
     */
    public static final int CRAWLERTRAPS_ROWS = 20;

    /**
     * The width of the field for the upload file.
     */
    public static final int UPLOAD_FILE_FIELD_WIDTH = 60;

    /** The width of the harvest template name. */
    public static final int TEMPLATE_NAME_WIDTH = 30;

    /** Optional argument for which page of the searchresult to show. */
    public static final String START_PAGE_PARAMETER = "START_PAGE_INDEX";

    /**
     * Domain query type parameter. Used in the jsp-page Definitions-find-domains.jsp.
     */
    public static final String DOMAIN_QUERY_TYPE_PARAM = "DOMAIN_QUERY_TYPE";

    /**
     * Domain search key parameter. Used in the jsp-page Definitions-find-domains.jsp.
     */
    public static final String DOMAIN_QUERY_STRING_PARAM = "DOMAIN_QUERY_STRING";

    public static final String TRAPS_DOMAIN_SEARCH = "crawlertraps";

    public static final String NAME_DOMAIN_SEARCH = "name";

    public static final String COMMENTS_DOMAIN_SEARCH = "comments";

    /**
     * The default domain search type is name.
     */
    public static final String DEFAULT_DOMAIN_SEARCH_TYPE = NAME_DOMAIN_SEARCH;

    public static final String JOB_ORDERING_BY_STARTDATE_PARAM = "ORDERING_BY_STARTDATE";
    
    /** Frontier report mode to display all queues or not  */
    public static final String FRONTIER_REPORT_MODE="frontierReportMode";

	
}
