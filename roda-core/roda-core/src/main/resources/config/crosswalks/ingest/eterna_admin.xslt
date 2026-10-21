<?xml version="1.0" encoding="UTF-8"?>
<!--
  Solr-indexering av administrativ metadata.
  Enum-falt indexeras som icke-analyserade strangar (*_s) sa att de
  fungerar som facetter och urvalskriterier for massredigeringen;
  fritextfalt som *_txt och granskningsdatum som *_dt.
-->
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:ea="https://eterna.whitered.se/xml/eterna_admin/1"
	xmlns:xs="http://www.w3.org/2001/XMLSchema"
	exclude-result-prefixes="ea xs">
	<xsl:output method="xml" indent="yes" encoding="UTF-8"
		omit-xml-declaration="yes" />

	<xsl:template match="/">
		<doc>
			<xsl:if test="normalize-space(//ea:eternaAdmin/ea:sekretess) != ''">
				<field name="sekretess_s">
					<xsl:value-of select="normalize-space(//ea:eternaAdmin/ea:sekretess)" />
				</field>
			</xsl:if>
			<xsl:if test="normalize-space(//ea:eternaAdmin/ea:sekretessgrund) != ''">
				<field name="sekretessgrund_txt">
					<xsl:value-of select="normalize-space(//ea:eternaAdmin/ea:sekretessgrund)" />
				</field>
			</xsl:if>
			<xsl:if test="normalize-space(//ea:eternaAdmin/ea:personuppgifter) != ''">
				<field name="personuppgifter_s">
					<xsl:value-of select="normalize-space(//ea:eternaAdmin/ea:personuppgifter)" />
				</field>
			</xsl:if>
			<xsl:if test="normalize-space(//ea:eternaAdmin/ea:personuppgiftsniva) != ''">
				<field name="personuppgiftsniva_s">
					<xsl:value-of select="normalize-space(//ea:eternaAdmin/ea:personuppgiftsniva)" />
				</field>
			</xsl:if>
			<xsl:if test="normalize-space(//ea:eternaAdmin/ea:pulGrund) != ''">
				<field name="pul_grund_txt">
					<xsl:value-of select="normalize-space(//ea:eternaAdmin/ea:pulGrund)" />
				</field>
			</xsl:if>
			<!-- xs:date kan ha tidszon (2024-01-01+02:00); kalenderdagen behålls -->
			<xsl:if test="normalize-space(//ea:eternaAdmin/ea:granskningsdatum) castable as xs:date">
				<field name="granskningsdatum_dt">
					<xsl:value-of select="format-date(xs:date(normalize-space(//ea:eternaAdmin/ea:granskningsdatum)), '[Y0001]-[M01]-[D01]')" />
					<xsl:text>T00:00:00Z</xsl:text>
				</field>
			</xsl:if>
		</doc>
	</xsl:template>
</xsl:stylesheet>
