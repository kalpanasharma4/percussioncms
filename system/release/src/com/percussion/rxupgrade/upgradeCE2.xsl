<?xml version="1.0" encoding="UTF-8"?>
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

<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
   <xsl:output method="xml" indent="no" omit-xml-declaration="no" encoding="UTF-8"/>
    <xsl:template match="@*|node()"> 
       <xsl:copy> 
          <xsl:apply-templates select="@*|node()" /> 
       </xsl:copy> 
    </xsl:template>
   <xsl:template match="SectionLinkList"><!-- removes SecionLinkList/PSXUrlRequest nodes referring only to URLs used by the relatedcontent shared group, only if the relatedcontent shared group was included a a SharedFieldIncludes/SharedFieldGroupName. -->
      	<xsl:choose>
	   	<xsl:when test="//PSXContentEditor/@enableRelatedContent='yes'"> <!-- if the relatedcontent shared group was included -->
	   	<SectionLinkList>
	   	<xsl:for-each select="PSXUrlRequest">
	   		<xsl:choose>
	   			<xsl:when test="./@name='RelatedLookupURL'"/> <!-- RelatedLookupURL, VariantListURL, and ContentSlotLookupURL are the URLs used by related content in the relatedcontent shared group.  If encountered, the PSXUrlRequest containing it gets ignored -->
	   			<xsl:when test="./@name='VariantListURL'"/>
	   			<xsl:when test="./@name='ContentSlotLookupURL'"/>
	   			 <xsl:otherwise> <!-- all other PSXUrlRequests get copied -->
	   			 	<xsl:copy>
	   			 		<xsl:for-each select="@*">
	   			 			<xsl:copy/>
	   			 		</xsl:for-each>
	   			 		<xsl:apply-templates/>
	   			 	</xsl:copy>
	   			 </xsl:otherwise>
	   		 </xsl:choose>
	   	</xsl:for-each>
	   	</SectionLinkList>
	   	</xsl:when>
	   	<xsl:otherwise>  <!-- if the relatedcontent shared group was not included, copy the entire SectionLinkList node -->
 			 <xsl:copy>
 			 	<xsl:for-each select="@*">
 			 		<xsl:copy/>
 			 	</xsl:for-each>
 			 	<xsl:apply-templates/>
 			 </xsl:copy>
 		</xsl:otherwise>
   	</xsl:choose>
   </xsl:template>
  					
<xsl:template match="PSXDisplayMapping"> <!-- remove PSXDisplayMapping overrides for the relatedcontent control if the relatedcontent shared field group was included in the CE-->
	<xsl:choose>
 		<xsl:when test="//PSXContentEditor/@enableRelatedContent='yes'"> 
   			<xsl:choose>
				<xsl:when test="./FieldRef='relatedcontent'"/>  <!-- ignore the relatedcontent PSXDisplayMapping -->
				<xsl:otherwise>  <!-- copy all others -->
 			 		<xsl:copy>
 			 			<xsl:for-each select="@*">
 			 				<xsl:copy/>
 			 			</xsl:for-each>
 			 			<xsl:apply-templates/>
 					 </xsl:copy>
 				</xsl:otherwise>
			</xsl:choose>
		</xsl:when>
		 <xsl:otherwise> 
			 <xsl:copy>
 			 	<xsl:for-each select="@*">
 			 		<xsl:copy/>
 			 	</xsl:for-each>
 			 	<xsl:apply-templates/>
 			 </xsl:copy>
 		</xsl:otherwise>
   	</xsl:choose>
</xsl:template>
</xsl:stylesheet>
