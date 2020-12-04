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

import java.util.Map;
import java.util.Objects;

import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.extensions.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.topologygraph.model.ToscaEdge;
import org.eclipse.winery.topologygraph.model.ToscaEntity;
import org.eclipse.winery.topologygraph.model.ToscaNode;

public class ToscaPatternDetectionMatcher extends ToscaTypeMatcher {

    private final OTTopologyFragmentRefinementModel prm;

    public ToscaPatternDetectionMatcher(OTTopologyFragmentRefinementModel prm) {
        this.prm = prm;
    }

    @Override
    public boolean isCompatible(ToscaNode left, ToscaNode right) {
        return super.isCompatible(left, right)
            && propsCompatible(left, right);
    }

    @Override
    public boolean isCompatible(ToscaEdge left, ToscaEdge right) {
        return super.isCompatible(left, right)
            && propsCompatible(left, right);
    }

    private boolean propsCompatible(ToscaEntity left, ToscaEntity right) {
        TEntityTemplate refinementElement = this.prm.getRefinementTopology().getNodeTemplateOrRelationshipTemplate()
            .contains(left.getTemplate()) ? left.getTemplate() : right.getTemplate();
        TEntityTemplate candidate = !this.prm.getRefinementTopology().getNodeTemplateOrRelationshipTemplate()
            .contains(left.getTemplate()) ? left.getTemplate() : right.getTemplate();

        boolean compatible = true;
        // TODO the implementation (currently) works for KV properties only
        if (ModelUtilities.hasKvProperties(refinementElement) && ModelUtilities.hasKvProperties(candidate)) {
            Map<String, String> refinementProps = ModelUtilities.getPropertiesKV(refinementElement);
            Map<String, String> candidateProps = ModelUtilities.getPropertiesKV(candidate);

            compatible = refinementProps.entrySet().stream()
                .allMatch(refinementProp -> refinementProp.getValue() == null || refinementProp.getValue().isEmpty()
                    || refinementProp.getValue().equalsIgnoreCase(candidateProps.get(refinementProp.getKey()))
                    || relatedBehaviorPatternCanBeRemoved(refinementElement, refinementProp.getKey())
                );
        }
        return compatible;
    }

    private boolean relatedBehaviorPatternCanBeRemoved(TEntityTemplate refinementElement, String propKey) {
        return this.prm.getBehaviorPatternMappings().stream()
            // props that are referenced by a behavior pattern mapping can be ignored
            // as the related behavior pattern can be removed later if the props aren't compatible
            .anyMatch(bpm -> bpm.getRefinementElement().getId().equals(refinementElement.getId())
                && bpm.getRefinementProperty().getKey().equals(propKey)
            );
    }
}
