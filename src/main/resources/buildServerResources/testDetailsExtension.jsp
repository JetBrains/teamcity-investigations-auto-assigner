<%-- Referenced from jetbrains.buildServer.iaa.representation.TestDetailsExtension --%>
<%@ include file="/include.jsp" %>

<style type="text/css">
  <%--@elvariable id="myCssPath" type="java.lang.String"--%>
  @import "${myCssPath}";
</style>


<%--@elvariable id="responsibility" type="jetbrains.buildServer.iaa.common.Responsibility"--%>
<div class="investigations-auto-assigner-results">
  <c:if test="${not empty responsibility}">
    <div>
      <strong>Investigation auto-assigner:</strong>
    </div>
    <div>
      <strong>
        ${responsibility.user.username}
      </strong>
      ${responsibility.presentableDescription}
    </div>
  </c:if>
</div>
