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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.NodeTypeImplementationId;
import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.common.version.VersionUtils;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TDeploymentArtifact;
import org.eclipse.winery.model.tosca.TDeploymentArtifacts;
import org.eclipse.winery.model.tosca.TNodeTypeImplementation;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.repository.backend.IRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfContainmentPackager {

    private final static Logger logger = LoggerFactory.getLogger(SelfContainmentPackager.class);
    private final IRepository repository;
    private final List<SelfContainmentPlugin> selfContainmentPlugins;

    public SelfContainmentPackager(IRepository repository) {
        this.repository = repository;
        this.selfContainmentPlugins = Arrays.asList(
            new DockerPlugin(),
            new UbuntuVMPlugin(),
            new ScriptPlugin()
        );
    }

    // For now, this constructor is only intended for testing.
    public SelfContainmentPackager(IRepository repository, List<SelfContainmentPlugin> selfContainmentPlugins) {
        this.repository = repository;
        this.selfContainmentPlugins = selfContainmentPlugins;
    }

    public DefinitionsChildId createSelfContainedVersion(DefinitionsChildId entryId) throws IOException {
        ServiceTemplateId newServiceTemplateId = new ServiceTemplateId(VersionUtils.getSelfContainedVersion(entryId.getQName()));

        if (!repository.exists(newServiceTemplateId)) {
            repository.duplicate(entryId, newServiceTemplateId);
            TServiceTemplate serviceTemplate = repository.getElement(newServiceTemplateId);

            Collection<DefinitionsChildId> referencedElements = repository.getReferencedDefinitionsChildIds(newServiceTemplateId);

            for (DefinitionsChildId elementId : referencedElements) {
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
                                .filter(plugin -> plugin.canHandleNodeType(elementId.getQName(), repository))
                                .findFirst();

                            if (nodeTypeBasedPlugin.isPresent()) {
                                QName implQName = new QName(impl.getTargetNamespace(), impl.getIdFromIdOrNameField());
                                QName selfContainedVersion = VersionUtils.getSelfContainedVersion(implQName);
                                NodeTypeImplementationId nodeTypeImplementationId = new NodeTypeImplementationId(selfContainedVersion);
                                try {
                                    repository.duplicate(new NodeTypeImplementationId(implQName), nodeTypeImplementationId);

                                    TNodeTypeImplementation selfContained = this.repository.getElement(nodeTypeImplementationId);
                                    nodeTypeBasedPlugin.get()
                                        .downloadDependenciesBasedOnNodeType(selfContained, this.repository);

                                    repository.setElement(nodeTypeImplementationId, selfContained);
                                } catch (IOException e) {
                                    logger.error("While creating self-contained Node Type Implementation", e);
                                }
                            } else if (impl.getImplementationArtifacts() != null) {
                                createSelfContainedNodeTypeImplementation(impl);
                            }
                        }
                    }
                } else if (elementId instanceof ArtifactTemplateId) {
                    if (serviceTemplate.getTopologyTemplate() != null) {
                        TArtifactTemplate artifactTemplate = repository.getElement(elementId);
                        SelfContainmentPlugin.GeneratedArtifacts generatedArtifacts = this.downloadArtifacts(elementId.getQName(), artifactTemplate.getType());

                        if (generatedArtifacts != null && generatedArtifacts.selfContainedArtifactQName != null) {
                            // first, we need to identify the element that is referencing the artifact
                            serviceTemplate.getTopologyTemplate().getNodeTemplates().stream()
                                .filter(node -> node.getDeploymentArtifacts() != null)
                                .map(node -> node.getDeploymentArtifacts().getDeploymentArtifact())
                                .filter(daList -> daList.stream().anyMatch(da -> da.getArtifactRef() != null
                                    && da.getArtifactRef().equals(elementId.getQName())))
                                .flatMap(Collection::stream)
                                .forEach(da -> da.setArtifactRef(generatedArtifacts.selfContainedArtifactQName));
                        }
                    }
                }
            }
            repository.setElement(newServiceTemplateId, serviceTemplate);
        } else {
            logger.info("Self-contained version already exists! '{}'", newServiceTemplateId.getQName());
        }

        return newServiceTemplateId;
    }

    private void createSelfContainedNodeTypeImplementation(TNodeTypeImplementation impl) {
        if (impl.getImplementationArtifacts() != null) {
            List<SelfContainmentPlugin.GeneratedArtifacts> generatedArtifacts = impl.getImplementationArtifacts().getImplementationArtifact().stream()
                .filter(ia -> Objects.nonNull(ia.getArtifactRef()))
                .filter(ia -> VersionUtils.isSelfContained(ia.getArtifactRef()))
                .map(ia -> this.downloadArtifacts(ia.getArtifactRef(), ia.getArtifactType()))
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
                        if (implementation.getImplementationArtifacts() != null) {
                            implementation.getImplementationArtifacts().getImplementationArtifact()
                                .forEach(ia -> {
                                    if (generatedArtifact.artifactToReplaceQName.equals(ia.getArtifactRef())) {
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
                            for (QName artifactTemplate : generatedArtifact.deploymentArtifactsToAdd) {
                                TArtifactTemplate generatedAT = repository.getElement(new ArtifactTemplateId(artifactTemplate));

                                TDeploymentArtifact da = new TDeploymentArtifact();
                                da.setArtifactRef(artifactTemplate);
                                da.setName(artifactTemplate.getLocalPart());
                                da.setArtifactType(generatedAT.getType());

                                deploymentArtifacts.getDeploymentArtifact().add(da);
                            }
                        }
                    });
                } catch (IOException e) {
                    logger.error("Error while creating new self-contained NodeTypeImplementation of {}", impl, e);
                }
            }
        } else {
            logger.info("No pocessable IAs found in Node Type Implementation {}", impl.getQName());
        }
    }

    private SelfContainmentPlugin.GeneratedArtifacts downloadArtifacts(QName artifact, QName artifactType) {
        Optional<SelfContainmentPlugin> optionalPlugin = this.selfContainmentPlugins.stream()
            .filter(plugin -> plugin.canHandleArtifactType(artifactType, repository))
            .findFirst();

        if (optionalPlugin.isPresent()) {
            return optionalPlugin.get().downloadDependenciesBasedOnArtifact(artifact, repository);
        } else {
            logger.info(
                "Did not find self-containment plugin for ArtifactTemplate {} of type {}",
                artifact, artifactType
            );
        }
        return null;
    }
}
