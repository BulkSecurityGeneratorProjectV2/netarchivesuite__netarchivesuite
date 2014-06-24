
package dk.netarkivet.harvester.datamodel.extendedfield;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.harvester.datamodel.HarvestDBConnection;
import dk.netarkivet.harvester.datamodel.HarvesterDatabaseTables;

/**
 * A database-based implementation of the ExtendedFieldDBDAO class.
 */
public class ExtendedFieldDBDAO extends ExtendedFieldDAO {
    /** The logger for this class. */
    private final Log log = LogFactory.getLog(getClass());

    /**
     * Constructor for the ExtendedFieldDBDAO object.
     */
    public ExtendedFieldDBDAO() {

        Connection connection = HarvestDBConnection.get();
        try {
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELD);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELDVALUE);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }
    
    @Override
    public synchronized void create(ExtendedField aExtendedField) {
        ArgumentNotValid.checkNotNull(aExtendedField, "aExtendedField");

        Connection connection = HarvestDBConnection.get();
        if (aExtendedField.getExtendedFieldID() != null) {
            log.warn("The extendedFieldID for this extended Field is "
                    + "already set. This should probably never happen.");
        } else {
            aExtendedField.setExtendedFieldID(generateNextID(connection));
        }

        log.debug("Creating " + aExtendedField.toString());

        PreparedStatement statement = null;
        try {
            connection.setAutoCommit(false);
            statement = connection.prepareStatement(""
                    + "INSERT INTO extendedfield "
                    + "            (extendedfield_id, "
                    + "             extendedfieldtype_id, "
                    + "             name, " 
                    + "             format, "
                    + "             defaultvalue, " 
                    + "             options, "
                    + "             datatype, " 
                    + "             mandatory, "
                    + "             sequencenr, " 
                    + "             maxlen) " 
                    + "VALUES      (?, "
                    + "             ?, " 
                    + "             ?, "
                    + "             ?, " 
                    + "             ?, "
                    + "             ?, " 
                    + "             ?, "
                    + "             ?, " 
                    + "             ?, " 
                    + "             ?) ");

            statement.setLong(1, aExtendedField.getExtendedFieldID());
            statement.setLong(2, aExtendedField.getExtendedFieldTypeID());
            statement.setString(3, aExtendedField.getName());
            statement.setString(4, aExtendedField.getFormattingPattern());
            statement.setString(5, aExtendedField.getDefaultValue());
            statement.setString(6, aExtendedField.getOptions());
            statement.setInt(7, aExtendedField.getDatatype());
            // the following conversion from boolean to int is necessary, 
            // because the database column 'mandatory' is a integer field 
            // and not a boolean (NAS-2127)
            statement.setInt(8, aExtendedField.isMandatory()? 1: 0); 
            statement.setInt(9, aExtendedField.getSequencenr());
            statement.setInt(10, aExtendedField.getMaxlen());
            //TODO
            log.debug(statement.toString());

            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            String message = "SQL error creating extended field "
                    + aExtendedField + " in database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
            DBUtils.rollbackIfNeeded(connection, "create extended field",
                    aExtendedField);
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Generates the next id of a extended field. this implementation retrieves
     * the maximum value of extendedfield_id in the DB, and returns this value +
     * 1.
     * @param c an open connection to the HarvestDatabase
     * 
     * @return The next available ID
     */
    private Long generateNextID(Connection c) {
        Long maxVal = DBUtils.selectLongValue(c,
                "SELECT max(extendedfield_id) FROM extendedfield");

        if (maxVal == null) {
            maxVal = 0L;
        }
        return maxVal + 1L;
    }

    /**
     * Check whether a particular extended Field exists.
     * 
     * @param aExtendedfieldId
     *            Id of the extended field.
     * @return true if the extended field exists.
     */
    public boolean exists(Long aExtendedfieldId) {
        ArgumentNotValid.checkNotNull(aExtendedfieldId,
                "Long aExtendedfieldId");

        Connection c = HarvestDBConnection.get();
        try {
            return exists(c, aExtendedfieldId);
        } finally {
            HarvestDBConnection.release(c);
        }
    }
    
    /**
     * Check, if there exists an ExtendedField with a given ID.
     * @param c An open connection to the HarvestDatabase
     * @param aExtendedfieldId An Id for a given Extended Field.
     * @return true, if the extended field with the Id exists; 
     * otherwise false
     */
    private synchronized boolean exists(Connection c, Long aExtendedfieldId) {
        return 1 == DBUtils.selectLongValue(c,
                "SELECT COUNT(*) FROM extendedfield WHERE extendedfield_id = ?",
                aExtendedfieldId);
    }

    @Override
    public synchronized void update(ExtendedField aExtendedField) {
        ArgumentNotValid.checkNotNull(aExtendedField, "aExtendedField");

        Connection connection = HarvestDBConnection.get();

        PreparedStatement statement = null;
        try {
            final Long extendedfieldId = aExtendedField.getExtendedFieldID();
            if (!exists(connection, extendedfieldId)) {
                throw new UnknownID("Extended Field id " + extendedfieldId
                        + " is not known in persistent storage");
            }

            connection.setAutoCommit(false);

            statement = connection.prepareStatement(""
                    + "UPDATE extendedfield " 
            		+ "SET    extendedfield_id = ?, "
                    + "       extendedfieldtype_id = ?, " 
            		+ "       name = ?, "
                    + "       format = ?, " 
            		+ "       defaultvalue = ?, "
                    + "       options = ?, " 
            		+ "       datatype = ?, "
                    + "       mandatory = ?, " 
            		+ "       sequencenr = ?, "
            		+ "       maxlen = ? "
                    + "WHERE  extendedfield_id = ? ");

            statement.setLong(1, aExtendedField.getExtendedFieldID());
            statement.setLong(2, aExtendedField.getExtendedFieldTypeID());
            statement.setString(3, aExtendedField.getName());
            statement.setString(4, aExtendedField.getFormattingPattern());
            statement.setString(5, aExtendedField.getDefaultValue());
            statement.setString(6, aExtendedField.getOptions());
            statement.setInt(7, aExtendedField.getDatatype());
            // the following conversion from boolean to int is necessary, 
            // because the database column 'mandatory' is a int field 
            // and not a boolean (NAS-2127)
            statement.setInt(8, aExtendedField.isMandatory()? 1: 0);
            statement.setInt(9, aExtendedField.getSequencenr());
            statement.setInt(10, aExtendedField.getMaxlen());
            statement.setLong(11, aExtendedField.getExtendedFieldID());

            //TODO
            log.debug(statement.toString());

            statement.executeUpdate();
            
            connection.commit();
        } catch (SQLException e) {
            String message = "SQL error updating extendedfield "
                    + aExtendedField + " in database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(statement);
            DBUtils.rollbackIfNeeded(connection, "update extendedfield",
                    aExtendedField);
            HarvestDBConnection.release(connection);
        }
    }

    @Override
    public synchronized ExtendedField read(Long aExtendedfieldId) {
        ArgumentNotValid.checkNotNull(aExtendedfieldId, "aExtendedfieldId");
        Connection connection = HarvestDBConnection.get();
        try {
            return read(connection, aExtendedfieldId);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }
    
    /**
     * Read an ExtendedField from database.
     * @param connection A connection to the harvestDatabase
     * @param aExtendedfieldId The ID for a given ExtendedField 
     * @return An ExtendedField object for the given ID.
     */
    private synchronized ExtendedField read(Connection connection,
            Long aExtendedfieldId) {
        if (!exists(connection, aExtendedfieldId)) {
            throw new UnknownID("Extended Field id " + aExtendedfieldId
                    + " is not known in persistent storage");
        }

        ExtendedField extendedField = null;
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(""
                    + "SELECT extendedfieldtype_id, " 
            		+ "       name, "
                    + "       format, " 
            		+ "       defaultvalue, "
                    + "       options, " 
            		+ "       datatype, "
                    + "       mandatory, " 
            		+ "       sequencenr, "
            		+ "       maxlen "
                    + "FROM   extendedfield " 
                    + "WHERE  extendedfield_id = ? ");

            statement.setLong(1, aExtendedfieldId);
            ResultSet result = statement.executeQuery();
            result.next();

            long extendedfieldtypeId = result.getLong(1);
            String name = result.getString(2);
            String format = result.getString(3);
            String defaultvalue = result.getString(4);
            String options = result.getString(5);
            int datatype = result.getInt(6);
            //TODO maybe this cast is not necessary
            boolean mandatory = (result.getInt(7) != 0); 
            int sequencenr = result.getInt(8);
            int maxlen = result.getInt(9);

            extendedField = new ExtendedField(aExtendedfieldId,
                    extendedfieldtypeId, name, format, datatype, mandatory,
                    sequencenr, defaultvalue, options, maxlen);

            return extendedField;
        } catch (SQLException e) {
            String message = "SQL error reading extended Field "
                    + aExtendedfieldId + " in database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
    }
    @Override
    public synchronized List<ExtendedField> getAll(long aExtendedFieldTypeId) {
        Connection c = HarvestDBConnection.get();
        try {
            List<Long> idList = DBUtils.selectLongList(c,
                    "SELECT extendedfield_id FROM extendedfield "
                            + "WHERE extendedfieldtype_id = ? "
                            + "ORDER BY sequencenr ASC", aExtendedFieldTypeId);
            List<ExtendedField> extendedFields 
                = new LinkedList<ExtendedField>();
            for (Long extendedfieldId : idList) {
                extendedFields.add(read(c, extendedfieldId));
            }
            return extendedFields;
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public void delete(long aExtendedfieldId) throws IOFailure {
        ArgumentNotValid.checkNotNull(aExtendedfieldId, "aExtendedfieldId");

        Connection c = HarvestDBConnection.get();
        PreparedStatement stm = null;
        try {
            c.setAutoCommit(false);

            stm = c.prepareStatement(
                    "DELETE FROM extendedfieldvalue WHERE extendedfield_id = ?"
                    );
            stm.setLong(1, aExtendedfieldId);
            stm.executeUpdate();
            stm.close();
            stm = c.prepareStatement(
                    "DELETE FROM extendedfield WHERE extendedfield_id = ?");
            stm.setLong(1, aExtendedfieldId);
            stm.executeUpdate();

            c.commit();

        } catch (SQLException e) {
            String message = "SQL error deleting extended fields for ID "
                    + aExtendedfieldId + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(stm);
            DBUtils.rollbackIfNeeded(c, "delete extended field",
                    aExtendedfieldId);
            HarvestDBConnection.release(c);
        }

    }
}
