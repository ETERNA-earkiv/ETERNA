<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:erms="https://DILCIS.eu/XML/ERMS"
	exclude-result-prefixes="erms">
	<xsl:output method="xml" indent="yes" encoding="UTF-8"
		omit-xml-declaration="yes" />

	<xsl:template match="/">
		<doc>
			<!-- Title: prefer aggregation, fallback to record -->
			<xsl:if test="//erms:aggregation[1]/erms:title/text()">
				<field name="title">
					<xsl:value-of select="//erms:aggregation[1]/erms:title/text()" />
				</field>
			</xsl:if>
			<xsl:if test="not(//erms:aggregation[1]/erms:title/text()) and //erms:record[1]/erms:title/text()">
				<field name="title">
					<xsl:value-of select="//erms:record[1]/erms:title/text()" />
				</field>
			</xsl:if>

			<!-- systemIdentifier (attribute on aggregation or record) -->
			<xsl:if test="//erms:aggregation[1]/@systemIdentifier">
				<field name="erms_systemIdentifier_txt">
					<xsl:value-of select="//erms:aggregation[1]/@systemIdentifier" />
				</field>
			</xsl:if>
			<xsl:if test="not(//erms:aggregation[1]/@systemIdentifier) and //erms:record[1]/@systemIdentifier">
				<field name="erms_systemIdentifier_txt">
					<xsl:value-of select="//erms:record[1]/@systemIdentifier" />
				</field>
			</xsl:if>

			<!-- status (@value attribute) -->
			<xsl:if test="//erms:aggregation[1]/erms:status/@value">
				<field name="erms_status_txt">
					<xsl:value-of select="//erms:aggregation[1]/erms:status/@value" />
				</field>
			</xsl:if>
			<xsl:if test="not(//erms:aggregation[1]/erms:status/@value) and //erms:record[1]/erms:status/@value">
				<field name="erms_status_txt">
					<xsl:value-of select="//erms:record[1]/erms:status/@value" />
				</field>
			</xsl:if>

			<!-- openDate: look for dateType="opened" or "opening_date" -->
			<xsl:variable name="openDateValue">
				<xsl:choose>
					<xsl:when test="//erms:aggregation[1]/erms:dates/erms:date[@dateType='opened' or @dateType='opening_date'][1]/text()">
						<xsl:value-of select="//erms:aggregation[1]/erms:dates/erms:date[@dateType='opened' or @dateType='opening_date'][1]/text()" />
					</xsl:when>
					<xsl:when test="//erms:record[1]/erms:dates/erms:date[@dateType='opened' or @dateType='opening_date'][1]/text()">
						<xsl:value-of select="//erms:record[1]/erms:dates/erms:date[@dateType='opened' or @dateType='opening_date'][1]/text()" />
					</xsl:when>
				</xsl:choose>
			</xsl:variable>
			<xsl:if test="normalize-space($openDateValue) != ''">
				<xsl:analyze-string regex="^(\d{{4}}-\d{{2}}-\d{{2}}T\d{{2}}:\d{{2}}:\d{{2}})"
					select="normalize-space($openDateValue)">
					<xsl:matching-substring>
						<field name="erms_openDate_dt">
							<xsl:value-of select="regex-group(1)" />
							<xsl:text>Z</xsl:text>
						</field>
					</xsl:matching-substring>
				</xsl:analyze-string>
			</xsl:if>

			<!-- parentAggregationId -->
			<xsl:if test="//erms:aggregation[1]/erms:parentAggregationId/text()">
				<field name="erms_parentAggregationId_txt">
					<xsl:value-of select="//erms:aggregation[1]/erms:parentAggregationId/text()" />
				</field>
			</xsl:if>
			<xsl:if test="not(//erms:aggregation[1]/erms:parentAggregationId/text()) and //erms:record[1]/erms:parentAggregationId/text()">
				<field name="erms_parentAggregationId_txt">
					<xsl:value-of select="//erms:record[1]/erms:parentAggregationId/text()" />
				</field>
			</xsl:if>
		</doc>
	</xsl:template>
</xsl:stylesheet>
