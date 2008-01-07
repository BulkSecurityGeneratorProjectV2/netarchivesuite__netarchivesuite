/*$Id: HarvestDefinitionGUITester.java 25 2007-07-27 14:25:27Z kfc $
* $Revision: 25 $
* $Date: 2007-07-27 16:25:27 +0200 (Fri, 27 Jul 2007) $
* $Author: kfc $
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.harvester.datamodel;

import dk.netarkivet.common.webinterface.SiteSection;

/**
 * A site section for test use.
 */
public class TestSiteSection extends SiteSection {
    public TestSiteSection() {
        super("Test", "Test", 1, new String[][]{{"Test", "Test"}},
              "Test", "Test");
    }
}
