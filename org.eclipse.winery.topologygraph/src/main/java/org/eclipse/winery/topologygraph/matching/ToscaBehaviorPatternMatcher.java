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

import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.extensions.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.topologygraph.model.ToscaEntity;

public class ToscaBehaviorPatternMatcher extends ToscaPrmPropertyMatcher {

    private final OTTopologyFragmentRefinementModel prm;

    public ToscaBehaviorPatternMatcher(OTTopologyFragmentRefinementModel prm) {
        super(prm.getDetector().getNodeTemplateOrRelationshipTemplate(), RepositoryFactory.getRepository().getNamespaceManager());
        this.prm = prm;
    }

    @Override
    public boolean propertiesCompatible(ToscaEntity left, ToscaEntity right) {
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
}
