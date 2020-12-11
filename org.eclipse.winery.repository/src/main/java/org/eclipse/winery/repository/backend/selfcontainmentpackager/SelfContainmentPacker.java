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

package org.eclipse.winery.repository.backend.selfcontainmentpackager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.NodeTypeImplementationId;
import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.common.version.VersionUtils;
import org.eclipse.winery.model.tosca.TDeploymentArtifact;
import org.eclipse.winery.model.tosca.TDeploymentArtifacts;
import org.eclipse.winery.model.tosca.TNodeTypeImplementation;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.converter.support.TopologyTemplateUtils;
import org.eclipse.winery.repository.exceptions.RepositoryCorruptException;
import org.eclipse.winery.repository.export.ExportedState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfContainmentPacker {

    private final static Logger logger = LoggerFactory.getLogger(SelfContainmentPacker.class);
    private final IRepository repository;
    private final List<SelfContainmentPlugin> selfContainmentPlugins;

    public SelfContainmentPacker(IRepository repository) {
        this.repository = repository;
        this.selfContainmentPlugins = Arrays.asList(
            new DockerPlugin(),
            new VirtualMachinePlugin(),
            new ScriptPlugin()
        );
    }

    public DefinitionsChildId createSelfContainedVersion(DefinitionsChildId entryId) throws IOException {
        ExportedState exportedStateTemp = new ExportedState();
        DefinitionsChildId loopId;
        SelfContainmentUtil packager = new SelfContainmentUtil();
        ServiceTemplateId newServiceTemplateId = new ServiceTemplateId(QName.valueOf(entryId.getQName() + "-self"));

        if (!repository.exists(newServiceTemplateId)) {
            repository.duplicate(entryId, newServiceTemplateId);
            entryId = newServiceTemplateId;

            Collection<DefinitionsChildId> referencedElements = repository.getReferencedDefinitionsChildIds(newServiceTemplateId);

            referencedElements.forEach(elementId -> {
                if (elementId instanceof NodeTypeId) {
                    Collection<NodeTypeImplementationId> nodeTypeImplementationIds = repository.getAllElementsReferencingGivenType(NodeTypeImplementationId.class, elementId.getQName());

                    if (nodeTypeImplementationIds.stream().noneMatch(VersionUtils::isSelfContained)) {
                        // self-contained element does not exist yet!
                        List<TNodeTypeImplementation> nodeTypeImplementations = nodeTypeImplementationIds
                            .stream()
                            .map(repository::getElement)
                            .filter(element -> element.getImplementationArtifacts() != null)
                            .collect(Collectors.toList());

                        for (TNodeTypeImplementation impl : nodeTypeImplementations) {
                            Optional<SelfContainmentPlugin> nodeTypeBasedPlugin = this.selfContainmentPlugins.stream()
                                .filter(plugin -> plugin.canHandleNodeType(elementId.getQName()))
                                .findFirst();

                            if (nodeTypeBasedPlugin.isPresent()) {
                                nodeTypeBasedPlugin.get().downloadDependencies(elementId.getQName(), impl, this.repository);
                            } else if (impl.getImplementationArtifacts() != null) {
                                List<SelfContainmentPlugin.GeneratedArtifacts> generatedArtifacts = impl.getImplementationArtifacts().getImplementationArtifact().stream()
                                    .filter(ia -> Objects.nonNull(ia.getArtifactRef()))
                                    .filter(ia -> VersionUtils.isSelfContained(ia.getArtifactRef()))
                                    .map(ia -> {
                                        QName artifact = ia.getArtifactRef();

                                        Optional<SelfContainmentPlugin> optionalPlugin = this.selfContainmentPlugins.stream()
                                            .filter(plugin -> plugin.canHandleArtifactType(ia.getArtifactType()))
                                            .findFirst();

                                        if (optionalPlugin.isPresent()) {
                                            return optionalPlugin.get().downloadDependencies(artifact, impl, repository);
                                        } else {
                                            logger.info(
                                                "Did not find self-containment plugin for ArtifactTemplate {} of type {}",
                                                artifact, ia.getArtifactType()
                                            );
                                        }
                                        return null;
                                    })
                                    .filter(Objects::nonNull)
                                    .filter(SelfContainmentPlugin.GeneratedArtifacts::containsNewElements)
                                    .collect(Collectors.toList());

                                if (!generatedArtifacts.isEmpty()) {
                                    QName selfContainedVersion = VersionUtils.getSelfContainedVersion(impl.getQName());
                                    NodeTypeImplementationId nodeTypeImplementationId = new NodeTypeImplementationId(selfContainedVersion);
                                    try {
                                        this.repository.duplicate(new NodeTypeImplementationId(impl.getQName()), nodeTypeImplementationId);
                                        TNodeTypeImplementation implementation = this.repository.getElement(nodeTypeImplementationId);

                                        generatedArtifacts.forEach(generatedArtifact -> {
                                            implementation.getImplementationArtifacts().getImplementationArtifact()
                                                .forEach(ia -> {
                                                    if (generatedArtifact.artifactTemplateToReplace.equals(ia.getArtifactRef())) {
                                                        ia.setArtifactRef(generatedArtifact.selfContainedArtifactQName);
                                                        ia.setArtifactType(generatedArtifact.selfContainedArtifactTemplate.getType());
                                                    }
                                                });
                                            TDeploymentArtifacts deploymentArtifacts = implementation.getDeploymentArtifacts();
                                            if (deploymentArtifacts == null) {
                                                deploymentArtifacts = new TDeploymentArtifacts();
                                            } else {
                                                deploymentArtifacts.getDeploymentArtifact()
                                                    .removeIf(da -> da.getArtifactRef() != null
                                                        && generatedArtifact.deploymentArtifactsToRemove.contains(da.getArtifactRef()));
                                            }
                                            for (Map.Entry<QName, QName> entry : generatedArtifact.deploymentArtifactsToAdd.entrySet()) {
                                                QName artifactTemplate = entry.getKey();
                                                QName artifactType = entry.getValue();
                                                TDeploymentArtifact da = new TDeploymentArtifact();
                                                da.setArtifactRef(artifactTemplate);
                                                da.setName(artifactTemplate.getLocalPart());
                                                da.setArtifactType(artifactType);
                                                deploymentArtifacts.getDeploymentArtifact().add(da);
                                            }
                                        });
                                    } catch (IOException e) {
                                        logger.error("Error while creating new self-contained NodeTypeImplementation of {}", impl, e);
                                    }
                                }
                            }
                        }
                    }
                } else if (elementId instanceof ArtifactTemplateId) {

                    // todo - somehow same stuff as starting from ia handling oder halt au id

                }
            });

            // EMRE:
            try {
                for (DefinitionsChildId nodeTypeId : referencedElements) {
                    loopId = nodeTypeId;
                    if (loopId instanceof NodeTypeId) {
                        do {
                            Collection<DefinitionsChildId> referencedDefinitionsChildIds = repository.getReferencedDefinitionsChildIds(loopId);

                            Map<String, Collection<DefinitionsChildId>> updatedIds = null;
                            updatedIds = packager.manageSelfContainedDefinitions(referencedDefinitionsChildIds, repository);

                            if (!updatedIds.isEmpty()) {
                                TNodeTypeImplementation nodeTypeImplementation = getNodeTypeImplementation(nodeTypeId.getQName(), repository);
                                SelfContainmentUtil.createNodeTypeImplSelf((NodeTypeId) nodeTypeId, nodeTypeImplementation, repository, updatedIds);
                            }

                            exportedStateTemp.flagAsExported(loopId);
                            exportedStateTemp.flagAsExportRequired(referencedDefinitionsChildIds);

                            loopId = exportedStateTemp.pop();
                        } while (loopId != null);
                    } else if (loopId instanceof ArtifactTemplateId) {
                        Collection<DefinitionsChildId> onlyArtifactList = new HashSet<>();
                        onlyArtifactList.add(loopId);
                        Map<String, Collection<DefinitionsChildId>> updatedArtifactIds = packager.manageSelfContainedDefinitions(onlyArtifactList, repository);

                        if (!updatedArtifactIds.isEmpty()) {
                            Collection<DefinitionsChildId> deploymentArtifacts = updatedArtifactIds.get("DeploymentArtifacts");
                            if (!deploymentArtifacts.isEmpty()) {
                                TopologyTemplateUtils.updateServiceTemplateWithResolvedDa(entryId, repository, loopId, deploymentArtifacts.iterator().next());
                            }
                        }
                    }
                }
            } catch (JAXBException | RepositoryCorruptException e) {
                e.printStackTrace();
            }
        }

        return newServiceTemplateId;
    }

    private TNodeTypeImplementation getNodeTypeImplementation(QName nodeType, IRepository repository) {
        return repository.getAllDefinitionsChildIds(NodeTypeImplementationId.class)
            .stream()
            .map(repository::getElement)
            .filter(entry -> entry.getNodeType().equals(nodeType))
            .findAny().orElse(null);
    }
}
