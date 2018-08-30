<%-- Referenced from jetbrains.buildServer.iaa.representation.TestDetailsExtension --%>
<%@ include file="/include.jsp" %>

<style type="text/css">
  <%--@elvariable id="teamcityPluginResourcesPath" type="java.lang.String"--%>
  @import "${teamcityPluginResourcesPath}testDetailsExtension.css";
</style>


<%--@elvariable id="responsibility" type="jetbrains.buildServer.iaa.common.Responsibility"--%>
<div class="investigations-auto-assigner-results">
  <c:if test="${not empty responsibility}">
    <div>
      <c:out value="${responsibility.description}"/>
    </div>
  </c:if>
</div>
