<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ page import="jetbrains.buildServer.iaa.common.Constants" %>
<jsp:useBean id="buildForm" type="jetbrains.buildServer.controllers.admin.projects.BuildTypeForm" scope="request"/>

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
    <em>This build feature automatically assigns investigations of build failures to users.
      <a class='helpIcon' onclick='BS.AutoAssignerFeature.showHomePage()' title='View help'>
        <i class='icon icon16 tc-icon_help_small'></i>
      </a>
    </em>
  </td>
</tr>
<tr>
  <th>
    <label for="<%= Constants.DEFAULT_RESPONSIBLE%>">Default investigator username:</label>
  </th>
  <td>
    <props:textProperty name="<%= Constants.DEFAULT_RESPONSIBLE%>" className="longField textProperty_max-width js_max-width"/>
    <span class="smallNote">Username of a user to whom an investigation is assigned if no other possible investigator is found</span>
  </td>
</tr>