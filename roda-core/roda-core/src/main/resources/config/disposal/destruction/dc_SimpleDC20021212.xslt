<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="UTF-8"
                omit-xml-declaration="yes"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Remove sensitive descriptive metadata; retain identifier and date -->
    <xsl:template match="*:title"/>
    <xsl:template match="*:description"/>
    <xsl:template match="*:subject"/>
    <xsl:template match="*:creator"/>
    <xsl:template match="*:contributor"/>
    <xsl:template match="*:publisher"/>
    <xsl:template match="*:language"/>
    <xsl:template match="*:coverage"/>

</xsl:stylesheet>
