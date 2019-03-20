<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="authz" tagdir="/WEB-INF/tags/authz" %>
<style type="text/css">
  <%--@elvariable id="myCssPath" type="java.lang.String"--%>
  @import "${myCssPath}";
</style>
<div class="investigations-auto-assigner-results">

  <%--@elvariable id="buildId" type="java.lang.String"--%>
  <%--@elvariable id="projectId" type="java.lang.String"--%>
  <%--@elvariable id="testGroupId" type="java.lang.String"--%>
  <%--@elvariable id="userId" type="java.lang.String"--%>
  <%--@elvariable id="userName" type="java.lang.String"--%>
  <%--@elvariable id="shownDescription" type="java.lang.String"--%>
  <%--@elvariable id="investigationDescription" type="java.lang.String"--%>
  <%--@elvariable id="test" type="jetbrains.buildServer.serverSide.STest"--%>
  <c:if test="${not empty userId}">
    <c:set var="autoassignerComment">
      Investigation was assigned to ${userName} who ${investigationDescription}.
    </c:set>
    <c:set var="escapedComment">
      '<bs:escapeForJs text="${autoassignerComment}" forHTMLAttribute="${true}"/>'
    </c:set>
    <div>
      <authz:authorize projectId="${projectId}" allPermissions="ASSIGN_INVESTIGATION">
        <jsp:attribute name="ifAccessGranted">
          <span class="btn-group investigations-auto-assigner-btn-group">
            <button class="btn btn_mini action investigations-auto-assigner-btn" type="button"
                    onclick="return BS.AutoAssignerFeature.assignInvestigationOneClick(${userId}, '${test.testNameId}', '${buildId}', ${escapedComment});"
                    title="Assign investigation">Assign investigation to ${userName}</button><button
                class="btn btn_mini btn_append investigations-auto-assigner-btn-append" type="button"
                onclick="BS.AutoAssignerFeature.assignInvestigationManually('${test.testNameId}', '${buildId}', '${test.projectExternalId}', ${escapedComment}, ${userId});"
                title="Custom investigation assignment">...</button>
          </span>
        </jsp:attribute>
      </authz:authorize>
      <div class="investigations-auto-assigner-description">
        <bs:out value='${userName}'/> ${shownDescription}.
      </div>
      <div id="empty-div"></div>
    </div>
  </c:if>
</div>