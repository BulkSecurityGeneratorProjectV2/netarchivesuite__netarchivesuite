/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.harvester.harvesting;

import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.crawler.datamodel.CandidateURI;
import org.archive.crawler.datamodel.CoreAttributeConstants;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.frontier.HostnameQueueAssignmentPolicy;

import dk.netarkivet.common.utils.DomainUtils;

/**
 * This is modified version of the {@link DomainnameQueueAssignmentPolicy}
 * where domainname returned is the domainname of the candidateURI
 * except where the domainname of the SeedURI is a different one. 
 * 
 * 
 * Using the domain as the queue-name.
 * The domain is defined as the last two names in the entire hostname or
 * the entirety of an IP address.
 * x.y.z -> y.z
 * y.z -> y.z
 * nn.nn.nn.nn -> nn.nn.nn.nn
 * 
 */
public class AlternateDomainnameQueueAssignmentPolicy
        extends HostnameQueueAssignmentPolicy {
    
    /** A key used for the cases when we can't figure out the URI.
     *  This is taken from parent, where it has private access.  Parent returns
     *  this on things like about:blank.
     */
    static final String DEFAULT_CLASS_KEY = "default...";

    private Log log
            = LogFactory.getLog(getClass());

    /** Return a key for queue names based on domain names (last two parts of
     * host name) or IP address.  They key may include a #<portnr> at the end.
     *
     * @param controller The controller the crawl is running on.
     * @param cauri A potential URI.
     * @return a class key (really an arbitrary string), one of <domainOrIP>,
     * <domainOrIP>#<port>, or "default...".
     * @see HostnameQueueAssignmentPolicy#getClassKey(
     *  org.archive.crawler.framework.CrawlController,
     *  org.archive.crawler.datamodel.CandidateURI)
     */
     public String getClassKey(CrawlController controller, CandidateURI cauri) {
        String candidate;
        try {
            // Since getClassKey has no contract, we must encapsulate it from
            // errors.
            candidate = super.getClassKey(controller, cauri);
        } catch (NullPointerException e) {
            log.debug("Heritrix broke getting class key candidate for "
                      + cauri);
            candidate = DEFAULT_CLASS_KEY;
        }
        
        String sourceCandidate = null;  
        try {
            sourceCandidate = cauri.getString(CoreAttributeConstants.A_SOURCE_TAG);
        } catch (NoSuchElementException e) {
            log.warn("source-tag-seeds not set in Heritrix template!");
        }
        if (sourceCandidate != null) {
            log.info("sourceCandidate: " + sourceCandidate);
        }
        
        String[] hostnameandportnr = candidate.split("#");
        if (hostnameandportnr.length == 0 || hostnameandportnr.length > 2) {
            return candidate;
        }
        String domainName = DomainUtils.domainNameFromHostname(hostnameandportnr[0]);
        if (domainName == null) { // Not valid according to our rules
            log.debug("Illegal class key candidate '" + candidate
                      + "' for '" + cauri + "'");
            return candidate;
        }
        return domainName;
    }
}