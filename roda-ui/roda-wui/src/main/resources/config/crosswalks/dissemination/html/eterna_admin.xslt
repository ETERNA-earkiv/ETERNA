<?xml version="1.0" encoding="UTF-8"?>
<!--
  HTML-visning av administrativ metadata.
  Etiketter kommer fran ServerMessages* via i18n-parametrarna
  (crosswalks.dissemination.html.eterna_admin.*). normalize-space()
  anvands genomgaende sa att tomma element inte ger tomma div:ar.
-->
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:ea="https://eterna.whitered.se/xml/eterna_admin/1"
	exclude-result-prefixes="ea">
	<xsl:output method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes" />

	<xsl:param name="i18n.sekretessSection" select="'Confidentiality'"/>
	<xsl:param name="i18n.sekretess" select="'Confidentiality assessment'"/>
	<xsl:param name="i18n.sekretessgrund" select="'Legal basis (OSL)'"/>
	<xsl:param name="i18n.puSection" select="'Personal data'"/>
	<xsl:param name="i18n.personuppgifter" select="'Personal data assessment'"/>
	<xsl:param name="i18n.personuppgiftsniva" select="'Personal data level'"/>
	<xsl:param name="i18n.personuppgiftsniva.niva_1" select="'Level 1 - harmless'"/>
	<xsl:param name="i18n.personuppgiftsniva.niva_2" select="'Level 2 - protected'"/>
	<xsl:param name="i18n.personuppgiftsniva.niva_3" select="'Level 3 - sensitive'"/>
	<xsl:param name="i18n.pulGrund" select="'Legal basis for processing'"/>
	<xsl:param name="i18n.granskningSection" select="'Review'"/>
	<xsl:param name="i18n.granskningsdatum" select="'Review date'"/>
	<xsl:param name="i18n.anteckning" select="'Note'"/>
	<xsl:param name="i18n.sekretess.ej_bedomd" select="'Not assessed'"/>
	<xsl:param name="i18n.sekretess.ingen_sekretess" select="'No confidentiality'"/>
	<xsl:param name="i18n.sekretess.sekretess_forekommer" select="'Contains confidential information'"/>
	<xsl:param name="i18n.sekretess.helt_sekretessbelagd" select="'Entirely confidential'"/>
	<xsl:param name="i18n.personuppgifter.ej_bedomd" select="'Not assessed'"/>
	<xsl:param name="i18n.personuppgifter.inga" select="'No personal data'"/>
	<xsl:param name="i18n.personuppgifter.forekommer" select="'Contains personal data'"/>
	<xsl:param name="i18n.personuppgifter.kansliga_forekommer" select="'Contains sensitive personal data'"/>

	<xsl:template match="/">
		<div class="descriptiveMetadata">
			<div class="form-separator">
				<xsl:value-of select="$i18n.sekretessSection" />
			</div>
			<xsl:call-template name="fieldWithVocab">
				<xsl:with-param name="label" select="$i18n.sekretess" />
				<xsl:with-param name="vocab" select="'sekretess'" />
				<xsl:with-param name="value" select="normalize-space(//ea:eternaAdmin/ea:sekretess)" />
			</xsl:call-template>
			<xsl:call-template name="field">
				<xsl:with-param name="label" select="$i18n.sekretessgrund" />
				<xsl:with-param name="value" select="normalize-space(//ea:eternaAdmin/ea:sekretessgrund)" />
			</xsl:call-template>

			<div class="form-separator">
				<xsl:value-of select="$i18n.puSection" />
			</div>
			<xsl:call-template name="fieldWithVocab">
				<xsl:with-param name="label" select="$i18n.personuppgifter" />
				<xsl:with-param name="vocab" select="'personuppgifter'" />
				<xsl:with-param name="value" select="normalize-space(//ea:eternaAdmin/ea:personuppgifter)" />
			</xsl:call-template>
			<xsl:call-template name="fieldWithVocab">
				<xsl:with-param name="label" select="$i18n.personuppgiftsniva" />
				<xsl:with-param name="value" select="normalize-space(//ea:eternaAdmin/ea:personuppgiftsniva)" />
			</xsl:call-template>
			<xsl:call-template name="field">
				<xsl:with-param name="label" select="$i18n.pulGrund" />
				<xsl:with-param name="value" select="normalize-space(//ea:eternaAdmin/ea:pulGrund)" />
			</xsl:call-template>

			<div class="form-separator">
				<xsl:value-of select="$i18n.granskningSection" />
			</div>
			<xsl:call-template name="field">
				<xsl:with-param name="label" select="$i18n.granskningsdatum" />
				<xsl:with-param name="value" select="normalize-space(//ea:eternaAdmin/ea:granskningsdatum)" />
			</xsl:call-template>
			<xsl:call-template name="field">
				<xsl:with-param name="label" select="$i18n.anteckning" />
				<xsl:with-param name="value" select="normalize-space(//ea:eternaAdmin/ea:anteckning)" />
			</xsl:call-template>
		</div>
	</xsl:template>

	<xsl:template name="field">
		<xsl:param name="label" />
		<xsl:param name="value" />
		<xsl:if test="$value != ''">
			<div class="field">
				<div class="label">
					<xsl:value-of select="$label" />
				</div>
				<div class="value prewrap">
					<xsl:value-of select="$value" />
				</div>
			</div>
		</xsl:if>
	</xsl:template>

	<!-- Vokabularvarden visas med lasbar text i stallet for koden -->
	<xsl:template name="fieldWithVocab">
		<xsl:param name="label" />
		<xsl:param name="value" />
		<xsl:param name="vocab" />
		<xsl:if test="$value != ''">
			<div class="field">
				<div class="label">
					<xsl:value-of select="$label" />
				</div>
				<div class="value prewrap">
					<xsl:choose>
						<xsl:when test="$vocab = 'sekretess' and $value = 'ej_bedomd'"><xsl:value-of select="$i18n.sekretess.ej_bedomd" /></xsl:when>
						<xsl:when test="$vocab = 'sekretess' and $value = 'ingen_sekretess'"><xsl:value-of select="$i18n.sekretess.ingen_sekretess" /></xsl:when>
						<xsl:when test="$vocab = 'sekretess' and $value = 'sekretess_forekommer'"><xsl:value-of select="$i18n.sekretess.sekretess_forekommer" /></xsl:when>
						<xsl:when test="$vocab = 'sekretess' and $value = 'helt_sekretessbelagd'"><xsl:value-of select="$i18n.sekretess.helt_sekretessbelagd" /></xsl:when>
						<xsl:when test="$vocab = 'personuppgifter' and $value = 'ej_bedomd'"><xsl:value-of select="$i18n.personuppgifter.ej_bedomd" /></xsl:when>
						<xsl:when test="$vocab = 'personuppgifter' and $value = 'inga'"><xsl:value-of select="$i18n.personuppgifter.inga" /></xsl:when>
						<xsl:when test="$vocab = 'personuppgifter' and $value = 'forekommer'"><xsl:value-of select="$i18n.personuppgifter.forekommer" /></xsl:when>
						<xsl:when test="$vocab = 'personuppgifter' and $value = 'kansliga_forekommer'"><xsl:value-of select="$i18n.personuppgifter.kansliga_forekommer" /></xsl:when>
						<!-- personuppgiftsniva: etiketter via i18n-parametrarna
						     (ServerMessages*); standardvokabular som kan anpassas -->
						<xsl:when test="$value = 'niva_1'"><xsl:value-of select="$i18n.personuppgiftsniva.niva_1" /></xsl:when>
						<xsl:when test="$value = 'niva_2'"><xsl:value-of select="$i18n.personuppgiftsniva.niva_2" /></xsl:when>
						<xsl:when test="$value = 'niva_3'"><xsl:value-of select="$i18n.personuppgiftsniva.niva_3" /></xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$value" />
						</xsl:otherwise>
					</xsl:choose>
				</div>
			</div>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
