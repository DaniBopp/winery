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
package org.eclipse.winery.repository.rest.resources.servicetemplates.plans;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import org.eclipse.winery.model.tosca.TPlan;
import org.eclipse.winery.model.tosca.TPlan.PlanModelReference;
import org.eclipse.winery.repository.rest.datatypes.TypeWithShortName;
import org.eclipse.winery.repository.rest.resources.admin.types.PlanLanguagesManager;
import org.eclipse.winery.repository.rest.resources.admin.types.PlanTypesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

public class PlansResourceData {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlansResourceData.class);

    // data: [ [id, pre, name, type, lang]* ]
    private String embeddedPlansTableData;

    // data: [ [id, pre, name, type, lang, reference]* ]
    private String linkedPlansTableData;


    /**
     * Data object for the JSP
     *
     * @param plans the plans this resource manages
     */
    public PlansResourceData(List<TPlan> plans) {
        if (plans.isEmpty()) {
            this.embeddedPlansTableData = "[]";
            this.linkedPlansTableData = "[]";
            return;
        }
        JsonFactory jsonFactory = new JsonFactory();
        StringWriter embeddedPlansTableDataSW = new StringWriter();
        StringWriter linkedPlansTableDataSW = new StringWriter();
        try {
            JsonGenerator jGeneratorEmbedded = jsonFactory.createGenerator(embeddedPlansTableDataSW);
            JsonGenerator jGeneratorLinked = jsonFactory.createGenerator(linkedPlansTableDataSW);

            jGeneratorEmbedded.writeStartArray();
            jGeneratorLinked.writeStartArray();

            for (TPlan plan : plans) {
                String name = plan.getName();
                if (name == null) {
                    // name defaults to id
                    name = plan.getId();
                }
                String type = PlanTypesManager.INSTANCE.getShortName(plan.getPlanType());
                String language = PlanLanguagesManager.INSTANCE.getShortName(plan.getPlanLanguage());
                PlanModelReference planModelReference = plan.getPlanModelReference();
                String reference = planModelReference != null ? planModelReference.getReference() : null;
                JsonGenerator gen;
                boolean writeReference;
                if (reference == null) {
                    gen = jGeneratorEmbedded;
                    writeReference = false;
                } else if (reference.startsWith("../")) {
                    gen = jGeneratorEmbedded;
                    writeReference = false;
                } else {
                    gen = jGeneratorLinked;
                    writeReference = true;
                }

                gen.writeStartArray();
                gen.writeString(plan.getId());
                gen.writeString(""); // precondition
                gen.writeString(name);
                gen.writeString(type);
                gen.writeString(language);
                if (writeReference) {
                    gen.writeString(reference);
                }
                gen.writeEndArray();
            }

            jGeneratorEmbedded.writeEndArray();
            jGeneratorLinked.writeEndArray();

            jGeneratorEmbedded.close();
            embeddedPlansTableDataSW.close();
            jGeneratorLinked.close();
            linkedPlansTableDataSW.close();
        } catch (JsonGenerationException e) {
            PlansResourceData.LOGGER.error(e.getMessage(), e);
            this.embeddedPlansTableData = "[]";
            this.linkedPlansTableData = "[]";
            return;
        } catch (IOException e) {
            PlansResourceData.LOGGER.error("", e);
            this.embeddedPlansTableData = "[]";
            this.linkedPlansTableData = "[]";
            return;
        }
        this.embeddedPlansTableData = embeddedPlansTableDataSW.toString();
        this.linkedPlansTableData = linkedPlansTableDataSW.toString();
    }

    public String getEmbeddedPlansTableData() {
        return this.embeddedPlansTableData;
    }

    public String getLinkedPlansTableData() {
        return this.linkedPlansTableData;
    }

    public Collection<TypeWithShortName> getPlanTypes() {
        return PlanTypesManager.INSTANCE.getTypes();
    }

    public Collection<TypeWithShortName> getPlanLanguages() {
        return PlanLanguagesManager.INSTANCE.getTypes();
    }

}
