<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<jsp:useBean id="buildForm" type="jetbrains.buildServer.controllers.admin.projects.BuildTypeForm" scope="request"/>
<jsp:useBean id="bean" class="jetbrains.buildServer.iaa.AutoAssignerBean"/>

<script type="text/javascript">
  BS.AutoAssignerFeature = {
    showHomePage: function () {
      var winSize = BS.Util.windowSize();
      BS.Util.popupWindow('https://github.com/JetBrains/teamcity-investigations-auto-assigner/wiki', 'Investigations Auto Assigner',
        {width: 0.9 * winSize[0], height: 0.9 * winSize[1]});
      BS.stopPropagation(event);
    }
  }
</script>

<tr>
  <td colspan="2">
    <em>This build feature executes auto assigning investigations according to a set of rules.
      <a class='helpIcon' onclick='BS.AutoAssignerFeature.showHomePage()' title='View help'>
        <i class='icon icon16 tc-icon_help_small'></i>
      </a>
    </em>
  </td>
</tr>
<tr>
  <td>
    <label for="${bean.defaultResponsible}">Default Responsible:</label>
  </td>
  <td>
    <props:textProperty name="${bean.defaultResponsible}" maxlength="100"/>
  </td>
</tr>
