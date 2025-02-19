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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.Named;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.TLD;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.dao.DAOProviderFactory;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendableEntity;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypes;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValue;
import dk.netarkivet.harvester.utils.CrawlertrapsUtils;

/**
 * Represents known information about a domain A domain is identified by a domain name (ex: kb.dk)
 * <p>
 * The following information is used to control how a domain is harvested: Seedlists, configurations and passwords. Each
 * seedlist defines one or more URL's that the harvester should use as starting points. A configuration defines a
 * specific combination of settings (seedlist, harvester settings, passwords) that should be used during harvest.
 * Passwords define user names and passwords that might be used for the domain.
 * <p>
 * Information about previous harvests of this domain is available via the domainHistory.
 * <p>
 * Information from the domain registrant (DK-HOSTMASTER) about the domain registration is available in the
 * registration. This includes the dates where the domain was known to exist (included in a domain list), together with
 * domain owner information.
 * <p>
 * Notice that each configuration references one of the seedlists by name, and possibly one of the Passwords.
 */
@SuppressWarnings({"rawtypes"})
public class Domain extends ExtendableEntity implements Named {

    /** The logger for this class. */
    protected static final Logger log = LoggerFactory.getLogger(Domain.class);

    /** The identification used to lookup the domain. */

    private String domainName;

    /**
     * Map<String, DomainConfiguration> the various harvest configurations that can be used to harvest this domain.
     */
    private Map<String, DomainConfiguration> domainConfigurations;

    /** Use this configuration unless otherwise specified. */
    private String defaultConfigName;

    /**
     * Map<String, SeedList> The different seedlists used as starting points by the harvesters.
     */
    private Map<String, SeedList> seedlists;

    /** Map<String, Password> with an entry for each known password. */
    private Map<String, Password> passwords;

    /**
     * List of crawler traps, that is regexps that should be ignored for this domain.
     */
    private List<String> crawlerTraps;

    /** Records all historical information about the domain. */
    private DomainHistory history;

    /**
     * List<DomainOwnerInfo> contains information about the known owners of this domain.
     */
    private List<DomainOwnerInfo> domainOwnerInfos;

    /** Comments that the user has entered. */
    private String comments;

    /** Edition is used by the DAO to keep track of changes. */
    long edition = -1;

    /**
     * If non-null, this domain is considered an alias of the domain named. The field must be either null or aliasInfo
     * that defines an alias from this domain to another, and the time the alias field was last updated. This is used to
     * allow operators to check the domains that have been aliases for a long time.
     * <p>
     * Note that we do not allow transitive aliases, so the domain named in this field is not allowed to become an alias
     * itself.
     */
    private AliasInfo aliasInfo;

    /** ID autogenerated by DB DAO. */
    private Long id;

    /**
     * Create new instance of a domain. It is generally recommended that getDefaultDomain is used instead of this
     * constructor.
     *
     * @param theDomainName Name used to reference the domain
     * @throws ArgumentNotValid if either of the arguments are null or empty, or if the domain does not match the regex
     * for valid domains
     */
    protected Domain(String theDomainName) {
        super(DAOProviderFactory.getExtendedFieldDAOProvider());
        ArgumentNotValid.checkNotNullOrEmpty(theDomainName, "theDomainName");
        if (!DomainUtils.isValidDomainName(theDomainName)) {
            throw new ArgumentNotValid("Domain '" + theDomainName + "' does not match the regexp "
                    + "defining valid domains: " + TLD.getInstance().getValidDomainMatcher().pattern());
        }
        domainName = theDomainName;
        comments = "";
        domainConfigurations = new HashMap<String, DomainConfiguration>();
        seedlists = new HashMap<String, SeedList>();
        passwords = new HashMap<String, Password>();
        crawlerTraps = Collections.emptyList();
        history = new DomainHistory();
        domainOwnerInfos = new ArrayList<DomainOwnerInfo>();
    }

