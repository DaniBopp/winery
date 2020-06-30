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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.model.tosca.OTComponentSet;
import org.eclipse.winery.model.tosca.OTPrmMapping;
import org.eclipse.winery.model.tosca.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.addPermutabilityMapping;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.getAllMappingsForRefinementNode;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.getAllMappingsForRefinementNodeWithoutDetectorNode;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.isStayPlaceholder;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.isStayingElement;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.permutabilityMappingExistsForDetectorElement;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.permutabilityMappingExistsForRefinementNode;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.stayMappingExistsForRefinementNode;

public class PermutationGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PermutationGenerator.class);

    protected final Map<QName, TRelationshipType> relationshipTypes = new HashMap<>();
    protected final Map<QName, TNodeType> nodeTypes = new HashMap<>();

    public PermutationGenerator() {
        this.relationshipTypes.putAll(RepositoryFactory.getRepository().getQNameToElementMapping(RelationshipTypeId.class));
        this.nodeTypes.putAll(RepositoryFactory.getRepository().getQNameToElementMapping(NodeTypeId.class));
    }

    public PermutationGenerator(Map<QName, TRelationshipType> relationshipTypes, Map<QName, TNodeType> nodeTypes) {
        this.relationshipTypes.putAll(relationshipTypes);
        this.nodeTypes.putAll(nodeTypes);
    }

    public boolean checkPermutability(OTTopologyFragmentRefinementModel refinementModel) {
        logger.debug("Starting permutability check of {}", refinementModel.getIdFromIdOrNameField());
        List<TNodeTemplate> detectorNodeTemplates = refinementModel.getRefinementTopology().getNodeTemplates();
        Set<TNodeTemplate> permutableNodes = detectorNodeTemplates.stream()
            .filter(nodeTemplate -> isStayingElement(nodeTemplate, refinementModel))
            .collect(Collectors.toSet());

        MutableSet<MutableSet<TNodeTemplate>> permutations = Sets.powerSet(permutableNodes);

        for (TNodeTemplate detectorNode : detectorNodeTemplates) {
            // todo: other mappings? 
            refinementModel.getRelationMappings().stream()
                .filter(relationMapping -> relationMapping.getRefinementNode().equals(detectorNode))
                .filter(relationMapping -> relationMapping.getRefinementNode() instanceof TNodeTemplate)
                .map(relationMapping -> (TNodeTemplate) relationMapping.getRefinementNode())
                .forEach(refinementNode ->
                    this.checkComponentPermutability(refinementNode, detectorNode, refinementModel, permutations)
                );

            ModelUtilities.getIncomingRelationshipTemplates(refinementModel.getDetector(), detectorNode)
                .forEach(relation -> {
                    TNodeTemplate dependantNode = (TNodeTemplate) relation.getSourceElement().getRef();
                    if (refinementModel.getRelationMappings() != null) {
                        refinementModel.getRelationMappings().stream()
                            .filter(relMap -> relMap.getDetectorNode().equals(dependantNode))
                            .filter(relMap -> RefinementUtils.canRedirectRelation(relMap, relation, this.relationshipTypes, this.nodeTypes))
                            .findFirst()
                            .ifPresent(relMap ->
                                addPermutabilityMapping(relMap.getDetectorNode(), relMap.getRefinementNode(), refinementModel)
                            );
                    }
                });
        }

        long numberOfUnmappedRefinementNodes = refinementModel.getRefinementStructure().getNodeTemplates().stream()
            .filter(refinementNode -> isStayPlaceholder(refinementNode, refinementModel))
            .filter(refinementNode -> permutabilityMappingExistsForRefinementNode(refinementNode, refinementModel))
            .count();

        boolean detectorNodeRefinesToMultipleNodes = false;
        // if there are incoming relations that cannot be redirected clearly during the generation of the permutations
        for (TNodeTemplate detectorNode : refinementModel.getDetector().getNodeTemplates()) {
            Optional<TRelationshipTemplate> unMappableRelation =
                ModelUtilities.getIncomingRelationshipTemplates(refinementModel.getDetector(), detectorNode).stream()
                    .filter(relation -> !permutabilityMappingExistsForDetectorElement(relation, refinementModel))
                    .findFirst();

            if (unMappableRelation.isPresent()) {
                // If there is no permutation mapping for the relation and there is more than one permutation
                // mapping sourcing from the source node, the relation cannot be redirected automatically 
                detectorNodeRefinesToMultipleNodes = refinementModel.getPermutationMappings().stream()
                    .anyMatch(pm -> pm.getDetectorNode().equals(detectorNode) &&
                        refinementModel.getPermutationMappings().stream()
                            .filter(pm2 -> pm2.getDetectorNode().equals(detectorNode))
                            .anyMatch(pm2 -> !pm.getRefinementNode().equals(pm2.getRefinementNode()))
                    );
                if (detectorNodeRefinesToMultipleNodes) {
                    break;
                }
            }
        }

        if (numberOfUnmappedRefinementNodes > 0 || detectorNodeRefinesToMultipleNodes) {
            logger.debug("Permutations cannot be determined automatically!");
            return false;
        }

        return true;
    }

    private void checkComponentPermutability(TNodeTemplate refinementNode,
                                             TNodeTemplate detectorNode,
                                             OTTopologyFragmentRefinementModel refinementModel,
                                             Set<MutableSet<TNodeTemplate>> permutations) {
        logger.debug("Checking component permutability of detectorNode \"{}\" to refinementNode \"{}\"",
            refinementNode.getId(), detectorNode.getId());

        if (!permutabilityMappingExistsForRefinementNode(refinementNode, refinementModel) &&
            !stayMappingExistsForRefinementNode(refinementNode, refinementModel)) {
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
        ModelUtilities.getOutgoingRelationshipTemplates(refinementModel.getRefinementTopology(), refinementNode)
            .stream()
            .map(element -> (TNodeTemplate) element.getTargetElement().getRef())
            .filter(dependentNode -> !isStayPlaceholder(dependentNode, refinementModel))
            .filter(dependentNode -> getAllMappingsForRefinementNode(dependentNode, refinementModel).size() == 0)
            .forEach(dependentNode -> this.checkComponentPermutability(detectorNode, dependentNode, refinementModel, permutations));
    }
}
