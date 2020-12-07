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
import java.util.stream.Collectors;

import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementCandidate;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementChooser;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils;
import org.eclipse.winery.model.adaptation.substitution.refinement.topologyrefinement.TopologyFragmentRefinement;
import org.eclipse.winery.model.ids.extensions.PatternRefinementModelId;
import org.eclipse.winery.model.tosca.HasPolicies;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.extensions.OTBehaviorPatternMapping;
import org.eclipse.winery.model.tosca.extensions.OTRefinementModel;
import org.eclipse.winery.model.tosca.extensions.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.topologygraph.matching.IToscaMatcher;
import org.eclipse.winery.topologygraph.matching.ToscaPatternDetectionMatcher;
import org.eclipse.winery.topologygraph.model.ToscaEdge;
import org.eclipse.winery.topologygraph.model.ToscaEntity;
import org.eclipse.winery.topologygraph.model.ToscaNode;

import org.jgrapht.GraphMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatternDetection extends TopologyFragmentRefinement {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatternDetection.class);

    public PatternDetection(RefinementChooser refinementChooser) {
        super(refinementChooser, PatternRefinementModelId.class, "detected");

        // for pattern detection the detector is the "refinement" and the refinement is the detector
        this.refinementModels.stream()
            .map(prm -> (OTTopologyFragmentRefinementModel) prm)
            .forEach(prm -> {
                TTopologyTemplate refinement = prm.getDetector();
                prm.setDetector(prm.getRefinementTopology());
                prm.setRefinementTopology(refinement);

                if (prm.getStayMappings() != null) {
                    prm.getStayMappings().forEach(stayMapping -> {
                        TEntityTemplate refinementElement = stayMapping.getDetectorElement();
                        stayMapping.setDetectorElement(stayMapping.getRefinementElement());
                        stayMapping.setRefinementElement(refinementElement);
                    });
                }
                if (prm.getBehaviorPatternMappings() != null) {
                    prm.getBehaviorPatternMappings().forEach(bpm -> {
                        TEntityTemplate refinementElement = bpm.getDetectorElement();
                        bpm.setDetectorElement(bpm.getRefinementElement());
                        bpm.setRefinementElement(refinementElement);
                    });
                }
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
        // TODO
        return true;
    }

    @Override
    public void applyRefinement(RefinementCandidate refinement, TTopologyTemplate topology) {
        if (!(refinement.getRefinementModel() instanceof OTTopologyFragmentRefinementModel)) {
            throw new UnsupportedOperationException("The refinement candidate is not a PRM!");
        }
        OTTopologyFragmentRefinementModel prm = (OTTopologyFragmentRefinementModel) refinement.getRefinementModel();

        TTopologyTemplate refinementStructure = removeIncompatibleBehaviorPatterns(refinement);
        List<TEntityTemplate> stayingElements = RefinementUtils.getStayingRefinementElements(prm).stream()
            // behavior patterns removed -> stayingElement.equals() doesn't work anymore -> get equivalent elements
            .map(staying -> refinementStructure.getNodeTemplateOrRelationshipTemplate().stream()
                .filter(entityTemplate -> entityTemplate.getId().equals(staying.getId()))
                .findFirst().get())
            .collect(Collectors.toList());
        Map<String, String> idMapping = BackendUtils.mergeTopologyTemplateAinTopologyTemplateB(
            refinementStructure,
            topology,
            stayingElements
        );

        refinement.getDetectorGraph().vertexSet().forEach(vertex -> {
            TNodeTemplate candidateElement = refinement.getGraphMapping().getVertexCorrespondence(vertex, false)
                .getTemplate();

            redirectInternalRelations(prm, vertex.getTemplate(), candidateElement, topology);
            if (!hasStayMappings(vertex.getTemplate(), prm)) {
                topology.getNodeTemplateOrRelationshipTemplate().remove(candidateElement);
            }
        });

        refinement.getDetectorGraph().edgeSet().forEach(edge -> {
            TRelationshipTemplate candidateElement = refinement.getGraphMapping().getEdgeCorrespondence(edge, false)
                .getTemplate();
            if (!hasStayMappings(edge.getTemplate(), prm)) {
                topology.getNodeTemplateOrRelationshipTemplate().remove(candidateElement);
            }
        });
    }

    private TTopologyTemplate removeIncompatibleBehaviorPatterns(RefinementCandidate refinement) {
        OTTopologyFragmentRefinementModel prm = (OTTopologyFragmentRefinementModel) refinement.getRefinementModel();
        TTopologyTemplate refinementStructure = prm.getRefinementStructure();

        for (TEntityTemplate refinementElement : refinementStructure.getNodeTemplateOrRelationshipTemplate()) {
            if (((HasPolicies) refinementElement).getPolicies() == null) {
                continue;
            }
            ((HasPolicies) refinementElement).getPolicies().getPolicy().removeIf(refinementPolicy -> {
                for (OTBehaviorPatternMapping bpm : prm.getBehaviorPatternMappings()) {
                    if (!bpm.getRefinementElement().getId().equals(refinementElement.getId())
                        || !bpm.getBehaviorPattern().equals(refinementPolicy.getName())) {
                        continue;
                    }
                    ToscaEntity detectorElement = refinement.getDetectorGraph()
                        .getEntity(bpm.getDetectorElement().getId()).get();
                    TEntityTemplate candidateElement = getEntityCorrespondence(detectorElement, refinement.getGraphMapping());

                    if (ModelUtilities.hasKvProperties(detectorElement.getTemplate())
                        && ModelUtilities.hasKvProperties(candidateElement)) {
                        String detectorValue = ModelUtilities.getPropertiesKV(detectorElement.getTemplate())
                            .get(bpm.getProperty().getKey());
                        String candidateValue = ModelUtilities.getPropertiesKV(candidateElement)
                            .get(bpm.getProperty().getKey());
                        return detectorValue != null && !detectorValue.isEmpty() &&
                            !detectorValue.equalsIgnoreCase(candidateValue);
                    }
                }
                // behavior patterns without mapping will be removed
                return true;
            });
        }
        return refinementStructure;
    }

    private TEntityTemplate getEntityCorrespondence(ToscaEntity entity, GraphMapping<ToscaNode, ToscaEdge> graphMapping) {
        if (entity instanceof ToscaNode) {
            return graphMapping.getVertexCorrespondence((ToscaNode) entity, false).getTemplate();
        } else {
            return graphMapping.getEdgeCorrespondence((ToscaEdge) entity, false).getTemplate();
        }
    }

    private boolean hasStayMappings(TEntityTemplate current, OTTopologyFragmentRefinementModel prm) {
        return getStayMappingsOfCurrentElement(prm, current).findFirst().isPresent();
    }
}
