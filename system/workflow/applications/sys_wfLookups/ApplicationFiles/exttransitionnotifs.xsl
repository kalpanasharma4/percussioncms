<?xml version='1.0' encoding='UTF-8'?>
<!--
  ~     Percussion CMS
  ~     Copyright (C) 1999-2020 Percussion Software, Inc.
  ~
  ~     This program is free software: you can redistribute it and/or modify
  ~     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  ~
  ~     This program is distributed in the hope that it will be useful,
  ~     but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~     GNU Affero General Public License for more details.
  ~
  ~     Mailing Address:
  ~
  ~      Percussion Software, Inc.
  ~      PO Box 767
  ~      Burlington, MA 01803, USA
  ~      +01-781-438-9900
  ~      support@percussion.com
  ~      https://www.percusssion.com
  ~
  ~     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
  -->

<!DOCTYPE xsl:stylesheet [
	<!ENTITY % HTMLlat1 SYSTEM "/Rhythmyx/DTD/HTMLlat1x.ent">
		%HTMLlat1;
	<!ENTITY % HTMLsymbol SYSTEM "/Rhythmyx/DTD/HTMLsymbolx.ent">
		%HTMLsymbol;
	<!ENTITY % HTMLspecial SYSTEM "/Rhythmyx/DTD/HTMLspecialx.ent">
		%HTMLspecial;
]>

<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="psxi18n" xmlns:psxi18n="urn:www.percussion.com/i18n" >
  <xsl:template match="*" mode="exttransitionnotifs">
		<tr>
		  <td colspan="10" class="headercellfont" align="right">
			 <a href="{transitionnotifs/newlink}">New Transition Notification</a>
		  </td>
		</tr>
      <tr class="headercell2">
        <td height="21" width="5%" align="center" class="headercell2font">&nbsp;</td>
        <td height="21" width="25%" align="left" class="headercell2font">Subject (Notification ID)</td>
        <td height="21" width="20%" align="left" class="headercell2font">State Role Recipient Type</td>
        <td height="21" width="25%" align="left" class="headercell2font">Additional Recipient List</td>
        <td height="21" width="25%" align="left" class="headercell2font">CC List</td>
      </tr>
    <xsl:for-each select="transitionnotifs">
     <xsl:if test="not(notificationid = '')">
      <tr class="datacell1">
			<xsl:attribute name="class"> 
				<xsl:choose> 
					<xsl:when test="position() mod 2 = 1"> 
						<xsl:value-of select="'datacell1'"/> 
					</xsl:when> 
					<xsl:otherwise> 
						<xsl:value-of select="'datacell2'"/> 
					</xsl:otherwise> 
				</xsl:choose> 
			</xsl:attribute> 
        <td class="datacell1font" height="25" align="center">
            <a>
              <xsl:attribute name="href">javascript: delConfirm('<xsl:value-of select="deletelink"/>' );</xsl:attribute>
              <img src="../sys_resources/images/delete.gif" width="21" height="21" border="0"/>
            </a>
        </td>
        <td class="datacell1font" height="20" align="left">
            <a>
              <xsl:attribute name="href">
                <xsl:value-of select="link"/>
              </xsl:attribute>
					<xsl:apply-templates select="subject"/>&nbsp;(<xsl:apply-templates select="notificationid"/>)&nbsp;
             </a>
       </td>
        <td class="datacell1font" height="20" align="left">
			<xsl:variable name="reciptype"><xsl:value-of select="staterolerecipienttypes"/></xsl:variable>
			<xsl:apply-templates select="document(reciptypeurl)/*/item[value=$reciptype]/display"/>&nbsp;
        </td>
        <td class="datacell1font" height="20" align="left">
          <xsl:apply-templates select="additionalrecipientlist"/>
        </td>
        <td class="datacell1font" height="20" align="left">
          <xsl:apply-templates select="cclist"/>
        </td>
      </tr>
     </xsl:if>
	  <xsl:if test="count(.)=1 and notificationid = ''">
		<tr class="datacell1">
			<td align="center" colspan="5" class="datacellnoentriesfound">
				No entries found.&nbsp;
			</td>
      </tr>
	  </xsl:if>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
