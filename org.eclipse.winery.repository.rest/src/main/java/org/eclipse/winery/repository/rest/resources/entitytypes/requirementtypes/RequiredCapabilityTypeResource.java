/*******************************************************************************
 * Copyright (c) 2012-2013 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.repository.rest.resources.entitytypes.requirementtypes;

import com.sun.jersey.api.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.winery.common.ids.definitions.CapabilityTypeId;
import org.eclipse.winery.model.tosca.TRequirementType;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.resources.apiData.AvailableSuperclassesApiData;
import org.eclipse.winery.repository.rest.resources.apiData.RequiredCapabilityTypeApiData;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.namespace.QName;

public class RequiredCapabilityTypeResource {

    private RequirementTypeResource requirementTypeResource;

    public RequiredCapabilityTypeResource(RequirementTypeResource requirementTypeResource) {
        this.requirementTypeResource = requirementTypeResource;
    }

    public TRequirementType getRequirementType() {
        return this.requirementTypeResource.getRequirementType();
    }

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    public Response putRequiredCapabilityType(String type) {
        if (StringUtils.isEmpty(type)) {
            return Response.status(Status.BAD_REQUEST).entity("type must not be empty").build();
        }
        QName qname = QName.valueOf(type);
        CapabilityTypeId id = new CapabilityTypeId(qname);
        if (RepositoryFactory.getRepository().exists(id)) {
            // everything allright. Store new reference
            this.getRequirementType().setRequiredCapabilityType(qname);
            return RestUtils.persist(this.requirementTypeResource);
        } else {
            throw new NotFoundException("Given QName could not be resolved to an existing capability type");
        }
    }

    @DELETE
    public Response deleteRequiredCapabilityType() {
        this.getRequirementType().setRequiredCapabilityType(null);
        return RestUtils.persist(this.requirementTypeResource);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RequiredCapabilityTypeApiData getAllCapabilityTypes() {
        return new RequiredCapabilityTypeApiData(new AvailableSuperclassesApiData(CapabilityTypeId.class), this.getRequirementType().getRequiredCapabilityType());
    }
}
