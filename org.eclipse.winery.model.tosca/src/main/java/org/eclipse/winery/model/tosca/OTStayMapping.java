/*******************************************************************************
 * Copyright (c) 2019-2020 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.model.tosca;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.eclipse.winery.model.tosca.visitor.Visitor;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "otStayMapping")
public class OTStayMapping extends OTPrmMapping {

    @XmlAttribute(name = "modelElementType")
    private OTPrmModelElementType modelElementType;

    @Override
    public boolean equals(Object obj) {
        return obj instanceof OTStayMapping
            && getId().equals(((OTStayMapping) obj).getId());
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public OTPrmModelElementType getModelElementType() {
        return modelElementType;
    }

    public void setModelElementType(OTPrmModelElementType modelElementType) {
        this.modelElementType = modelElementType;
    }
}
