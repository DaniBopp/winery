/*******************************************************************************
 * Copyright (c) 2018-2020 Contributors to the Eclipse Foundation
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.winery.model.adaptation.substitution.refinement.PermutationGenerator;
import org.eclipse.winery.model.tosca.extensions.OTAttributeMapping;
import org.eclipse.winery.model.tosca.extensions.OTDeploymentArtifactMapping;
import org.eclipse.winery.model.tosca.extensions.OTPatternRefinementModel;
import org.eclipse.winery.model.tosca.extensions.OTPermutationMapping;
import org.eclipse.winery.model.tosca.extensions.OTStayMapping;
import org.eclipse.winery.model.tosca.extensions.OTTopologyFragmentRefinementModel;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.repository.rest.resources._support.AbstractRefinementModelResource;
import org.eclipse.winery.repository.rest.resources.apiData.PermutationsResponse;
import org.eclipse.winery.repository.rest.resources.apiData.PrmDeploymentArtifactMappingApiData;
import org.eclipse.winery.repository.rest.resources.apiData.PrmPermutationMappingApiData;
import org.eclipse.winery.repository.rest.resources.apiData.RelationMappingApiData;
import org.eclipse.winery.repository.rest.resources.servicetemplates.topologytemplates.PrmTemplateResource;
import org.eclipse.winery.repository.rest.resources.servicetemplates.topologytemplates.TopologyTemplateResource;

public class TopologyFragmentRefinementModelResource extends AbstractRefinementModelResource {

    public TopologyFragmentRefinementModelResource(DefinitionsChildId id) {
        super(id);
    }

    @Override
    protected OTPatternRefinementModel createNewElement() {
        return new OTPatternRefinementModel(new OTPatternRefinementModel.Builder());
    }

    public OTTopologyFragmentRefinementModel getTRefinementModel() {
        return (OTTopologyFragmentRefinementModel) this.getElement();
    }

    @Path("refinementstructure")
    public TopologyTemplateResource getRefinementTopologyResource() {
        return new TopologyTemplateResource(this, this.getTRefinementModel().getRefinementTopology(), REFINEMENT_TOPOLOGY);
    }

    @Path("grafikprmmodelling")
    public TopologyTemplateResource getPrmModelling() {
        return new PrmTemplateResource(this, this.getTRefinementModel(), GRAFIC_PRM_MODEL);
    }

    //TODO

    @GET
    @Path("prmmappingtypes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TRelationshipType> getPrmMappingTypes() {
        List<TRelationshipType> relationshipTypesForPrmMappingTypes = new ArrayList<>();
        List<String> mappingTypes = new
            ArrayList<>(Arrays.asList("PermutationMapping", "RelationshipMapping", "DeploymentArtifactMapping"));

        for (String mappingType : mappingTypes) {
            TRelationshipType relType = new TRelationshipType(new
                TRelationshipType.Builder(mappingType));
            relType.setTargetNamespace("http://opentosca.org/prmMappingTypes");
            relationshipTypesForPrmMappingTypes.add(relType);
        }
        return relationshipTypesForPrmMappingTypes;
    }

    @PUT
    @Path("graphicPrmTopology")
    @Consumes(MediaType.APPLICATION_JSON)
    public TopologyTemplateResource savePrmMappingTopology(TTopologyTemplate topologyTemplate) {
        for (TRelationshipTemplate relTemplate : topologyTemplate.getRelationshipTemplates()) {
            if (relTemplate.getType().getLocalPart().startsWith("PermutationMapping")) {
                PrmPermutationMappingApiData newPermutationMapping = new PrmPermutationMappingApiData();
                newPermutationMapping.id = relTemplate.getId().substring(relTemplate.getId().indexOf("_") + 1);
                newPermutationMapping.detectorElement = relTemplate.getSourceElement().getRef().getId();
                newPermutationMapping.refinementElement = relTemplate.getTargetElement().getRef().getId();
                this.getPermutationMappings().addPermutationMapping(newPermutationMapping);
            }
            if (relTemplate.getType().getLocalPart().startsWith("RelationshipMapping")) {
                RelationMappingApiData newRelationshipMapping = new RelationMappingApiData();
                newRelationshipMapping.id = relTemplate.getId().substring(relTemplate.getId().indexOf("_") + 1);
                newRelationshipMapping.detectorElement = relTemplate.getSourceElement().getRef().getId();
                newRelationshipMapping.refinementElement = relTemplate.getTargetElement().getRef().getId();
                newRelationshipMapping.direction = null;
                newRelationshipMapping.relationType = null;
                newRelationshipMapping.validSourceOrTarget = null;
                this.getRelationMappings().addRelationMappingFromApi(newRelationshipMapping);
            }
            if (relTemplate.getType().getLocalPart().startsWith("StayMapping")) {

            }
            if (relTemplate.getType().getLocalPart().startsWith("AttributeMapping")) {

            }
            if (relTemplate.getType().getLocalPart().startsWith("DeploymentArtifactMapping")) {
                PrmDeploymentArtifactMappingApiData newDeploymentArtifactMapping = new PrmDeploymentArtifactMappingApiData();
                newDeploymentArtifactMapping.id = relTemplate.getId().substring(relTemplate.getId().indexOf("_") + 1);
                newDeploymentArtifactMapping.detectorElement = relTemplate.getSourceElement().getRef().getId();
                newDeploymentArtifactMapping.refinementElement = relTemplate.getTargetElement().getRef().getId();
                newDeploymentArtifactMapping.artifactType = null;
                this.getDeploymentArtifactMappings().addDeploymentArtifactMapping(newDeploymentArtifactMapping);
            }
        }
        return new PrmTemplateResource(this, this.getTRefinementModel(), GRAFIC_PRM_MODEL);
    }

    @Path("attributemappings")
    public AttributeMappingsResource getPropertyMappings() {
        List<OTAttributeMapping> propertyMappings = this.getTRefinementModel().getAttributeMappings();

        if (Objects.isNull(propertyMappings)) {
            propertyMappings = new ArrayList<>();
            this.getTRefinementModel().setAttributeMappings(propertyMappings);
        }

        return new AttributeMappingsResource(this, propertyMappings);
    }

    @Path("staymappings")
    public StayMappingsResource getStayMappings() {
        List<OTStayMapping> stayMappings = this.getTRefinementModel().getStayMappings();

        if (Objects.isNull(stayMappings)) {
            stayMappings = new ArrayList<>();
            this.getTRefinementModel().setStayMappings(stayMappings);
        }

        return new StayMappingsResource(this, stayMappings);
    }

    @Path("deploymentartifactmappings")
    public DeploymentArtifactMappingsResource getDeploymentArtifactMappings() {
        List<OTDeploymentArtifactMapping> artifactMappings = this.getTRefinementModel().getDeploymentArtifactMappings();

        if (Objects.isNull(artifactMappings)) {
            artifactMappings = new ArrayList<>();
            this.getTRefinementModel().setDeploymentArtifactMappings(artifactMappings);
        }

        return new DeploymentArtifactMappingsResource(this, artifactMappings);
    }

    @Path("permutationmappings")
    public PermutationMappingsResource getPermutationMappings() {
        List<OTPermutationMapping> permutationMappings = this.getTRefinementModel().getPermutationMappings();

        if (Objects.isNull(permutationMappings)) {
            permutationMappings = new ArrayList<>();
            this.getTRefinementModel().setPermutationMappings(permutationMappings);
        }
        return new PermutationMappingsResource(this, permutationMappings);
    }

    @Path("generatePermutations")
    @POST
    public PermutationsResponse generatePermutations() {
        PermutationsResponse permutationsResponse = new PermutationsResponse();

        PermutationGenerator permutationGenerator = new PermutationGenerator();
        try {
            permutationsResponse.setPermutations(permutationGenerator.generatePermutations(this.getTRefinementModel()));
            permutationsResponse.setMutable(true);
        } catch (Exception e) {
            permutationsResponse.setError(permutationGenerator.getMutabilityErrorReason());
        }

        return permutationsResponse;
    }

    @Path("checkMutability")
    @GET
    public PermutationsResponse getMutability() {
        PermutationsResponse permutationsResponse = new PermutationsResponse();

        PermutationGenerator permutationGenerator = new PermutationGenerator();
        permutationsResponse.setMutable(permutationGenerator.checkMutability(this.getTRefinementModel()));
        permutationsResponse.setError(permutationGenerator.getMutabilityErrorReason());

        return permutationsResponse;
    }
}
