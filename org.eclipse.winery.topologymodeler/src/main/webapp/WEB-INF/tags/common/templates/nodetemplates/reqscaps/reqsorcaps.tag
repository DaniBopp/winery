<%--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  ~ Copyright (c) 2012-2014 Contributors to the Eclipse Foundation
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
<%@tag description="Renders the list of requirements or capabilties" pageEncoding="UTF-8" %>

<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="wc" uri="http://www.eclipse.org/winery/functions" %>
<%@taglib prefix="nt" tagdir="/WEB-INF/tags/common/templates/nodetemplates/reqscaps" %>
<%@taglib prefix="props" tagdir="/WEB-INF/tags/common/templates" %>

<%@attribute name="list" required="false" type="java.util.List" %>
<%@attribute name="headerLabel" required="true" description="Used for the heading" %>
<%@attribute name="cssClassPrefix" required="true" %>
<%@attribute name="TReqOrCapTypeClass" required="true" type="java.lang.Class" %>
<%@attribute name="repositoryURL" required="true" %>
<%@attribute name="typeURLFragment" required="true" description="requirementtypes|capabilitytypes" %>
<%@attribute name="shortName" required="true" description="Used for diag id, function name suffix, Req|Cap" %>
<%@attribute name="client" required="true" description="IWineryRepository"
             type="org.eclipse.winery.common.interfaces.IWineryRepository" %>
<%@attribute name="pathToImages" required="true"
             description="The path (URI path) to the image/ url, where xml.png is available. Has to end with '/'" %>

<div class="${cssClassPrefix}Container">
    <div class="header">${headerLabel}</div>
    <div class="content">
        <c:forEach var="item" items="${list}">
            <div class="reqorcap ${cssClassPrefix} row" id="${item.id}">
                <div class="col-xs-4 id reqorcap">${item.id}</div>
                <div class="col-xs-4 name reqorcap">${item.name}</div>
                <div class="col-xs-4 type reqorcap">${wc:qname2href(repositoryURL, TReqOrCapTypeClass, item.type)}</div>
                <c:set var="type" value="${wc:getType(client, item.type, TReqOrCapTypeClass)}"/>
                <props:properties
                    template="${item}"
                    propertiesDefinition="${type.propertiesDefinition}"
                    wpd="${wc:winerysPropertiesDefinition(type)}"
                    pathToImages="${pathToImages}"/>
            </div>
        </c:forEach>
        <div class="addnewreqorcap row" style="display:none;">
            <button class="btn btn-default btn-xs center-block"
                    onclick="showAddOrUpdateDiagFor${shortName}($(this).parent().parent().parent().parent().attr('id'));">
                Add new
            </button>
        </div>
    </div>
</div>

<%-- parameters: o.id, o.name, o.type, o.xml. Has to be consistent with above HTML --%>
<script type="text/x-tmpl" id="tmpl-${shortName}">
	<div class="reqorcap ${cssClassPrefix}" id="{%=o.id%}">
		<div class="col-xs-4 id reqorcap">{%=o.id%}</div>
		<div class="col-xs-4 name reqorcap">{%=o.name%}</div>
		<div class="col-xs-4 type reqorcap">{%#require("winery-support-common").qname2href("${repositoryURL}", "${typeURLFragment}", o.type)%}</div>
		<div id="toBeReplacedByProperties"></div>
	</div>

</script>
