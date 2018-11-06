<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<style type="text/css">
  <%--@elvariable id="myCssPath" type="java.lang.String"--%>
  @import "${myCssPath}";
</style>
<%--<c:set var="optionalArgs" value="${{comment: '1 2 3'}}"/>--%>
<%--<script type="text/javascript">--%>
<%--var optionalArgs = {comment: '1 2 3'};--%>
<%--</script>--%>

<c:set var="autoassignerComment">
  Investigation was assigned to ${autoAssignedResponsibility.user.username} who ${autoAssignedResponsibility.description}.
</c:set>
<c:set var="escapedComment">
  '<bs:escapeForJs text="${autoassignerComment}" forHTMLAttribute="${true}"/>'
</c:set>
<div class="investigations-auto-assigner-results">

  <%--@elvariable id="buildId" type="java.lang.String"--%>
  <%--@elvariable id="testGroupId" type="java.lang.String"--%>
  <%--@elvariable id="test" type="jetbrains.buildServer.serverSide.STest"--%>
  <%--@elvariable id="autoAssignedResponsibility" type="jetbrains.buildServer.iaa.common.Responsibility"--%>
  <c:if test="${not empty autoAssignedResponsibility}">
    <div>
      <strong>Investigation auto-assigner:</strong>
    </div>
    <div>
      <strong>${autoAssignedResponsibility.user.username}</strong> ${autoAssignedResponsibility.description}.
      <a href="#" title="Assign investigation..."
         onclick="return BS.BulkInvestigateMuteTestDialog.showForTest('${test.testNameId}', '${buildId}', null, '${test.projectExternalId}', false, {comment: ${escapedComment}});">Assign
        investigation...</a>
    </div>
  </c:if>
</div>
