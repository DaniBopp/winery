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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.winery.model.adaptation.substitution.refinement.AbstractRefinement;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementCandidate;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementChooser;
import org.eclipse.winery.model.ids.extensions.PatternRefinementModelId;
import org.eclipse.winery.model.tosca.HasPolicies;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
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

public class PatternDetection extends AbstractRefinement {

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
        Map<String, String> idMapping = BackendUtils.mergeTopologyTemplateAinTopologyTemplateB(
            detector,
            topology,
            new ArrayList<>()
        );
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

    private TEntityTemplate getEntityCorrespondence(ToscaEntity entity, GraphMapping<ToscaNode, ToscaEdge> graphMapping) {
        if (entity instanceof ToscaNode) {
            return graphMapping.getVertexCorrespondence((ToscaNode) entity, false).getTemplate();
        } else {
            return graphMapping.getEdgeCorrespondence((ToscaEdge) entity, false).getTemplate();
        }
    }
}
