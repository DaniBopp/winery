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

package org.eclipse.winery.model.adaptation.substitution.refinement;

import java.sql.Ref;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.winery.common.ids.definitions.RefinementId;
import org.eclipse.winery.model.tosca.OTPatternRefinementModel;
import org.eclipse.winery.model.tosca.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TNodeTemplate;

import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.addPermutabilityMapping;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.detectorNodeIsTheOnlyMappingToThisRefinementNode;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.isStayingElement;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.permutabilityMappingsExistsForRefinementNode;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.stayMappingsExistsForRefinementNode;

public class PermutationGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PermutationGenerator.class);

    private final Class<? extends RefinementId> idClass;

    public PermutationGenerator(Class<? extends RefinementId> idClass) {
        this.idClass = idClass;
    }

    public boolean checkPermutability(OTTopologyFragmentRefinementModel refinementModel) {
        logger.debug("Starting permutability check of {}", refinementModel.getIdFromIdOrNameField());
        List<TNodeTemplate> detectorNodeTemplates = refinementModel.getRefinementTopology().getNodeTemplates();
        Set<TNodeTemplate> permutableNodes = detectorNodeTemplates.stream()
            .filter(nodeTemplate -> isStayingElement(nodeTemplate, refinementModel))
            .collect(Collectors.toSet());

        Set<MutableSet<TNodeTemplate>> permutations = Sets.powerSet(permutableNodes);

        for (TNodeTemplate detectorNodeTemplate : detectorNodeTemplates) {
            refinementModel.getRelationMappings().stream()
                .filter(relationMapping -> relationMapping.getRefinementNode().equals(detectorNodeTemplate))
                .forEach(relationMapping -> this.checkComponentPermutability(relationMapping.getRefinementNode(), detectorNodeTemplate, refinementModel));
        }

        return false;
    }

    private void checkComponentPermutability(TEntityTemplate refinementNode, TNodeTemplate detectorNode, OTTopologyFragmentRefinementModel refinementModel) {
        logger.debug("Checking component permutability of detectorNode \"{}\" to refinementNode \"{}\"", refinementNode.getId(), detectorNode.getId());

        if (!permutabilityMappingsExistsForRefinementNode(refinementNode, refinementModel) &&
            !stayMappingsExistsForRefinementNode(refinementNode, refinementModel)) {
            if (detectorNodeIsTheOnlyMappingToThisRefinementNode(detectorNode, refinementNode, refinementModel)) {
                addPermutabilityMapping(detectorNode, refinementNode, refinementModel);
            } else if (refinementModel instanceof OTPatternRefinementModel) {
                ArrayList<TNodeTemplate> patternSet = new ArrayList<>();
                // todo
            }
        }
    }
}
