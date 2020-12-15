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

package org.eclipse.winery.model.adaptation.substitution.patterndetection;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementCandidate;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementChooser;
import org.eclipse.winery.model.adaptation.substitution.refinement.topologyrefinement.TopologyFragmentRefinement;
import org.eclipse.winery.model.ids.extensions.PatternRefinementModelId;
import org.eclipse.winery.model.tosca.HasPolicies;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TPolicies;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.extensions.OTAttributeMapping;
import org.eclipse.winery.model.tosca.extensions.OTPrmMapping;
import org.eclipse.winery.model.tosca.extensions.OTRefinementModel;
import org.eclipse.winery.model.tosca.extensions.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.topologygraph.matching.IToscaMatcher;
import org.eclipse.winery.topologygraph.matching.ToscaBehaviorPatternMatcher;
import org.eclipse.winery.topologygraph.model.ToscaEdge;
import org.eclipse.winery.topologygraph.model.ToscaEntity;
import org.eclipse.winery.topologygraph.model.ToscaNode;

import org.jgrapht.GraphMapping;

public class PatternDetection extends TopologyFragmentRefinement {

    public PatternDetection(RefinementChooser refinementChooser) {
        super(refinementChooser, PatternRefinementModelId.class, "detected");
        swapDetectorWithRefinement();
    }

    private void swapDetectorWithRefinement() {
        // for pattern detection the detector is the "refinement" and the refinement is the detector
        this.refinementModels.stream()
            .map(prm -> (OTTopologyFragmentRefinementModel) prm)
            .forEach(prm -> {
                TTopologyTemplate refinement = prm.getDetector();
                prm.setDetector(prm.getRefinementTopology());
                prm.setRefinementTopology(refinement);

                Stream.of(
                    prm.getRelationMappings() == null ? Stream.empty() : prm.getRelationMappings().stream(),
                    prm.getPermutationMappings() == null ? Stream.empty() : prm.getPermutationMappings().stream(),
                    prm.getAttributeMappings() == null ? Stream.empty() : prm.getAttributeMappings().stream(),
                    prm.getStayMappings() == null ? Stream.empty() : prm.getStayMappings().stream(),
                    prm.getDeploymentArtifactMappings() == null ? Stream.empty() : prm.getDeploymentArtifactMappings().stream(),
                    prm.getBehaviorPatternMappings() == null ? Stream.empty() : prm.getBehaviorPatternMappings().stream()
                ).flatMap(Function.identity())
                    .map(OTPrmMapping.class::cast)
                    .forEach(mapping -> {
                        TEntityTemplate refinementElement = mapping.getDetectorElement();
                        mapping.setDetectorElement(mapping.getRefinementElement());
                        mapping.setRefinementElement(refinementElement);

                        if (mapping instanceof OTAttributeMapping) {
                            OTAttributeMapping attributeMapping = (OTAttributeMapping) mapping;
                            String refinementProp = attributeMapping.getDetectorProperty();
                            attributeMapping.setDetectorProperty(attributeMapping.getRefinementProperty());
                            attributeMapping.setRefinementProperty(refinementProp);
                        }
                    });
            });
    }

    @Override
    public boolean getLoopCondition(TTopologyTemplate topology) {
        return true;
    }

    @Override
    public IToscaMatcher getMatcher(OTRefinementModel prm) {
        return new ToscaBehaviorPatternMatcher((OTTopologyFragmentRefinementModel) prm);
    }

    @Override
    public boolean isApplicable(RefinementCandidate candidate, TTopologyTemplate topology) {
        return super.isApplicable(candidate, topology);
    }

    @Override
    public Map<String, String> applyRefinement(RefinementCandidate refinement, TTopologyTemplate topology) {
        Map<String, String> idMapping = super.applyRefinement(refinement, topology);
        OTTopologyFragmentRefinementModel prm = (OTTopologyFragmentRefinementModel) refinement.getRefinementModel();

        // stay elements have been falsely imported (equals does not work anymore because of swapping?)
        topology.getNodeTemplateOrRelationshipTemplate()
            .removeIf(entityTemplate -> prm.getStayMappings() != null
                && prm.getStayMappings().stream()
                .anyMatch(stayMapping -> stayMapping.getRefinementElement().getId().equals(entityTemplate.getId()))
            );

        prm.getRefinementStructure().getNodeTemplateOrRelationshipTemplate().stream()
            .filter(refinementElement -> ((HasPolicies) refinementElement).getPolicies() != null)
            .forEach(refinementElement -> {
                String newId = idMapping.get(refinementElement.getId());
                TEntityTemplate addedElement = newId != null ? topology.getNodeTemplateOrRelationshipTemplate(newId) : null;

                if (addedElement == null) {
                    // for staying elements
                    addCompatibleBehaviorPatterns(refinementElement, refinement);
                } else {
                    removeIncompatibleBehaviorPatterns(refinementElement, addedElement, refinement);
                }
            });
        return idMapping;
    }

