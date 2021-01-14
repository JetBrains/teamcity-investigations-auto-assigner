<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="authz" tagdir="/WEB-INF/tags/authz" %>
<%--
  ~ Copyright 2000-2021 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<style type="text/css">
  <%--@elvariable id="myCssPath" type="java.lang.String"--%>
  @import "${myCssPath}";
</style>
<div class="investigations-auto-assigner-results">

  <%--@elvariable id="buildId" type="java.lang.String"--%>
  <%--@elvariable id="isFilteredDescription" type="java.lang.Boolean"--%>
  <%--@elvariable id="projectId" type="java.lang.String"--%>
  <%--@elvariable id="testGroupId" type="java.lang.String"--%>
  <%--@elvariable id="userId" type="java.lang.String"--%>
  <%--@elvariable id="userName" type="java.lang.String"--%>
  <%--@elvariable id="shownDescription" type="java.lang.String"--%>
  <%--@elvariable id="investigationDescription" type="java.lang.String"--%>
  <%--@elvariable id="test" type="jetbrains.buildServer.serverSide.STest"--%>
  <c:if test="${not empty userId and not isFilteredDescription}">
    <c:set var="autoassignerComment">
      Investigation was assigned to ${userName} who ${investigationDescription}.
    </c:set>
    <c:set var="escapedComment">
      <bs:escapeForJs text="${autoassignerComment}" forHTMLAttribute="${true}"/>
    </c:set>
    <div>
      <authz:authorize projectId="${projectId}" allPermissions="ASSIGN_INVESTIGATION">
        <jsp:attribute name="ifAccessGranted">
          <span class="btn-group investigations-auto-assigner-btn-group">
            <button class="btn btn_mini action investigations-auto-assigner-btn" type="button"
                    onclick="return BS.AutoAssignerFeature.assignInvestigationOneClick(${userId}, '${test.testNameId}', '${buildId}', '${escapedComment}');"
                    title="Assign investigation">Assign investigation to <c:out value='${userName}'/></button><button
                class="btn btn_mini btn_append investigations-auto-assigner-btn-append" type="button"
                onclick="BS.AutoAssignerFeature.assignInvestigationManually('${test.testNameId}', '${buildId}', '${test.projectExternalId}', '${escapedComment}', ${userId});"
                title="Custom investigation assignment">...</button>
          </span>
        </jsp:attribute>
      </authz:authorize>
      <div class="investigations-auto-assigner-description">
        <c:out value='${userName}'/> <c:out value='${shownDescription}'/>.
      </div>
      <div id="empty-div"></div>
    </div>
  </c:if>
  <c:if test="${not empty userId and isFilteredDescription}">
    ${investigationDescription}.
  </c:if>

</div>