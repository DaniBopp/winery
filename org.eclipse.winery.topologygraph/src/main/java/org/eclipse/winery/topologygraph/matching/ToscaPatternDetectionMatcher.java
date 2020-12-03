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
        TEntityTemplate refinementStructure = this.prm.getRefinementTopology().getNodeTemplateOrRelationshipTemplate()
            .contains(left.getTemplate()) ? left.getTemplate() : right.getTemplate();
        TEntityTemplate candidate = !this.prm.getRefinementTopology().getNodeTemplateOrRelationshipTemplate()
            .contains(left.getTemplate()) ? left.getTemplate() : right.getTemplate();

        boolean compatible = true;
        // TODO the implementation (currently) works for KV properties only
        if (hasKvProperties(refinementStructure) && hasKvProperties(candidate)) {
            Map<String, String> refinementProps = ModelUtilities.getPropertiesKV(refinementStructure);
            Map<String, String> candidateProps = ModelUtilities.getPropertiesKV(candidate);

            compatible = refinementProps.entrySet().stream()
                .allMatch(entry -> empty(entry)
                    || entry.getValue().equalsIgnoreCase(candidateProps.get(entry.getKey()))
                    || relatedBehaviorPatternCanBeRemoved(entry)
                );
        }
        return compatible;
    }

    private boolean hasKvProperties(TEntityTemplate entityTemplate) {
        return Objects.nonNull(entityTemplate.getProperties())
            && Objects.nonNull(ModelUtilities.getPropertiesKV(entityTemplate));
    }

    private boolean empty(Map.Entry<String, String> entry) {
        return entry.getValue() == null || entry.getValue().isEmpty();
    }

    private boolean relatedBehaviorPatternCanBeRemoved(Map.Entry<String, String> entry) {
        return this.prm.getBehaviorPatternMappings().stream()
            // we know which behavior pattern is implemented by the property
            // -> behavior patterns related to non matching properties can be removed later
            .anyMatch(bpm -> bpm.getRefinementProperty().getKey().equals(entry.getKey()));
    }
}
