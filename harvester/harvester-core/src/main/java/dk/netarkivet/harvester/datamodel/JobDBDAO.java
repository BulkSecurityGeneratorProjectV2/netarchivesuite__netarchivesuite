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

package dk.netarkivet.harvester.datamodel;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.webinterface.HarvestStatus;
import dk.netarkivet.harvester.webinterface.HarvestStatusQuery;
import dk.netarkivet.harvester.webinterface.HarvestStatusQuery.SORT_ORDER;

/**
 * A database-based implementation of the JobDAO class. The statements to create the tables are now in
 * scripts/sql/createfullhddb.sql
 */
public class JobDBDAO extends JobDAO {

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(JobDBDAO.class);

    /**
     * Create a new JobDAO implemented using database. This constructor also tries to upgrade the jobs and jobs_configs
     * tables in the current database. throws and IllegalState exception, if it is impossible to make the necessary
     * updates.
     */
    protected JobDBDAO() {
        Connection connection = HarvestDBConnection.get();
        try {
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.JOBS);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.JOBCONFIGS);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Creates an instance in persistent storage of the given job. 
     * If the job doesn't have an ID (which it shouldn't at this point, one is generated for it.
     * After that the harvestnamePrefix is set. Both existing harvestnameprefix factory-classes depends on
     * the JobID being set before being called. 
     *
     * @param job a given job to add to persistent storage
     * @throws PermissionDenied If a job already exists in persistent storage with the same id as the given job
     * @throws IOFailure If some IOException occurs while writing the job to persistent storage
     */
    public synchronized void create(Job job) {
        ArgumentNotValid.checkNotNull(job, "Job job");
        // Check that job.getOrigHarvestDefinitionID() refers to existing harvestdefinition.
        Long harvestId = job.getOrigHarvestDefinitionID();
        if (!HarvestDefinitionDAO.getInstance().exists(harvestId)) {
            throw new UnknownID("No harvestdefinition with ID=" + harvestId);
        }

        Connection connection = HarvestDBConnection.get();
        if (job.getJobID() != null) {
            log.warn("The jobId for the job is already set. This should probably never happen.");
        } else {
            job.setJobID(generateNextID(connection));
        }
        // Set the harvestNamePrefix. Every current implementation depends on the JobID being set before
        // being initialized.
        job.setDefaultHarvestNamePrefix();
        
        
        if (job.getCreationDate() != null) {
            log.warn("The creation time for the job is already set. This should probably never happen.");
        } else {
            job.setCreationDate(new Date());
        }

        log.debug("Creating " + job.toString());

        PreparedStatement statement = null;
        try {
            connection.setAutoCommit(false);
            statement = connection.prepareStatement("INSERT INTO jobs "
                    + "(job_id, harvest_id, status, channel, forcemaxcount, "
                    + "forcemaxbytes, forcemaxrunningtime, orderxml, " + "orderxmldoc, seedlist, "
                    + "harvest_num, startdate, enddate, submitteddate, creationdate, "
                    + "num_configs, edition, resubmitted_as_job, harvestname_prefix, snapshot) "
                    + "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," + "?, ?, ?, ?, ?, ?)");

            statement.setLong(1, job.getJobID());
            statement.setLong(2, job.getOrigHarvestDefinitionID());
            statement.setInt(3, job.getStatus().ordinal());
            statement.setString(4, job.getChannel());
            statement.setLong(5, job.getForceMaxObjectsPerDomain());
            statement.setLong(6, job.getMaxBytesPerDomain());
            statement.setLong(7, job.getMaxJobRunningTime());
            DBUtils.setStringMaxLength(statement, 8, job.getOrderXMLName(), Constants.MAX_NAME_SIZE, job,
                    "order.xml name");
            final String orderString = job.getOrderXMLdoc().getXML();
            DBUtils.setClobMaxLength(statement, 9, orderString, Constants.MAX_ORDERXML_SIZE, job, "order.xml");
            DBUtils.setClobMaxLength(statement, 10, job.getSeedListAsString(), Constants.MAX_COMBINED_SEED_LIST_SIZE,
                    job, "seedlist");
            statement.setInt(11, job.getHarvestNum());
            DBUtils.setDateMaybeNull(statement, 12, job.getActualStart());
            DBUtils.setDateMaybeNull(statement, 13, job.getActualStop());
            DBUtils.setDateMaybeNull(statement, 14, job.getSubmittedDate());
            DBUtils.setDateMaybeNull(statement, 15, job.getCreationDate());

            // The size of the configuration map == number of configurations
            statement.setInt(16, job.getDomainConfigurationMap().size());
            long initialEdition = 1;
            statement.setLong(17, initialEdition);
            DBUtils.setLongMaybeNull(statement, 18, job.getResubmittedAsJob());
            statement.setString(19, job.getHarvestFilenamePrefix());
            statement.setBoolean(20, job.isSnapshot());
            statement.executeUpdate();
            createJobConfigsEntries(connection, job);
            connection.commit();
            job.setEdition(initialEdition);
        } catch (SQLException e) {
            String message = "SQL error creating job " + job + " in database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.rollbackIfNeeded(connection, "create job", job);
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Create the entries in the job_configs table for this job. Since some jobs have up to 10000 configs, this must be
     * optimized. The entries are only created, if job.configsChanged is true.
     *
     * @param dbconnection A connection to work on
     * @param job The job to store entries for
     * @throws SQLException If any problems occur during creation of the new entries in the job_configs table.
     */
    private void createJobConfigsEntries(Connection dbconnection, Job job) throws SQLException {
        if (job.configsChanged) {
            PreparedStatement statement = null;
            String tmpTable = null;
            Long jobID = job.getJobID();
            try {
                statement = dbconnection.prepareStatement("DELETE FROM job_configs WHERE job_id = ?");
                statement.setLong(1, jobID);
                statement.executeUpdate();
                statement.close();
                tmpTable = DBSpecifics.getInstance().getJobConfigsTmpTable(dbconnection);
                final Map<String, String> domainConfigurationMap = job.getDomainConfigurationMap();
                statement = dbconnection.prepareStatement("INSERT INTO " + tmpTable
                        + " ( domain_name, config_name ) VALUES ( ?, ?)");
                for (Map.Entry<String, String> entry : domainConfigurationMap.entrySet()) {
                    statement.setString(1, entry.getKey());
                    statement.setString(2, entry.getValue());
                    statement.executeUpdate();
                    statement.clearParameters();
                }
                statement.close();
                // Now we have a temp table with all the domains and configs
                statement = dbconnection.prepareStatement("INSERT INTO job_configs " + "( job_id, config_id ) "
                        + "SELECT ?, configurations.config_id " + "  FROM domains, configurations, " + tmpTable
                        + " WHERE domains.name = " + tmpTable + ".domain_name"
                        + "   AND domains.domain_id = configurations.domain_id"
                        + "   AND configurations.name = " + tmpTable + ".config_name"
                );
                statement.setLong(1, jobID);
                int rows = statement.executeUpdate();
                if (rows != domainConfigurationMap.size()) {
                    log.debug("Domain or configuration in table for {} missing: Should have {}, got {}", job,
                            domainConfigurationMap.size(), rows);
                }
                dbconnection.commit();
            } finally {
                if (tmpTable != null) {
                    DBSpecifics.getInstance().dropJobConfigsTmpTable(dbconnection, tmpTable);
                }
                job.configsChanged = false;
            }
        }
    }

    /**
     * Check whether a particular job exists.
     *
     * @param jobID Id of the job.
     * @return true if the job exists in any state.
     */
    @Override
    public boolean exists(Long jobID) {
        ArgumentNotValid.checkNotNull(jobID, "Long jobID");

        Connection c = HarvestDBConnection.get();
        try {
            return exists(c, jobID);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Check whether a particular job exists.
     *
     * @param c an open connection to the harvestDatabase
     * @param jobID Id of the job.
     * @return true if the job exists in any state.
     */
    private boolean exists(Connection c, Long jobID) {
        return 1 == DBUtils.selectLongValue(c, "SELECT COUNT(*) FROM jobs WHERE job_id = ?", jobID);
    }

    /**
     * Generates the next id of job.
     *
     * @param c an open connection to the harvestDatabase
     * @return id
     */
    private Long generateNextID(Connection c) {
        // Set to zero original, can be set after admin machine breakdown,
        // and the use this as the point of reference.
        Long restoreId = Settings.getLong(Constants.NEXT_JOB_ID);

        Long maxVal = DBUtils.selectLongValue(c, "SELECT MAX(job_id) FROM jobs");
        if (maxVal == null) {
            maxVal = 0L;
        }
        // return the largest number of the two numbers: the NEXT_JOB_ID
        // declared in settings and max value of job_id used
        // in the jobs table.
        return ((restoreId > maxVal) ? restoreId : maxVal + 1L);
    }

    /**
     * Update a Job in persistent storage.
     *
     * @param job The Job to update
     * @throws ArgumentNotValid If the Job is null
     * @throws UnknownID If the Job doesn't exist in the DAO
     * @throws IOFailure If writing the job to persistent storage fails
     * @throws PermissionDenied If the job has been updated behind our backs
     */
    @Override
    public synchronized void update(Job job) {
        ArgumentNotValid.checkNotNull(job, "job");

        Connection connection = HarvestDBConnection.get();
        // Not done as a transaction as it's awfully big.
        // TODO Make sure that a failed job update does... what?
        PreparedStatement statement = null;
        try {
            final Long jobID = job.getJobID();
            if (!exists(connection, jobID)) {
                throw new UnknownID("Job id " + jobID + " is not known in persistent storage");
            }

            connection.setAutoCommit(false);
            statement = connection.prepareStatement("UPDATE jobs SET " + "harvest_id = ?, status = ?, channel = ?, "
                    + "forcemaxcount = ?, forcemaxbytes = ?, " + "forcemaxrunningtime = ?," + "orderxml = ?, "
                    + "orderxmldoc = ?, seedlist = ?, " + "harvest_num = ?, harvest_errors = ?, "
                    + "harvest_error_details = ?, upload_errors = ?, " + "upload_error_details = ?, startdate = ?,"
                    + "enddate = ?, num_configs = ?, edition = ?, " + "submitteddate = ?, creationdate = ?, "
                    + "resubmitted_as_job = ?, harvestname_prefix = ?," + "snapshot = ?"
                    + " WHERE job_id = ? AND edition = ?");
            statement.setLong(1, job.getOrigHarvestDefinitionID());
            statement.setInt(2, job.getStatus().ordinal());
            statement.setString(3, job.getChannel());
            statement.setLong(4, job.getForceMaxObjectsPerDomain());
            statement.setLong(5, job.getMaxBytesPerDomain());
            statement.setLong(6, job.getMaxJobRunningTime());
            DBUtils.setStringMaxLength(statement, 7, job.getOrderXMLName(), Constants.MAX_NAME_SIZE, job,
                    "order.xml name");
            final String orderreader = job.getOrderXMLdoc().getXML();
            DBUtils.setClobMaxLength(statement, 8, orderreader, Constants.MAX_ORDERXML_SIZE, job, "order.xml");
            DBUtils.setClobMaxLength(statement, 9, job.getSeedListAsString(), Constants.MAX_COMBINED_SEED_LIST_SIZE,
                    job, "seedlist");
            statement.setInt(10, job.getHarvestNum()); // Not in job yet
            DBUtils.setStringMaxLength(statement, 11, job.getHarvestErrors(), Constants.MAX_ERROR_SIZE, job,
                    "harvest_error");
            DBUtils.setStringMaxLength(statement, 12, job.getHarvestErrorDetails(), Constants.MAX_ERROR_DETAIL_SIZE,
                    job, "harvest_error_details");
            DBUtils.setStringMaxLength(statement, 13, job.getUploadErrors(), Constants.MAX_ERROR_SIZE, job,
                    "upload_error");
            DBUtils.setStringMaxLength(statement, 14, job.getUploadErrorDetails(), Constants.MAX_ERROR_DETAIL_SIZE,
                    job, "upload_error_details");
            long edition = job.getEdition() + 1;
            DBUtils.setDateMaybeNull(statement, 15, job.getActualStart());
            DBUtils.setDateMaybeNull(statement, 16, job.getActualStop());
            statement.setInt(17, job.getDomainConfigurationMap().size());
            statement.setLong(18, edition);
            DBUtils.setDateMaybeNull(statement, 19, job.getSubmittedDate());
            DBUtils.setDateMaybeNull(statement, 20, job.getCreationDate());
            DBUtils.setLongMaybeNull(statement, 21, job.getResubmittedAsJob());
            statement.setString(22, job.getHarvestFilenamePrefix());
            statement.setBoolean(23, job.isSnapshot());
            statement.setLong(24, job.getJobID());
            statement.setLong(25, job.getEdition());
            final int rows = statement.executeUpdate();
            if (rows == 0) {
                String message = "Edition " + job.getEdition() + " has expired, not updating";
                log.debug(message);
                throw new PermissionDenied(message);
            }
            createJobConfigsEntries(connection, job);
            connection.commit();
            job.setEdition(edition);
        } catch (SQLException e) {
            String message = "SQL error updating job " + job + " in database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.rollbackIfNeeded(connection, "update job", job);
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Read a single job from the job database.
     *
     * @param jobID ID of the job.
     * @return A Job object
     * @throws UnknownID if the job id does not exist.
     * @throws IOFailure if there was some problem talking to the database.
     */
    @Override
    public Job read(long jobID) {
        Connection connection = HarvestDBConnection.get();
        try {
            return read(connection, jobID);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    protected static final String GET_JOB_BY_ID_SQL = ""
    		+ "SELECT "
	    		+ "harvest_id,"
	    		+ "status,"
	    		+ "channel,"
	            + "forcemaxcount,"
	            + "forcemaxbytes,"
	            + "forcemaxrunningtime,"
	            + "orderxml,"
	            + "orderxmldoc,"
	            + "seedlist,"
	            + "harvest_num,"
	            + "harvest_errors,"
	            + "harvest_error_details,"
	            + "upload_errors,"
	            + "upload_error_details,"
	            + "startdate,"
	            + "enddate,"
	            + "submitteddate,"
	            + "creationdate,"
	            + "edition,"
	            + "resubmitted_as_job,"
	            + "continuationof,"
	            + "harvestname_prefix,"
	            + "snapshot "
            + "FROM jobs WHERE job_id = ?";

    /**
     * Read a single job from the job database.
     *
     * @param jobID ID of the job.
     * @param connection an open connection to the harvestDatabase
     * @return A Job object
     * @throws UnknownID if the job id does not exist.
     * @throws IOFailure if there was some problem talking to the database.
     */
    private synchronized Job read(Connection connection, Long jobID) {
        if (!exists(connection, jobID)) {
            throw new UnknownID("Job id " + jobID + " is not known in persistent storage");
        }
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(GET_JOB_BY_ID_SQL);
            statement.setLong(1, jobID);
            ResultSet result = statement.executeQuery();
            result.next();
            long harvestID = result.getLong(1);
            JobStatus status = JobStatus.fromOrdinal(result.getInt(2));
            String channel = result.getString(3);
            long forceMaxCount = result.getLong(4);
            long forceMaxBytes = result.getLong(5);
            long forceMaxRunningTime = result.getLong(6);
            String orderxml = result.getString(7);

            HeritrixTemplate orderXMLdoc = null;

            boolean useClobs = DBSpecifics.getInstance().supportsClob();
            String tmpStr;
            if (useClobs) {
                Clob clob = result.getClob(8);
                tmpStr = clob.getSubString(1L, (int)clob.length());
            } else {
                tmpStr = result.getString(8);
            }
            orderXMLdoc = HeritrixTemplate.getTemplateFromString(-1, tmpStr);
            String seedlist = "";
            if (useClobs) {
                Clob clob = result.getClob(9);
                seedlist = clob.getSubString(1, (int) clob.length());
            } else {
                seedlist = result.getString(9);
            }

            int harvestNum = result.getInt(10);
            String harvestErrors = result.getString(11);
            String harvestErrorDetails = result.getString(12);
            String uploadErrors = result.getString(13);
            String uploadErrorDetails = result.getString(14);
            Date startdate = DBUtils.getDateMaybeNull(result, 15);
            Date stopdate = DBUtils.getDateMaybeNull(result, 16);
            Date submittedDate = DBUtils.getDateMaybeNull(result, 17);
            Date creationDate = DBUtils.getDateMaybeNull(result, 18);
            Long edition = result.getLong(19);
            Long resubmittedAsJob = DBUtils.getLongMaybeNull(result, 20);
            Long continuationOfJob = DBUtils.getLongMaybeNull(result, 21);
            String harvestnamePrefix = result.getString(22);
            boolean snapshot = result.getBoolean(23);
            statement.close();
            // IDs should match up in a natural join
            // The following if-block is an attempt to fix Bug 1856, an
            // unexplained derby deadlock, by making this statement a dirty
            // read.
            String domainStatement = "SELECT domains.name, configurations.name "
                    + "FROM domains, configurations, job_configs " + "WHERE job_configs.job_id = ?"
                    + "  AND job_configs.config_id = configurations.config_id"
                    + "  AND domains.domain_id = configurations.domain_id";
            if (Settings.get(CommonSettings.DB_SPECIFICS_CLASS).contains(CommonSettings.DB_IS_DERBY_IF_CONTAINS)) {
                statement = connection.prepareStatement(domainStatement + " WITH UR");
            } else {
                statement = connection.prepareStatement(domainStatement);
            }
            statement.setLong(1, jobID);
            result = statement.executeQuery();
            Map<String, String> configurationMap = new HashMap<String, String>();
            while (result.next()) {
                String domainName = result.getString(1);
                String configName = result.getString(2);
                configurationMap.put(domainName, configName);
            }
            final Job job = new Job(harvestID, configurationMap, channel, snapshot, forceMaxCount, forceMaxBytes,
                    forceMaxRunningTime, status, orderxml, orderXMLdoc, seedlist, harvestNum, continuationOfJob);
            job.appendHarvestErrors(harvestErrors);
            job.appendHarvestErrorDetails(harvestErrorDetails);
            job.appendUploadErrors(uploadErrors);
            job.appendUploadErrorDetails(uploadErrorDetails);
            if (startdate != null) {
                job.setActualStart(startdate);
            }
            if (stopdate != null) {
                job.setActualStop(stopdate);
            }

            if (submittedDate != null) {
                job.setSubmittedDate(submittedDate);
            }

            if (creationDate != null) {
                job.setCreationDate(creationDate);
            }

            job.configsChanged = false;
            job.setJobID(jobID);
            job.setEdition(edition);

            if (resubmittedAsJob != null) {
                job.setResubmittedAsJob(resubmittedAsJob);
            }
            if (harvestnamePrefix == null) {
                job.setDefaultHarvestNamePrefix();
            } else {
                job.setHarvestFilenamePrefix(harvestnamePrefix);
            }
            return job;
        } catch (SQLException e) {
            String message = "SQL error reading job " + jobID + " in database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally  {
        	try {
				statement.close();
        	} catch (SQLException e) {
        		log.warn("Exception thrown when trying to close statement", e);
			}
        }
    }

    

    /**
     * Return a list of all jobs with the given status, ordered by id.
     *
     * @param status A given status.
     * @return A list of all job with given status
     */
    @Override
    public synchronized Iterator<Job> getAll(JobStatus status) {
        ArgumentNotValid.checkNotNull(status, "JobStatus status");

        Connection c = HarvestDBConnection.get();
        try {
            List<Long> idList = DBUtils.selectLongList(c, "SELECT job_id FROM jobs WHERE status = ? "
                    + "ORDER BY job_id", status.ordinal());
            List<Job> orderedJobs = new LinkedList<Job>();
            for (Long jobId : idList) {
                orderedJobs.add(read(c, jobId));
            }
            return orderedJobs.iterator();
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Return a list of all job_id's representing jobs with the given status.
     *
     * @param status A given status.
     * @return A list of all job_id's representing jobs with given status
     * @throws ArgumentNotValid If the given status is not one of the five valid statuses specified in Job.
     */
    @Override
    public Iterator<Long> getAllJobIds(JobStatus status) {
        ArgumentNotValid.checkNotNull(status, "JobStatus status");

        Connection c = HarvestDBConnection.get();
        try {
            List<Long> idList = DBUtils.selectLongList(c, "SELECT job_id FROM jobs WHERE status = ? ORDER BY job_id",
                    status.ordinal());
            return idList.iterator();
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public Iterator<Long> getAllJobIds(JobStatus status, HarvestChannel channel) {
        ArgumentNotValid.checkNotNull(status, "JobStatus status");
        ArgumentNotValid.checkNotNull(channel, "Channel");

        Connection c = HarvestDBConnection.get();
        try {
            List<Long> idList = DBUtils.selectLongList(c, "SELECT job_id FROM jobs WHERE status = ? AND channel = ? "
                    + "ORDER BY job_id", status.ordinal(), channel.getName());
            return idList.iterator();
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Return a list of all jobs.
     *
     * @return A list of all jobs
     */
    @Override
    public synchronized Iterator<Job> getAll() {
        Connection c = HarvestDBConnection.get();
        try {
            List<Long> idList = DBUtils.selectLongList(c, "SELECT job_id FROM jobs ORDER BY job_id");
            List<Job> orderedJobs = new LinkedList<Job>();
            for (Long jobId : idList) {
                orderedJobs.add(read(c, jobId));
            }
            return orderedJobs.iterator();
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Return a list of all job_ids .
     *
     * @return A list of all job_ids
     */
    public Iterator<Long> getAllJobIds() {
        Connection c = HarvestDBConnection.get();
        try {
            List<Long> idList = DBUtils.selectLongList(c, "SELECT job_id FROM jobs ORDER BY job_id");
            return idList.iterator();
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Get a list of small and immediately usable status information for given status and in given order. Is used by
     * getStatusInfo functions in order to share code (and SQL)
     *  TODO should also include given harvest run
     *
     * @param connection an open connection to the harvestDatabase
     * @param jobStatusCode code for jobstatus, -1 if all
     * @param asc true if it is to be sorted in ascending order, false if it is to be sorted in descending order
     * @return List of JobStatusInfo objects for all jobs.
     * @throws ArgumentNotValid for invalid jobStatusCode
     * @throws IOFailure on trouble getting data from database
     */
    private List<JobStatusInfo> getStatusInfo(Connection connection, int jobStatusCode, boolean asc) {
        // Validate jobStatusCode
        // Throws ArgumentNotValid if it is an invalid job status
        if (jobStatusCode != JobStatus.ALL_STATUS_CODE) {
            JobStatus.fromOrdinal(jobStatusCode);
        }

        StringBuffer sqlBuffer = new StringBuffer("SELECT jobs.job_id, status, jobs.harvest_id, "
                + "harvestdefinitions.name, harvest_num, harvest_errors,"
                + " upload_errors, orderxml, num_configs, submitteddate, creationdate, startdate,"
                + " enddate, resubmitted_as_job" + " FROM jobs, harvestdefinitions "
                + " WHERE harvestdefinitions.harvest_id = jobs.harvest_id ");

        if (jobStatusCode != JobStatus.ALL_STATUS_CODE) {
            sqlBuffer.append(" AND status = ").append(jobStatusCode);
        }
        sqlBuffer.append(" ORDER BY jobs.job_id");
        if (!asc) { // Assume default is ASCENDING
            sqlBuffer.append(" " + HarvestStatusQuery.SORT_ORDER.DESC.name());
        }

        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(sqlBuffer.toString());
            ResultSet res = statement.executeQuery();
            return makeJobStatusInfoListFromResultset(res);
        } catch (SQLException e) {
            String message = "SQL error asking for job status list in database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
    }

    /**
     * Get a list of small and immediately usable status information for given job status.
     *
     * @param status The status asked for.
     * @return List of JobStatusInfo objects for all jobs with given job status
     * @throws ArgumentNotValid for invalid jobStatus
     * @throws IOFailure on trouble getting data from database
     */
    @Override
    public List<JobStatusInfo> getStatusInfo(JobStatus status) {
        ArgumentNotValid.checkNotNull(status, "status");
        Connection c = HarvestDBConnection.get();
        try {
            return getStatusInfo(c, status.ordinal(), true);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Get a list of small and immediately usable status information for given job status and in given job id order.
     *
     * @param query the user query
     * @throws IOFailure on trouble getting data from database
     */
    @Override
    public HarvestStatus getStatusInfo(HarvestStatusQuery query) {
        log.debug("Constructing Harveststatus based on given query.");
        PreparedStatement s = null;
        Connection c = HarvestDBConnection.get();

        try {
            // Obtain total count without limit
            // NB this will be a performance bottleneck if the table gets big
            long totalRowsCount = 0;

            final HarvestStatusQueryBuilder harvestStatusQueryBuilder = buildSqlQuery(query, true);
            log.debug("Unpopulated query is {}.", harvestStatusQueryBuilder);
            s = harvestStatusQueryBuilder.getPopulatedStatement(c);
            log.debug("Query is {}.", s);
            ResultSet res = s.executeQuery();
            res.next();
            totalRowsCount = res.getLong(1);

            s = buildSqlQuery(query, false).getPopulatedStatement(c);
            res = s.executeQuery();
            List<JobStatusInfo> jobs = makeJobStatusInfoListFromResultset(res);

            log.debug("Harveststatus constructed based on given query.");
            return new HarvestStatus(totalRowsCount, jobs);
        } catch (SQLException e) {
            String message = "SQL error asking for job status list in database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Calculate all jobIDs to use for duplication reduction.
     * <p>
     * More precisely, this method calculates the following: If the job ID corresponds to a partial harvest, all jobIDs
     * from the previous scheduled harvest are returned, or the empty list if this harvest hasn't been scheduled before.
     * <p>
     * If the job ID corresponds to a full harvest, the entire chain of harvests this is based on is returned, and all
     * jobIDs from the previous chain of full harvests is returned.
     * <p>
     * This method is synchronized to avoid DB locking.
     *
     * @param jobID The job ID to find duplicate reduction data for.
     * @return A list of job IDs (possibly empty) of potential previous harvests of this job, to use for duplicate
     * reduction.
     * @throws UnknownID if job ID is unknown
     * @throws IOFailure on trouble querying database
     */
    public synchronized List<Long> getJobIDsForDuplicateReduction(long jobID) throws UnknownID {

        Connection connection = HarvestDBConnection.get();
        List<Long> jobs;
        // Select the previous harvest from the same harvestdefinition
        try {
            if (!exists(connection, jobID)) {
                throw new UnknownID("Job ID '" + jobID + "' does not exist in database");
            }

            jobs = DBUtils.selectLongList(connection, "SELECT jobs.job_id FROM jobs, jobs AS original_jobs"
                    + " WHERE original_jobs.job_id=?" + " AND jobs.harvest_id=original_jobs.harvest_id"
                    + " AND jobs.harvest_num=original_jobs.harvest_num-1", jobID);
            List<Long> harvestDefinitions = getPreviousFullHarvests(connection, jobID);
            if (!harvestDefinitions.isEmpty()) {
                // Select all jobs from a given list of harvest definitions
                jobs.addAll(DBUtils.selectLongList(connection, "SELECT jobs.job_id FROM jobs"
                        + " WHERE jobs.harvest_id IN (" + StringUtils.conjoin(",", harvestDefinitions) + ")"));
            }
            return jobs;
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Find the harvest definition ids from this chain of snapshot harvests and the previous chain of snapshot harvests.
     *
     * @param connection an open connection to the harvestDatabase
     * @param jobID The ID of the job
     * @return A (possibly empty) list of harvest definition ids
     */
    private List<Long> getPreviousFullHarvests(Connection connection, long jobID) {
        List<Long> results = new ArrayList<Long>();
        // Find the jobs' fullharvest id
        Long thisHarvest = DBUtils.selectFirstLongValueIfAny(connection,
                "SELECT jobs.harvest_id FROM jobs, fullharvests WHERE jobs.harvest_id=fullharvests.harvest_id"
                        + " AND jobs.job_id=?", jobID);

        if (thisHarvest == null) {
            // Not a full harvest
            return results;
        }

        // Follow the chain of orginating IDs back
        for (Long originatingHarvest = thisHarvest; originatingHarvest != null; originatingHarvest = DBUtils
                .selectFirstLongValueIfAny(connection, "SELECT previoushd FROM fullharvests"
                        + " WHERE fullharvests.harvest_id=?", originatingHarvest)) {
            if (!originatingHarvest.equals(thisHarvest)) {
                results.add(originatingHarvest);
            }
        }

        // Find the first harvest in the chain
        Long firstHarvest = thisHarvest;
        if (!results.isEmpty()) {
            firstHarvest = results.get(results.size() - 1);
        }

        // Find the last harvest in the chain before
        Long olderHarvest = DBUtils.selectFirstLongValueIfAny(connection, "SELECT fullharvests.harvest_id"
                + " FROM fullharvests, harvestdefinitions," + "  harvestdefinitions AS currenthd"
                + " WHERE currenthd.harvest_id=?" + " AND fullharvests.harvest_id" + "=harvestdefinitions.harvest_id"
                + " AND harvestdefinitions.submitted<currenthd.submitted" + " ORDER BY harvestdefinitions.submitted "
                + HarvestStatusQuery.SORT_ORDER.DESC.name(), firstHarvest);
        // Follow the chain of originating IDs back
        // FIXME Rewrite this loop!
        for (Long originatingHarvest = olderHarvest; originatingHarvest != null; originatingHarvest = DBUtils
                .selectFirstLongValueIfAny(connection,
                        "SELECT previoushd FROM fullharvests WHERE fullharvests.harvest_id=?", originatingHarvest)) {
            results.add(originatingHarvest);
        }
        return results;
    }

    /**
     * Returns the number of existing jobs.
     *
     * @return Number of jobs in 'jobs' table
     */
    @Override
    public int getCountJobs() {
        Connection c = HarvestDBConnection.get();
        try {
            return DBUtils.selectIntValue(c, "SELECT COUNT(*) FROM jobs");
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public synchronized long rescheduleJob(long oldJobID) {
        Connection connection = HarvestDBConnection.get();
        long newJobID = generateNextID(connection);
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("SELECT status FROM jobs WHERE job_id = ?");
            statement.setLong(1, oldJobID);
            ResultSet res = statement.executeQuery();
            if (!res.next()) {
                throw new UnknownID("No job with ID " + oldJobID + " to resubmit");
            }
            final JobStatus currentJobStatus = JobStatus.fromOrdinal(res.getInt(1));
            if (currentJobStatus != JobStatus.SUBMITTED && currentJobStatus != JobStatus.FAILED) {
                throw new IllegalState("Job " + oldJobID + " is not ready to be copied.");
            }

            // Now do the actual copying.
            // Note that startdate, and enddate is not copied.
            // They must be null in JobStatus NEW.
            statement.close();
            connection.setAutoCommit(false);

            statement = connection.prepareStatement("INSERT INTO jobs "
                    + " (job_id, harvest_id, channel, snapshot, status," + "  forcemaxcount, forcemaxbytes, orderxml,"
                    + "  orderxmldoc, seedlist, harvest_num," + "  num_configs, edition, continuationof) "
                    + " SELECT ?, harvest_id, channel, snapshot, ?," + "  forcemaxcount, forcemaxbytes, orderxml,"
                    + "  orderxmldoc, seedlist, harvest_num," + " num_configs, ?, ?" + " FROM jobs WHERE job_id = ?");
            statement.setLong(1, newJobID);
            statement.setLong(2, JobStatus.NEW.ordinal());
            long initialEdition = 1;
            statement.setLong(3, initialEdition);
            Long continuationOf = null;
            // In case we want to try to continue using the Heritrix recover log
            if (currentJobStatus == JobStatus.FAILED) {
                continuationOf = oldJobID;
            }
            DBUtils.setLongMaybeNull(statement, 4, continuationOf);

            statement.setLong(5, oldJobID);

            statement.executeUpdate();
            statement.close();
            statement = connection.prepareStatement("INSERT INTO job_configs "
                    + "( job_id, config_id ) SELECT ?, config_id FROM job_configs WHERE job_id = ?");
            statement.setLong(1, newJobID);
            statement.setLong(2, oldJobID);
            statement.executeUpdate();
            statement.close();
            statement = connection.prepareStatement("UPDATE jobs SET status = ?, resubmitted_as_job = ? "
                    + " WHERE job_id = ?");
            statement.setInt(1, JobStatus.RESUBMITTED.ordinal());
            statement.setLong(2, newJobID);
            statement.setLong(3, oldJobID);
            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            String message = "SQL error rescheduling job #" + oldJobID + " in database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
            DBUtils.rollbackIfNeeded(connection, "resubmit job", oldJobID);
            HarvestDBConnection.release(connection);
        }
        log.info("Job #{} successfully as job #{}", oldJobID, newJobID);
        return newJobID;
    }

    /**
     * Helper-method that constructs a list of JobStatusInfo objects from the given resultset.
     *
     * @param res a given resultset
     * @return a list of JobStatusInfo objects
     * @throws SQLException If any problem with accessing the data in the ResultSet
     */
    private List<JobStatusInfo> makeJobStatusInfoListFromResultset(ResultSet res) throws SQLException {
        List<JobStatusInfo> joblist = new ArrayList<JobStatusInfo>();
        while (res.next()) {
            final long jobId = res.getLong(1);
            joblist.add(new JobStatusInfo(jobId, JobStatus.fromOrdinal(res.getInt(2)), res.getLong(3),
                    res.getString(4), res.getInt(5), res.getString(6), res.getString(7), res.getString(8), res
                            .getInt(9), DBUtils.getDateMaybeNull(res, 10), DBUtils.getDateMaybeNull(res, 11), DBUtils
                            .getDateMaybeNull(res, 12), DBUtils.getDateMaybeNull(res, 13), DBUtils.getLongMaybeNull(
                            res, 14)));
        }
        return joblist;
    }

    /**
     * Internal utility class to build a SQL query using a prepared statement.
     */
    private class HarvestStatusQueryBuilder {
        /** The sql string. */
        private String sqlString;
        // from java.sql.Types
        /** list of parameter classes. */
        private LinkedList<Class<?>> paramClasses = new LinkedList<Class<?>>();
        /** list of parameter values. */
        private LinkedList<Object> paramValues = new LinkedList<Object>();

        /**
         * Constructor.
         */
        HarvestStatusQueryBuilder() {
            super();
        }

        @Override public String toString() {
            return sqlString;
        }

        /**
         * @param sqlString the sqlString to set
         */
        void setSqlString(String sqlString) {
            this.sqlString = sqlString;
        }

        /**
         * Add the given class and given value to the list of paramClasses and paramValues respectively.
         *
         * @param clazz a given class.
         * @param value a given value
         */
        void addParameter(Class<?> clazz, Object value) {
            paramClasses.addLast(clazz);
            paramValues.addLast(value);
        }

        /**
         * Prepare a statement for the database that uses the sqlString, and the paramClasses, and paramValues. Only
         * Integer, Long, String, and Date values accepted.
         *
         * @param c an Open connection to the harvestDatabase
         * @return the prepared statement
         * @throws SQLException If unable to prepare the statement
         * @throws UnknownID If one of the parameter classes is unexpected
         */
        PreparedStatement getPopulatedStatement(Connection c) throws SQLException {
            PreparedStatement stm = c.prepareStatement(sqlString);

            Iterator<Class<?>> pClasses = paramClasses.iterator();
            Iterator<Object> pValues = paramValues.iterator();
            int pIndex = 0;
            while (pClasses.hasNext()) {
                pIndex++;
                Class<?> pClass = pClasses.next();
                Object pVal = pValues.next();

                if (Integer.class.equals(pClass)) {
                    stm.setInt(pIndex, (Integer) pVal);
                } else if (Long.class.equals(pClass)) {
                    stm.setLong(pIndex, (Long) pVal);
                } else if (String.class.equals(pClass)) {
                    stm.setString(pIndex, (String) pVal);
                } else if (java.sql.Date.class.equals(pClass)) {
                    stm.setDate(pIndex, (java.sql.Date) pVal);
                } else {
                    throw new UnknownID("Unexpected parameter class " + pClass);
                }
            }
            return stm;
        }


    }

    /**
     * Builds a query to fetch jobs according to selection criteria.
     *
     * @param query the selection criteria.
     * @param count build a count query instead of selecting columns.
     * @return the proper SQL query.
     */
    private HarvestStatusQueryBuilder buildSqlQuery(HarvestStatusQuery query, boolean count) {
        HarvestStatusQueryBuilder sq = new HarvestStatusQueryBuilder();
        StringBuffer sql = new StringBuffer("SELECT");
        if (count) {
            sql.append(" count(*)");
        } else {
            sql.append(" jobs.job_id, status, jobs.harvest_id,");
            sql.append(" harvestdefinitions.name, harvest_num,");
            sql.append(" harvest_errors, upload_errors, orderxml,");
            sql.append(" num_configs, submitteddate, creationdate, startdate, enddate,");
            sql.append(" resubmitted_as_job");
        }
        sql.append(" FROM jobs, harvestdefinitions ");
        sql.append(" WHERE harvestdefinitions.harvest_id = jobs.harvest_id ");

        JobStatus[] jobStatuses = query.getSelectedJobStatuses();
        if (jobStatuses.length > 0) {
            if (jobStatuses.length == 1) {
                int statusOrdinal = jobStatuses[0].ordinal();
                sql.append(" AND status = ?");
                sq.addParameter(Integer.class, statusOrdinal);
            } else {
                sql.append("AND (status = ");
                sql.append(jobStatuses[0].ordinal());
                for (int i = 1; i < jobStatuses.length; i++) {
                    sql.append(" OR status = ?");
                    sq.addParameter(Integer.class, jobStatuses[i].ordinal());
                }
                sql.append(")");
            }
        }

        String harvestName = query.getHarvestName();
        boolean caseSensitiveHarvestName = query.getCaseSensitiveHarvestName();
        if (!harvestName.isEmpty()) {
            if (caseSensitiveHarvestName) {
                if (harvestName.indexOf(HarvestStatusQuery.HARVEST_NAME_WILDCARD) == -1) {
                    // No wildcard, exact match
                    sql.append(" AND harvestdefinitions.name = ?");
                    sq.addParameter(String.class, harvestName);
                } else {
                    String harvestNamePattern = harvestName.replaceAll("\\*", "%");
                    sql.append(" AND harvestdefinitions.name LIKE ?");
                    sq.addParameter(String.class, harvestNamePattern);
                }
            } else {
                harvestName = harvestName.toUpperCase();
                if (harvestName.indexOf(HarvestStatusQuery.HARVEST_NAME_WILDCARD) == -1) {
                    // No wildcard, exact match
                    sql.append(" AND UPPER(harvestdefinitions.name) = ?");
                    sq.addParameter(String.class, harvestName);
                } else {
                    String harvestNamePattern = harvestName.replaceAll("\\*", "%");
                    sql.append(" AND UPPER(harvestdefinitions.name)  LIKE ?");
                    sq.addParameter(String.class, harvestNamePattern);
                }
            }
        }

        Long harvestRun = query.getHarvestRunNumber();
        if (harvestRun != null) {
            sql.append(" AND jobs.harvest_num = ?");
            log.debug("Added harvest run number param {}.", harvestRun);
            sq.addParameter(Long.class, harvestRun);
        }

        Long harvestId = query.getHarvestId();
        if (harvestId != null) {
            sql.append(" AND harvestdefinitions.harvest_id = ?");
            log.debug("Added harvest_id param {}.", harvestId);
            sq.addParameter(Long.class, harvestId);
        }

        long startDate = query.getStartDate();
        if (startDate != HarvestStatusQuery.DATE_NONE) {
            sql.append(" AND startdate >= ?");
            sq.addParameter(java.sql.Date.class, new java.sql.Date(startDate));
        }

        long endDate = query.getEndDate();
        if (endDate != HarvestStatusQuery.DATE_NONE) {
            sql.append(" AND enddate < ?");
            // end date must be set +1 day at midnight
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(endDate);
            cal.roll(Calendar.DAY_OF_YEAR, 1);
            sq.addParameter(java.sql.Date.class, new java.sql.Date(cal.getTimeInMillis()));
        }
        
        List<String> jobIdRangeIds = query.getPartialJobIdRangeAsList(false);
        List<String> jobIdRanges = query.getPartialJobIdRangeAsList(true);
        if (!jobIdRangeIds.isEmpty()) {
        	String comma = "";
        	sql.append(" AND (jobs.job_id IN (");
        	for(String id : jobIdRangeIds) {
        		//id
        		sql.append(comma);
        		comma = ",";
        		sql.append("?");
                sq.addParameter(Long.class, Long.parseLong(id));
        	}
        	sql.append(") ");

        	
        }
        if(!jobIdRanges.isEmpty()) {
        	String andOr = "AND";
        	if (!jobIdRangeIds.isEmpty()) {
        		andOr = "OR";
        	}
        	
        	for(String range : jobIdRanges) {
        		String[] r = range.split("-");
        		sql.append(" "+andOr+" jobs.job_id BETWEEN ? AND ? ");
            	sq.addParameter(Long.class, Long.parseLong(r[0]));
            	sq.addParameter(Long.class, Long.parseLong(r[1]));
        	}
        }
        if (!jobIdRangeIds.isEmpty()) {
    		sql.append(")");
    	}

        if (!count) {
            sql.append(" ORDER BY jobs.job_id");
            if (!query.isSortAscending()) {
                sql.append(" " + SORT_ORDER.DESC.name());
            } else {
                sql.append(" " + SORT_ORDER.ASC.name());
            }

            long pagesize = query.getPageSize();
            if (pagesize != HarvestStatusQuery.PAGE_SIZE_NONE) {
                sql.append(" "
                        + DBSpecifics.getInstance().getOrderByLimitAndOffsetSubClause(pagesize,
                                (query.getStartPageIndex() - 1) * pagesize));
            }
        }

        sq.setSqlString(sql.toString());
        return sq;
    }

    /**
     * Get Jobstatus for the job with the given id.
     *
     * @param jobID A given Jobid
     * @return the Jobstatus for the job with the given id.
     * @throws UnknownID if no job exists with id jobID
     */
    public JobStatus getJobStatus(Long jobID) {
        ArgumentNotValid.checkNotNull(jobID, "Long jobID");

        Connection c = HarvestDBConnection.get();
        try {
            Integer statusAsInteger = DBUtils.selectIntValue(c, "SELECT status FROM jobs WHERE job_id = ?", jobID);
            if (statusAsInteger == null) {
                throw new UnknownID("No known job with id=" + jobID);
            }
            return JobStatus.fromOrdinal(statusAsInteger);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Get a list of AliasInfo objects for all the domains included in the job.
     *
     * @return a list of AliasInfo objects for all the domains included in the job.
     */
    public List<AliasInfo> getJobAliasInfo(Job job) {
        List<AliasInfo> aliases = new ArrayList<AliasInfo>();
        DomainDAO dao = DomainDAO.getInstance();
        for (String domain : job.getDomainConfigurationMap().keySet()) {
            aliases.addAll(dao.getAliases(domain));
        }
        return aliases;
    }
}