    /**
     * Get a new domain, initialised with default values.
     *
     * @param domainName The name of the domain
     * @return a domain with the given name
     * @throws ArgumentNotValid if name is null or empty
     */
    public static Domain getDefaultDomain(String domainName) {
        Domain myDomain;
        myDomain = new Domain(domainName);

        // Create default seed list containing one seed: http://www.domain
        // or http://1.2.3.4 for IP-named domains.
        String defaultSeedListName = Settings.get(HarvesterSettings.DEFAULT_SEEDLIST);

        SeedList seedlist;
        if (Constants.IP_KEY_REGEXP.matcher(domainName).matches()) {
            // IP domains should not get www
            seedlist = new SeedList(defaultSeedListName, "http://" + domainName + "\nhttps://" + domainName);
        } else {
            seedlist = new SeedList(defaultSeedListName,
                    "http://www." + domainName +
                            "\nhttps://www." + domainName +
                            "\nhttp://" + domainName +
                            "\nhttps://" + domainName);
        }
        myDomain.addSeedList(seedlist);

        List<SeedList> seedlists = Arrays.asList(seedlist);

        // Create default configuration using the default seedlist
        String domainDefaultConfig = Settings.get(HarvesterSettings.DOMAIN_DEFAULT_CONFIG);

        DomainConfiguration cfg = new DomainConfiguration(domainDefaultConfig, myDomain, seedlists,
                new ArrayList<Password>());
        cfg.setOrderXmlName(Settings.get(HarvesterSettings.DOMAIN_DEFAULT_ORDERXML));
        cfg.setMaxRequestRate(Integer.parseInt(Settings.get(HarvesterSettings.DOMAIN_CONFIG_MAXRATE)));
        myDomain.addConfiguration(cfg);

        return myDomain;
    }

    /**
     * Adds a new configuration to the domain. If this is the first configuration added, it becomes the default
     * configuration. The seedlist referenced by the configuration must already be registered in this domain otherwise
     * an UnknownID exception is thrown.
     *
     * @param cfg the configuration that is added
     * @throws UnknownID if the name of the seedlist referenced by cfg is unknown
     * @throws PermissionDenied if a configuration with the same name already exists
     * @throws ArgumentNotValid if null supplied
     */
    public void addConfiguration(DomainConfiguration cfg) {
        ArgumentNotValid.checkNotNull(cfg, "cfg");

        if (domainConfigurations.containsKey(cfg.getName())) {
            throw new PermissionDenied("A configuration already exists with the name:" + cfg.getName()
                    + "; in the domain:" + getName() + ";");
        }

        putConfiguration(cfg);

        if (domainConfigurations.size() == 1) {
            defaultConfigName = cfg.getName();
        }
    }

    /**
     * Set a configuration in the domain. This checks that the seedlists and passwords are legal.
     *
     * @param cfg The configuration to add.
     */
    private void putConfiguration(DomainConfiguration cfg) {
        checkListContainsNamed(cfg, cfg.getSeedLists(), "seedlist", seedlists);
        checkListContainsNamed(cfg, cfg.getPasswords(), "passwords", passwords);

        domainConfigurations.put(cfg.getName(), cfg);
    }

    /**
     * Helper method used to verify that a configuration does not reference seedlists or passwords that do not exist in
     * this domain.
     *
     * @param cfg the configuration being checked
     * @param items an iterator to the references that are checked (seedlists or passwords)
     * @param typename the name of the references being checked
     * @param m the corresponding domain map that must contain entries matching the names in the items
     * @param <T> The type contained in items iterator. The type extends Named
     */
    private <T extends Named> void checkListContainsNamed(DomainConfiguration cfg, final Iterator<T> items,
            final String typename, final Map m) {
        while (items.hasNext()) {
            Named named = items.next();

            if (!m.containsKey(named.getName())) {
                throw new UnknownID("Configuration:" + cfg.getName() + "; uses unknown " + typename + ":"
                        + named.getName() + "; in the domain:" + getName() + ";");
            }
        }
    }

    /**
     * Helper method that adds or updates an entry in a map. Used to add/update entries in seedlists and passwords maps
     *
     * @param m the map to modify
     * @param name the name of the element to add or update
     * @param addAction when true an add action is performed and en entry with the name is not allowed to exist in the
     * map before the operation, when false an update operation is performed and an entry must already exists with the
     * name in the map.
     * @param value the object to add to m
     * @param <T> The type contained as values in the map m.
     */
    private <T extends Named> void put(Map<String, T> m, String name, boolean addAction, T value) {
        boolean alreadyExist = m.containsKey(name);

        if (addAction && alreadyExist) {
            throw new PermissionDenied("An entry already exists with the name:" + name + "; in the domain:" + getName()
                    + ";");
        }

        if ((!addAction) && (!alreadyExist)) {
            throw new UnknownID("No entry exists with the name '" + name + "' in the domain '" + getName() + "'");
        }

        m.put(name, value);
    }

