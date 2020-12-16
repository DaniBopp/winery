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
import org.eclipse.winery.model.tosca.extensions.OTAttributeMappingType;
import org.eclipse.winery.model.tosca.extensions.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.topologygraph.model.ToscaEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToscaBehaviorPatternMatcher extends ToscaPrmPropertyMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToscaBehaviorPatternMatcher.class);
    private final OTTopologyFragmentRefinementModel prm;

    public ToscaBehaviorPatternMatcher(OTTopologyFragmentRefinementModel prm) {
        super(prm.getDetector().getNodeTemplateOrRelationshipTemplate(), RepositoryFactory.getRepository().getNamespaceManager());
        this.prm = prm;
    }

    @Override
    public boolean propertiesCompatible(ToscaEntity left, ToscaEntity right) {
        TEntityTemplate detectorElement = prm.getDetector().getNodeTemplateOrRelationshipTemplate()
            .contains(left.getTemplate()) ? left.getTemplate() : right.getTemplate();
        TEntityTemplate candidateElement = !prm.getDetector().getNodeTemplateOrRelationshipTemplate()
            .contains(left.getTemplate()) ? left.getTemplate() : right.getTemplate();

        boolean compatible = true;
        if (ModelUtilities.hasKvProperties(detectorElement) && ModelUtilities.hasKvProperties(candidateElement)) {
            Map<String, String> detectorProps = ModelUtilities.getPropertiesKV(detectorElement);
            Map<String, String> candidateProps = ModelUtilities.getPropertiesKV(candidateElement);

            // only attributes with attribute mappings need to match
            // properties referenced in behavior pattern mappings may match -> related behavior patterns can be removed later if not
            // all other properties are considered irrelevant for pattern detection
            if (prm.getAttributeMappings() != null) {
                compatible = prm.getAttributeMappings().stream()
                    .filter(mapping -> mapping.getDetectorElement().getId().equals(detectorElement.getId()))
                    .allMatch(mapping -> {
                        if (mapping.getType() == OTAttributeMappingType.ALL) {
                            // correspondence between detector, refinement and candidate properties is not clear
                            // -> not really applicable
                            LOGGER.error("Attribute mapping type 'ALL' is not supported for pattern detection, " +
                                "use SELECTIVE instead");
                            return false;
                        }
                        Map<String, String> refinementProps = ModelUtilities.getPropertiesKV(mapping.getRefinementElement());
                        if (refinementProps != null && refinementProps.containsValue("*")) {
                            // wildcards are only set in the refinement, applicability not clear
                            LOGGER.warn("Wildcard values (*) are not supported for pattern detection, will be ignored");
                        }

                        String detectorValue = detectorProps.get(mapping.getDetectorProperty());
                        String candidateValue = candidateProps.get(mapping.getDetectorProperty());
                        return detectorValue == null || detectorValue.isEmpty()
                            || detectorValue.equalsIgnoreCase(candidateValue);
                    });
            }
        }
        return compatible;
    }

    @Override
    public boolean characterizingPatternsCompatible(ToscaEntity left, ToscaEntity right) {
        // TODO: only if equal behavior patterns
        // TODO: ignore initial annotated behavior patterns
        return super.characterizingPatternsCompatible(left, right);
    }
}
