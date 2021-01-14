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