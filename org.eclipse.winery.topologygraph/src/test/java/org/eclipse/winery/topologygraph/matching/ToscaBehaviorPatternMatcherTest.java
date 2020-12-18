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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TPolicies;
import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.extensions.OTAttributeMapping;
import org.eclipse.winery.model.tosca.extensions.OTAttributeMappingType;
import org.eclipse.winery.model.tosca.extensions.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.NamespaceManager;
import org.eclipse.winery.topologygraph.model.ToscaEntity;
import org.eclipse.winery.topologygraph.model.ToscaNode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToscaBehaviorPatternMatcherTest {

    @Test
    public void propertiesCompatible() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        LinkedHashMap<String, String> detectorProps = new LinkedHashMap<>();
        detectorProps.put("null", null);
        detectorProps.put("empty", "");
        detectorProps.put("match", "this has to match");
        detectorProps.put("ignoreCase", "THIS HAS TO MATCH INDEPENDENT OF CASE");
        LinkedHashMap<String, String> candidateProps = new LinkedHashMap<>();
        candidateProps.put("null", "does not have to be null");
        candidateProps.put("empty", "does not have to be empty");
        candidateProps.put("match", "this has to match");
        candidateProps.put("ignoreCase", "this has to match independent of case");

        TNodeTemplate detector = new TNodeTemplate();
        detector.setId("detector");
        ModelUtilities.setPropertiesKV(detector, detectorProps);
        ToscaNode detectorNode = new ToscaNode();
        detectorNode.setNodeTemplate(detector);
        TNodeTemplate candidate = new TNodeTemplate();
        candidate.setId("candidate");
        ModelUtilities.setPropertiesKV(candidate, candidateProps);
        ToscaNode candidateNode = new ToscaNode();
        candidateNode.setNodeTemplate(candidate);

        List<OTAttributeMapping> attributeMappings = new ArrayList<>();
        attributeMappings.add(new OTAttributeMapping(new OTAttributeMapping.Builder()
            .setType(OTAttributeMappingType.ALL)
            .setDetectorElement(detector)
        ));
        OTTopologyFragmentRefinementModel prm = new OTTopologyFragmentRefinementModel();
        prm.setAttributeMappings(attributeMappings);
        prm.setDetector(new TTopologyTemplate(new TTopologyTemplate.Builder()
            .addNodeTemplates(detector)
        ));

        Method method = ToscaBehaviorPatternMatcher.class
            .getDeclaredMethod("propertiesCompatible", ToscaEntity.class, ToscaEntity.class);
        method.setAccessible(true);
        ToscaBehaviorPatternMatcher matcher = new ToscaBehaviorPatternMatcher(prm, null);
        assertTrue((Boolean) method.invoke(matcher, detectorNode, candidateNode));

        detectorProps.put("doesNotMatch", "something");
        candidateProps.put("doesNotMatch", "something else");
        assertFalse((Boolean) method.invoke(matcher, detectorNode, candidateNode));
        prm.getAttributeMappings().clear();
        assertTrue((Boolean) method.invoke(matcher, detectorNode, candidateNode));

        prm.getAttributeMappings().add(new OTAttributeMapping(new OTAttributeMapping.Builder()
            .setType(OTAttributeMappingType.SELECTIVE)
            .setDetectorElement(detector)
            .setDetectorProperty("match")
        ));
        assertTrue((Boolean) method.invoke(matcher, detectorNode, candidateNode));

        prm.getAttributeMappings().add(new OTAttributeMapping(new OTAttributeMapping.Builder()
            .setType(OTAttributeMappingType.SELECTIVE)
            .setDetectorElement(detector)
            .setDetectorProperty("doesNotMatch")
        ));
        assertFalse((Boolean) method.invoke(matcher, detectorNode, candidateNode));
    }

    @Test
    public void behaviorPatternsCompatible() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        NamespaceManager namespaceManager = new MockNamespaceManager() {
            @Override
            public boolean isPatternNamespace(String namespace) {
                return namespace.equals("patternNs");
            }
        };
        TPolicies detectorPolicies = new TPolicies();
        detectorPolicies.getPolicy().add(new TPolicy(new TPolicy.Builder(QName.valueOf("{ns}type1"))));
        TPolicies candidatePolicies = new TPolicies();

        TNodeTemplate detector = new TNodeTemplate();
        detector.setPolicies(detectorPolicies);
        ToscaNode detectorNode = new ToscaNode();
        detectorNode.setNodeTemplate(detector);
        TNodeTemplate candidate = new TNodeTemplate();
        candidate.setPolicies(candidatePolicies);
        ToscaNode candidateNode = new ToscaNode();
        candidateNode.setNodeTemplate(candidate);

        OTTopologyFragmentRefinementModel prm = new OTTopologyFragmentRefinementModel();
        prm.setDetector(new TTopologyTemplate(new TTopologyTemplate.Builder()
            .addNodeTemplates(detector)
        ));

        Method method = ToscaBehaviorPatternMatcher.class
            .getDeclaredMethod("behaviorPatternsCompatible", ToscaEntity.class, ToscaEntity.class);
        method.setAccessible(true);
        ToscaBehaviorPatternMatcher matcher = new ToscaBehaviorPatternMatcher(prm, namespaceManager);
        assertTrue((Boolean) method.invoke(matcher, detectorNode, candidateNode));

        detectorPolicies.getPolicy().clear();
        candidatePolicies.getPolicy().add(new TPolicy(new TPolicy.Builder(QName.valueOf("{ns}type1"))));
        assertTrue((Boolean) method.invoke(matcher, detectorNode, candidateNode));

        detectorPolicies.getPolicy().add(new TPolicy(new TPolicy.Builder(QName.valueOf("{patternNs}type2"))));
        assertFalse((Boolean) method.invoke(matcher, detectorNode, candidateNode));

        candidatePolicies.getPolicy().add(new TPolicy(new TPolicy.Builder(QName.valueOf("{patternNs}type2"))));
        assertTrue((Boolean) method.invoke(matcher, detectorNode, candidateNode));

        candidatePolicies.getPolicy().add(new TPolicy(new TPolicy.Builder(QName.valueOf("{patternNs}type3"))));
        assertFalse((Boolean) method.invoke(matcher, detectorNode, candidateNode));
    }
}
