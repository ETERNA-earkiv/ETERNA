<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:sie="http://www.sie.se/sie5"
  exclude-result-prefixes="sie">
  <xsl:output method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes"/>

  <xsl:template match="/">
    <doc>
      <xsl:apply-templates select="//sie:SIE"/>
    </doc>
  </xsl:template>

  <xsl:template match="sie:SIE">
    <!-- Company name → title (primary search/display field) -->
    <xsl:if test="sie:Company/@name and normalize-space(sie:Company/@name) != ''">
      <field name="title">
        <xsl:value-of select="normalize-space(sie:Company/@name)"/>
      </field>
      <field name="title_txt">
        <xsl:value-of select="normalize-space(sie:Company/@name)"/>
      </field>
    </xsl:if>

    <!-- Organization number -->
    <xsl:if test="sie:Company/@clientId and normalize-space(sie:Company/@clientId) != ''">
      <field name="orgId_txt">
        <xsl:value-of select="normalize-space(sie:Company/@clientId)"/>
      </field>
    </xsl:if>

    <!-- Primary fiscal year dates → dateInitial / dateFinal -->
    <xsl:variable name="primaryYear" select="sie:Company/sie:FiscalYears/sie:FiscalYear[@primaryYear='true'][1]"/>
    <xsl:variable name="firstYear"   select="sie:Company/sie:FiscalYears/sie:FiscalYear[1]"/>
    <xsl:variable name="fiscalYear"  select="if ($primaryYear) then $primaryYear else $firstYear"/>

    <xsl:if test="$fiscalYear/@start and normalize-space($fiscalYear/@start) != ''">
      <field name="dateInitial">
        <xsl:value-of select="normalize-space($fiscalYear/@start)"/>
      </field>
      <field name="fiscalYearStart_txt">
        <xsl:value-of select="normalize-space($fiscalYear/@start)"/>
      </field>
    </xsl:if>

    <xsl:if test="$fiscalYear/@end and normalize-space($fiscalYear/@end) != ''">
      <field name="dateFinal">
        <xsl:value-of select="normalize-space($fiscalYear/@end)"/>
      </field>
      <field name="fiscalYearEnd_txt">
        <xsl:value-of select="normalize-space($fiscalYear/@end)"/>
      </field>
    </xsl:if>

    <!-- Company type -->
    <xsl:if test="sie:FileInfo/sie:CompanyTypeInfo/@type and normalize-space(sie:FileInfo/sie:CompanyTypeInfo/@type) != ''">
      <field name="companyType_txt">
        <xsl:value-of select="normalize-space(sie:FileInfo/sie:CompanyTypeInfo/@type)"/>
      </field>
    </xsl:if>

    <!-- Accounting plan type (e.g. BAS2020) -->
    <xsl:if test="sie:Company/sie:AccountingPlan/@type and normalize-space(sie:Company/sie:AccountingPlan/@type) != ''">
      <field name="accountingPlanType_txt">
        <xsl:value-of select="normalize-space(sie:Company/sie:AccountingPlan/@type)"/>
      </field>
    </xsl:if>

    <!-- Software that produced the file -->
    <xsl:if test="sie:FileInfo/sie:SoftwareProduct/@name and normalize-space(sie:FileInfo/sie:SoftwareProduct/@name) != ''">
      <field name="softwareProduct_txt">
        <xsl:value-of select="normalize-space(sie:FileInfo/sie:SoftwareProduct/@name)"/>
      </field>
    </xsl:if>

    <!-- File creation timestamp -->
    <xsl:if test="sie:FileInfo/sie:FileCreation/@time and normalize-space(sie:FileInfo/sie:FileCreation/@time) != ''">
      <field name="fileCreationTime_txt">
        <xsl:value-of select="normalize-space(sie:FileInfo/sie:FileCreation/@time)"/>
      </field>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
