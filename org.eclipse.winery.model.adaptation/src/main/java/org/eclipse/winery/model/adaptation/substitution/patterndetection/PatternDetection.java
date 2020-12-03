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

import org.eclipse.winery.model.adaptation.substitution.refinement.AbstractRefinement;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementCandidate;
import org.eclipse.winery.model.adaptation.substitution.refinement.RefinementChooser;
import org.eclipse.winery.model.ids.extensions.PatternRefinementModelId;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.extensions.OTRefinementModel;
import org.eclipse.winery.topologygraph.matching.IToscaMatcher;
import org.eclipse.winery.topologygraph.matching.ToscaPatternDetectionMatcher;

public class PatternDetection extends AbstractRefinement {

    public PatternDetection(RefinementChooser refinementChooser) {
        super(refinementChooser, PatternRefinementModelId.class, "detected");
    }

    @Override
    public TTopologyTemplate getDetector(OTRefinementModel prm) {
        return prm.getRefinementTopology();
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
        // TODO
        return true;
    }

    @Override
    public void applyRefinement(RefinementCandidate refinement, TTopologyTemplate topology) {
        // TODO
    }
}
