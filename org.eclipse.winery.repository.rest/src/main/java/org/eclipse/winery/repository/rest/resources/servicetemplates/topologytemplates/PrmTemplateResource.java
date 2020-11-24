/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.repository.rest.resources.servicetemplates.topologytemplates;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.OTPermutationMapping;
import org.eclipse.winery.model.tosca.OTPrmMapping;
import org.eclipse.winery.model.tosca.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.repository.rest.resources._support.AbstractComponentInstanceResourceContainingATopology;

public class PrmTemplateResource extends TopologyTemplateResource {
    private static TTopologyTemplate prmModellingTopologyTemplate = new TTopologyTemplate();

    /**
     * A topology template is always nested in a service template
     */
    public PrmTemplateResource(AbstractComponentInstanceResourceContainingATopology parent, OTTopologyFragmentRefinementModel refinementModel, String type) {
        super(parent, prmModellingTopologyTemplate, type);
        createPrmTopologyTemplate(refinementModel);
    }

    private void createPrmTopologyTemplate(OTTopologyFragmentRefinementModel refinementModel) {
        prmModellingTopologyTemplate.setNodeTemplates(new ArrayList());
        prmModellingTopologyTemplate.setRelationshipTemplates(new ArrayList());

        for (TNodeTemplate nodeTemplate : refinementModel.getDetector().getNodeTemplates()) {
            prmModellingTopologyTemplate.addNodeTemplate(nodeTemplate);
        }
        for (TNodeTemplate nodeTemplate : refinementModel.getRefinementTopology().getNodeTemplates()) {
            TNodeTemplate newNodeTemplate = nodeTemplate;
            int newX = Integer.parseInt(newNodeTemplate.getX()) + 500;
            newNodeTemplate.setX(String.valueOf(newX));
            prmModellingTopologyTemplate.addNodeTemplate(newNodeTemplate);
        }
        for (TRelationshipTemplate relationshipTemplate : refinementModel.getDetector().getRelationshipTemplates()) {
            prmModellingTopologyTemplate.addRelationshipTemplate(relationshipTemplate);
        }
        for (TRelationshipTemplate relationshipTemplate : refinementModel.getRefinementTopology().getRelationshipTemplates()) {
            prmModellingTopologyTemplate.addRelationshipTemplate(relationshipTemplate);
        }
        createRelationshipsForMappings(refinementModel);
    }

    private void createRelationshipsForMappings(OTTopologyFragmentRefinementModel refinementModel) {
        for (OTPermutationMapping mapping : refinementModel.getPermutationMappings()) {
            TRelationshipType permutationMapping = new TRelationshipType(new TRelationshipType.Builder("Permutation Mapping"));
            TRelationshipTemplate.SourceOrTargetElement sourceElement = new TRelationshipTemplate.SourceOrTargetElement();
            sourceElement.setRef(new TNodeTemplate(mapping.getDetectorElement().getId()));
            TRelationshipTemplate.SourceOrTargetElement targetElement = new TRelationshipTemplate.SourceOrTargetElement();
            targetElement.setRef(new TNodeTemplate(mapping.getRefinementElement().getId()));

            TRelationshipTemplate.Builder builder = new TRelationshipTemplate.Builder("Permutation Mapping", new QName(mapping.getId()), sourceElement, targetElement);
            builder.setName(mapping.getId());
            TRelationshipTemplate templateForMapping = new TRelationshipTemplate(builder);
            //templateForMapping.setName(permutationMapping.getName());
            prmModellingTopologyTemplate.addRelationshipTemplate(templateForMapping);
        }
    }

    //TODO
    private void createRelationships(List<OTPrmMapping> prmmappings) {
        for (OTPrmMapping mapping : prmmappings) {
            TRelationshipTemplate.SourceOrTargetElement sourceElement = new TRelationshipTemplate.SourceOrTargetElement();
            sourceElement.setRef(new TNodeTemplate((mapping.getDetectorElement().getId())));
            TRelationshipTemplate.SourceOrTargetElement targetElement = new TRelationshipTemplate.SourceOrTargetElement();
            targetElement.setRef(new TNodeTemplate(mapping.getRefinementElement().getId()));

            TRelationshipTemplate.Builder builder = new TRelationshipTemplate.Builder("Permutation Mapping", new QName(mapping.getId()), sourceElement, targetElement);
            builder.setName(mapping.getId());
            TRelationshipTemplate templateForMapping = new TRelationshipTemplate(builder);

            prmModellingTopologyTemplate.addRelationshipTemplate(templateForMapping);
        }
    }
}
