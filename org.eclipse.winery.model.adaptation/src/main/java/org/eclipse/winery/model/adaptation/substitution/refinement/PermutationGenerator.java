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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.PatternRefinementModelId;
import org.eclipse.winery.common.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.common.ids.definitions.TopologyFragmentRefinementModelId;
import org.eclipse.winery.common.version.VersionUtils;
import org.eclipse.winery.model.tosca.HasId;
import org.eclipse.winery.model.tosca.OTComponentSet;
import org.eclipse.winery.model.tosca.OTPatternRefinementModel;
import org.eclipse.winery.model.tosca.OTPermutationMapping;
import org.eclipse.winery.model.tosca.OTPermutationOption;
import org.eclipse.winery.model.tosca.OTPrmMapping;
import org.eclipse.winery.model.tosca.OTStayMapping;
import org.eclipse.winery.model.tosca.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.addPermutabilityMapping;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.getAllMappingsForDetectorNode;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.getAllMappingsForRefinementNodeWithoutDetectorNode;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.isStayPlaceholder;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.isStayingElement;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.noMappingExistsForRefinementNode;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.permutabilityMappingExistsForDetectorElement;
import static org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils.permutabilityMappingExistsForRefinementNode;

public class PermutationGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PermutationGenerator.class);

    protected final Map<QName, TRelationshipType> relationshipTypes = new HashMap<>();
    protected final Map<QName, TNodeType> nodeTypes = new HashMap<>();

    protected final String errorMessage = "Permutations cannot be determined automatically! Reason: {}.";
    protected String permutabilityErrorReason = "";

    public PermutationGenerator() {
        this.relationshipTypes.putAll(RepositoryFactory.getRepository().getQNameToElementMapping(RelationshipTypeId.class));
        this.nodeTypes.putAll(RepositoryFactory.getRepository().getQNameToElementMapping(NodeTypeId.class));
    }

    public PermutationGenerator(Map<QName, TRelationshipType> relationshipTypes, Map<QName, TNodeType> nodeTypes) {
        this.relationshipTypes.putAll(relationshipTypes);
        this.nodeTypes.putAll(nodeTypes);
    }

    public boolean checkPermutability(OTTopologyFragmentRefinementModel refinementModel) {
        logger.info("Starting permutability check of {}", refinementModel.getIdFromIdOrNameField());
        this.permutabilityErrorReason = "";

        List<TNodeTemplate> detectorNodeTemplates = refinementModel.getDetector().getNodeTemplates();
        Set<TNodeTemplate> permutableNodes = detectorNodeTemplates.stream()
            .filter(nodeTemplate -> !isStayingElement(nodeTemplate, refinementModel))
            .collect(Collectors.toSet());

        ArrayList<OTPermutationOption> permutationOptions = new ArrayList<>();
        refinementModel.setPermutationOptions(permutationOptions);
        Sets.powerSet(permutableNodes).stream()
            .filter(set -> !(
                set.size() == 0 || set.size() == refinementModel.getDetector().getNodeTemplates().size()
            )).forEach(permutation -> permutationOptions.add(
            new OTPermutationOption(
                permutation.stream()
                    .map(HasId::getId)
                    .collect(Collectors.toList())
            )
        ));

        refinementModel.setComponentSets(new ArrayList<>());

        for (TNodeTemplate detectorNode : detectorNodeTemplates) {
            getAllMappingsForDetectorNode(detectorNode, refinementModel).stream()
                .filter(mapping -> mapping.getDetectorElement().equals(detectorNode))
                .filter(mapping -> mapping.getRefinementElement() instanceof TNodeTemplate)
                .map(mapping -> (TNodeTemplate) mapping.getRefinementElement())
                .forEach(refinementNode ->
                    this.checkComponentPermutability(refinementNode, detectorNode, refinementModel)
                );

            ModelUtilities.getIncomingRelationshipTemplates(refinementModel.getDetector(), detectorNode)
                .forEach(relation -> {
                    TNodeTemplate dependantNode = (TNodeTemplate) relation.getSourceElement().getRef();
                    if (refinementModel.getRelationMappings() != null) {
                        refinementModel.getRelationMappings().stream()
                            .filter(relMap -> relMap.getDetectorElement().equals(dependantNode))
                            .filter(relMap -> RefinementUtils.canRedirectRelation(relMap, relation, this.relationshipTypes, this.nodeTypes))
                            .findFirst()
                            .ifPresent(relMap ->
                                addPermutabilityMapping(relMap.getDetectorElement(), relMap.getRefinementElement(), refinementModel)
                            );
                    }
                });
        }

        if (refinementModel.getPermutationMappings() == null) {
            this.permutabilityErrorReason = "No permutation mappings could be identified";
            logger.info(this.errorMessage, this.permutabilityErrorReason);
            return false;
        }

        List<String> unmappedDetectorNodes = refinementModel.getDetector().getNodeTemplates().stream()
            .filter(detectorNode -> !isStayingElement(detectorNode, refinementModel))
            .filter(detectorNode -> !permutabilityMappingExistsForDetectorElement(detectorNode, refinementModel))
            .map(HasId::getId)
            .collect(Collectors.toList());

        if (unmappedDetectorNodes.size() > 0) {
            this.permutabilityErrorReason = "There are detector nodes which could not be mapped to a refinement node: "
                + String.join(", ", unmappedDetectorNodes);
            logger.info(this.errorMessage, this.permutabilityErrorReason);
            return false;
        }

        List<String> unmappedRefinementNodes = refinementModel.getRefinementStructure().getNodeTemplates().stream()
            .filter(refinementNode -> !isStayPlaceholder(refinementNode, refinementModel))
            .filter(refinementNode -> !permutabilityMappingExistsForRefinementNode(refinementNode, refinementModel))
            .map(HasId::getId)
            .collect(Collectors.toList());

        if (unmappedRefinementNodes.size() > 0) {
            this.permutabilityErrorReason = "There are refinement nodes which could not be mapped to a detector node: "
                + String.join(", ", unmappedRefinementNodes);
            logger.info(this.errorMessage, this.permutabilityErrorReason);
            return false;
        }

        boolean detectorNodeRefinesToMultipleNodes = false;
        // if there are incoming relations that cannot be redirected clearly during the generation of the permutations
        for (TNodeTemplate detectorNode : refinementModel.getDetector().getNodeTemplates()) {
            Optional<TRelationshipTemplate> unMappableRelation =
                ModelUtilities.getIncomingRelationshipTemplates(refinementModel.getDetector(), detectorNode).stream()
                    .filter(relation -> !permutabilityMappingExistsForDetectorElement(relation, refinementModel))
                    .findFirst();

            if (unMappableRelation.isPresent()) {
                TRelationshipTemplate unmappable = unMappableRelation.get();
                boolean relationBetweenNodesInAComponentSet = refinementModel.getComponentSets().stream()
                    .anyMatch(componentSet ->
                        componentSet.getComponentSet().containsAll(Arrays.asList(
                            unmappable.getSourceElement().getRef().getId(),
                            unmappable.getTargetElement().getRef().getId())));

                if (!relationBetweenNodesInAComponentSet) {
                    // If there is no permutation mapping for the relation and there is more than one permutation
                    // mapping sourcing from the source node, the relation cannot be redirected automatically
                    detectorNodeRefinesToMultipleNodes = refinementModel.getPermutationMappings().stream()
                        .anyMatch(pm -> pm.getDetectorElement().equals(detectorNode) &&
                            refinementModel.getPermutationMappings().stream()
                                .filter(pm2 -> pm2.getDetectorElement().equals(detectorNode))
                                .anyMatch(pm2 -> !pm.getRefinementElement().equals(pm2.getRefinementElement()))
                        );
                }
                if (detectorNodeRefinesToMultipleNodes) {
                    break;
                }
            }
        }

        if (detectorNodeRefinesToMultipleNodes) {
            this.permutabilityErrorReason = "There are relations that cannot be redirected during the generation";
            logger.info(this.errorMessage, this.permutabilityErrorReason);
            return false;
        }

        return true;
    }

    private void checkComponentPermutability(TNodeTemplate refinementNode,
                                             TNodeTemplate detectorNode,
                                             OTTopologyFragmentRefinementModel refinementModel) {
        logger.debug("Checking component permutability of detectorNode \"{}\" to refinementNode \"{}\"",
            detectorNode.getId(), refinementNode.getId());

        List<OTPrmMapping> mappingsWithoutDetectorNode =
            getAllMappingsForRefinementNodeWithoutDetectorNode(detectorNode, refinementNode, refinementModel);

        if (noMappingExistsForRefinementNode(detectorNode, refinementNode, refinementModel)) {
            logger.debug("Adding PermutabilityMapping between detector Node \"{}\" and refinement node \"{}\"",
                detectorNode.getId(), refinementNode.getId());
            addPermutabilityMapping(detectorNode, refinementNode, refinementModel);
        } else if (mappingsWithoutDetectorNode.size() > 0) {
            // Determine the set of pattern which must be refined together as they define overlapping mappings
            ArrayList<String> patternSet = new ArrayList<>();
            patternSet.add(detectorNode.getId());

            mappingsWithoutDetectorNode.stream()
                .map(OTPrmMapping::getDetectorElement)
                .forEach(node -> patternSet.add(node.getId()));

            logger.debug("Found pattern set of components: {}", String.join(",", patternSet));

            if (refinementModel.getComponentSets() == null) {
                refinementModel.setComponentSets(new ArrayList<>());
            }

            refinementModel.getPermutationOptions()
                .removeIf(permutationOption -> !(permutationOption.getOptions().containsAll(patternSet)
                    || permutationOption.getOptions().stream().noneMatch(patternSet::contains))
                );

            boolean added = false;
            for (OTComponentSet componentSet : refinementModel.getComponentSets()) {
                List<String> existingPatternSet = componentSet.getComponentSet();
                if (existingPatternSet.stream().anyMatch(patternSet::contains)) {
                    added = true;
                    patternSet.forEach(id -> {
                        if (!existingPatternSet.contains(id)) {
                            existingPatternSet.add(id);
                        }
                    });
                    logger.debug("Added pattern set to existing set: {}",
                        String.join(",", existingPatternSet));
                    break;
                }
            }

            if (!added) {
                refinementModel.getComponentSets().add(new OTComponentSet(patternSet));
            }
        }

        // Only check dependees if the current component can be mapped clearly to the current detector node
        if (mappingsWithoutDetectorNode.size() == 0) {
            // For all nodes the current refinement node is dependent on and which do not have any other dependants
            // or maps from different detector nodes, check their permutability.
            ModelUtilities.getOutgoingRelationshipTemplates(refinementModel.getRefinementTopology(), refinementNode)
                .stream()
                .map(element -> (TNodeTemplate) element.getTargetElement().getRef())
                .filter(dependee -> noMappingExistsForRefinementNode(detectorNode, dependee, refinementModel))
                .filter(dependee -> ModelUtilities.getIncomingRelationshipTemplates(refinementModel.getRefinementTopology(), dependee)
                    .stream()
                    .filter(relation -> !relation.getSourceElement().getRef().getId().equals(refinementNode.getId()))
                    .map(relationship -> (TNodeTemplate) relationship.getSourceElement().getRef())
                    .anyMatch(source -> noMappingExistsForRefinementNode(detectorNode, source, refinementModel))
                ).forEach(dependee -> this.checkComponentPermutability(dependee, detectorNode, refinementModel));
        }
    }

    public Map<String, OTTopologyFragmentRefinementModel> generatePermutations(OTTopologyFragmentRefinementModel refinementModel) {
        Map<String, OTTopologyFragmentRefinementModel> permutations = new HashMap<>();
        IRepository repository = RepositoryFactory.getRepository();

        if (!checkPermutability(refinementModel)) {
            throw new UnsupportedOperationException("The refinement model cannot be permuted!");
        }

        QName refinementModelQName = new QName(refinementModel.getTargetNamespace(), refinementModel.getName());
        DefinitionsChildId refinementModelId = new TopologyFragmentRefinementModelId(refinementModelQName);
        if (refinementModel instanceof OTPatternRefinementModel) {
            refinementModelId = new PatternRefinementModelId(refinementModelQName);
        }

        for (OTPermutationOption options : refinementModel.getPermutationOptions()) {
            String permutationName = VersionUtils.getNewComponentVersionId(refinementModelId,
                "permutation-" + String.join("-", options.getOptions()));
            QName permutationQName = new QName(refinementModel.getTargetNamespace(), permutationName);

            DefinitionsChildId permutationModelId = new TopologyFragmentRefinementModelId(permutationQName);
            if (refinementModel instanceof OTPatternRefinementModel) {
                permutationModelId = new PatternRefinementModelId(permutationQName);
            }

            try {
                repository.duplicate(refinementModelId, permutationModelId);
            } catch (IOException e) {
                logger.error("Error while creating permutation!", e);
                break;
            }

            OTTopologyFragmentRefinementModel permutation = repository.getElement(permutationModelId);
            permutations.put(permutationName, permutation);

            // algo here
            Map<String, String> alreadyAdded = new HashMap<>();
            for (String option : options.getOptions()) {
                permutation.getPermutationMappings().stream()
                    .filter(permutationMap -> permutationMap.getDetectorElement().getId().equals(option))
                    .map(OTPrmMapping::getRefinementElement)
                    .filter(refinementElement -> refinementElement instanceof TNodeTemplate)
                    .map(refinementElement -> (TNodeTemplate) refinementElement)
                    .forEach(refinementElement -> {
                        TNodeTemplate addedDetectorElement = alreadyAdded.containsKey(refinementElement.getId())
                            ? permutation.getDetector().getNodeTemplate(alreadyAdded.get(refinementElement.getId()))
                            : addNodeFromRefinementStructureToDetector(refinementElement, permutation, alreadyAdded);

                        // region outgoing relations of the currently permuted refinementElement
                        for (TRelationshipTemplate relation : permutation.getRefinementStructure().getRelationshipTemplates()) {
                            if (relation.getSourceElement().getRef().getId().equals(refinementElement.getId())) {
                                // using the permutation maps defined in the original model as we remove them in the permutation
                                for (OTPermutationMapping permutationMap : refinementModel.getPermutationMappings()) {
                                    if (permutationMap.getRefinementElement().getId().equals(relation.getTargetElement().getRef().getId())) {
                                        // TODO: An der stelle nicht nur das aktuelle element anschauen, sondern alle aus einem pattern set?!?
                                        //       Oder sogar nochmal was neues einfuehren weil was is wenn ich 2 rels auf 1 hab?
                                        //       Bsp.: PermutationGeneratorTest 166-174 ohne 12 und 11 zeigt zusaetzlich auf 14
                                        // Vielleicht war das already contains schon genug, zumindest fuer das pattern set
                                        // ausserdem umgehen wir damit die behandlung von eingehenden relations (algo 21-23) --> algo anpassen 
                                        if (permutationMap.getDetectorElement().getId().equals(option) || alreadyAdded.containsKey(option)) {
                                            String alreadyAddedElement = alreadyAdded.get(relation.getTargetElement().getRef().getId());
                                            TNodeTemplate target = alreadyAddedElement == null
                                                ? addNodeFromRefinementStructureToDetector((TNodeTemplate) relation.getTargetElement().getRef(), permutation, alreadyAdded)
                                                : permutation.getDetector().getNodeTemplate(alreadyAddedElement);
                                            ModelUtilities.createRelationshipTemplateAndAddToTopology(addedDetectorElement, target, relation.getType(), permutation.getDetector());
                                        } else {
                                            ModelUtilities.createRelationshipTemplateAndAddToTopology(addedDetectorElement, (TNodeTemplate) permutationMap.getDetectorElement(), relation.getType(), permutation.getDetector());
                                        }
                                    }
                                }

                                if (permutation.getStayMappings() != null) {
                                    for (OTStayMapping stayMapping : permutation.getStayMappings()) {
                                        if (stayMapping.getRefinementElement().getId().equals(relation.getTargetElement().getRef().getId())) {
                                            ModelUtilities.createRelationshipTemplateAndAddToTopology(addedDetectorElement, (TNodeTemplate) stayMapping.getDetectorElement(), relation.getType(), permutation.getDetector());
                                        }
                                    }
                                }
                            }
                        }
                        //endregion

                        // region handle ingoing relations in the detector
                        for (TRelationshipTemplate relation : permutation.getDetector().getRelationshipTemplates()) {
                            Optional<OTPermutationMapping> relationTarget = permutation.getPermutationMappings().stream()
                                .filter(permutationMap -> permutationMap.getDetectorElement().getId().equals(relation.getId()))
                                .filter(permutationMap -> permutationMap.getRefinementElement().getId().equals(refinementElement.getId()))
                                .findFirst();

                            long refinementEquivalents = permutation.getPermutationMappings().stream()
                                .filter(permutationMap -> permutationMap.getDetectorElement().getId().equals(option))
                                .map(OTPrmMapping::getRefinementElement)
                                .distinct()
                                .count();
                            if (relationTarget.isPresent() || refinementEquivalents == 1) {
                                ModelUtilities.createRelationshipTemplateAndAddToTopology(
                                    (TNodeTemplate) relation.getSourceElement().getRef(),
                                    addedDetectorElement,
                                    relation.getType(),
                                    permutation.getDetector()
                                );
                                break;
                            }
                        }
                        // endregion
                    });

                // region remove permuted
                if (permutation.getAttributeMappings() != null) {
                    permutation.getRelationMappings()
                        .removeIf(map -> map.getDetectorElement().getId().equals(option));
                }
                if (permutation.getAttributeMappings() != null) {
                    permutation.getAttributeMappings()
                        .removeIf(map -> map.getDetectorElement().getId().equals(option));
                }
                if (permutation.getDeploymentArtifactMappings() != null) {
                    permutation.getDeploymentArtifactMappings()
                        .removeIf(map -> map.getDetectorElement().getId().equals(option));
                }
                permutation.getPermutationMappings()
                    .removeIf(permMap -> permMap.getDetectorElement().getId().equals(option));

                permutation.getDetector().getNodeTemplateOrRelationshipTemplate()
                    .removeIf(template -> template instanceof TRelationshipTemplate &&
                        (((TRelationshipTemplate) template).getSourceElement().getRef().getId().equals(option)
                            || ((TRelationshipTemplate) template).getTargetElement().getRef().getId().equals(option))
                        || template.getId().equals(option)
                    );

                try {
                    RepositoryFactory.getRepository().setElement(permutationModelId, permutation);
                } catch (IOException e) {
                    logger.error("Error while saving permutation!", e);
                    break;
                }
            }
        }

        return permutations;
    }

    public String getPermutabilityErrorReason() {
        return permutabilityErrorReason;
    }

    private TNodeTemplate addNodeFromRefinementStructureToDetector(TNodeTemplate refinementElement,
                                                                   OTTopologyFragmentRefinementModel permutation,
                                                                   Map<String, String> alreadyAdded) {
        TNodeTemplate clone = SerializationUtils.clone(refinementElement);
        ModelUtilities.generateNewIdOfTemplate(clone, permutation.getDetector());
        permutation.getDetector().addNodeTemplate(clone);
        alreadyAdded.put(refinementElement.getId(), clone.getId());

        if (permutation.getStayMappings() == null) {
            permutation.setStayMappings(new ArrayList<>());
        }
        permutation.getStayMappings().add(new OTStayMapping(clone, refinementElement));

        return clone;
    }
}
