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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.PartialHarvest;
import dk.netarkivet.harvester.datamodel.Schedule;
import dk.netarkivet.harvester.datamodel.ScheduleDAO;
import dk.netarkivet.harvester.datamodel.SparseDomainConfiguration;
import dk.netarkivet.harvester.datamodel.SparsePartialHarvest;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypes;

/**
 * This class contains the methods for updating data for selective harvests.
 */
@SuppressWarnings({"unchecked"})
public final class SelectiveHarvestUtil {
    
	static final Logger log = LoggerFactory.getLogger(SelectiveHarvestUtil.class);
	
	/**
     * Utility class. No instances.
     */
    private SelectiveHarvestUtil() {
    }
    

    /**
     * Update or create a partial harvest definition.
     *
     * @param context JSP context of this call. Contains parameters as described in
     * Definitions-edit-selective-harvest.jsp
     * @param i18n Translation information.
     * @param unknownDomains List to which unknown legal domains are added.
     * @param illegalDomains List to which illegal domains are added,
     */
    public static void processRequest(PageContext context, I18n i18n, List<String> unknownDomains,
            List<String> illegalDomains) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");
        ArgumentNotValid.checkNotNull(unknownDomains, "List unknownDomains");
        ArgumentNotValid.checkNotNull(illegalDomains, "List illegalDomains");
        log.info("Starting method processRequest");
        // Was the set next date button pressed?
        boolean setNextDateOnly = HTMLUtils.parseOptionalBoolean(context, Constants.NEXTDATE_SUBMIT, false);
        if (setNextDateOnly) {
            HTMLUtils.forwardOnEmptyParameter(context, Constants.NEXTDATE_PARAM, Constants.NEXTDATE_PARAM);
            HTMLUtils.forwardOnEmptyParameter(context, Constants.HARVEST_ID, Constants.HARVEST_ID);

            // If the override date is set, parse it and set the override date.
            Date date = HTMLUtils.parseOptionalDate(context, Constants.NEXTDATE_PARAM, I18n.getString(
                    dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE, context.getResponse().getLocale(),
                    "harvestdefinition.schedule.edit.timeformat"), null);
            long harvestId = HTMLUtils.parseOptionalLong(context, Constants.HARVEST_ID, -1L);

            HarvestDefinitionDAO.getInstance().updateNextdate(harvestId, date);

            return; // nothin' more to do!
        }

        ServletRequest request = context.getRequest();
        if (request.getParameter(Constants.UPDATE_PARAM) == null) {
            return; // nothing to do.
        }

        String deleteConfig = request.getParameter(Constants.DELETECONFIG_PARAM);
        // Case where we are removing a configuration
        // In this we make the delete, and then return;
        if (deleteConfig != null) {
            HTMLUtils.forwardOnEmptyParameter(context, Constants.DELETECONFIG_PARAM);
            deleteConfig(context, i18n, deleteConfig);
            return;
        }

        String deleteAllConfigs = request.getParameter(Constants.DELETEALL_CONFIGS_PARAM);
        // Case where we are removing a configuration
        // In this we make the delete, and then return;
        if (deleteAllConfigs != null && Boolean.valueOf(deleteAllConfigs) == true ) {
            HTMLUtils.forwardOnEmptyParameter(context, Constants.DELETEALL_CONFIGS_PARAM);
            deleteAllConfigs(context, i18n);
            return;
        }
        
        PartialHarvest hdd = updateHarvestDefinition(context, i18n, unknownDomains, illegalDomains);

        ExtendedFieldValueDefinition.processRequest(context, i18n, hdd, ExtendedFieldTypes.HARVESTDEFINITION);
        HarvestDefinitionDAO.getInstance().update(hdd);

        boolean changed = false;

        // If the override date is set, parse it and set the override date.
        Date date = HTMLUtils.parseOptionalDate(context, Constants.NEXTDATE_PARAM, I18n.getString(
                dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE, context.getResponse().getLocale(),
                "harvestdefinition.schedule.edit.timeformat"), null);
        if (date != null) {
            hdd.setNextDate(date);
            changed |= true;
        }

        // Case where we are adding domains that didn't exist before
        // This uses two parameters because it has an input field and a submit
        // button.
        if (request.getParameter(Constants.ADDDOMAINS_PARAM) != null) {
            HTMLUtils.forwardOnMissingParameter(context, Constants.UNKNOWN_DOMAINS_PARAM);
            changed |= addDomainsToHarvest(hdd, request.getParameter(Constants.UNKNOWN_DOMAINS_PARAM));
        }

