
package dk.netarkivet.archive;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import dk.netarkivet.archive.arcrepository.ArchiveArcRepositoryTesterSuite;
import dk.netarkivet.archive.arcrepository.CommonDistributeArcrepositoryTesterSuite;
import dk.netarkivet.archive.arcrepository.bitpreservation.ArchiveArcrepositoryBitPreservationTesterSuite;
import dk.netarkivet.archive.arcrepository.distribute.ArchiveArcrepositoryDistributeTesterSuite;
import dk.netarkivet.archive.arcrepositoryadmin.ArchiveArcRepositoryAdminTesterSuite;
import dk.netarkivet.archive.bitarchive.ArchiveBitarchiveTesterSuite;
import dk.netarkivet.archive.bitarchive.distribute.ArchiveBitarchiveDistributeTesterSuite;
import dk.netarkivet.archive.checksum.ArchiveChecksumTesterSuite;
import dk.netarkivet.archive.checksum.distribute.ArchiveChecksumDistributeTesterSuite;
import dk.netarkivet.archive.distribute.ArchiveDistributeTesterSuite;
import dk.netarkivet.archive.tools.ArchiveToolsTesterSuite;
import dk.netarkivet.archive.webinterface.ArchiveWebinterfaceTesterSuite;
//import dk.netarkivet.harvester.indexserver.ArchiveIndexServerTesterSuite;
//import dk.netarkivet.harvester.indexserver.distribute.ArchiveIndexserverDistributeTesterSuite;

/**
 * This class runs all the archive module unit tests.
 */
public class UnitTesterSuite {
    public static void addToSuite(TestSuite suite) {
        ArchiveArcRepositoryTesterSuite.addToSuite(suite);
        ArchiveArcRepositoryAdminTesterSuite.addToSuite(suite);
        ArchiveArcrepositoryBitPreservationTesterSuite.addToSuite(suite);
        ArchiveArcrepositoryDistributeTesterSuite.addToSuite(suite);
        ArchiveBitarchiveTesterSuite.addToSuite(suite);
        ArchiveBitarchiveDistributeTesterSuite.addToSuite(suite);
        ArchiveChecksumTesterSuite.addToSuite(suite);
        ArchiveChecksumDistributeTesterSuite.addToSuite(suite);
        ArchiveDistributeTesterSuite.addToSuite(suite);
// FIXME:  Pulls in whole harvester test suite...  Move it there instead?
//        ArchiveIndexserverDistributeTesterSuite.addToSuite(suite);
//        ArchiveIndexServerTesterSuite.addToSuite(suite);
        ArchiveToolsTesterSuite.addToSuite(suite);
        ArchiveWebinterfaceTesterSuite.addToSuite(suite);
        CommonDistributeArcrepositoryTesterSuite.addToSuite(suite);
    }

    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(UnitTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void main(String[] args) {
        String[] args2 = {"-noloading", UnitTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
