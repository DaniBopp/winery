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
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.OTDeploymentArtifactMapping;
import org.eclipse.winery.model.tosca.OTPermutationMapping;
import org.eclipse.winery.model.tosca.OTPrmMapping;
import org.eclipse.winery.model.tosca.OTRelationDirection;
import org.eclipse.winery.model.tosca.OTRelationMapping;
import org.eclipse.winery.model.tosca.OTStayMapping;
import org.eclipse.winery.model.tosca.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TNodeTemplate;

public abstract class RefinementUtils {

    public static boolean isStayingElement(TEntityTemplate element, OTTopologyFragmentRefinementModel prm) {
        return getStayingModelElements(prm).stream()
            .anyMatch(stayingElement -> stayingElement.getId().equals(element.getId()));
    }

    public static List<TEntityTemplate> getStayingModelElements(OTTopologyFragmentRefinementModel prm) {
        return prm.getStayMappings() == null ? new ArrayList<>() :
            prm.getStayMappings().stream()
                .map(OTPrmMapping::getRefinementNode)
                .collect(Collectors.toList());
    }

    public static boolean permutabilityMappingsExistsForRefinementNode(TEntityTemplate refinementNode, OTTopologyFragmentRefinementModel prm) {
        return prm.getPermutationMappings() != null &&
            prm.getPermutationMappings().stream()
                .anyMatch(permutationMap -> permutationMap.getRefinementNode().equals(refinementNode));
    }

    public static boolean stayMappingsExistsForRefinementNode(TEntityTemplate refinementNode, OTTopologyFragmentRefinementModel prm) {
        return prm.getStayMappings() != null &&
            prm.getStayMappings().stream()
                .anyMatch(permutationMap -> permutationMap.getRefinementNode().equals(refinementNode));
    }

    public static void addPermutabilityMapping(TNodeTemplate detectorNode, TEntityTemplate refinementNode,
                                               OTTopologyFragmentRefinementModel prm) {
        prm.setPermutationMappings(
            addMapping(detectorNode, refinementNode, new OTPermutationMapping(), prm.getPermutationMappings())
        );
    }

    public static void addStayMapping(TNodeTemplate detectorNode, TEntityTemplate refinementNode,
                                      OTTopologyFragmentRefinementModel prm) {
        prm.setStayMappings(
            addMapping(detectorNode, refinementNode, new OTStayMapping(), prm.getStayMappings())
        );
    }

    public static void addRelationMapping(TNodeTemplate detectorNode, TEntityTemplate refinementNode,
                                          QName relationType, OTRelationDirection direction, QName validSourOrTarget,
                                          OTTopologyFragmentRefinementModel prm) {
        OTRelationMapping mapping = new OTRelationMapping();
        prm.setRelationMappings(
            addMapping(detectorNode, refinementNode, mapping, prm.getRelationMappings())
        );

        mapping.setRelationType(relationType);
        mapping.setDirection(direction);
        mapping.setValidSourceOrTarget(validSourOrTarget);
    }

    public static void addDeploymentArtifactMapping(TNodeTemplate detectorNode, TEntityTemplate refinementNode,
                                                    QName artifactType, OTTopologyFragmentRefinementModel prm) {
        OTDeploymentArtifactMapping mapping = new OTDeploymentArtifactMapping();
        prm.setDeploymentArtifactMappings(
            addMapping(detectorNode, refinementNode, mapping, prm.getDeploymentArtifactMappings())
        );

        mapping.setArtifactType(artifactType);
    }

    private static <T extends OTPrmMapping> List<T> addMapping(TNodeTemplate detectorNode, TEntityTemplate refinementNode,
                                                               T mapping, List<T> mappings) {
        if (mappings == null) {
            mappings = new ArrayList<>();
        }

        mapping.setDetectorNode(detectorNode);
        mapping.setRefinementNode(refinementNode);
        mapping.setId(UUID.randomUUID().toString());
        mappings.add(mapping);

        return mappings;
    }

    private static <T extends OTPrmMapping> List<T> getMappingsForRefinementNodeButNotFromDetectorNode(TNodeTemplate detectorNode,
                                                                                                       TEntityTemplate refinementNode,
                                                                                                       List<T> mappings) {
        return mappings == null ? new ArrayList<>() :
            mappings.stream()
                .filter(mapping -> mapping.getRefinementNode().equals(refinementNode))
                .filter(mapping -> !mapping.getDetectorNode().equals(detectorNode))
                .collect(Collectors.toList());
    }

    public static List<OTPrmMapping> getAllMappingsForRefinementNodeWithoutDetectorNode(TNodeTemplate detectorNode,
                                                                                        TEntityTemplate refinementNode,
                                                                                        OTTopologyFragmentRefinementModel refinementModel) {
        ArrayList<OTPrmMapping> mappings = new ArrayList<>();
        mappings.addAll(getMappingsForRefinementNodeButNotFromDetectorNode(detectorNode, refinementNode, refinementModel.getRelationMappings()));
        mappings.addAll(getMappingsForRefinementNodeButNotFromDetectorNode(detectorNode, refinementNode, refinementModel.getAttributeMappings()));
        mappings.addAll(getMappingsForRefinementNodeButNotFromDetectorNode(detectorNode, refinementNode, refinementModel.getDeploymentArtifactMappings()));
        return mappings;
    }
}
