<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ page import="jetbrains.buildServer.investigationsAutoAssigner.common.Constants" %>
<jsp:useBean id="buildForm" type="jetbrains.buildServer.controllers.admin.projects.BuildTypeForm" scope="request"/>

<script type="text/javascript">
  BS.AutoAssignerFeature = BS.AutoAssignerFeature || {};

  BS.AutoAssignerFeature.showHomePage = function () {
    var winSize = BS.Util.windowSize();
    BS.Util.popupWindow('https://confluence.jetbrains.com/display/TCD18/Investigations+Auto+Assigner',
      'Investigations Auto Assigner', {width: 0.9 * winSize[0], height: 0.9 * winSize[1]});
    BS.stopPropagation(event);
  };
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
<tr class="advancedSetting">
  <th>
    <label for="<%= Constants.SHOULD_DELAY_ASSIGNMENTS%>">Assign strategy: </label>
  </th>
  <td>
    <props:selectProperty name="${Constants.SHOULD_DELAY_ASSIGNMENTS}">
      <props:option value="false">Immediate assignment</props:option>
      <props:option value="true">Delayed assignment</props:option>
    </props:selectProperty>
    <span class="smallNote">Strategy of investigations assignment. Immediate assignment - assigns investigations
      right after they were found. Delayed assignment - assigns investigations after the problem appears for second time.</span>
  </td>
</tr>

<th>
    <label for="<%= Constants.DEFAULT_RESPONSIBLE%>">Default assignee:</label>
  </th>
  <td>
    <props:textProperty name="<%= Constants.DEFAULT_RESPONSIBLE%>" className="longField textProperty_max-width js_max-width"/>
    <span class="smallNote">Username of a user to assign the investigation to if no other assignee can be found.</span>
  </td>
</tr>
<tr>
  <th>
    <label for="<%= Constants.USERS_TO_IGNORE%>">Users to ignore:</label>
  </th>
  <td>
    <props:multilineProperty name="<%= Constants.USERS_TO_IGNORE%>" cols="58" rows="6" linkTitle="Edit users to ignore"
                             expanded="true" className="longField"/>
    <span class="smallNote">The newline-separated list of usernames to exclude from investigations auto-assignment.</span>
  </td>
</tr>
<tr class="advancedSetting">
  <th>
    <label>Build problems to ignore:</label>
  </th>
  <td>
    <props:checkboxProperty name="<%= Constants.SHOULD_IGNORE_COMPILATION_PROBLEMS%>"/>
      <label for="<%= Constants.SHOULD_IGNORE_COMPILATION_PROBLEMS%>">Compilation error build problems</label>
      <br/>
    <props:checkboxProperty name="<%=Constants.SHOULD_IGNORE_EXITCODE_PROBLEMS%>"/>
      <label for="<%=Constants.SHOULD_IGNORE_EXITCODE_PROBLEMS%>">Exit code build problems</label>
      <span class="smallNote">Type of build problems to ignore in investigations auto-assignment.</span>
</tr>