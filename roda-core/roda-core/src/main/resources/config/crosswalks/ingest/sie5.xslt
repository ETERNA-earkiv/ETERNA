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
    <xsl:variable name="invoices" select="sie:Company/sie:SupplierInvoices/sie:SupplierInvoice"/>
    <xsl:variable name="singleInvoice" select="count($invoices) = 1"/>

    <!-- Title: leverantör + fakturanummer för enskild faktura, annars företagsnamn -->
    <xsl:choose>
      <xsl:when test="$singleInvoice">
        <xsl:variable name="inv"      select="$invoices[1]"/>
        <xsl:variable name="suppName" select="sie:Company/sie:Suppliers/sie:Supplier[@id = $inv/@supplierRef]/@name"/>
        <xsl:variable name="title">
          <xsl:value-of select="concat(
            if (normalize-space($suppName) != '') then normalize-space($suppName) else 'Okänd leverantör',
            ' – Faktura ',
            normalize-space($inv/@id)
          )"/>
        </xsl:variable>
        <field name="title"><xsl:value-of select="$title"/></field>
        <field name="title_txt"><xsl:value-of select="$title"/></field>
      </xsl:when>
      <xsl:otherwise>
        <xsl:if test="normalize-space(sie:Company/@name) != ''">
          <field name="title"><xsl:value-of select="normalize-space(sie:Company/@name)"/></field>
          <field name="title_txt"><xsl:value-of select="normalize-space(sie:Company/@name)"/></field>
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>

    <!-- Organisationsnummer -->
    <xsl:if test="normalize-space(sie:Company/@clientId) != ''">
      <field name="orgId_txt"><xsl:value-of select="normalize-space(sie:Company/@clientId)"/></field>
    </xsl:if>

    <!-- Räkenskapsår → dateInitial/dateFinal (åsidosätts nedan för enskild faktura) -->
    <xsl:variable name="fy" select="(sie:Company/sie:FiscalYears/sie:FiscalYear[@primaryYear='true'], sie:Company/sie:FiscalYears/sie:FiscalYear)[1]"/>
    <xsl:if test="not($singleInvoice) and normalize-space($fy/@start) != ''">
      <field name="dateInitial"><xsl:value-of select="normalize-space($fy/@start)"/></field>
      <field name="fiscalYearStart_txt"><xsl:value-of select="normalize-space($fy/@start)"/></field>
    </xsl:if>
    <xsl:if test="not($singleInvoice) and normalize-space($fy/@end) != ''">
      <field name="dateFinal"><xsl:value-of select="normalize-space($fy/@end)"/></field>
      <field name="fiscalYearEnd_txt"><xsl:value-of select="normalize-space($fy/@end)"/></field>
    </xsl:if>

    <!-- Alltid: räkenskapsårsinfo -->
    <xsl:if test="normalize-space($fy/@start) != ''">
      <field name="fiscalYearStart_txt"><xsl:value-of select="normalize-space($fy/@start)"/></field>
    </xsl:if>
    <xsl:if test="normalize-space($fy/@end) != ''">
      <field name="fiscalYearEnd_txt"><xsl:value-of select="normalize-space($fy/@end)"/></field>
    </xsl:if>

    <!-- Beskrivningsnivå → styr ikon i UI -->
    <field name="level">item</field>

    <!-- Bolagstyp, kontoplan, programvara, skapandetid -->
    <xsl:if test="normalize-space(sie:FileInfo/sie:CompanyTypeInfo/@type) != ''">
      <field name="companyType_txt"><xsl:value-of select="normalize-space(sie:FileInfo/sie:CompanyTypeInfo/@type)"/></field>
    </xsl:if>
    <xsl:if test="normalize-space(sie:Company/sie:AccountingPlan/@type) != ''">
      <field name="accountingPlanType_txt"><xsl:value-of select="normalize-space(sie:Company/sie:AccountingPlan/@type)"/></field>
    </xsl:if>
    <xsl:if test="normalize-space(sie:FileInfo/sie:SoftwareProduct/@name) != ''">
      <field name="softwareProduct_txt"><xsl:value-of select="normalize-space(sie:FileInfo/sie:SoftwareProduct/@name)"/></field>
    </xsl:if>
    <xsl:if test="normalize-space(sie:FileInfo/sie:FileCreation/@time) != ''">
      <field name="fileCreationTime_txt"><xsl:value-of select="normalize-space(sie:FileInfo/sie:FileCreation/@time)"/></field>
    </xsl:if>

    <!-- Fakturaspecifika fält (endast när en enskild faktura) -->
    <xsl:if test="$singleInvoice">
      <xsl:variable name="inv"      select="$invoices[1]"/>
      <xsl:variable name="suppRef"  select="$inv/@supplierRef"/>
      <xsl:variable name="suppName" select="sie:Company/sie:Suppliers/sie:Supplier[@id = $suppRef]/@name"/>

      <xsl:if test="normalize-space($inv/@id) != ''">
        <field name="invoiceId_txt"><xsl:value-of select="normalize-space($inv/@id)"/></field>
      </xsl:if>
      <xsl:if test="normalize-space($inv/@supplierInvoiceId) != ''">
        <field name="supplierInvoiceId_txt"><xsl:value-of select="normalize-space($inv/@supplierInvoiceId)"/></field>
      </xsl:if>
      <xsl:if test="normalize-space($suppName) != ''">
        <field name="supplierName_txt"><xsl:value-of select="normalize-space($suppName)"/></field>
      </xsl:if>
      <xsl:if test="normalize-space($suppRef) != ''">
        <field name="supplierId_txt"><xsl:value-of select="normalize-space($suppRef)"/></field>
      </xsl:if>
      <xsl:variable name="suppOrgId" select="sie:Company/sie:Suppliers/sie:Supplier[@id = $suppRef]/@organizationId"/>
      <xsl:if test="normalize-space($suppOrgId) != ''">
        <field name="supplierOrgId_txt"><xsl:value-of select="normalize-space($suppOrgId)"/></field>
      </xsl:if>
      <xsl:if test="normalize-space($inv/@invoiceDate) != ''">
        <field name="dateInitial"><xsl:value-of select="substring(normalize-space($inv/@invoiceDate), 1, 10)"/></field>
        <field name="invoiceDate_txt"><xsl:value-of select="substring(normalize-space($inv/@invoiceDate), 1, 10)"/></field>
        <field name="invoiceDate_dt"><xsl:value-of select="substring(normalize-space($inv/@invoiceDate), 1, 10)"/></field>
      </xsl:if>
      <xsl:if test="normalize-space($inv/@dueDate) != ''">
        <field name="dateFinal"><xsl:value-of select="substring(normalize-space($inv/@dueDate), 1, 10)"/></field>
        <field name="dueDate_txt"><xsl:value-of select="substring(normalize-space($inv/@dueDate), 1, 10)"/></field>
      </xsl:if>
      <xsl:if test="normalize-space($inv/@paymentDate) != ''">
        <field name="paymentDate_txt"><xsl:value-of select="substring(normalize-space($inv/@paymentDate), 1, 10)"/></field>
        <field name="paymentDate_dt"><xsl:value-of select="substring(normalize-space($inv/@paymentDate), 1, 10)"/></field>
      </xsl:if>
      <xsl:if test="normalize-space($inv/@grossAmount) != ''">
        <field name="grossAmount_txt"><xsl:value-of select="normalize-space($inv/@grossAmount)"/></field>
      </xsl:if>
      <xsl:if test="normalize-space($inv/@vatAmount) != ''">
        <field name="vatAmount_txt"><xsl:value-of select="normalize-space($inv/@vatAmount)"/></field>
      </xsl:if>
      <xsl:if test="normalize-space($inv/@status) != ''">
        <field name="invoiceStatus_txt"><xsl:value-of select="normalize-space($inv/@status)"/></field>
      </xsl:if>
      <xsl:if test="normalize-space($inv/@approvedBy) != ''">
        <field name="approvedBy_txt"><xsl:value-of select="normalize-space($inv/@approvedBy)"/></field>
      </xsl:if>
      <xsl:if test="normalize-space($inv/@approvedDate) != ''">
        <field name="approvedDate_txt"><xsl:value-of select="substring(normalize-space($inv/@approvedDate), 1, 10)"/></field>
        <field name="approvedDate_dt"><xsl:value-of select="substring(normalize-space($inv/@approvedDate), 1, 10)"/></field>
      </xsl:if>
      <xsl:if test="normalize-space($inv/@currency) != ''">
        <field name="currency_txt"><xsl:value-of select="normalize-space($inv/@currency)"/></field>
      </xsl:if>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
