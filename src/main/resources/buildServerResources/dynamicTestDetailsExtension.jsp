<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="ring" tagdir="/WEB-INF/tags/ring" %>
<%@ taglib prefix="authz" tagdir="/WEB-INF/tags/authz" %>


<style type="text/css">
  <%--@elvariable id="myCssPath" type="java.lang.String"--%>
  @import "${myCssPath}";
</style>
<div class="investigations-auto-assigner-results ${isSakuraUI ? '' : 'investigations-auto-assigner-results-classic'}">

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
          <c:choose>
            <c:when test="${param.isSakuraUI}">
              <ring:buttonGroup>
                <ring:button
                        onclick="return BS.AutoAssignerFeature.assignInvestigationOneClick(${userId}, '${test.testNameId}', '${buildId}', '${escapedComment}');"
                        title="Assign investigation">Assign investigation to <c:out value='${userName}'/></ring:button><ring:button
                    onclick="BS.AutoAssignerFeature.assignInvestigationManually('${test.testNameId}', '${buildId}', '${test.projectExternalId}', '${escapedComment}', ${userId});"
                    title="Custom investigation assignment">...</ring:button>
              </ring:buttonGroup>
            </c:when>
            <c:otherwise>
              <span class="btn-group investigations-auto-assigner-btn-group">
                <button class="btn btn_mini action investigations-auto-assigner-btn" type="button"
                        onclick="return BS.AutoAssignerFeature.assignInvestigationOneClick(${userId}, '${test.testNameId}', '${buildId}', '${escapedComment}');"
                        title="Assign investigation">Assign investigation to <c:out value='${userName}'/></button><button
                    class="btn btn_mini btn_append investigations-auto-assigner-btn-append" type="button"
                    onclick="BS.AutoAssignerFeature.assignInvestigationManually('${test.testNameId}', '${buildId}', '${test.projectExternalId}', '${escapedComment}', ${userId});"
                    title="Custom investigation assignment">...</button>
              </span>
            </c:otherwise>
          </c:choose>
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