    /**
     * Adds a seed list to the domain.
     *
     * @param seedlist the actual seedslist.
     * @throws ArgumentNotValid if an argument is null
     * @throws PermissionDenied if the seedName already exists
     */
    public void addSeedList(SeedList seedlist) {
        ArgumentNotValid.checkNotNull(seedlist, "seedlist");
        put(seedlists, seedlist.getName(), true, seedlist);
    }

    /**
     * Update a seed list to the domain. Replaces an existing seedlist with the same name.
     *
     * @param seedlist the actual seedslist.
     * @throws ArgumentNotValid if an argument is null
     * @throws UnknownID if the seedlist.getName() does not exists
     */
    public void updateSeedList(SeedList seedlist) {
        ArgumentNotValid.checkNotNull(seedlist, "seedlist");
        put(seedlists, seedlist.getName(), false, seedlist);
    }

    /**
     * Adds a password to the domain.
     *
     * @param password A password object to add.
     * @throws ArgumentNotValid if the argument is null
     * @throws PermissionDenied if a password already exists with this name
     */
    public void addPassword(Password password) {
        ArgumentNotValid.checkNotNull(password, "password");
        put(passwords, password.getName(), true, password);
    }

    /**
     * Updates a password on the domain.
     *
     * @param password A password object to update.
     * @throws ArgumentNotValid if the argument is null
     * @throws PermissionDenied if no password exists with this name
     */
    public void updatePassword(Password password) {
        ArgumentNotValid.checkNotNull(password, "password");
        put(passwords, password.getName(), false, password);
    }

    /**
     * Mark a configuration as the default configuration to use. The configuration name must match an already added
     * configuration, otherwise an UnknownID exception is thrown.
     *
     * @param cfgName a name of a configuration
     * @throws UnknownID when the cfgName does not match an added configuration
     * @throws ArgumentNotValid if cfgName is null or empty
     */
    public void setDefaultConfiguration(String cfgName) {
        ArgumentNotValid.checkNotNullOrEmpty(cfgName, "cfgName");

        if (!domainConfigurations.containsKey(cfgName)) {
            throw new UnknownID("Default configuration not registered:" + cfgName + "; in the domain:" + getName()
                    + ";");
        }

        defaultConfigName = cfgName;
    }

    /**
     * Returns an already registered configuration.
     *
     * @param cfgName the name of an registered configuration
     * @return the configuration
     * @throws UnknownID if the name is not a registered configuration
     * @throws ArgumentNotValid if cfgName is null or empty
     */
    public DomainConfiguration getConfiguration(String cfgName) {
        ArgumentNotValid.checkNotNullOrEmpty(cfgName, "cfgName");

        if (!domainConfigurations.containsKey(cfgName)) {
            throw new UnknownID("Configuration '" + cfgName + "' not registered in the domain '" + getName() + "'");
        }
        DomainConfiguration cfg = domainConfigurations.get(cfgName);
        cfg.setDomainhistory(this.getHistory());
        return cfg;
    }

    /**
     * Gets the default configuration. If no configuration has been explicitly set the first configuration added to this
     * domain is returned. If no configurations have been added at all a UnknownID exception is thrown.
     *
     * @return the default configuration (never null)
     * @throws UnknownID if no configurations exists
     */
    public DomainConfiguration getDefaultConfiguration() {
        if (domainConfigurations.size() == 0) {
            throw new UnknownID("No configurations have been registered in the domain:" + getName() + ";");
        }

        return getConfiguration(defaultConfigName);
    }

    /**
     * Gets the name of this domain.
     *
     * @return the name of this domain
     */
    public String getName() {
        return domainName;
    }

    /**
     * @return the domain comments.
     */
    public String getComments() {
        return comments;
    }

    /**
     * Get the domain history.
     *
     * @return the domain history
     */
    public DomainHistory getHistory() {
        return history;
    }

    /**
     * Get a specific seedlist previously added to this domain.
     *
     * @param name the name of the seedlist to return
     * @return the specified seedlist
     * @throws ArgumentNotValid if name is null or empty
     * @throws UnknownID if no seedlist has been added with the supplied name
     */
    public SeedList getSeedList(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");

        if (!hasSeedList(name)) {
            throw new UnknownID("Seedlist '" + name + " has not been registered in the domain '" + getName() + "'");
        }

        return seedlists.get(name);
    }

