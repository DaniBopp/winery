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

import java.util.Map;

import org.eclipse.winery.common.ids.definitions.PatternRefinementModelId;
import org.eclipse.winery.model.tosca.OTPatternRefinementModel;
import org.eclipse.winery.model.tosca.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.repository.TestWithGitBackedRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import org.junit.jupiter.api.Test;

import static org.eclipse.winery.model.adaptation.substitution.refinement.PermutationHelper.addAllPermutationMappings;
import static org.eclipse.winery.model.adaptation.substitution.refinement.PermutationHelper.generatePrm;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PermutationGeneratorTestWithGitBackedRepository extends TestWithGitBackedRepository {

    @Test
    void testGeneration() throws Exception {
        this.setRevisionTo("origin/plain");

        OTPatternRefinementModel refinementModel = generatePrm();
        addAllPermutationMappings(refinementModel);

        PatternRefinementModelId id = new PatternRefinementModelId(refinementModel.getTargetNamespace(), refinementModel.getIdFromIdOrNameField(), false);
        RepositoryFactory.getRepository().setElement(id, refinementModel);

        PermutationGenerator generator = new PermutationGenerator();
        Map<String, OTTopologyFragmentRefinementModel> permutations = generator.generatePermutations(refinementModel);

        assertEquals(2, permutations.size());
        
        // TODO: not finished yet, --> rels are strange
    }
}