    private void addCompatibleBehaviorPatterns(TEntityTemplate refinementElement, RefinementCandidate refinement) {
        OTTopologyFragmentRefinementModel prm = (OTTopologyFragmentRefinementModel) refinement.getRefinementModel();
        TEntityTemplate detectorElement = prm.getStayMappings().stream()
            .filter(stayMapping -> stayMapping.getRefinementElement().getId().equals(refinementElement.getId()))
            .findFirst().get()
            .getDetectorElement();
        ToscaEntity detectorEntity = refinement.getDetectorGraph().getEntity(detectorElement.getId()).get();
        TEntityTemplate stayingElement = getEntityCorrespondence(detectorEntity, refinement.getGraphMapping());

        TPolicies behaviorPatterns = ((HasPolicies) refinementElement).getPolicies();
        TPolicies policies = ((HasPolicies) stayingElement).getPolicies();
        if (behaviorPatterns != null) {
            if (policies != null) {
                // avoid duplicate behavior patterns
                behaviorPatterns.getPolicy().forEach(behaviorPattern -> {
                    boolean behaviorPatternExists = policies.getPolicy().stream()
                        .anyMatch(policy -> policy.getPolicyType().equals(behaviorPattern.getPolicyType()));
                    if (!behaviorPatternExists) {
                        policies.getPolicy().add(behaviorPattern);
                    }
                });
            } else {
                ((HasPolicies) stayingElement).setPolicies(behaviorPatterns);
            }
            removeIncompatibleBehaviorPatterns(refinementElement, stayingElement, refinement);
        }
    }

    private void removeIncompatibleBehaviorPatterns(TEntityTemplate refinementElement, TEntityTemplate addedElement,
                                                    RefinementCandidate refinement) {
        OTTopologyFragmentRefinementModel prm = (OTTopologyFragmentRefinementModel) refinement.getRefinementModel();
        TPolicies addedElementPolicies = ((HasPolicies) addedElement).getPolicies();

        // TODO: remove non behavior pattern policies?
        prm.getBehaviorPatternMappings().forEach(bpm -> {
            ToscaEntity detectorElement = refinement.getDetectorGraph()
                .getEntity(bpm.getDetectorElement().getId()).get();
            TEntityTemplate candidateElement = getEntityCorrespondence(detectorElement, refinement.getGraphMapping());

            if (ModelUtilities.hasKvProperties(detectorElement.getTemplate())
                && ModelUtilities.hasKvProperties(candidateElement)) {
                String detectorValue = ModelUtilities.getPropertiesKV(detectorElement.getTemplate())
                    .get(bpm.getProperty().getKey());
                String candidateValue = ModelUtilities.getPropertiesKV(candidateElement)
                    .get(bpm.getProperty().getKey());
                boolean propsNotCompatible = detectorValue != null && !detectorValue.isEmpty()
                    && !detectorValue.equalsIgnoreCase(candidateValue);

                if (propsNotCompatible) {
                    addedElementPolicies.getPolicy()
                        .removeIf(policy -> bpm.getRefinementElement().getId().equals(refinementElement.getId())
                            && bpm.getBehaviorPattern().equals(policy.getName()));
                }
            }
            // TODO: add existing policies of candidateElement to addedElement?
        });
    }

    private TEntityTemplate getEntityCorrespondence(ToscaEntity entity, GraphMapping<ToscaNode, ToscaEdge> graphMapping) {
        if (entity instanceof ToscaNode) {
            return graphMapping.getVertexCorrespondence((ToscaNode) entity, false).getTemplate();
        } else {
            return graphMapping.getEdgeCorrespondence((ToscaEdge) entity, false).getTemplate();
        }
    }
}
