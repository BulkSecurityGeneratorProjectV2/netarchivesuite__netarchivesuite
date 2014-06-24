
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
 * Implementation of the ExtendedFieldTypeDAO interface
 * for creating and accessing extended fields in persistent storage.
 */
public class ExtendedFieldTypeDBDAO extends ExtendedFieldTypeDAO {
    /** The logger for this class. */
    private final Log log = LogFactory.getLog(getClass());

    /**
     * Default constructor of this class. Tries to make any necessary migration
     * of the database.
     */
    protected ExtendedFieldTypeDBDAO() {

        Connection connection = HarvestDBConnection.get();
        try {
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELDTYPE);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELD);
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.EXTENDEDFIELDVALUE);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    @Override
    public boolean exists(Long aExtendedfieldtypeId) {
        ArgumentNotValid.checkNotNull(aExtendedfieldtypeId,
                "Long aExtendedfieldtypeId");

        Connection c = HarvestDBConnection.get();
        try {
            return exists(c, aExtendedfieldtypeId);
        } finally {
            HarvestDBConnection.release(c);
        }

    }
    
    /**
     * Tests if exists an ExtendedFieldType with the given ID.
     * @param c an open connection to the database
     * @param aExtendedfieldtypeId an id belonging to a ExtendedFieldType
     * @return true, if there exists an ExtendedFieldType with the given ID,
     * otherwise returns false.
     */
    private synchronized boolean exists(Connection c, 
            Long aExtendedfieldtypeId) {
        return 1 == DBUtils
                .selectLongValue(
                        c,
                        "SELECT COUNT(*) FROM extendedfieldtype "
                        + "WHERE extendedfieldtype_id = ?",
                        aExtendedfieldtypeId);
    }

    @Override
    public synchronized ExtendedFieldType read(Long aExtendedfieldtypeId) {
        ArgumentNotValid.checkNotNull(aExtendedfieldtypeId,
                "aExtendedfieldtypeId");
        Connection connection = HarvestDBConnection.get();
        try {
            return read(connection, aExtendedfieldtypeId);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }
    
    /**
     * Read an ExtendedFieldType from database belonging to the given id.
     * @param connection an open connection to the database
     * @param aExtendedfieldtypeId an id belonging to a ExtendedFieldType
     * @return an ExtendedFieldType from database belonging to the given id.
     */
    private synchronized ExtendedFieldType read(Connection connection,
            Long aExtendedfieldtypeId) {
        if (!exists(connection, aExtendedfieldtypeId)) {
            throw new UnknownID("Extended FieldType id " + aExtendedfieldtypeId
                    + " is not known in persistent storage");
        }

        ExtendedFieldType extendedFieldType = null;
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("" + "SELECT name "
                    + "FROM   extendedfieldtype "
                    + "WHERE  extendedfieldtype_id = ? ");

            statement.setLong(1, aExtendedfieldtypeId);
            ResultSet result = statement.executeQuery();
            result.next();

            String name = result.getString(1);

            extendedFieldType = new ExtendedFieldType(aExtendedfieldtypeId,
                    name);

            return extendedFieldType;
        } catch (SQLException e) {
            String message = "SQL error reading extended Field "
                    + aExtendedfieldtypeId + " in database" + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
    }

    @Override
    public synchronized List<ExtendedFieldType> getAll() {
        Connection c = HarvestDBConnection.get();
        try {
            List<Long> idList = DBUtils.selectLongList(c,
                    "SELECT extendedfieldtype_id FROM extendedfieldtype");
            List<ExtendedFieldType> extendedFieldTypes 
                = new LinkedList<ExtendedFieldType>();
            for (Long extendedfieldtypeId : idList) {
                extendedFieldTypes.add(read(c, extendedfieldtypeId));
            }
            return extendedFieldTypes;
        } finally {
            HarvestDBConnection.release(c);
        }
    }
}