        if (changed) {
            HarvestDefinitionDAO.getInstance().update(hdd);
        }

    }

    /**
     * Updates the harvest definition with posted values.
     *
     * @param context The context that the web request processing happens in
     * @param i18n Translation information for this site section.
     * @param unknownDomains List to add unknown but legal domains to.
     * @param illegalDomains List to add illegal domains to.
     * @return The updated harvest definition. This object holds an edition that is legal to use for further updates
     * (adding or deleting domains)
     */
    private static PartialHarvest updateHarvestDefinition(PageContext context, I18n i18n, List<String> unknownDomains,
            List<String> illegalDomains) {
        ServletRequest request = context.getRequest();
        HTMLUtils.forwardOnEmptyParameter(context, Constants.HARVEST_PARAM, Constants.SCHEDULE_PARAM);
        String name = request.getParameter(Constants.HARVEST_PARAM);
        String oldname = request.getParameter(Constants.HARVEST_OLD_PARAM);
        if (oldname == null) {
            oldname = "";
        }

        HTMLUtils.forwardOnMissingParameter(context, Constants.COMMENTS_PARAM, Constants.DOMAINLIST_PARAM,
                Constants.AUDIENCE_PARAM);
        String scheduleName = request.getParameter(Constants.SCHEDULE_PARAM);
        Schedule sched = ScheduleDAO.getInstance().read(scheduleName);
        if (sched == null) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;unknown.schedule.0", scheduleName);
            throw new ForwardedToErrorPage("Schedule '" + scheduleName + "' not found");
        }

        String comments = request.getParameter(Constants.COMMENTS_PARAM);
        String audience = request.getParameter(Constants.AUDIENCE_PARAM);

        List<DomainConfiguration> dc = getDomainConfigurations(request.getParameterMap());
        addDomainsToConfigurations(dc, request.getParameter(Constants.DOMAINLIST_PARAM), unknownDomains, illegalDomains);

        // If necessary create harvest from scratch
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        if ((request.getParameter(Constants.CREATENEW_PARAM) != null)) {
            if (hddao.exists(name)) {
                HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;harvest.definition.0.already.exists", name);
                throw new ForwardedToErrorPage("A harvest definition " + "called '" + name + "' already exists");
            }
            PartialHarvest hdd = new PartialHarvest(dc, sched, name, comments, audience);
            hdd.setActive(false);
            hddao.create(hdd);
            return hdd;
        } else {
            long edition = HTMLUtils.parseOptionalLong(context, Constants.EDITION_PARAM, Constants.NO_EDITION);
            PartialHarvest hdd;
            if (oldname.equals(name)) {
                hdd = (PartialHarvest) hddao.getHarvestDefinition(name);
            } else {
                if (hddao.exists(name)) {
                    HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;harvest.definition.0.already.exists", name);
                    throw new ForwardedToErrorPage("A harvest definition " + "called '" + name + "' already exists");
                } else {
                    hdd = (PartialHarvest) hddao.getHarvestDefinition(oldname);
                    hdd.setName(name);
                }
            }
            if (hdd.getEdition() != edition) {
                HTMLUtils.forwardWithRawErrorMessage(context, i18n, "errormsg;harvest.definition.changed.0.retry.1",
                        "<br/><a href=\"Definitions-edit-selective-harvest.jsp?" + Constants.HARVEST_PARAM + "="
                                + HTMLUtils.encodeAndEscapeHTML(name) + "\">", "</a>");
                throw new ForwardedToErrorPage("Harvest definition '" + name + "' has changed");
            }
            // update the harvest definition
            hdd.setDomainConfigurations(dc);
            hdd.setSchedule(sched);
            hdd.setComments(comments);
            hdd.setAudience(audience);
            hddao.update(hdd);
            return hdd;
        }
    }

    /**
     * Delete a domain configuration from a harvestdefinition.
     *
     * @param context The web server context for the JSP page.
     * @param i18n Translation information for this site section.
     * @param deleteConfig the configuration to delete, in the form of a domain name, a colon, a configuration name.
     */
    private static void deleteConfig(PageContext context, I18n i18n, String deleteConfig) {
        HTMLUtils.forwardOnEmptyParameter(context, Constants.HARVEST_PARAM, Constants.SCHEDULE_PARAM);
        ServletRequest request = context.getRequest();
        String name = request.getParameter(Constants.HARVEST_PARAM);
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        if (!hddao.exists(name)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;harvestdefinition.0.does.not.exist", name);
            throw new ForwardedToErrorPage("Harvestdefinition '" + name + "' does not exist");
        }
        SparsePartialHarvest sph = hddao.getSparsePartialHarvest(name);

        String[] domainConfigPair = deleteConfig.split(":", 2);
        if (domainConfigPair.length < 2) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;malformed.domain.config.pair.0", deleteConfig);
            throw new ForwardedToErrorPage("Malformed domain-config pair " + deleteConfig);
        }
        String domainName = domainConfigPair[0];
        String configName = domainConfigPair[1];
        SparseDomainConfiguration key = new SparseDomainConfiguration(domainName, configName);

        hddao.removeDomainConfiguration(sph.getOid(), key);
    }

    /**
     * Delete all configuration from a harvestdefinition.
     *
     * @param context The web server context for the JSP page.
     * @param i18n Translation information for this site section.
     * @param deleteConfig the configuration to delete, in the form of a domain name, a colon, a configuration name.
     */
    private static void deleteAllConfigs(PageContext context, I18n i18n) {
        HTMLUtils.forwardOnEmptyParameter(context, Constants.HARVEST_PARAM, Constants.SCHEDULE_PARAM);
        ServletRequest request = context.getRequest();
        String name = request.getParameter(Constants.HARVEST_PARAM);
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        if (!hddao.exists(name)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;harvestdefinition.0.does.not.exist", name);
            throw new ForwardedToErrorPage("Harvestdefinition '" + name + "' does not exist");
        }
        SparsePartialHarvest sph = hddao.getSparsePartialHarvest(name);
        hddao.removeAllConfigurations(sph.getOid());
    }
    
    /**
     * Extract domain configuration list from a map of parameters. All key that starts with Constants.DOMAIN_IDENTIFIER
     * are treated as a concatenation of : DOMAIN_IDENTIFIER + domain name. The corresponding value in the map is
     * treated as the configuration name Entries that do not match this pattern are ignored.
     *
     * @param configurations a mapping (domain to its configurations)
     * @return a list of domain configurations
     */
    private static List<DomainConfiguration> getDomainConfigurations(Map<String, String[]> configurations) {
        List<DomainConfiguration> dcList = new ArrayList<DomainConfiguration>();

        for (Map.Entry<String, String[]> param : configurations.entrySet()) {
            if (param.getKey().startsWith(Constants.DOMAIN_IDENTIFIER)) {
                String domainName = param.getKey().substring(Constants.DOMAIN_IDENTIFIER.length());
                Domain domain = DomainDAO.getInstance().read(domainName);
                for (String configurationName : param.getValue()) {
                    dcList.add(domain.getConfiguration(configurationName));
                }
            }
        }
        return dcList;
    }

    /**
     * Given a list of domain configurations and a list of domains, add the default configurations for the domains to
     * the configuration list. If any of the domains are unknown, their names are instead appended to the argument
     * unknownDomains (with newline separation)
     *
     * @param dcList the initial list of configurations
     * @param extraDomains the domains to be added to dcList with default configurations
     * @param unknownDomains a list to add unknown, legal domains to
     * @param illegalDomains a list to add illegal domains to
     */
    private static void addDomainsToConfigurations(List<DomainConfiguration> dcList, String extraDomains,
            List<String> unknownDomains, List<String> illegalDomains) {
        String[] domains = extraDomains.split("\\s+");
        DomainDAO ddao = DomainDAO.getInstance();
        for (String domain : domains) {
            domain = domain.trim();
            if (domain.length() > 0) {
                if (ddao.exists(domain)) {
                    Domain d = ddao.read(domain);
                    if (!dcList.contains(d.getDefaultConfiguration())) {
                        dcList.add(d.getDefaultConfiguration());
                    }
                } else {
                    if (DomainUtils.isValidDomainName(domain)) {
                        unknownDomains.add(domain);
                    } else {
                        illegalDomains.add(domain);
                    }
                }
            }
        }
    }

    /**
     * Given a harvest and list of domains, this method creates all the specified domains and adds them to the harvest
     * with their default configuration.
     *
     * @param hdd The harvest definition to change.
     * @param domains a whitespace-separated list of domains to create and add to harvest
     * @return True if changes were made to hdd.
     */
    private static boolean addDomainsToHarvest(PartialHarvest hdd, String domains) {
        String[] domainsS = domains.split("\\s");
        List<DomainConfiguration> configurations = new ArrayList<DomainConfiguration>();
        for (String domainName : domainsS) {
            if (domainName != null && !domainName.isEmpty()) {
                if (DomainUtils.isValidDomainName(domainName)) {
                    Domain domain = Domain.getDefaultDomain(domainName);
                    DomainDAO.getInstance().create(domain);
                    configurations.add(domain.getDefaultConfiguration());
                }
            }
        }
        if (configurations.size() > 0) {
            Iterator<DomainConfiguration> existingConfigurations = hdd.getDomainConfigurations();
            while (existingConfigurations.hasNext()) {
                configurations.add(existingConfigurations.next());
            }
            hdd.setDomainConfigurations(configurations);
            return true;
        } else {
            return false;
        }
    }    
}
