<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="UTF-8"
                omit-xml-declaration="yes" />

    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Remove sensitive descriptive metadata; retain unitid and unitdate -->
    <xsl:template match="*:titleproper"/>
    <xsl:template match="*:unittitle"/>
    <xsl:template match="*:abstract"/>
    <xsl:template match="*:scopecontent"/>
    <xsl:template match="*:bioghist"/>
    <xsl:template match="*:origination"/>
    <xsl:template match="*:controlaccess"/>
    <xsl:template match="*:odd"/>
    <xsl:template match="*:acqinfo"/>
    <xsl:template match="*:custodhist"/>
    <xsl:template match="*:relatedmaterial"/>
    <xsl:template match="*:separatedmaterial"/>
    <xsl:template match="*:arrangement"/>
    <xsl:template match="*:langmaterial"/>
    <xsl:template match="*:dao"/>
    <xsl:template match="*:daogrp"/>
    <xsl:template match="*:note"/>
    <xsl:template match="*:accessrestrict"/>
    <xsl:template match="*:originalsloc"/>
</xsl:stylesheet>
