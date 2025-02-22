<?xml version="1.0" encoding="UTF-8"?>


<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!-- 
	This stylesheet is used to add sprinta to the JdbcDriverConfigs in the configuration file 	
	"rxconfig/Server/config.xml".  Uses  the value of the Rhythmyx server
	type in the Installshield panel whose bean id is "RxServerTypePanelBean". The contents
	of this stylesheet is then resolved. During resolution "$W(RxServerTypePanelBean.serverType)"
	changes to the server type chosen by the user. 
	-->
	<xsl:template match="/">
		<xsl:apply-templates select="." mode="copy"/>
	</xsl:template>
	<!-- copy any attribute or template -->
	<xsl:template match="@*|*" mode="copy">
		<xsl:copy>
			<xsl:apply-templates select="@*" mode="copy"/>
			<xsl:apply-templates mode="copy"/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="JdbcDriverConfigs" mode="copy">
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates mode="copy"/>
			<xsl:if test="not(PSXJdbcDriverConfig/@driverName='inetdae7')">
				<PSXJdbcDriverConfig className="com.inet.tds.TdsDriver" containerTypeMapping="MS SQLSERVER2000" driverName="inetdae7"/>
			</xsl:if>
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
