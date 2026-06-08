<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:sie="http://www.sie.se/sie5"
  exclude-result-prefixes="sie">
  <xsl:output method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes"/>

  <xsl:param name="i18n.companyName"/>
  <xsl:param name="i18n.orgId"/>
  <xsl:param name="i18n.companyType"/>
  <xsl:param name="i18n.fiscalYearStart"/>
  <xsl:param name="i18n.fiscalYearEnd"/>
  <xsl:param name="i18n.accountingPlanType"/>
  <xsl:param name="i18n.softwareProduct"/>
  <xsl:param name="i18n.fileCreationTime"/>

  <xsl:template match="/">
    <div class="descriptiveMetadata">
      <xsl:apply-templates select="//sie:SIE"/>
    </div>
  </xsl:template>

  <xsl:template match="sie:SIE">
    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.companyName"/>
      <xsl:with-param name="value" select="normalize-space(sie:Company/@name)"/>
    </xsl:call-template>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.orgId"/>
      <xsl:with-param name="value" select="normalize-space(sie:Company/@clientId)"/>
    </xsl:call-template>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.companyType"/>
      <xsl:with-param name="value" select="normalize-space(sie:FileInfo/sie:CompanyTypeInfo/@type)"/>
    </xsl:call-template>

    <xsl:variable name="primaryYear" select="sie:Company/sie:FiscalYears/sie:FiscalYear[@primaryYear='true'][1]"/>
    <xsl:variable name="firstYear"   select="sie:Company/sie:FiscalYears/sie:FiscalYear[1]"/>
    <xsl:variable name="fiscalYear"  select="if ($primaryYear) then $primaryYear else $firstYear"/>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.fiscalYearStart"/>
      <xsl:with-param name="value" select="normalize-space($fiscalYear/@start)"/>
    </xsl:call-template>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.fiscalYearEnd"/>
      <xsl:with-param name="value" select="normalize-space($fiscalYear/@end)"/>
    </xsl:call-template>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.accountingPlanType"/>
      <xsl:with-param name="value" select="normalize-space(sie:Company/sie:AccountingPlan/@type)"/>
    </xsl:call-template>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.softwareProduct"/>
      <xsl:with-param name="value" select="normalize-space(sie:FileInfo/sie:SoftwareProduct/@name)"/>
    </xsl:call-template>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.fileCreationTime"/>
      <xsl:with-param name="value" select="normalize-space(sie:FileInfo/sie:FileCreation/@time)"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="field">
    <xsl:param name="label"/>
    <xsl:param name="value"/>
    <xsl:if test="normalize-space($value) != ''">
      <div class="field">
        <div class="label">
          <xsl:value-of select="$label"/>
        </div>
        <div class="value">
          <xsl:value-of select="$value"/>
        </div>
      </div>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
