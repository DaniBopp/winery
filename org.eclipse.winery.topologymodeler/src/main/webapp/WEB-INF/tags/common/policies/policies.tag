<%--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  ~ Copyright (c) 2013-2014 Contributors to the Eclipse Foundation
  ~
  ~ See the NOTICE file(s) distributed with this work for additional
  ~ information regarding copyright ownership.
  ~
  ~ This program and the accompanying materials are made available under the
  ~ terms of the Eclipse Public License 2.0 which is available at
  ~ http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
  ~ which is available at https://www.apache.org/licenses/LICENSE-2.0.
  ~
  ~ SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~--%>

<%-- This is mostly inspired by the reqscaps handling. Future work: Generalize the dialogs somehow to avoid copy'n'paste of code --%>

<%@tag description="Renders the list of policies of a node template" pageEncoding="UTF-8" %>

<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="wc" uri="http://www.eclipse.org/winery/functions" %>

<%@attribute name="list" required="true" type="java.util.List" %>
<%@attribute name="repositoryURL" required="true" type="java.lang.String" %>

<div class="policiesContainer">
    <div class="header">Policies</div>
    <div class="content">
        <div class="row">
            <div class="col-xs-4">Name</div>
            <div class="cell col-xs-4">Type</div>
            <div class="cell col-xs-4">Template</div>
        </div>
        <c:forEach var="item" items="${list}"
                   varStatus="loopStatus"><%-- this HTML has to be kept consistent with the tmpl-policy HTML at policydiag.tag --%>
            <div class="policy row ${loopStatus.index % 2 == 0 ? 'even' : 'odd'}">
                <div class="col-xs-4 policy name">${item.name}</div>

                <c:set var="clazz" value="<%=org.eclipse.winery.model.tosca.TPolicyType.class%>"/>
                <div class="col-xs-4 policy type">${wc:qname2href(repositoryURL, clazz, item.policyType)}</div>
                <span class="type">${item.policyType}</span>

                <c:set var="clazz" value="<%=org.eclipse.winery.model.tosca.TPolicyTemplate.class%>"/>
                <div class="col-xs-4 policy template">${wc:qname2href(repositoryURL, clazz, item.policyRef)}</div>
                <span class="template">${item.policyRef}</span>

                <c:set var="clazz" value="<%=org.eclipse.winery.model.tosca.TPolicy.class%>"/>
                <textarea class="policy_xml">${wc:XMLAsString(clazz, item)}</textarea>
            </div>
        </c:forEach>
        <div class="addnewpolicy row" style="display:none;">
            <button class="btn btn-default center-block btn-xs"
                    onclick="showAddDiagForPolicy($(this).parent().parent().parent().parent());">Add new
            </button>
        </div>
    </div>
</div>
