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

package org.eclipse.winery.model.adaptation.substitution.patterndetection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.winery.model.adaptation.substitution.refinement.AbstractRefinement;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementCandidate;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementChooser;
import org.eclipse.winery.model.ids.extensions.PatternRefinementModelId;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.extensions.OTRefinementModel;
import org.eclipse.winery.topologygraph.matching.IToscaMatcher;
import org.eclipse.winery.topologygraph.matching.ToscaIsomorphismMatcher;
import org.eclipse.winery.topologygraph.matching.ToscaPatternDetectionMatcher;
import org.eclipse.winery.topologygraph.model.ToscaEdge;
import org.eclipse.winery.topologygraph.model.ToscaGraph;
import org.eclipse.winery.topologygraph.model.ToscaNode;
import org.eclipse.winery.topologygraph.transformation.ToscaTransformer;

import org.jgrapht.GraphMapping;

public class PatternDetection extends AbstractRefinement {

    public PatternDetection(RefinementChooser refinementChooser) {
        super(refinementChooser, PatternRefinementModelId.class, "detected");
    }

    @Override
    public void refineTopology(TTopologyTemplate topology) {
        ToscaIsomorphismMatcher isomorphismMatcher = new ToscaIsomorphismMatcher();
        int[] id = new int[1];

        ToscaGraph topologyGraph = ToscaTransformer.createTOSCAGraph(topology);
        List<RefinementCandidate> candidates = new ArrayList<>();
        this.refinementModels.forEach(prm -> {
            ToscaGraph detectorGraph = ToscaTransformer.createTOSCAGraph(prm.getDetector());
            IToscaMatcher matcher = this.getMatcher(prm);
            Iterator<GraphMapping<ToscaNode, ToscaEdge>> matches = isomorphismMatcher.findMatches(detectorGraph, topologyGraph, matcher);

            matches.forEachRemaining(mapping -> {
                RefinementCandidate candidate = new RefinementCandidate(prm, mapping, detectorGraph, id[0]++);
                candidates.add(candidate);
            });
        });

        this.refinementChooser.chooseRefinement(candidates, this.refinementServiceTemplateId, topology);
    }

    @Override
    public boolean getLoopCondition(TTopologyTemplate topology) {
        return true;
    }

    @Override
    public IToscaMatcher getMatcher(OTRefinementModel prm) {
        return new ToscaPatternDetectionMatcher();
    }

    @Override
    public boolean isApplicable(RefinementCandidate candidate, TTopologyTemplate topology) {
        return false;
    }

    @Override
    public void applyRefinement(RefinementCandidate refinement, TTopologyTemplate topology) {

    }
}
