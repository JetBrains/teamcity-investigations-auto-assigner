<%-- Referenced from jetbrains.buildServer.investigationsAutoAssigner.representation.TestDetailsExtension --%>
<%@ include file="/include.jsp" %>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>

<%--@elvariable id="loadedTestRun" type="jetbrains.buildServer.serverSide.STestRun"--%>
<c:set var="buildId" value="${not empty loadedTestRun ? loadedTestRun.buildId : 0}"/>
<c:set var="testId" value="${not empty loadedTestRun ? loadedTestRun.testRunId : 0}"/>
<c:set var="autoAssignerBlockId">iaa_${util:uniqueId()}</c:set>

<div id="div_${autoAssignerBlockId}"></div>

<script type="text/javascript">
  BS.AutoAssignerFeature = BS.AutoAssignerFeature || {};
  BS.AutoAssignerFeature.assignInvestigationManually =
    function (testNameId, buildId, projectExternalId, escapedComment, userId) {
      var args = {
        comment: escapedComment,
        responsibilityRemovalMethod: 0,
        investigatorId: userId
      };

      BS.ajaxRequest('autoAssignerStatisticsReporter.html', {method: 'get'});
      return BS.BulkInvestigateMuteTestDialog.showForTest(testNameId, buildId, null, projectExternalId, false, args);
    };

  BS.AutoAssignerFeature.assignInvestigationOneClick =
    function (userId, testNameId, buildId, description) {
      return BS.ajaxUpdater('empty-div', 'assignInvestigation.html',
        {
          method: 'put',
          parameters: {
            userId : userId,
            testNameId: testNameId,
            buildId: buildId,
            description: description
          },
          evalScripts: true,
          onComplete: BS.reload(true)
        });
    };

  (function () {
    var divWithData = $('div_${autoAssignerBlockId}');
    BS.ajaxUpdater(divWithData, "autoAssignerController.html?buildId=" + ${buildId} +"&testId=" + ${testId}, {
      evalScripts: true
    });
  })();
</script>