    /**
     * Return true if the named seedlist exists in this domain.
     *
     * @param name String representing a possible seedlist for the domain.
     * @return true, if the named seedlist exists in this domain
     */
    public boolean hasSeedList(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");

        return seedlists.containsKey(name);
    }

    /**
     * Removes a seedlist from this Domain. The seedlist must not be in use by any of the configurations, otherwise a
     * PermissionDenied exception is thrown.
     *
     * @param name the name of the seedlist to remove
     * @throws PermissionDenied if the seedlist is in use by a configuration or this is the last seedlist in this Domain
     * @throws UnknownID if the no seedlist exists with the name
     * @throws ArgumentNotValid if a null argument is supplied
     */
    public void removeSeedList(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");

        if (!seedlists.containsKey(name)) {
            throw new UnknownID("Seedlist has not been registered:" + name + "; in the domain:" + getName() + ";");
        }

        if (seedlists.size() <= 1) {
            throw new PermissionDenied("Can not remove the last seedlist:" + name + ";");
        }

        for (String cfgname : domainConfigurations.keySet()) {
            DomainConfiguration cfg = domainConfigurations.get(cfgname);

            for (Iterator<SeedList> i = cfg.getSeedLists(); i.hasNext();) {
                SeedList seedlist = i.next();

                if (seedlist.getName().equals(name)) {
                    throw new PermissionDenied("The seedlist:" + name + "; is used by the configuration:" + cfgname
                            + ";");
                }
            }
        }

        // if we get here without an exception - the seedlist is not in use
        seedlists.remove(name);
    }

    /**
     * Removes a password from this Domain. The password must not be in use by any of the configurations, otherwise a
     * PermissionDenied exception is thrown.
     *
     * @param name the name of the password to remove
     * @throws PermissionDenied if the password is in use by a configuration or this is the last password in this Domain
     * @throws UnknownID if the no password exists with the name
     * @throws ArgumentNotValid if a null argument is supplied
     */
    public void removePassword(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");

        if (!passwords.containsKey(name)) {
            throw new UnknownID("Password has not been registered:" + name + "; in the domain:" + getName() + ";");
        }

        for (String cfgname : domainConfigurations.keySet()) {
            DomainConfiguration cfg = domainConfigurations.get(cfgname);

            if (cfg.usesPassword(name)) {
                throw new PermissionDenied("The password:" + name + "; is used by the configuration:" + cfgname + ";");
            }
        }

        // if we get here without an exception - the password is not in use
        passwords.remove(name);
    }

    /**
     * Removes a configuration from this domain. The default configuration can not be removed, instead PermissionDenied
     * is thrown. It is not possible to remove a configuration that is referenced by one or more HarvestDefinitions
     *
     * @param configName The name of a configuration to remove.
     * @throws ArgumentNotValid if name is null or empty
     * @throws PermissionDenied if the default configuration is attempted removed or if one or more HarvestDefinitions
     * reference the configuration
     */
    public void removeConfiguration(String configName) {
        ArgumentNotValid.checkNotNullOrEmpty(configName, "configName");

        if (defaultConfigName.equals(configName)) {
            throw new PermissionDenied("The default configuration can not be removed:" + configName + ";");
        }

        if (!domainConfigurations.containsKey(configName)) {
            throw new UnknownID("Configuration not registered:" + configName + ";");
        }

        // Test that no harvest definition uses this configuration
        final DomainDAO dao = DomainDAO.getInstance();
        if (!dao.mayDelete(getConfiguration(configName))) {
            // Since this is an error case, spend a little time getting better
            // info. This could be done a lot faster by adding a function to
            // the DomainDAO.
            HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
            Iterator<HarvestDefinition> hds = hddao.getAllHarvestDefinitions();
            List<String> usages = new ArrayList<String>();
            while (hds.hasNext()) {
                HarvestDefinition hd = hds.next();
                Iterator<DomainConfiguration> configs = hd.getDomainConfigurations();
                while (configs.hasNext()) {
                    DomainConfiguration dc = configs.next();
                    if (dc.getName().equals(configName) && dc.getDomainName().equals(getName())) {
                        usages.add(hd.getName());
                    }
                }
            }
            throw new PermissionDenied("Cannot delete domain configuration '" + configName + "', because it is used "
                    + "by the following " + "harvest definitions: " + usages);
        }

        domainConfigurations.remove(configName);
    }

