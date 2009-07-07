/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright Det Kongelige Bibliotek og Statsbiblioteket, Danmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.wayback;

import java.io.OutputStream;

import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.utils.arc.ARCBatchJob;

/**
 * Returns a cdx file using the appropriate format for wayback, including
 * canonicalisation of urls. The returned files are unsorted.
 *
 * @author csr
 * @since Jul 1, 2009
 */

public class ExtractWaybackCDXBatchJob extends ARCBatchJob {

    public void initialize(OutputStream os) {
        //TODO: implement method
        throw new RuntimeException("Not implemented");
    }

    public void processRecord(ARCRecord record, OutputStream os) {
        //TODO: implement method
        throw new RuntimeException("Not implemented");
    }

    public void finish(OutputStream os) {
        //TODO: implement method
        throw new RuntimeException("Not implemented");
    }
}
