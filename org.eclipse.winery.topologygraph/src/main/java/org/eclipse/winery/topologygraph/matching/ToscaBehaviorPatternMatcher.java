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

import org.eclipse.winery.model.tosca.HasPolicies;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TPolicies;
import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.extensions.OTAttributeMappingType;
import org.eclipse.winery.model.tosca.extensions.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.NamespaceManager;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.topologygraph.model.ToscaEdge;
import org.eclipse.winery.topologygraph.model.ToscaEntity;
import org.eclipse.winery.topologygraph.model.ToscaNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToscaBehaviorPatternMatcher extends ToscaTypeMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToscaBehaviorPatternMatcher.class);
    private final NamespaceManager namespaceManager;
    private final OTTopologyFragmentRefinementModel prm;
    private final List<TPolicy> initialBehaviorPatterns;

    public ToscaBehaviorPatternMatcher(OTTopologyFragmentRefinementModel prm, List<TPolicy> initialBehaviorPatterns) {
        this.namespaceManager = RepositoryFactory.getRepository().getNamespaceManager();
        this.prm = prm;
        this.initialBehaviorPatterns = initialBehaviorPatterns;
    }

    @Override
    public boolean isCompatible(ToscaNode left, ToscaNode right) {
        return super.isCompatible(left, right)
            && propertiesCompatible(left, right)
            && behaviorPatternsCompatible(left, right);
    }

    @Override
    public boolean isCompatible(ToscaEdge left, ToscaEdge right) {
        return super.isCompatible(left, right)
            && propertiesCompatible(left, right)
            && behaviorPatternsCompatible(left, right);
    }

    private boolean propertiesCompatible(ToscaEntity left, ToscaEntity right) {
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
                            LOGGER.warn("Attribute type mapping ALL is not really applicable to pattern detection, " +
                                "use SELECTIVE for best results");
                            return detectorProps.entrySet().stream()
                                .allMatch(detectorProp -> propertiesCompatible(
                                    detectorProp.getValue(),
                                    candidateProps.get(detectorProp.getKey())
                                ));
                        } else {
                            String detectorValue = detectorProps.get(mapping.getDetectorProperty());
                            String candidateValue = candidateProps.get(mapping.getDetectorProperty());
                            return propertiesCompatible(detectorValue, candidateValue);
                        }
                    });
            }
        }
        return compatible;
    }

    private boolean propertiesCompatible(String detectorValue, String candidateValue) {
        // wildcard values (*) are not supported for pattern detection as they are only set in the refinement
        // -> applicability not clear
        return detectorValue == null || detectorValue.isEmpty()
            || detectorValue.equalsIgnoreCase(candidateValue);
    }

    private boolean behaviorPatternsCompatible(ToscaEntity left, ToscaEntity right) {
        TEntityTemplate detectorElement = prm.getDetector().getNodeTemplateOrRelationshipTemplate()
            .contains(left.getTemplate()) ? left.getTemplate() : right.getTemplate();
        TEntityTemplate candidateElement = !prm.getDetector().getNodeTemplateOrRelationshipTemplate()
            .contains(left.getTemplate()) ? left.getTemplate() : right.getTemplate();
        TPolicies detectorPolicies = ((HasPolicies) detectorElement).getPolicies();
        TPolicies candidatePolicies = ((HasPolicies) candidateElement).getPolicies();

        // behavior patterns have to be equal
        return behaviorPatternsCompatible(detectorPolicies, candidatePolicies)
            && behaviorPatternsCompatible(candidatePolicies, detectorPolicies);
    }

    private boolean behaviorPatternsCompatible(TPolicies detectorPolicies, TPolicies candidatePolices) {
        boolean compatible = true;

        if (detectorPolicies != null && candidatePolices != null) {
            compatible = detectorPolicies.getPolicy().stream().allMatch(detectorPolicy -> {
                boolean isBehaviorPattern = namespaceManager.isPatternNamespace(detectorPolicy.getPolicyType().getNamespaceURI());
                boolean isInitialBehaviorPattern = initialBehaviorPatterns.contains(detectorPolicy);

                if (isBehaviorPattern && !isInitialBehaviorPattern) {
                    return candidatePolices.getPolicy().stream().anyMatch(candidatePolicy -> {
                        boolean typeEquals = candidatePolicy.getPolicyType().equals(detectorPolicy.getPolicyType());

                        if (typeEquals && detectorPolicy.getPolicyRef() != null) {
                            return candidatePolicy.getPolicyRef() != null
                                && candidatePolicy.getPolicyRef().equals(detectorPolicy.getPolicyRef());
                        }
                        return typeEquals;
                    });
                }
                return true;
            });
        } else if (detectorPolicies != null) {
            compatible = detectorPolicies.getPolicy().stream().noneMatch(detectorPolicy -> {
                boolean isBehaviorPattern = namespaceManager.isPatternNamespace(detectorPolicy.getPolicyType().getNamespaceURI());
                boolean isInitialBehaviorPattern = initialBehaviorPatterns.contains(detectorPolicy);
                return isBehaviorPattern && !isInitialBehaviorPattern;
            });
        }
        return compatible;
    }
}
