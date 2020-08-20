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

package org.eclipse.winery.model.adaptation.substitution.refinement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.OTAttributeMapping;
import org.eclipse.winery.model.tosca.OTAttributeMappingType;
import org.eclipse.winery.model.tosca.OTDeploymentArtifactMapping;
import org.eclipse.winery.model.tosca.OTPatternRefinementModel;
import org.eclipse.winery.model.tosca.OTPermutationOption;
import org.eclipse.winery.model.tosca.OTRelationDirection;
import org.eclipse.winery.model.tosca.OTRelationMapping;
import org.eclipse.winery.model.tosca.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermutationGeneratorTest extends AbstractRefinementTest {

    @Test
    void checkSimplePermutabilityTest() {
        setUp();

        PermutationGenerator permutationGenerator = new PermutationGenerator();
        OTTopologyFragmentRefinementModel refinementModel = (OTTopologyFragmentRefinementModel) candidateForTopology3WithDa.getRefinementModel();

        assertTrue(permutationGenerator.checkPermutability(refinementModel));

        List<OTPermutationOption> options = refinementModel.getPermutationOptions();
        assertNotNull(options);
        assertEquals(2, options.size());

        options.forEach(permutationOption -> assertEquals(1, permutationOption.getOptions().size()));
    }

    @Test
    void checkPermutabilityWithPatternSet() {
        OTPatternRefinementModel refinementModel = generatePrm();

        PermutationGenerator permutationGenerator = new PermutationGenerator();

        assertFalse(permutationGenerator.checkPermutability(refinementModel));

        assertEquals(3, refinementModel.getPermutationMappings().size());
        refinementModel.getPermutationMappings()
            .forEach(mapping -> {
                assertFalse("1".equals(mapping.getDetectorElement().getId())
                    && "14".equals(mapping.getRefinementElement().getId()));
                assertFalse("2".equals(mapping.getDetectorElement().getId())
                    && "14".equals(mapping.getRefinementElement().getId()));
                assertFalse("2".equals(mapping.getDetectorElement().getId())
                    && "15".equals(mapping.getRefinementElement().getId()));
                assertFalse("2".equals(mapping.getDetectorElement().getId())
                    && "16".equals(mapping.getRefinementElement().getId()));
                assertFalse("3".equals(mapping.getDetectorElement().getId())
                    && "15".equals(mapping.getRefinementElement().getId()));
            });
        assertTrue(refinementModel.getPermutationMappings().removeIf(permutationMap ->
            permutationMap.getDetectorElement().getId().equals("1") && permutationMap.getRefinementElement().getId().equals("11")
        ));
        assertTrue(refinementModel.getPermutationMappings().removeIf(permutationMap ->
            permutationMap.getDetectorElement().getId().equals("1") && permutationMap.getRefinementElement().getId().equals("12")
        ));
        assertTrue(refinementModel.getPermutationMappings().removeIf(permutationMap ->
            permutationMap.getDetectorElement().getId().equals("2") && permutationMap.getRefinementElement().getId().equals("13")
        ));

        assertEquals(1, refinementModel.getComponentSets().size());
        assertEquals(2, refinementModel.getPermutationOptions().size());

        assertTrue(refinementModel.getComponentSets().get(0).getComponentSet().containsAll(Arrays.asList("2", "3")));

        assertTrue(refinementModel.getPermutationOptions().removeIf(option -> option.getOptions().contains("1")));
        assertTrue(refinementModel.getPermutationOptions().removeIf(option -> option.getOptions().containsAll(Arrays.asList("2", "3"))));
    }

    private OTPatternRefinementModel generatePrm() {
        /*                +------------------------+   
                          |                        \| 
        ########----------+      ########        ######## 
        #  (1) #---------------> # (11) # -----> # (12) # 
        ########                 ########        ######## 
            |                        | (2)           | (2)
            | (2)                   \/              \/    
           \/                    ########        ######## 
        ########---------------> # (13) #-------># (14) # 
        #  (2) #                 ########        ######## 
        ########--------+            | (2)           |
            |           |           \/               |      
            | (2)       +------> ########            |
           \/                    # (15) #        (2) |       
        ########---------------> ########            |
        #  (3) #                     | (2)           |
        ########                    \/               |
                                 ########            |
                                 # (16) #<-----------+ 
                                 ######## 
         */

        // region detector
        TNodeTemplate pattern_1 = new TNodeTemplate();
        pattern_1.setType("{http://ex.org}nodeType_1");
        pattern_1.setId("1");

        TNodeTemplate pattern_2 = new TNodeTemplate();
        pattern_2.setType("{http://ex.org}nodeType_2");
        pattern_2.setId("2");

        TNodeTemplate pattern_3 = new TNodeTemplate();
        pattern_3.setType("{http://ex.org}nodeType_3");
        pattern_3.setId("3");

        TRelationshipTemplate pattern1_hostedOn_pattern2 = new TRelationshipTemplate();
        pattern1_hostedOn_pattern2.setType("{http://ex.org}relType_hostedOn");
        pattern1_hostedOn_pattern2.setId("p1-p2");
        pattern1_hostedOn_pattern2.setSourceNodeTemplate(pattern_1);
        pattern1_hostedOn_pattern2.setTargetNodeTemplate(pattern_2);

        TRelationshipTemplate pattern2_hostedOn_pattern3 = new TRelationshipTemplate();
        pattern2_hostedOn_pattern3.setType("{http://ex.org}relType_hostedOn");
        pattern2_hostedOn_pattern3.setId("p1-p2");
        pattern2_hostedOn_pattern3.setSourceNodeTemplate(pattern_1);
        pattern2_hostedOn_pattern3.setTargetNodeTemplate(pattern_2);

        TTopologyTemplate detector = new TTopologyTemplate();
        detector.addNodeTemplate(pattern_1);
        detector.addNodeTemplate(pattern_2);
        detector.addNodeTemplate(pattern_3);
        detector.addRelationshipTemplate(pattern1_hostedOn_pattern2);
        detector.addRelationshipTemplate(pattern2_hostedOn_pattern3);
        // endregion

        // region refinement structure
        TNodeTemplate refinementNode_11 = new TNodeTemplate();
        refinementNode_11.setType("{http://ex.org}nodeType_11");
        refinementNode_11.setId("11");

        TNodeTemplate refinementNode_12 = new TNodeTemplate();
        refinementNode_12.setType("{http://ex.org}nodeType_12");
        refinementNode_12.setId("12");

        TNodeTemplate refinementNode_13 = new TNodeTemplate();
        refinementNode_13.setType("{http://ex.org}nodeType_13");
        refinementNode_13.setId("13");

        TNodeTemplate refinementNode_14 = new TNodeTemplate();
        refinementNode_14.setType("{http://ex.org}nodeType_14");
        refinementNode_14.setId("14");

        TNodeTemplate refinementNode_15 = new TNodeTemplate();
        refinementNode_15.setType("{http://ex.org}nodeType_15");
        refinementNode_15.setId("15");

        TNodeTemplate refinementNode_16 = new TNodeTemplate();
        refinementNode_16.setType("{http://ex.org}nodeType_16");
        refinementNode_16.setId("16");

        TRelationshipTemplate node11_connectsTo_node12 = new TRelationshipTemplate();
        node11_connectsTo_node12.setType("{http://ex.org}relType_connectsTo");
        node11_connectsTo_node12.setId("n11-n12");
        node11_connectsTo_node12.setSourceNodeTemplate(refinementNode_11);
        node11_connectsTo_node12.setTargetNodeTemplate(refinementNode_12);

        TRelationshipTemplate node11_hostedOn_node13 = new TRelationshipTemplate();
        node11_hostedOn_node13.setType("{http://ex.org}relType_hostedOn");
        node11_hostedOn_node13.setId("n11-n13");
        node11_hostedOn_node13.setSourceNodeTemplate(refinementNode_11);
        node11_hostedOn_node13.setTargetNodeTemplate(refinementNode_13);

        TRelationshipTemplate node12_hostedOn_node14 = new TRelationshipTemplate();
        node12_hostedOn_node14.setType("{http://ex.org}relType_hostedOn");
        node12_hostedOn_node14.setId("n12-n14");
        node12_hostedOn_node14.setSourceNodeTemplate(refinementNode_12);
        node12_hostedOn_node14.setTargetNodeTemplate(refinementNode_14);

        TRelationshipTemplate node13_connectsTo_node14 = new TRelationshipTemplate();
        node13_connectsTo_node14.setType("{http://ex.org}relType_connectsTo");
        node13_connectsTo_node14.setId("n13-n14");
        node13_connectsTo_node14.setSourceNodeTemplate(refinementNode_13);
        node13_connectsTo_node14.setTargetNodeTemplate(refinementNode_14);

        TRelationshipTemplate node13_hostedOn_node15 = new TRelationshipTemplate();
        node13_hostedOn_node15.setType("{http://ex.org}relType_hostedOn");
        node13_hostedOn_node15.setId("n13-n15");
        node13_hostedOn_node15.setSourceNodeTemplate(refinementNode_13);
        node13_hostedOn_node15.setTargetNodeTemplate(refinementNode_15);

        TRelationshipTemplate node14_hostedOn_node16 = new TRelationshipTemplate();
        node14_hostedOn_node16.setType("{http://ex.org}relType_hostedOn");
        node14_hostedOn_node16.setId("n14-n16");
        node14_hostedOn_node16.setSourceNodeTemplate(refinementNode_14);
        node14_hostedOn_node16.setTargetNodeTemplate(refinementNode_16);

        TRelationshipTemplate node15_hostedOn_node16 = new TRelationshipTemplate();
        node15_hostedOn_node16.setType("{http://ex.org}relType_hostedOn");
        node15_hostedOn_node16.setId("n15-n16");
        node15_hostedOn_node16.setSourceNodeTemplate(refinementNode_15);
        node15_hostedOn_node16.setTargetNodeTemplate(refinementNode_16);

        TTopologyTemplate refinementStructure = new TTopologyTemplate();
        refinementStructure.addNodeTemplate(refinementNode_11);
        refinementStructure.addNodeTemplate(refinementNode_12);
        refinementStructure.addNodeTemplate(refinementNode_13);
        refinementStructure.addNodeTemplate(refinementNode_14);
        refinementStructure.addNodeTemplate(refinementNode_15);
        refinementStructure.addNodeTemplate(refinementNode_16);
        refinementStructure.addRelationshipTemplate(node11_connectsTo_node12);
        refinementStructure.addRelationshipTemplate(node11_hostedOn_node13);
        refinementStructure.addRelationshipTemplate(node12_hostedOn_node14);
        refinementStructure.addRelationshipTemplate(node13_connectsTo_node14);
        refinementStructure.addRelationshipTemplate(node13_hostedOn_node15);
        refinementStructure.addRelationshipTemplate(node14_hostedOn_node16);
        refinementStructure.addRelationshipTemplate(node15_hostedOn_node16);
        // endregion

        // region mappings
        OTRelationMapping pattern1_to_node11 = new OTRelationMapping();
        pattern1_to_node11.setId("p1_to_n11");
        pattern1_to_node11.setRelationType(QName.valueOf("{http://ex.org}relType_connectsTo"));
        pattern1_to_node11.setDirection(OTRelationDirection.INGOING);
        pattern1_to_node11.setDetectorNode(pattern_1);
        pattern1_to_node11.setRefinementElement(refinementNode_11);

        OTDeploymentArtifactMapping pattern1_to_node12 = new OTDeploymentArtifactMapping();
        pattern1_to_node11.setId("p1_to_n12");
        pattern1_to_node12.setDetectorNode(pattern_1);
        pattern1_to_node12.setRefinementElement(refinementNode_12);
        pattern1_to_node12.setArtifactType(QName.valueOf("{http://ex.org}artType_war"));

        OTAttributeMapping pattern2_to_node13 = new OTAttributeMapping();
        pattern2_to_node13.setId("p2_to_n13");
        pattern2_to_node13.setType(OTAttributeMappingType.ALL);
        pattern2_to_node13.setDetectorNode(pattern_2);
        pattern2_to_node13.setRefinementElement(refinementNode_13);

        OTRelationMapping pattern2_to_node15 = new OTRelationMapping();
        pattern2_to_node15.setId("p2_to_n15");
        pattern2_to_node15.setRelationType(QName.valueOf("{http://ex.org}relType_connectsTo"));
        pattern2_to_node15.setDirection(OTRelationDirection.INGOING);
        pattern2_to_node15.setDetectorNode(pattern_2);
        pattern2_to_node15.setRefinementElement(refinementNode_15);

        OTRelationMapping pattern3_to_node15 = new OTRelationMapping();
        pattern3_to_node15.setId("p3_to_n15");
        pattern3_to_node15.setRelationType(QName.valueOf("{http://ex.org}relType_connectsTo"));
        pattern3_to_node15.setDirection(OTRelationDirection.INGOING);
        pattern3_to_node15.setDetectorNode(pattern_3);
        pattern3_to_node15.setRefinementElement(refinementNode_15);
        // endregion

        OTPatternRefinementModel refinementModel = new OTPatternRefinementModel();
        refinementModel.setId("1");
        refinementModel.setDetector(detector);
        refinementModel.setRefinementTopology(refinementStructure);
        refinementModel.setAttributeMappings(Collections.singletonList(pattern2_to_node13));
        refinementModel.setRelationMappings(Arrays.asList(pattern1_to_node11, pattern2_to_node15, pattern3_to_node15));
        refinementModel.setDeploymentArtifactMappings(Collections.singletonList(pattern1_to_node12));

        return refinementModel;
    }
}