    /**
     * Gets all configurations belonging to this domain.
     *
     * @return all configurations belonging to this domain.
     */
    public Iterator<DomainConfiguration> getAllConfigurations() {
        return domainConfigurations.values().iterator();
    }

    /**
     * Get all seedlists belonging to this domain.
     *
     * @return all seedlists belonging to this domain
     */
    public Iterator<SeedList> getAllSeedLists() {
        return seedlists.values().iterator();
    }

    /**
     * Return the passwords defined for this domain.
     *
     * @return Iterator<Password> of known passwords.
     */
    public Iterator<Password> getAllPasswords() {
        return passwords.values().iterator();
    }

    /**
     * Gets all configurations belonging to this domain. The returned list is sorted by name according to language given
     * in the parameter.
     *
     * @param loc contains the language sorting must adhere to
     * @return all configurations belonging to this domain sorted according to language
     */
    public List<DomainConfiguration> getAllConfigurationsAsSortedList(Locale loc) {
        ArgumentNotValid.checkNotNull(loc, "loc");
        List<DomainConfiguration> resultSet = new ArrayList<DomainConfiguration>(domainConfigurations.values());
        NamedUtils.sortNamedObjectList(loc, resultSet);
        return resultSet;
    }

    /**
     * Gets all seedlists belonging to this domain. The returned list is sorted by name according to language given in
     * the parameter.
     *
     * @param loc contains the language sorting must adhere to
     * @return all seedlists belonging to this domain sorted according to language
     */
    public List<SeedList> getAllSeedListsAsSortedList(Locale loc) {
        ArgumentNotValid.checkNotNull(loc, "loc");
        List<SeedList> resultSet = new ArrayList<SeedList>(seedlists.values());
        NamedUtils.sortNamedObjectList(loc, resultSet);
        return resultSet;
    }

    /**
     * Returns the passwords defined for this domain. The returned list is sorted by name according to language given in
     * the parameter.
     *
     * @param loc contains the language sorting must adhere to
     * @return a sorted list of known passwords according to language
     */
    public List<Password> getAllPasswordsAsSortedList(Locale loc) {
        ArgumentNotValid.checkNotNull(loc, "loc");
        List<Password> resultSet = new ArrayList<Password>(passwords.values());
        NamedUtils.sortNamedObjectList(loc, resultSet);
        return resultSet;
    }

    /**
     * Add owner information.
     *
     * @param owner owner
     */
    public void addOwnerInfo(DomainOwnerInfo owner) {
        ArgumentNotValid.checkNotNull(owner, "owner");
        domainOwnerInfos.add(owner);
    }

    /**
     * Get array of domain owner information.
     *
     * @return array containing information about the domain owner(s)
     */
    public DomainOwnerInfo[] getAllDomainOwnerInfo() {
        return domainOwnerInfos.toArray(new DomainOwnerInfo[0]);
    }

    /**
     * Get password information.
     *
     * @param name the id of the password settings to retrieve
     * @return the password information
     * @throws UnknownID if no password info exists with the id "name"
     */
    public Password getPassword(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");

        if (!passwords.containsKey(name)) {
            throw new UnknownID("Password has not been registered:" + name + "; in the domain:" + getName() + ";");
        }

        return passwords.get(name);
    }

    /**
     * Set the comments for this domain.
     *
     * @param comments The new comments (can be null)
     */
    public void setComments(String comments) {
        this.comments = comments;
    }

    /**
     * Replaces existing configuration with cfg, using cfg.getName() as the id for the configuration.
     *
     * @param cfg the configuration to update
     * @throws UnknownID if no configuration exists with the id cfg.getName(). ArgumentNotValid if cfg is null.
     */
    public void updateConfiguration(DomainConfiguration cfg) {
        ArgumentNotValid.checkNotNull(cfg, "cfg");

        if (!domainConfigurations.containsKey(cfg.getName())) {
            throw new UnknownID("No configuration exists with the name:" + cfg.getName() + "; in the domain:"
                    + getName() + ";");
        }

        putConfiguration(cfg);
    }

