<%@ page import="com.percussion.content.ui.aa.actions.*" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="org.json.JSONObject" %>


<%--Generates the New Item Creation dialog box.--%>
<%--Gets the content types for the given slot, and fills the content types combobox.--%>
<%
   PSAAClientActionFactory factory = PSAAClientActionFactory.getInstance();
   IPSAAClientAction action = factory.getAction("GetAllowedContentTypeForSlot");
   String objId = request.getParameter(IPSAAClientAction.OBJECT_ID_PARAM);
   Map params = new HashMap(1);
   params.put(IPSAAClientAction.OBJECT_ID_PARAM, objId);
   PSActionResponse aresponse = action.execute(params);
   String rawObj = aresponse.getResponseData();
   JSONArray temps = new JSONArray(rawObj);   
   
%>
<style>
      table.ps_content_browse_viewtable {
         font-family:Lucida Grande, Verdana;
         font-size:1.0em;
         width:100%;
         border:0px solid #ccc;
         border-collapse:collapse;
         cursor:default;
      }
   .box {
      display: block;
      text-align: center;
   }
   .box .dojoButton {
      float: right;
      margin-right: 10px;
   }
</style>
<table>
   <tr>
      <td>
      <div>Title:&nbsp;&nbsp;<input class="PsAaTextInput" type="text" size="75" id="ps.createitem.itemTitle"/>
      </div>
      </td>
   </tr>
   <tr>
      <td>
      <div>Type:&nbsp;<select dojoType="Select" style="width: 200px;" autocomplete="false" readonly="true" name="ps.createitem.contentType" id="ps.createitem.contentType">
              <%
                 int len = temps.length();
                 for (int i = 0; i < len; i++)
                 {
                   JSONObject jobj = temps.getJSONObject(i);
                   String ctypeid = jobj.get("contenttypeid").toString();
                   ctypeid = ctypeid.substring(2, ctypeid.length() - 2);
                   String name = jobj.get("name").toString();
                   name = name.substring(2, name.length() - 2);
              %>
              <option value='<%= ctypeid %>'><%= name %></option>
              <%
                 }
              %>
            
         </select>
      </div>
      
      </td>
   </tr>
   <tr>
      <td>Template:
      </td>
   </tr>
   <tr>
      <td style="width:100%;align:center;border: 1px solid #6290D2;margin-top:10px"><div style="height:10px;">&nbsp;</div><div dojoType="PSImageGallery" style="margin-top:10px" id="ps.createitem.templateGallery"/>
      </td>
   </tr>
   <tr>
      <td>
            Folder:&nbsp;<input class="PsAaTextInput" type="text" size="75" id="ps.createitem.folderPath" readonly="true"/></td>
      </td>
   </tr>
   <tr>
      <td>
         <div class="box">
            <button dojoType="ps:PSButton" id="ps.createitem.wgtButtonCancel">Cancel</button>
            <button dojoType="ps:PSButton" id="ps.createitem.wgtButtonSelect">Create</button>
         </div>
      </td>
   </tr>
</table>
