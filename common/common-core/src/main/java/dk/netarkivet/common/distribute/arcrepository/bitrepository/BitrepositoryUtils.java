/*
 * #%L
 * Netarchivesuite - common
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.common.distribute.arcrepository.bitrepository;

import java.io.File;
import java.util.UUID;

import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumType;
import org.bitrepository.common.utils.Base16Utils;
import org.bitrepository.common.utils.CalendarUtils;
import org.bitrepository.common.utils.ChecksumUtils;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** Utilities used by the Bitrepository class. */
public class BitrepositoryUtils {
    /**
     * Creates the data structure for encapsulating the validation checksums for validation of the PutFile operation.
     * @param file The file to have the checksum calculated.
     * @param csSpec A given ChecksumSpecTYPE
     * @return The ChecksumDataForFileTYPE for the pillars to validate the PutFile operation.
     */
    public static ChecksumDataForFileTYPE getValidationChecksum(File file, ChecksumSpecTYPE csSpec) {
        ArgumentNotValid.checkExistsNormalFile(file, "File file");
        ArgumentNotValid.checkNotNull(csSpec, "ChecksumSpecTYPE csSpec");
        String checksum = ChecksumUtils.generateChecksum(file, csSpec);
        ChecksumDataForFileTYPE res = new ChecksumDataForFileTYPE();
        res.setCalculationTimestamp(CalendarUtils.getNow());
        res.setChecksumSpec(csSpec);
        res.setChecksumValue(Base16Utils.encodeBase16(checksum));
        return res;
    }

    /**
     *  Specify a checksum.
     *  @param checksumtype a given type of checksum
     *  @param salt A string to salt the checksum with (if null, no salting)
     * @return The requested checksum spec
     */
    public static ChecksumSpecTYPE getRequestChecksumSpec(ChecksumType checksumtype, String salt) {
        ChecksumSpecTYPE res = new ChecksumSpecTYPE();
        res.setChecksumType(checksumtype);
        if (salt != null) {
            res.setChecksumSalt(Base16Utils.encodeBase16(salt));
        }
        return res;
    }

    /**
     * Generates a component id, which includes the hostname and a random UUID.
     * @return The Bitrepository component id for this NetarchiveSuite application.
     */
    public static String generateComponentID() {
        String hn = HostName.getHostName();
        return "NetarchivesuiteClient-" + hn + "-" + UUID.randomUUID();
    }
}