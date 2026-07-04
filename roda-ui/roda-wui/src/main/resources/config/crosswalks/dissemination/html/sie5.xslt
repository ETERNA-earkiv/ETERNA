<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:sie="http://www.sie.se/sie5"
  exclude-result-prefixes="sie">
  <xsl:output method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes"/>

  <xsl:param name="i18n.supplierName"/>
  <xsl:param name="i18n.supplierInvoiceId"/>
  <xsl:param name="i18n.invoiceId"/>
  <xsl:param name="i18n.invoiceDate"/>
  <xsl:param name="i18n.supplierOrgId"/>
  <xsl:param name="i18n.currency"/>
  <xsl:param name="i18n.approvedBy"/>
  <xsl:param name="i18n.approvedDate"/>
  <xsl:param name="i18n.paymentDate"/>
  <xsl:param name="i18n.fiscalYearStart"/>
  <xsl:param name="i18n.fiscalYearEnd"/>

  <xsl:template match="/">
    <div class="descriptiveMetadata">
      <xsl:apply-templates select="//sie:SIE"/>
    </div>
  </xsl:template>

  <xsl:template match="sie:SIE">
    <xsl:variable name="inv" select="sie:Company/sie:SupplierInvoices/sie:SupplierInvoice[1]"/>
    <xsl:variable name="sup" select="sie:Company/sie:Suppliers/sie:Supplier[@id = $inv/@supplierRef][1]"/>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.supplierName"/>
      <xsl:with-param name="value" select="normalize-space($sup/@name)"/>
    </xsl:call-template>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.supplierInvoiceId"/>
      <xsl:with-param name="value" select="normalize-space($inv/@supplierInvoiceId)"/>
    </xsl:call-template>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.invoiceId"/>
      <xsl:with-param name="value" select="normalize-space($inv/@id)"/>
    </xsl:call-template>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.invoiceDate"/>
      <xsl:with-param name="value" select="normalize-space($inv/@invoiceDate)"/>
    </xsl:call-template>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.supplierOrgId"/>
      <xsl:with-param name="value" select="normalize-space($sup/@organizationId)"/>
    </xsl:call-template>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.currency"/>
      <xsl:with-param name="value" select="normalize-space($inv/@currency)"/>
    </xsl:call-template>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.approvedBy"/>
      <xsl:with-param name="value" select="normalize-space($inv/@approvedBy)"/>
    </xsl:call-template>

    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.approvedDate"/>
      <xsl:with-param name="value" select="normalize-space($inv/@approvedDate)"/>
    </xsl:call-template>
    <xsl:call-template name="field">
      <xsl:with-param name="label" select="$i18n.paymentDate"/>
      <xsl:with-param name="value" select="normalize-space($inv/@paymentDate)"/>
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
