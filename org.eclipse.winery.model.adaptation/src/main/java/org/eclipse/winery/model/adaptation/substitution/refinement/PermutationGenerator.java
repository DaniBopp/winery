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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.winery.model.tosca.OTComponentSet;
import org.eclipse.winery.model.tosca.OTPrmMapping;
import org.eclipse.winery.model.tosca.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TNodeTemplate;

import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.addPermutabilityMapping;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.getAllMappingsForRefinementNodeWithoutDetectorNode;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.isStayingElement;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.permutabilityMappingsExistsForRefinementNode;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.stayMappingsExistsForRefinementNode;

public class PermutationGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PermutationGenerator.class);

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
                .forEach(relationMapping -> this.checkComponentPermutability(relationMapping.getRefinementNode(),
                    detectorNodeTemplate,
                    refinementModel,
                    permutations));
        }

        return false;
    }

    private void checkComponentPermutability(TEntityTemplate refinementNode,
                                             TNodeTemplate detectorNode,
                                             OTTopologyFragmentRefinementModel refinementModel,
                                             Set<MutableSet<TNodeTemplate>> permutations) {
        logger.debug("Checking component permutability of detectorNode \"{}\" to refinementNode \"{}\"",
            refinementNode.getId(), detectorNode.getId());

        if (!permutabilityMappingsExistsForRefinementNode(refinementNode, refinementModel) &&
            !stayMappingsExistsForRefinementNode(refinementNode, refinementModel)) {
            List<OTPrmMapping> mappingsWithoutDetectorNode =
                getAllMappingsForRefinementNodeWithoutDetectorNode(detectorNode, refinementNode, refinementModel);

            if (mappingsWithoutDetectorNode.size() == 0) {
                logger.debug("Adding PermutabilityMapping between \"{}\" and \"{}\"", detectorNode.getId(), refinementNode.getId());
                addPermutabilityMapping(detectorNode, refinementNode, refinementModel);
            } else {
                ArrayList<String> patternSet = new ArrayList<>();
                mappingsWithoutDetectorNode.stream()
                    .map(OTPrmMapping::getDetectorNode)
                    .distinct()
                    .forEach(node -> {
                        permutations.forEach(permutationOption ->
                            permutationOption.removeIf(option -> option.equals(detectorNode))
                        );
                        patternSet.add(detectorNode.getId());
                    });

                logger.debug("Found pattern set of components: {}", String.join(",", patternSet));

                if (refinementModel.getComponentSets() == null) {
                    refinementModel.setComponentSets(new ArrayList<>());
                } else {
                    boolean added = false;
                    for (OTComponentSet componentSet : refinementModel.getComponentSets()) {
                        List<String> nodesNotInComponentSet = patternSet.stream()
                            .filter(node -> componentSet.getComponentSet().stream()
                                .anyMatch(nodeId -> !nodeId.equals(node))
                            )
                            .collect(Collectors.toList());
                        if (nodesNotInComponentSet.size() != patternSet.size()) {
                            added = componentSet.getComponentSet().addAll(nodesNotInComponentSet);
                            logger.debug("Added pattern set to existing set: {}",
                                String.join(",", componentSet.getComponentSet()));
                            break;
                        }
                    }

                    if (!added) {
                        if (refinementModel.getComponentSets() == null) {
                            refinementModel.setComponentSets(new ArrayList<>());
                        }
                        refinementModel.getComponentSets().add(OTComponentSet.of(patternSet));
                    }
                }
            }
        }
        
        // For all nodes the current refinement node is dependent on which do not have any maps, check the permutability
        
        // todo
    }
}
