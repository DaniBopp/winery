/*******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/
package org.eclipse.winery.repository.backend.filebased;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.winery.model.tosca.constants.Namespaces;
import org.eclipse.winery.repository.backend.NamespaceManager;

import org.apache.commons.configuration.Configuration;

public class ConfigurationBasedNamespaceManager implements NamespaceManager {

    private Configuration configuration;
    private Map<String, String> namespaceToPrefixMap = new HashMap<>();

    /**
     * @param configuration The configuration to read from and store data into
     */
    public ConfigurationBasedNamespaceManager(Configuration configuration) {
        this.configuration = configuration;

        // globally set prefixes

        // if that behavior is not desired, the code has to be moved to "generatePrefix" which checks for existence, ...
        this.configuration.setProperty(Namespaces.TOSCA_NAMESPACE, "tosca");
        this.configuration.setProperty(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, "winery");
        this.configuration.setProperty(Namespaces.W3C_XML_SCHEMA_NS_URI, "xsd");
        this.configuration.setProperty(Namespaces.W3C_NAMESPACE_URI, "xmlns");

        // example namespaces opened for users to create new types
        this.configuration.setProperty(Namespaces.EXAMPLE_NAMESPACE_URI, "ex");
    }

    @Override
    public String getPrefix(String namespace) {
        if (namespace == null) {
            namespace = "";
        }

        // configuration stores the permanent mapping
        // this has precedence
        String prefix = configuration.getString(namespace);
        if (prefix == null || prefix.isEmpty()) {
            // in case no permanent mapping is found - or the prefix is invalid, check the in-memory ones
            prefix = this.namespaceToPrefixMap.get(namespace);
            if (prefix == null) {
                prefix = this.generatePrefix(namespace);
                this.namespaceToPrefixMap.put(namespace, prefix);
            }
        }
        return prefix;
    }

    @Override
    public boolean hasPermanentPrefix(String namespace) {
        return this.configuration.containsKey(namespace);
    }

    @Override
    public void removePermanentPrefix(String namespace) {
        this.configuration.clearProperty(namespace);
        // ensure that in-memory mapping also does not have the key any more
        this.namespaceToPrefixMap.remove(namespace);
    }

    @Override
    public void setPermanentPrefix(String namespace, String prefix) {
        if (Objects.isNull(namespace) || Objects.isNull(prefix)) {
            return;
        }
        if (namespace.isEmpty() || prefix.isEmpty()) {
            return;
        }
        if (!this.getAllPermanentPrefixes().contains(prefix)) {
            this.configuration.setProperty(namespace, prefix);
            // ensure that in-memory mapping also does not have the key any more
            this.namespaceToPrefixMap.remove(namespace);
        }
    }

    /**
     * Generates a string indicating the kind of definitions children maintained by the namespace
     *
     * @return empty string if nothing could be matched
     */
    private String generateKindString(String namespace) {
        String mid;
        if (namespace.contains("servicetemplates/")) {
            mid = "ste";
        } else if (namespace.contains("nodetypes/")) {
            mid = "nty";
        } else if (namespace.contains("nodetypeimplementations/")) {
            mid = "ntyi";
        } else if (namespace.contains("relationshiptypes/")) {
            mid = "nty";
        } else if (namespace.contains("relationshiptypeimplementations/")) {
            mid = "rtyi";
        } else if (namespace.contains("artifacttypes/")) {
            mid = "aty";
        } else if (namespace.contains("artifacttemplates/")) {
            mid = "ate";
        } else if (namespace.contains("requirementtypes/")) {
            mid = "rty";
        } else if (namespace.contains("capabilitytypes/")) {
            mid = "cty";
        } else if (namespace.contains("policytypes/")) {
            mid = "pty";
        } else if (namespace.contains("policytemplates/")) {
            mid = "pte";
        } else if (namespace.contains("compliancerules/")) {
            mid = "cr";
        } else if (namespace.contains("types/")) {
            mid = "ty";
        } else if (namespace.contains("templates/")) {
            mid = "te";
        } else {
            mid = "";
        }
        return mid;
    }

    /**
     * Tries to generate a prefix based on the last part of the URL
     */
    public String generatePrefixProposal(String namespace, int round) {
        Objects.requireNonNull(namespace);
        String[] split = namespace.split("/");
        if (split.length == 0) {
            return String.format("ns%d", round);
        } else {
            String result;
            result = split[split.length - 1].replaceAll("[^A-Za-z]+", "");

            String prefix;
            if (namespace.startsWith(Namespaces.URI_START_OPENTOSCA)) {
                prefix = "ot";
            } else {
                prefix = "";
            }

            String mid = this.generateKindString(namespace);

            if (result.isEmpty()) {
                if (prefix.isEmpty()) {
                    if ((round == 0) && namespace.isEmpty()) {
                        return "null";
                    }
                    prefix = "ns";
                }
                return String.format("%s%s%d", prefix, mid, round);
            } else {
                if (namespace.contains("propertiesdefinition") && result.equals("winery")) {
                    // in case, it is a winery propertiesdefinition, end with "pd" (and not with winery or pdwinery)
                    result = "pd";
                }
                if (round == 0) {
                    return prefix + mid + result;
                } else {
                    return String.format("%s%s%s%d", prefix, mid, result, round);
                }
            }
        }
    }

    /**
     * Generates a prefix for the given namespace. There must not be a prefix existing for the namespace.
     */
    private String generatePrefix(String namespace) {
        Objects.requireNonNull(namespace);

        String prefix;
        Set<String> allPrefixes = new HashSet<>();
        allPrefixes.addAll(this.getAllPermanentPrefixes());
        allPrefixes.addAll(this.namespaceToPrefixMap.values());

        int round = 0;
        do {
            prefix = generatePrefixProposal(namespace, round);
            round++;
        } while (allPrefixes.contains(prefix));
        return prefix;
    }

    public Collection<String> getAllPermanentPrefixes() {
        Iterator<String> keys = this.configuration.getKeys();
        Set<String> res = new HashSet<>();
        while (keys.hasNext()) {
            String key = keys.next();
            String prefix = this.configuration.getString(key);
            res.add(prefix);
        }
        return res;
    }

    @Override
    public Collection<String> getAllPermanentNamespaces() {
        Iterator<String> keys = this.configuration.getKeys();
        Set<String> res = new HashSet<>();
        while (keys.hasNext()) {
            res.add(keys.next());
        }
        return res;
    }

    @Override
    public void clear() {
        this.configuration.clear();
    }
}