    /**
     * Returns true if this domain has the named password.
     *
     * @param passwordName the identifier of the password info
     * @return true if this domain has password info with id passwordname
     */
    public boolean hasPassword(String passwordName) {
        return passwords.containsKey(passwordName);
    }

    /**
     * Returns true if this domain has the named configuration.
     *
     * @param configName the identifier of the configuration
     * @return true if this domain has a configuration with id configNmae
     */
    public boolean hasConfiguration(String configName) {
        return domainConfigurations.containsKey(configName);
    }

    /**
     * Get the edition number.
     *
     * @return the edition number
     */
    public long getEdition() {
        return edition;
    }

    /**
     * Set the edition number.
     *
     * @param theNewEdition the new edition
     */
    public void setEdition(long theNewEdition) {
        edition = theNewEdition;
    }

    /**
     * Get the ID of this domain. Only for use by DBDAO
     *
     * @return Get the ID of this domain
     */
    public long getID() {
        return id;
    }

    /**
     * Set the ID of this domain. Only for use by DBDAO.
     *
     * @param newId The new ID for this domain.
     */
    void setID(long newId) {
        this.id = newId;
    }

    /**
     * Check if this harvestinfo has an ID set yet (doesn't happen until the DBDAO persists it).
     *
     * @return true, if this domain has an ID different from null
     */
    boolean hasID() {
        return id != null;
    }

    /**
     * Return a human-readable representation of this object.
     *
     * @return Some string identifying the object. Do not use this for machine processing.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Domain:").append(getName()).append(";\n");
        sb.append("Comment:").append(getComments()).append(";\n");

        sb.append("Configurations:\n");

        for (String cfgName : domainConfigurations.keySet()) {
            sb.append("\t").append(cfgName).append(";\n");
        }

        sb.append("Seedlists:\n");

        for (String seedName : seedlists.keySet()) {
            sb.append("\t").append(seedName).append(";\n");
        }

        sb.append("Passwords:\n");

        for (String pwName : passwords.keySet()) {
            sb.append("\t").append(pwName).append(";\n");
        }

        sb.append("Extended Fields:\n");

        for (int i = 0; i < extendedFieldValues.size(); i++) {
            ExtendedFieldValue efv = extendedFieldValues.get(i);
            sb.append("\t").append(efv.getExtendedFieldID() + ": " + efv.getContent()).append(";\n");
        }

        sb.append("---------------\n");

        return sb.toString();
    }

    /**
     * Sets a list of regular expressions defining urls that should never be harvested from this domain. The list (after
     * trimming the strings, and any empty strings have been removed) is copied to a list that is stored immutably.
     *
     * @param regExps The list defining urls never to be harvested.
     * @param strictMode If true, we throw ArgumentNotValid exception if invalid regexps are found
     * @throws ArgumentNotValid if regExps is null or regExps contains invalid regular expressions (unless strictMode is
     * false).
     */
    public void setCrawlerTraps(List<String> regExps, boolean strictMode) {
        ArgumentNotValid.checkNotNull(regExps, "List<String> regExps");
        List<String> cleanedListOfCrawlerTraps = new ArrayList<String>();
        for (String crawlerTrap : regExps) {
            log.trace("original trap: '" + crawlerTrap + "'");
            String trimmedString = crawlerTrap.trim();
            log.trace("trimmed  trap: '" + trimmedString + "'");
            if (!(trimmedString.length() == 0)) {
                cleanedListOfCrawlerTraps.add(crawlerTrap);
            } else {
                log.trace("Removed empty string from list of crawlertraps");
            }
        }
        // Validate regexps
        List<String> errMsgs = new ArrayList<String>();
        for (String regexp : cleanedListOfCrawlerTraps) {
        	
        	boolean wellformed = false;
            try {
                Pattern.compile(regexp);
                wellformed = CrawlertrapsUtils.isCrawlertrapsWellformedXML(regexp);
                if (!wellformed){
                	errMsgs.add("The expression '" + regexp + "' is not wellformed XML" 
                    		+ " . Please correct the expression.");
                }
            } catch (PatternSyntaxException e) {
                errMsgs.add("The expression '" + regexp + "' is not a proper regular expression: " 
                		+ e.getDescription() + " . Please correct the expression.");
            }
        }
        if (errMsgs.size() > 0) {
            if (strictMode){ 
                throw new ArgumentNotValid(errMsgs.size() +  " errors were found: " + StringUtils.conjoin(",", errMsgs));
            } else {
                log.warn(errMsgs.size() +  " errors were found: " + StringUtils.conjoin(",", errMsgs));
            }
        }
        crawlerTraps = Collections.unmodifiableList(cleanedListOfCrawlerTraps);
        if (!crawlerTraps.isEmpty()) {
            log.trace("Domain {} has {} crawlertraps", domainName, crawlerTraps.size());
        }
    }

