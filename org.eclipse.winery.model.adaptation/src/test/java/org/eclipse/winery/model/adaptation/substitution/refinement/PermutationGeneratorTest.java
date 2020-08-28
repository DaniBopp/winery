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
import java.util.List;

import org.eclipse.winery.model.tosca.OTPatternRefinementModel;
import org.eclipse.winery.model.tosca.OTPermutationOption;
import org.eclipse.winery.model.tosca.OTTopologyFragmentRefinementModel;

import org.junit.jupiter.api.Test;

import static org.eclipse.winery.model.adaptation.substitution.refinement.PermutationHelper.addAllPermutationMappings;
import static org.eclipse.winery.model.adaptation.substitution.refinement.PermutationHelper.addSomePermutationMappings;
import static org.eclipse.winery.model.adaptation.substitution.refinement.PermutationHelper.generateComplexPrmWithPatternSet;
import static org.eclipse.winery.model.adaptation.substitution.refinement.PermutationHelper.generatePrmWithoutPermutationMaps;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermutationGeneratorTest extends AbstractRefinementTest {

    @Test
    void checkSimpleMutabilityTest() {
        setUp();

        PermutationGenerator permutationGenerator = new PermutationGenerator();
        OTTopologyFragmentRefinementModel refinementModel = (OTTopologyFragmentRefinementModel) candidateForTopology3WithDa.getRefinementModel();

        assertTrue(permutationGenerator.checkMutability(refinementModel));

        List<OTPermutationOption> options = refinementModel.getPermutationOptions();
        assertNotNull(options);
        assertEquals(2, options.size());

        options.forEach(permutationOption -> assertEquals(1, permutationOption.getOptions().size()));
    }

    @Test
    void checkSimplePrmMutability() {
        OTPatternRefinementModel refinementModel = generatePrmWithoutPermutationMaps();

        PermutationGenerator permutationGenerator = new PermutationGenerator();
        assertTrue(permutationGenerator.checkMutability(refinementModel));
        assertEquals(6, refinementModel.getPermutationOptions().size());
        assertEquals(7, refinementModel.getPermutationMappings().size());
    }

    @Test
    void checkMutabilityOfNotMutablePrmWithPatternSet() {
        OTPatternRefinementModel refinementModel = generateComplexPrmWithPatternSet();

        PermutationGenerator permutationGenerator = new PermutationGenerator();
        assertFalse(permutationGenerator.checkMutability(refinementModel));
        assertEquals("There are detector nodes which could not be mapped to a refinement node: 3",
            permutationGenerator.getMutabilityErrorReason());

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

    @Test
    void checkMutabilityOfNotMutablePrmBecauseOfARelationThatCannotBeRedirected() {
        OTPatternRefinementModel refinementModel = generateComplexPrmWithPatternSet();
        addSomePermutationMappings(refinementModel);

        PermutationGenerator permutationGenerator = new PermutationGenerator();
        assertFalse(permutationGenerator.checkMutability(refinementModel));
        assertEquals("There are relations that cannot be redirected during the generation: 1--2",
            permutationGenerator.getMutabilityErrorReason());
    }

    @Test
    void checkMutabilityOfNotMutablePrmBecauseOfANotMappableRefinementNode() {
        OTPatternRefinementModel refinementModel = generateComplexPrmWithPatternSet();
        addSomePermutationMappings(refinementModel);

        refinementModel.getPermutationMappings().removeIf(map -> map.getId().equals("p2_to_n14"));

        PermutationGenerator permutationGenerator = new PermutationGenerator();
        assertFalse(permutationGenerator.checkMutability(refinementModel));
        assertEquals("There are refinement nodes which could not be mapped to a detector node: 14",
            permutationGenerator.getMutabilityErrorReason());
    }

    @Test
    void checkMutabilityOfMutablePrmWithPermutationMapping() {
        OTPatternRefinementModel refinementModel = generateComplexPrmWithPatternSet();
        addAllPermutationMappings(refinementModel);

        PermutationGenerator permutationGenerator = new PermutationGenerator();
        assertTrue(permutationGenerator.checkMutability(refinementModel));
        assertEquals("", permutationGenerator.getMutabilityErrorReason());

        assertEquals(7, refinementModel.getPermutationMappings().size());
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
}
