<style type="text/css">
  <%--@elvariable id="myCssPath" type="java.lang.String"--%>
  @import "${myCssPath}";
</style>

<div class="investigations-auto-assigner-results">
  <%--@elvariable id="autoAssignedResponsibility" type="jetbrains.buildServer.iaa.common.Responsibility"--%>
  <c:if test="${not empty autoAssignedResponsibility}">
    <div>
      <strong>Investigation auto-assigner:</strong>
    </div>
    <div>
      Investigation was automatically assigned to <strong>${autoAssignedResponsibility.user.username}</strong> who ${autoAssignedResponsibility.description}.
    </div>
  </c:if>
</div>
