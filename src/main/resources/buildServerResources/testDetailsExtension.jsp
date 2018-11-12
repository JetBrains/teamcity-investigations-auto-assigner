<%-- Referenced from jetbrains.buildServer.iaa.representation.TestDetailsExtension --%>
<%@ include file="/include.jsp" %>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>

<%--@elvariable id="loadedTestRun" type="jetbrains.buildServer.serverSide.STestRun"--%>
<c:set var="buildId" value="${not empty loadedTestRun ? loadedTestRun.buildId : 0}"/>
<c:set var="testId" value="${not empty loadedTestRun ? loadedTestRun.testRunId : 0}"/>
<c:set var="autoAssignerBlockId">iaa_${util:uniqueId()}</c:set>

<div id="div_${autoAssignerBlockId}"></div>

<script type="text/javascript">
  (function () {
    var divWithData = $('div_${autoAssignerBlockId}');
    BS.ajaxUpdater(divWithData, "autoAssignerController.html?buildId=" + ${buildId} + "&testId=" + ${testId}, {
      evalScripts: true
    });
  })();
</script>