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
import java.util.stream.Stream;

import org.eclipse.winery.model.adaptation.substitution.refinement.AbstractRefinement;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementCandidate;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementChooser;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementUtils;
import org.eclipse.winery.model.ids.extensions.PatternRefinementModelId;
import org.eclipse.winery.model.tosca.HasPolicies;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.extensions.OTRefinementModel;
import org.eclipse.winery.model.tosca.extensions.OTStayMapping;
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

public class PatternDetection extends AbstractRefinement {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatternDetection.class);

    public PatternDetection(RefinementChooser refinementChooser) {
        super(refinementChooser, PatternRefinementModelId.class, "detected");
    }

    @Override
    public TTopologyTemplate getDetector(OTRefinementModel prm) {
        return prm.getRefinementTopology();
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

        TTopologyTemplate detector = removeIncompatibleBehaviorPatterns(refinement);
        List<TEntityTemplate> stayingElements = RefinementUtils.getStayingDetectorElements(prm).stream()
            // behavior patterns removed -> stayingElement.equals() doesn't work anymore -> get equivalent elements
            .map(staying -> detector.getNodeTemplateOrRelationshipTemplate().stream()
                .filter(entityTemplate -> entityTemplate.getId().equals(staying.getId()))
                .findFirst().get())
            .collect(Collectors.toList());
        Map<String, String> idMapping = BackendUtils.mergeTopologyTemplateAinTopologyTemplateB(
            detector,
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
        TTopologyTemplate detector = prm.getDetector();

        prm.getBehaviorPatternMappings().forEach(bpm -> {
            ToscaEntity refinementElement = refinement.getDetectorGraph()
                .getEntity(bpm.getRefinementElement().getId()).get();
            TEntityTemplate candidateElement = getEntityCorrespondence(refinementElement, refinement.getGraphMapping());

            if (ModelUtilities.hasKvProperties(refinementElement.getTemplate()) &&
                ModelUtilities.hasKvProperties(candidateElement)) {
                String refinementValue = ModelUtilities.getPropertiesKV(refinementElement.getTemplate())
                    .get(bpm.getRefinementProperty().getKey());
                String candidateValue = ModelUtilities.getPropertiesKV(candidateElement)
                    .get(bpm.getRefinementProperty().getKey());

                if (refinementValue != null && !refinementValue.isEmpty() &&
                    !refinementValue.equalsIgnoreCase(candidateValue)) {
                    TEntityTemplate detectorElement = detector.getNodeTemplateOrRelationshipTemplate().stream()
                        .filter(et -> et.getId().equals(bpm.getDetectorElement().getId()))
                        .findFirst().get();
                    List<TPolicy> policies = ((HasPolicies) detectorElement).getPolicies().getPolicy();

                    policies.stream()
                        .filter(p -> p.getName().equals(bpm.getBehaviorPattern()))
                        .findFirst()
                        .ifPresent(policies::remove);
                }
            }
        });
        return detector;
    }

    // TODO: pull up refactoring?
    private void redirectInternalRelations(OTTopologyFragmentRefinementModel prm, TNodeTemplate currentDetectorNode,
                                           TNodeTemplate matchingNodeInTopology, TTopologyTemplate topology) {
        if (prm.getStayMappings() != null) {
            topology.getRelationshipTemplates()
                .forEach(relationship ->
                    // get all relationships that are either the source or the target of the current node that is staying
                    this.getStayMappings(currentDetectorNode, prm)
                        .forEach(staying -> {
                                String targetId = relationship.getTargetElement().getRef().getId();
                                String sourceId = relationship.getSourceElement().getRef().getId();

                                String idInRefinementStructure = staying.getDetectorElement().getId();

                                if (targetId.equals(idInRefinementStructure)) {
                                    LOGGER.debug("Redirecting target of {} to {}", relationship.getId(), matchingNodeInTopology.getId());
                                    relationship.getTargetElement().setRef(matchingNodeInTopology);
                                } else if (sourceId.equals(idInRefinementStructure)) {
                                    LOGGER.debug("Redirecting source of {} to {}", relationship.getId(), matchingNodeInTopology.getId());
                                    relationship.getSourceElement().setRef(matchingNodeInTopology);
                                }
                            }
                        )
                );
        }
    }

    private TEntityTemplate getEntityCorrespondence(ToscaEntity entity, GraphMapping<ToscaNode, ToscaEdge> graphMapping) {
        if (entity instanceof ToscaNode) {
            return graphMapping.getVertexCorrespondence((ToscaNode) entity, false).getTemplate();
        } else {
            return graphMapping.getEdgeCorrespondence((ToscaEdge) entity, false).getTemplate();
        }
    }

    private Stream<OTStayMapping> getStayMappings(TEntityTemplate current, OTTopologyFragmentRefinementModel prm) {
        return prm.getStayMappings() == null ? Stream.of() :
            prm.getStayMappings().stream()
                .filter(stayMapping -> stayMapping.getRefinementElement().getId().equals(current.getId()));
    }

    private boolean hasStayMappings(TEntityTemplate current, OTTopologyFragmentRefinementModel prm) {
        return this.getStayMappings(current, prm).findFirst().isPresent();
    }
}
