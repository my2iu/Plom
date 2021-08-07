<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes" doctype-system="about:legacy-compat"/> 

<xsl:include href="basetemplate.xsl"/>

<xsl:template match="plombutton"><span class="plombutton"><xsl:apply-templates/></span></xsl:template>

<xsl:template match="plomsnippet"><span class="plomsnippet"><xsl:apply-templates/></span></xsl:template>

<xsl:template match="plomcode"><div class="plomcode"><xsl:apply-templates/></div></xsl:template>

<xsl:template match="plom-inline-ide"><div><xsl:apply-templates/></div></xsl:template>


</xsl:stylesheet>
