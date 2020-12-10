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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementCandidate;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementChooser;
import org.eclipse.winery.model.adaptation.substitution.refinement.topologyrefinement.TopologyFragmentRefinement;
import org.eclipse.winery.model.ids.extensions.PatternRefinementModelId;
import org.eclipse.winery.model.tosca.HasPolicies;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.extensions.OTBehaviorPatternMapping;
import org.eclipse.winery.model.tosca.extensions.OTPrmMapping;
import org.eclipse.winery.model.tosca.extensions.OTRefinementModel;
import org.eclipse.winery.model.tosca.extensions.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.topologygraph.matching.IToscaMatcher;
import org.eclipse.winery.topologygraph.matching.ToscaPatternDetectionMatcher;
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

                // TODO: other mappings
                Stream.of(
                    prm.getRelationMappings().stream().map(OTPrmMapping.class::cast),
                    prm.getStayMappings().stream().map(OTPrmMapping.class::cast),
                    prm.getBehaviorPatternMappings().stream().map(OTPrmMapping.class::cast)
                ).flatMap(Function.identity()).forEach(mapping -> {
                    TEntityTemplate refinementElement = mapping.getDetectorElement();
                    mapping.setDetectorElement(mapping.getRefinementElement());
                    mapping.setRefinementElement(refinementElement);
                });
            });
    }

    @Override
    public boolean getLoopCondition(TTopologyTemplate topology) {
        return true;
    }

    @Override
    public IToscaMatcher getMatcher(OTRefinementModel prm) {
        return new ToscaPatternDetectionMatcher((OTTopologyFragmentRefinementModel) prm);
    }

    @Override
    public boolean isApplicable(RefinementCandidate candidate, TTopologyTemplate topology) {
        return super.isApplicable(candidate, topology);
    }

    @Override
    public Map<String, String> applyRefinement(RefinementCandidate refinement, TTopologyTemplate topology) {
        // TODO: better option than returning from super?
        Map<String, String> idMapping = super.applyRefinement(refinement, topology);
        OTTopologyFragmentRefinementModel prm = (OTTopologyFragmentRefinementModel) refinement.getRefinementModel();

        // TODO: fix stay elements being inserted

        // TODO: add compatible behavior patterns to staying elements
        prm.getRefinementStructure().getNodeTemplateOrRelationshipTemplate().stream()
            // element may not have been inserted because of stay mappings
            .filter(refinementElement -> idMapping.get(refinementElement.getId()) != null)
            .filter(refinementElement -> ((HasPolicies) refinementElement).getPolicies() != null)
            .forEach(refinementElement -> {
                String newId = idMapping.get(refinementElement.getId());
                TEntityTemplate addedElement = topology.getNodeTemplateOrRelationshipTemplate(newId);
                removeIncompatibleBehaviorPatterns(refinementElement, addedElement, refinement);
            });
        return idMapping;
    }

    private void removeIncompatibleBehaviorPatterns(TEntityTemplate refinementElement, TEntityTemplate addedElement,
                                                    RefinementCandidate refinement) {
        OTTopologyFragmentRefinementModel prm = (OTTopologyFragmentRefinementModel) refinement.getRefinementModel();

        ((HasPolicies) addedElement).getPolicies().getPolicy()
            .removeIf(policy -> {
                List<OTBehaviorPatternMapping> bpms = prm.getBehaviorPatternMappings().stream()
                    .filter(bpm -> bpm.getRefinementElement().getId().equals(refinementElement.getId())
                        && bpm.getBehaviorPattern().equals(policy.getName()))
                    .collect(Collectors.toList());

                return bpms.isEmpty() || bpms.stream().anyMatch(bpm -> {
                    ToscaEntity detectorElement = refinement.getDetectorGraph()
                        .getEntity(bpm.getDetectorElement().getId()).get();
                    TEntityTemplate candidateElement = getEntityCorrespondence(
                        detectorElement,
                        refinement.getGraphMapping()
                    );

                    if (ModelUtilities.hasKvProperties(detectorElement.getTemplate())
                        && ModelUtilities.hasKvProperties(candidateElement)) {
                        String detectorValue = ModelUtilities.getPropertiesKV(detectorElement.getTemplate())
                            .get(bpm.getProperty().getKey());
                        String candidateValue = ModelUtilities.getPropertiesKV(candidateElement)
                            .get(bpm.getProperty().getKey());
                        return detectorValue != null && !detectorValue.isEmpty() &&
                            !detectorValue.equalsIgnoreCase(candidateValue);
                    }
                    return false;
                });
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
