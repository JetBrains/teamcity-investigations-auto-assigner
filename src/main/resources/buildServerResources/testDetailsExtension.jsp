<%@ page import="jetbrains.buildServer.web.util.WebUtil" %>

<%-- Referenced from jetbrains.buildServer.investigationsAutoAssigner.representation.TestDetailsExtension --%>
<%@ include file="/include.jsp" %>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>

<%--@elvariable id="buildId" type="java.lang.Long"--%>
<%--@elvariable id="testId" type="java.lang.Integer"--%>
<c:set var="autoAssignerBlockId">iaa_${util:uniqueId()}</c:set>
<c:set var="isSakuraUI"><%=WebUtil.sakuraUIOpened(request)%></c:set>

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

      BS.ajaxRequest(base_uri + '/autoAssignerStatisticsReporter.html', {method: 'get'});
      return BS.BulkInvestigateMuteTestDialog.showForTest(testNameId, buildId, null, projectExternalId, false, args);
    };

  BS.AutoAssignerFeature.assignInvestigationOneClick =
    function (userId, testNameId, buildId, description) {
      return BS.ajaxUpdater('empty-div', base_uri + '/assignInvestigation.html',
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
    BS.ajaxUpdater(divWithData, base_uri + "/autoAssignerController.html?buildId=" + ${buildId} +"&testId=" + ${testId} <c:if test="${isSakuraUI}">+ "&isSakuraUI=true"</c:if>, {
      evalScripts: true
    });
  })();
</script>