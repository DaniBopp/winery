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

package org.eclipse.winery.repository.rest.resources.refinementmodels;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.winery.model.tosca.OTPermutationMapping;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.repository.rest.resources._support.AbstractRefinementModelMappingsResource;
import org.eclipse.winery.repository.rest.resources._support.AbstractRefinementModelResource;
import org.eclipse.winery.repository.rest.resources.apiData.PrmPermutationMappingApiData;

public class PermutationMappingsRessource extends AbstractRefinementModelMappingsResource<OTPermutationMapping> {
    public PermutationMappingsRessource(AbstractRefinementModelResource res, List<OTPermutationMapping> artifactMappings) {
        super(res);
        this.mappings = artifactMappings;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<OTPermutationMapping> addPermutationMapping(PrmPermutationMappingApiData[] mappings) {

        List<OTPermutationMapping> mappingsList = new ArrayList();

        for (PrmPermutationMappingApiData permutationMapping : mappings) {
            TEntityTemplate detectorElement = this.res.getDetectorResource().getTopologyTemplate().getNodeTemplate(permutationMapping.detectorElement);
            TEntityTemplate refinementElement = this.res.getRefinementTopologyResource().getTopologyTemplate().getNodeTemplate(permutationMapping.refinementElement);
            mappingsList = this.addMapping(permutationMapping.createPermutationMapping(detectorElement, refinementElement));
        }

        return mappingsList;
    }
}
