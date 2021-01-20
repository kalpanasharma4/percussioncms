<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE xsl:stylesheet [
	<!ENTITY % HTMLlat1 SYSTEM "/Rhythmyx/DTD/HTMLlat1x.ent">
		%HTMLlat1;
	<!ENTITY % HTMLsymbol SYSTEM "/Rhythmyx/DTD/HTMLsymbolx.ent">
		%HTMLsymbol;
	<!ENTITY % HTMLspecial SYSTEM "/Rhythmyx/DTD/HTMLspecialx.ent">
		%HTMLspecial;
]>

<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="psxi18n" xmlns:psxi18n="urn:www.percussion.com/i18n" >
  <xsl:variable name="this" select="/"/>
  <xsl:template match="/">
    <html>
      <head>
        <meta name="generator" content="Percussion XSpLit Version 3.0"/>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
        <meta name="GENERATOR" content="Microsoft Visual Studio 6.0"/>
        <title id="XSpLit"/>
      </head>

      <body>
        <table>
          <xsl:apply-templates select="*/variantid" mode="mode0"/>
          <xsl:apply-templates select="*/varianttype" mode="mode1"/>
          <xsl:apply-templates select="*/contenttypeid" mode="mode2"/>
          <xsl:apply-templates select="*/contenttype" mode="mode3"/>
        </table>

      </body>

    </html>

  </xsl:template>

  <xsl:template match="*">
    <xsl:choose>
      <xsl:when test="text()">
        <xsl:choose>
          <xsl:when test="@no-escaping">
            <xsl:value-of select="." disable-output-escaping="yes"/>
          </xsl:when>

          <xsl:otherwise>
            <xsl:value-of select="."/>
          </xsl:otherwise>

        </xsl:choose>

      </xsl:when>

      <xsl:otherwise>&nbsp;</xsl:otherwise>

    </xsl:choose>

    <xsl:if test="not(position()=last())">
      <br id="XSpLit"/>
    </xsl:if>

  </xsl:template>

  <xsl:template match="attribute::*">
    <xsl:value-of select="."/>
    <xsl:if test="not(position()=last())">
      <br id="XSpLit"/>
    </xsl:if>

  </xsl:template>

  <xsl:template match="*/variantid" mode="mode0">
    <xsl:for-each select=".">
      <tr>
        <td>
          <xsl:apply-templates select="."/>
        </td>

      </tr>

    </xsl:for-each>

  </xsl:template>

  <xsl:template match="*/varianttype" mode="mode1">
    <xsl:for-each select=".">
      <tr>
        <td>
          <xsl:apply-templates select="."/>
        </td>

      </tr>

    </xsl:for-each>

  </xsl:template>

  <xsl:template match="*/contenttypeid" mode="mode2">
    <xsl:for-each select=".">
      <tr>
        <td>
          <xsl:apply-templates select="."/>
        </td>

      </tr>

    </xsl:for-each>

  </xsl:template>

  <xsl:template match="*/contenttype" mode="mode3">
    <xsl:for-each select=".">
      <tr>
        <td>
          <xsl:apply-templates select="."/>
        </td>

      </tr>

    </xsl:for-each>

  </xsl:template>

</xsl:stylesheet>
