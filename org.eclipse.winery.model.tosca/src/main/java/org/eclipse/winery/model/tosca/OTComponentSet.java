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

package org.eclipse.winery.model.tosca;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "otComponentSet")
public class OTComponentSet {

    @XmlElement(name = "ComponentSet")
    @XmlList
    protected List<String> componentSet;

    public List<String> getComponentSet() {
        return componentSet;
    }

    public void setComponentSet(List<String> componentSet) {
        this.componentSet = componentSet;
    }
    
    public static OTComponentSet of(List<String> componentSet) {
        OTComponentSet c = new OTComponentSet();
        c.setComponentSet(componentSet);
        return c;
    }
}
