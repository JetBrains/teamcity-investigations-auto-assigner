<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<jsp:useBean id="buildForm" type="jetbrains.buildServer.controllers.admin.projects.BuildTypeForm" scope="request"/>
<jsp:useBean id="bean" class="jetbrains.buildServer.iaa.AutoAssignerBean"/>

<script type="text/javascript">
  BS.RunAsFeature = {
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
    <em>This build feature allows auto assigning investigations according to some rules
      <a class='helpIcon' onclick='BS.RunAsFeature.showHomePage()' title='View help'>
        <i class='icon icon16 tc-icon_help_small'></i>
      </a>
    </em>
  </td>
</tr>

<tr>
  <td>
    <label for="${bean.isEnabledKey}">Is Enabled:</label>
  </td>
  <td>
    <props:checkboxProperty name="${bean.isEnabledKey}" checked="false"/>
    <span class="smallNote">Enables the investigations auto assigner.</span>
  </td>
</tr>