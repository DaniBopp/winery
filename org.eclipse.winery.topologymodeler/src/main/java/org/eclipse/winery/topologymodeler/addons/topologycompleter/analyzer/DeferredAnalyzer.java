/*******************************************************************************
 * Copyright (c) 2013 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.topologymodeler.addons.topologycompleter.analyzer;

import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.topologymodeler.addons.topologycompleter.helper.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains a method to analyze a TOSCA {@link TTopologyTemplate} for
 * the occurrence of "Deferred"-{@link TRelationshipTemplate}s.
 * <p>
 * A "Deferred"-{@link TRelationshipTemplate} serves as place holder for any number of Node or Relationship
 * Templates.
 */
public class DeferredAnalyzer {

    /**
     * Iterates over all {@link TRelationshipTemplate} and checks if its type is "deferred".
     *
     * @param toscaAnalyzer the {@link TOSCAAnalyzer} object to access the data model
     * @return a list of found deferred {@link TRelationshipTemplate}s
     */
    public static List<TRelationshipTemplate> analyzeDeferredRelations(TOSCAAnalyzer toscaAnalyzer) {

        List<TRelationshipTemplate> foundDeferredRelations = new ArrayList<TRelationshipTemplate>();

        for (TRelationshipTemplate relationshipTemplate : toscaAnalyzer.getRelationshipTemplates()) {
            if (relationshipTemplate.getType() != null && relationshipTemplate.getType().getLocalPart().equals(Constants.DEFERRED_QNAME.getLocalPart()) &&
                relationshipTemplate.getType().getNamespaceURI().equals(Constants.DEFERRED_QNAME.getNamespaceURI())) {

                // TODO: This step has to be done until the "Provisioning-API"
                // is implemented. The Deferred RelationshipTemplate can only be
                // completed if Requirements exist at the source template.
                TNodeTemplate source = (TNodeTemplate) relationshipTemplate.getSourceElement().getRef();

                if (source.getRequirements() != null) {
                    foundDeferredRelations.add(relationshipTemplate);
                }
            }
        }
        return foundDeferredRelations;
    }
}
