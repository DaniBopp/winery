/*******************************************************************************
 * Copyright (c) 2015-2017 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.bpmn2bpel.model.param;

import javax.xml.namespace.QName;

/**
 * Represents a topology parameter.<br> To initialize the fields nodeType and property accordingly, the expected value
 * parameter format is <code>$NodeTypeName.$PropertyName</code>, e.g. <code>UbuntuVM.ImageName</code>
 */
public class TopologyParameter extends Parameter {

    private QName nodeTemplateId;

    private String property;

    public QName getNodeTemplateId() {
        return nodeTemplateId;
    }

    /**
     * Set the node template id
     *
     * @param name - the node template id with template prefix.
     */
    public void setNodeTemplateId(QName name) {
        this.nodeTemplateId = name;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String name) {
        this.property = name;
    }

    @Override
    public ParamType getType() {
        return ParamType.TOPOLOGY;
    }

    @Override
    public void setValue(String value) {
        /*
         * Decompose String into fully qualified NodeTemplate name and property
         * name, e.g. value "{http://example.com}UbuntuVM.ImageID" will be
         * decomposed into {http://example.com}UbuntuVM and ImageId
         */
        int idx = value.lastIndexOf(".");

        if (idx == -1) {
            throw new RuntimeException(TopologyParameter.class + " expects value in format '$QualifiedNodeTypeName.$PropertyName' but invalid value " + value + " was provided.");
        }

        String nodeTemplateName = value.substring(0, idx);
        String properyName = value.substring(idx + 1);

        setNodeTemplateId(QName.valueOf(nodeTemplateName));
        setProperty(properyName);
        super.setValue(value);
    }
}
