<%@ page import="java.util.*,com.percussion.i18n.PSI18nUtils" pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://rhythmyx.percussion.com/components"
	prefix="rxcomp"%>


<%
   String locale = PSI18nUtils.getSystemLanguage();
   
   String idPrefix = request.getParameter("idPrefix");
   // Get the Rhythmyx root
   String rawUrl = request.getRequestURL().toString();
   if(rawUrl.startsWith("http://"))
      rawUrl = rawUrl.substring(7);
   else if(rawUrl.startsWith("https://"))
      rawUrl = rawUrl.substring(8);
   int index = rawUrl.indexOf("/");
   String rxRoot = rawUrl.substring(index, rawUrl.indexOf("/", index + 1));
%>

<table dojoType="ContentPane" align="left" cellpadding="1" cellspacing="0" border="0"  id="<%= idPrefix %>addressbarpanel" sizeMin="30" sizeShare="5">
	<tr><td width="100%">
	<table cellpadding="1" cellspacing="0" border="0" width="82%">
	<tr>
		<td align="right">
		 <span class="PsDojoLabelText">&nbsp;${rxcomp:i18ntext('jsp_addressbarpanel@path',locale)}:</span>
		</td>
		<td align="left">
			<input type="text" id="<%= idPrefix %>pathText" size="70"/>			
		</td>
		<td>
		<table cellpadding="1" cellspacing="" border="0" width="100%">
		<tr>
		<td align="center">
			<button style="border: 1px solid black;" dojoType="Button" id="<%= idPrefix %>refreshButton">
				<img src="<%= rxRoot %>/../sys_resources/images/aa/refresh16.gif" width="16" height="16" title="${rxcomp:i18ntext('jsp_addressbarpanel@refresh',locale)}" alt="${rxcomp:i18ntext('jsp_addressbarpanel@refresh',locale)}">
			</button>			
		</td>
		<td align="center">
			<button style="border: 1px solid black;" dojoType="Button" id="<%= idPrefix %>backButton" disabled="true">
				<img src="<%= rxRoot %>/../sys_resources/images/aa/back16.gif" width="16" height="16" title="${rxcomp:i18ntext('jsp_addressbarpanel@back',locale)}" alt="${rxcomp:i18ntext('jsp_addressbarpanel@back',locale)}">
			</button>
		</td>
		<td align="center">
			<button style="border: 1px solid black;" dojoType="Button" id="<%= idPrefix %>upButton">
				<img src="<%= rxRoot %>/../sys_resources/images/aa/up16.gif" width="16" height="16" title="${rxcomp:i18ntext('jsp_addressbarpanel@up',locale)}" alt="${rxcomp:i18ntext('jsp_addressbarpanel@up',locale)}">
			</button>
		</td>		
		</tr>
		</table>
		</td>
	</tr>
	</table>
	</td></tr>
</table>