    /**
     * Returns the list of regexps never to be harvested from this domain, or the empty list if none. The returned list
     * should never be null.
     *
     * @return The list of regexps of url's never to be harvested when harvesting this domain. This list is immutable.
     */
    public List<String> getCrawlerTraps() {
        return crawlerTraps;
    }

    /**
     * Returns the alias info for this domain, or null if this domain is not an alias.
     *
     * @return A domain name.
     */
    public AliasInfo getAliasInfo() {
        return aliasInfo;
    }

    /**
     * Update which domain this domain is considered an alias of. Calling this function will a) cause some slightly
     * expensive checks to be performed, and b) set the time of last update. For object construction and copying, use
     * setAlias.
     *
     * @param alias The name (e.g. "netarkivet.dk") of the domain that this domain is an alias of.
     * @throws UnknownID If the given domain does not exist
     * @throws IllegalState If updating the alias info would violate constraints of alias: No transitivity, no
     * reflection.
     */
    public void updateAlias(String alias) {
        if (getName().equals(alias)) {
            String message = "Cannot make domain '" + this.getName() + "' an alias of itself";
            log.debug(message);
            throw new IllegalState(message);
        }

        if (alias != null) {
            DomainDAO dao = DomainDAO.getInstance();
            Domain otherD = dao.read(alias);
            if (otherD.aliasInfo != null) {
                String message = "Cannot make domain '" + this.getName() + "' an alias of '" + otherD.getName() + "',"
                        + " as that domain is already an alias of '" + otherD.aliasInfo.getAliasOf() + "'";
                log.debug(message);
                throw new IllegalState(message);
            }
            if (dao.getAliases(getName()).size() != 0) {
                List<String> aliasesForThisDomain = new ArrayList<String>();
                for (AliasInfo ai : dao.getAliases(getName())) {
                    aliasesForThisDomain.add(ai.getDomain());
                }
                String message = "Cannot make domain '" + this.getName() + "' an alias of '" + otherD.getName() + "',"
                        + " as the domains '" + StringUtils.conjoin(",", aliasesForThisDomain) + "' are "
                        + "already aliases of '" + this.getName() + "'";
                log.debug(message);
                throw new IllegalState(message);
            }
            setAliasInfo(new AliasInfo(domainName, alias, new Date()));
        } else {
            setAliasInfo(null);
        }
    }

    /**
     * Set the alias field on this object. This function performs no checking of existence of transitivity of alias
     * domains, but it does check that the alias info is for this domain
     *
     * @param aliasInfo Alias information
     * @throws ArgumentNotValid if the alias info is not for this domain
     */
    void setAliasInfo(AliasInfo aliasInfo) {
        if (aliasInfo != null && !aliasInfo.getDomain().equals(domainName)) {
            throw new ArgumentNotValid("AliasInfo must be for this domain");
        }
        this.aliasInfo = aliasInfo;
    }

    /**
     * Gets the harvest info giving best information for expectation or how many objects a harvest using a given
     * configuration will retrieve, we will prioritise the most recently harvest, where we have a full harvest.
     *
     * @param configName The name of the configuration
     * @return The Harvest Information for the harvest defining the best expectation, including the number retrieved and
     * the stop reason.
     */
    public HarvestInfo getBestHarvestInfoExpectation(String configName) {
        ArgumentNotValid.checkNotNullOrEmpty(configName, "String configName");
        return DomainHistory.getBestHarvestInfoExpectation(configName, this.getHistory());
    }

    /**
     * All derived classes allow ExtendedFields from Type ExtendedFieldTypes.DOMAIN
     *
     * @return ExtendedFieldTypes.DOMAIN
     */
    protected int getExtendedFieldType() {
        return ExtendedFieldTypes.DOMAIN;
    }

}
