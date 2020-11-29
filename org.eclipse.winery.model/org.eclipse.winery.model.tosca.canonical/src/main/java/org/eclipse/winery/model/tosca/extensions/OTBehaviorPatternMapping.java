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

package org.eclipse.winery.model.tosca.extensions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.visitor.Visitor;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import io.github.adr.embedded.ADR;
import org.eclipse.jdt.annotation.NonNull;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "otBehaviorPatternMapping")
public class OTBehaviorPatternMapping extends OTPrmMapping {

    @JsonIdentityReference(alwaysAsId = true)
    @XmlAttribute(name = "behaviorPattern", required = true)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    @NonNull
    private TPolicy behaviorPattern;

    @XmlAttribute(name = "refinementProperty", required = true)
    @NonNull
    private String refinementProperty;

    @Deprecated // used for XML deserialization of API request content
    public OTBehaviorPatternMapping() {
    }

    public OTBehaviorPatternMapping(Builder builder) {
        super(builder);
        this.behaviorPattern = builder.behaviorPattern;
        this.refinementProperty = builder.refinementProperty;
    }

    public TPolicy getBehaviorPattern() {
        return behaviorPattern;
    }

    public void setBehaviorPattern(TPolicy behaviorPattern) {
        this.behaviorPattern = behaviorPattern;
    }

    public String getRefinementProperty() {
        return refinementProperty;
    }

    public void setRefinementProperty(String refinementProperty) {
        this.refinementProperty = refinementProperty;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof OTBehaviorPatternMapping
            && getId().equals(((OTBehaviorPatternMapping) obj).getId());
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public static class Builder extends OTPrmMapping.Builder<Builder> {

        private TPolicy behaviorPattern;
        private String refinementProperty;

        public Builder() {
            super();
        }

        public Builder(String id) {
            super(id);
        }

        public OTBehaviorPatternMapping.Builder setBehaviorPattern(TPolicy behaviorPattern) {
            this.behaviorPattern = behaviorPattern;
            return self();
        }

        public OTBehaviorPatternMapping.Builder setRefinementProperty(String refinementProperty) {
            this.refinementProperty = refinementProperty;
            return self();
        }

        public OTBehaviorPatternMapping build() {
            return new OTBehaviorPatternMapping(this);
        }

        @Override
        public @ADR(11) OTBehaviorPatternMapping.Builder self() {
            return this;
        }
    }
}
