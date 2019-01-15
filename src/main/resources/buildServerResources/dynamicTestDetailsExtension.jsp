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
    <c:set var="optionalArgs">
      {
      comment: ${escapedComment},
      responsibilityRemovalMethod: 0,
      investigatorId: ${userId}
      }
    </c:set>
    <div>
      <authz:authorize projectId="${projectId}" allPermissions="ASSIGN_INVESTIGATION">
        <jsp:attribute name="ifAccessGranted">
          <span class="btn-group investigations-auto-assigner-btn-group">
            <button
                class="btn btn_mini action investigations-auto-assigner-btn" type="button"
                onclick="BS.ajaxUpdater('empty-div', 'autoAssignerStatisticsReporter.html',
                    {
                    method: 'get',
                    evalScripts: true,
                    onComplete: BS.reload(false)
                    }); return BS.BulkInvestigateMuteTestDialog.showForTest('${test.testNameId}', '${buildId}', null, '${test.projectExternalId}', false, ${optionalArgs});"
                title="Custom investigation assignment">Assign investigation to ${userName}...</button>
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
