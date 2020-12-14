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

package org.eclipse.winery.topologygraph.matching;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.winery.model.tosca.HasPolicies;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TPolicies;
import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.extensions.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.topologygraph.model.ToscaEdge;
import org.eclipse.winery.topologygraph.model.ToscaEntity;
import org.eclipse.winery.topologygraph.model.ToscaNode;

public class ToscaBehaviorPatternMatcher extends ToscaTypeMatcher {

    private final OTTopologyFragmentRefinementModel prm;
    private final List<TPolicy> existingBehaviorPatterns;

    public ToscaBehaviorPatternMatcher(OTTopologyFragmentRefinementModel prm, List<TPolicy> existingBehaviorPatterns) {
        this.prm = prm;
        this.existingBehaviorPatterns = existingBehaviorPatterns;
    }

    @Override
    public boolean isCompatible(ToscaNode left, ToscaNode right) {
        return super.isCompatible(left, right)
            && propsCompatible(left, right)
            && behaviorPatternsCompatible(left, right);
    }

    @Override
    public boolean isCompatible(ToscaEdge left, ToscaEdge right) {
        return super.isCompatible(left, right)
            && propsCompatible(left, right)
            && behaviorPatternsCompatible(left, right);
    }

    private boolean propsCompatible(ToscaEntity left, ToscaEntity right) {
        TEntityTemplate detectorElement = this.prm.getDetector().getNodeTemplateOrRelationshipTemplate()
            .contains(left.getTemplate()) ? left.getTemplate() : right.getTemplate();
        TEntityTemplate candidateElement = !this.prm.getDetector().getNodeTemplateOrRelationshipTemplate()
            .contains(left.getTemplate()) ? left.getTemplate() : right.getTemplate();

        boolean compatible = true;
        if (ModelUtilities.hasKvProperties(detectorElement) && ModelUtilities.hasKvProperties(candidateElement)) {
            Map<String, String> detectorProps = ModelUtilities.getPropertiesKV(detectorElement);
            Map<String, String> candidateProps = ModelUtilities.getPropertiesKV(candidateElement);

            compatible = detectorProps.entrySet().stream()
                .allMatch(detectorProp -> detectorProp.getValue() == null || detectorProp.getValue().isEmpty()
                    || detectorProp.getValue().equalsIgnoreCase(candidateProps.get(detectorProp.getKey()))
                    || relatedBehaviorPatternCanBeRemoved(detectorElement, detectorProp.getKey())
                );
        }
        return compatible;
    }

    private boolean relatedBehaviorPatternCanBeRemoved(TEntityTemplate detectorElement, String propKey) {
        return this.prm.getBehaviorPatternMappings().stream()
            // props that are referenced by a behavior pattern mapping can be ignored
            // as the related behavior pattern can be removed later if the props aren't compatible
            .anyMatch(bpm -> bpm.getDetectorElement().getId().equals(detectorElement.getId())
                && bpm.getProperty().getKey().equals(propKey));
    }

    private boolean behaviorPatternsCompatible(ToscaEntity left, ToscaEntity right) {
        TEntityTemplate detectorElement = this.prm.getDetector().getNodeTemplateOrRelationshipTemplate()
            .contains(left.getTemplate()) ? left.getTemplate() : right.getTemplate();
        TEntityTemplate candidateElement = !this.prm.getDetector().getNodeTemplateOrRelationshipTemplate()
            .contains(left.getTemplate()) ? left.getTemplate() : right.getTemplate();
        TPolicies detectorPolicies = ((HasPolicies) detectorElement).getPolicies();
        TPolicies candidatePolicies = ((HasPolicies) candidateElement).getPolicies();

        boolean compatible = true;
        if (Objects.nonNull(detectorPolicies) && Objects.nonNull(candidatePolicies)) {
            // TODO
        }
        return compatible;
    }
}
