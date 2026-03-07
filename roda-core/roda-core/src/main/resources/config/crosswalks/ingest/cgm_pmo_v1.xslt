<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:pmo="http://www.cgm.com/sv/pmo/v1"
    exclude-result-prefixes="pmo">
    <xsl:output method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes" />

    <xsl:template match="/">
        <doc>
            <xsl:apply-templates select="pmo:JournalExport" />
        </doc>
    </xsl:template>

    <xsl:template match="pmo:JournalExport">
        <xsl:variable name="name" select="pmo:PatientData/pmo:Patient/pmo:Name" />
        <xsl:variable name="code" select="pmo:PatientData/pmo:Patient/pmo:CodeNumber" />

        <!-- Title: Name + Personnummer -->
        <field name="title">
            <xsl:value-of select="$name" />
            <xsl:text> (</xsl:text>
            <xsl:value-of select="$code" />
            <xsl:text>)</xsl:text>
        </field>
        <field name="title_txt">
            <xsl:value-of select="$name" />
        </field>

        <!-- Description -->
        <field name="description">
            <xsl:text>Elevhälsojournal - </xsl:text>
            <xsl:value-of select="$name" />
            <xsl:text>, </xsl:text>
            <xsl:value-of select="$code" />
            <xsl:text>. Exporterad från </xsl:text>
            <xsl:value-of select="pmo:DataOrigin/pmo:System" />
            <xsl:text> </xsl:text>
            <xsl:value-of select="pmo:DataOrigin/pmo:SystemVersion" />
        </field>

        <!-- Date -->
        <xsl:variable name="created" select="substring(pmo:DataOrigin/pmo:Created, 1, 10)" />
        <xsl:if test="string-length($created) = 10">
            <field name="dateInitial"><xsl:value-of select="$created" />T00:00:00Z</field>
            <field name="dateFinal"><xsl:value-of select="$created" />T00:00:00Z</field>
        </xsl:if>

        <!-- Creator / Organization -->
        <field name="creator_txt">
            <xsl:value-of select="pmo:DataOrigin/pmo:OrganizationName" />
        </field>

        <!-- Level -->
        <field name="level">item</field>

        <!-- Searchable text fields -->
        <field name="identifier_txt"><xsl:value-of select="$code" /></field>

        <xsl:if test="normalize-space(pmo:PatientData/pmo:Patient/pmo:BirthDate) != ''">
            <field name="date_txt"><xsl:value-of select="pmo:PatientData/pmo:Patient/pmo:BirthDate" /></field>
        </xsl:if>

        <xsl:if test="normalize-space(pmo:CaseBookName) != ''">
            <field name="subject_txt"><xsl:value-of select="pmo:CaseBookName" /></field>
        </xsl:if>

        <field name="type_txt">Elevhälsojournal</field>
        <field name="language_txt">sv</field>
    </xsl:template>

</xsl:stylesheet>
