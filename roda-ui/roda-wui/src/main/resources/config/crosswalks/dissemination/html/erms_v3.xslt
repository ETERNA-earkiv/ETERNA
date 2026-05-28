<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:erms="https://DILCIS.eu/XML/ERMS"
	exclude-result-prefixes="erms">
	<xsl:output method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes"/>

	<!-- Section headers -->
	<xsl:param name="i18n.controlSection"/>
	<xsl:param name="i18n.identificationSection"/>
	<xsl:param name="i18n.contentSection"/>
	<xsl:param name="i18n.statusSection"/>
	<xsl:param name="i18n.relationsSection"/>
	<xsl:param name="i18n.agentsSection"/>
	<xsl:param name="i18n.accessSection"/>
	<xsl:param name="i18n.disposalSection"/>
	<xsl:param name="i18n.physicalSection"/>
	<xsl:param name="i18n.historySection"/>
	<xsl:param name="i18n.aggregation"/>
	<xsl:param name="i18n.record"/>

	<!-- Field labels -->
	<xsl:param name="i18n.identification"/>
	<xsl:param name="i18n.maintenanceStatus"/>
	<xsl:param name="i18n.agencyName"/>
	<xsl:param name="i18n.objectId"/>
	<xsl:param name="i18n.systemIdentifier"/>
	<xsl:param name="i18n.extraId"/>
	<xsl:param name="i18n.informationClass"/>
	<xsl:param name="i18n.securityClass"/>
	<xsl:param name="i18n.classification"/>
	<xsl:param name="i18n.levelName"/>
	<xsl:param name="i18n.parentAggregationId"/>
	<xsl:param name="i18n.hierarchicalParentClassId"/>
	<xsl:param name="i18n.runningNumber"/>
	<xsl:param name="i18n.recordType"/>
	<xsl:param name="i18n.recordPhysicalOrDigital"/>
	<xsl:param name="i18n.title"/>
	<xsl:param name="i18n.otherTitle"/>
	<xsl:param name="i18n.description"/>
	<xsl:param name="i18n.subject"/>
	<xsl:param name="i18n.keywords"/>
	<xsl:param name="i18n.direction"/>
	<xsl:param name="i18n.status"/>
	<xsl:param name="i18n.date"/>
	<xsl:param name="i18n.relation"/>
	<xsl:param name="i18n.agentName"/>
	<xsl:param name="i18n.agentRole"/>
	<xsl:param name="i18n.agentOrganisation"/>
	<xsl:param name="i18n.access"/>
	<xsl:param name="i18n.restriction"/>
	<xsl:param name="i18n.restrictionExplanatoryText"/>
	<xsl:param name="i18n.restrictionRegulation"/>
	<xsl:param name="i18n.disposalScheduleId"/>
	<xsl:param name="i18n.disposalAction"/>
	<xsl:param name="i18n.disposalPeriod"/>
	<xsl:param name="i18n.disposalMandate"/>
	<xsl:param name="i18n.disposalDescription"/>
	<xsl:param name="i18n.dispatchMode"/>
	<xsl:param name="i18n.physicalLocation"/>
	<xsl:param name="i18n.archivalHistory"/>
	<xsl:param name="i18n.note"/>

	<!-- ==================== MAIN TEMPLATE ==================== -->
	<xsl:template match="/">
		<div class="descriptiveMetadata">

			<!-- Control section -->
			<xsl:if test="//erms:control">
				<div class="form-separator"><xsl:value-of select="$i18n.controlSection"/></div>
				<xsl:for-each select="//erms:control/erms:identification">
					<xsl:call-template name="labelledField">
						<xsl:with-param name="baseLabel" select="$i18n.identification"/>
						<xsl:with-param name="qualifier" select="@identificationType"/>
						<xsl:with-param name="value" select="text()"/>
					</xsl:call-template>
				</xsl:for-each>
				<xsl:call-template name="simpleField">
					<xsl:with-param name="label" select="$i18n.maintenanceStatus"/>
					<xsl:with-param name="value" select="//erms:control/erms:maintenanceInformation/erms:maintenanceStatus/@value"/>
				</xsl:call-template>
				<xsl:for-each select="//erms:control/erms:maintenanceInformation/erms:maintenanceAgency/erms:agencyName">
					<xsl:call-template name="simpleField">
						<xsl:with-param name="label" select="$i18n.agencyName"/>
						<xsl:with-param name="value" select="text()"/>
					</xsl:call-template>
				</xsl:for-each>
			</xsl:if>

			<!-- Aggregations -->
			<xsl:apply-templates select="//erms:aggregations/erms:aggregation" mode="display"/>

			<!-- Top-level records -->
			<xsl:apply-templates select="//erms:records/erms:record" mode="display"/>

		</div>
	</xsl:template>

	<!-- ==================== AGGREGATION ==================== -->
	<xsl:template match="erms:aggregation" mode="display">
		<div class="form-separator"><xsl:value-of select="$i18n.aggregation"/></div>
		<xsl:call-template name="renderFields">
			<xsl:with-param name="entityType" select="'aggregation'"/>
		</xsl:call-template>
		<!-- Nested aggregations and records -->
		<xsl:apply-templates select="erms:aggregation" mode="display"/>
		<xsl:apply-templates select="erms:record" mode="display"/>
	</xsl:template>

	<!-- ==================== RECORD ==================== -->
	<xsl:template match="erms:record" mode="display">
		<div class="form-separator"><xsl:value-of select="$i18n.record"/></div>
		<xsl:call-template name="renderFields">
			<xsl:with-param name="entityType" select="'record'"/>
		</xsl:call-template>
	</xsl:template>

	<!-- ==================== SHARED FIELD RENDERING ==================== -->
	<xsl:template name="renderFields">
		<xsl:param name="entityType"/>

		<!-- IDENTIFICATION SECTION -->
		<xsl:if test="@systemIdentifier or erms:objectId or erms:extraId or erms:identification
		              or erms:classification or erms:levelName or erms:parentAggregationId
		              or erms:hierarchicalParentClassId or erms:runningNumber
		              or @recordType or @recordPhysicalOrDigital">
			<div class="form-separator"><xsl:value-of select="$i18n.identificationSection"/></div>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.systemIdentifier"/>
				<xsl:with-param name="value" select="@systemIdentifier"/>
			</xsl:call-template>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.objectId"/>
				<xsl:with-param name="value" select="erms:objectId/text()"/>
			</xsl:call-template>
			<xsl:for-each select="erms:extraId">
				<xsl:call-template name="labelledField">
					<xsl:with-param name="baseLabel" select="$i18n.extraId"/>
					<xsl:with-param name="qualifier" select="@extraIdType"/>
					<xsl:with-param name="value" select="text()"/>
				</xsl:call-template>
			</xsl:for-each>
			<xsl:for-each select="erms:identification">
				<xsl:call-template name="labelledField">
					<xsl:with-param name="baseLabel" select="$i18n.identification"/>
					<xsl:with-param name="qualifier" select="@identificationType"/>
					<xsl:with-param name="value" select="text()"/>
				</xsl:call-template>
			</xsl:for-each>
			<xsl:for-each select="erms:classification">
				<xsl:call-template name="labelledField">
					<xsl:with-param name="baseLabel" select="$i18n.classification"/>
					<xsl:with-param name="qualifier" select="@classificationCode"/>
					<xsl:with-param name="value" select="text()"/>
				</xsl:call-template>
			</xsl:for-each>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.levelName"/>
				<xsl:with-param name="value" select="erms:levelName/text()"/>
			</xsl:call-template>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.parentAggregationId"/>
				<xsl:with-param name="value" select="erms:parentAggregationId/text()"/>
			</xsl:call-template>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.hierarchicalParentClassId"/>
				<xsl:with-param name="value" select="erms:hierarchicalParentClassId/text()"/>
			</xsl:call-template>
			<xsl:if test="$entityType = 'record'">
				<xsl:call-template name="simpleField">
					<xsl:with-param name="label" select="$i18n.runningNumber"/>
					<xsl:with-param name="value" select="erms:runningNumber/text()"/>
				</xsl:call-template>
				<xsl:call-template name="simpleField">
					<xsl:with-param name="label" select="$i18n.recordType"/>
					<xsl:with-param name="value" select="@recordType"/>
				</xsl:call-template>
				<xsl:call-template name="simpleField">
					<xsl:with-param name="label" select="$i18n.recordPhysicalOrDigital"/>
					<xsl:with-param name="value" select="@recordPhysicalOrDigital"/>
				</xsl:call-template>
			</xsl:if>
		</xsl:if>

		<!-- CONTENT SECTION -->
		<xsl:if test="erms:title or erms:otherTitle or erms:description or erms:subject
		              or erms:keywords or erms:direction">
			<div class="form-separator"><xsl:value-of select="$i18n.contentSection"/></div>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.title"/>
				<xsl:with-param name="value" select="erms:title/text()"/>
			</xsl:call-template>
			<xsl:for-each select="erms:otherTitle">
				<xsl:call-template name="labelledField">
					<xsl:with-param name="baseLabel" select="$i18n.otherTitle"/>
					<xsl:with-param name="qualifier" select="@titleType"/>
					<xsl:with-param name="value" select="text()"/>
				</xsl:call-template>
			</xsl:for-each>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.description"/>
				<xsl:with-param name="value" select="erms:description/text()"/>
			</xsl:call-template>
			<xsl:for-each select="erms:subject">
				<xsl:call-template name="simpleField">
					<xsl:with-param name="label" select="$i18n.subject"/>
					<xsl:with-param name="value" select="text()"/>
				</xsl:call-template>
			</xsl:for-each>
			<xsl:for-each select="erms:keywords/erms:keyword">
				<xsl:call-template name="simpleField">
					<xsl:with-param name="label" select="$i18n.keywords"/>
					<xsl:with-param name="value" select="text()"/>
				</xsl:call-template>
			</xsl:for-each>
			<xsl:if test="$entityType = 'record'">
				<xsl:call-template name="simpleField">
					<xsl:with-param name="label" select="$i18n.direction"/>
					<xsl:with-param name="value" select="erms:direction/@directionDefinition"/>
				</xsl:call-template>
			</xsl:if>
		</xsl:if>

		<!-- STATUS AND DATES -->
		<xsl:if test="erms:status or erms:dates">
			<div class="form-separator"><xsl:value-of select="$i18n.statusSection"/></div>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.status"/>
				<xsl:with-param name="value" select="erms:status/@value"/>
			</xsl:call-template>
			<xsl:for-each select="erms:dates/erms:date">
				<xsl:call-template name="labelledField">
					<xsl:with-param name="baseLabel" select="$i18n.date"/>
					<xsl:with-param name="qualifier" select="@dateType"/>
					<xsl:with-param name="value" select="text()"/>
				</xsl:call-template>
			</xsl:for-each>
		</xsl:if>

		<!-- RELATIONS -->
		<xsl:if test="erms:relation">
			<div class="form-separator"><xsl:value-of select="$i18n.relationsSection"/></div>
			<xsl:for-each select="erms:relation">
				<xsl:call-template name="labelledField">
					<xsl:with-param name="baseLabel" select="$i18n.relation"/>
					<xsl:with-param name="qualifier" select="@relationType"/>
					<xsl:with-param name="value" select="text()"/>
				</xsl:call-template>
			</xsl:for-each>
		</xsl:if>

		<!-- AGENTS -->
		<xsl:if test="erms:agents/erms:agent">
			<div class="form-separator"><xsl:value-of select="$i18n.agentsSection"/></div>
			<xsl:for-each select="erms:agents/erms:agent">
				<xsl:call-template name="simpleField">
					<xsl:with-param name="label" select="$i18n.agentName"/>
					<xsl:with-param name="value" select="erms:name/text()"/>
				</xsl:call-template>
				<xsl:call-template name="simpleField">
					<xsl:with-param name="label" select="$i18n.agentOrganisation"/>
					<xsl:with-param name="value" select="erms:organisation/text()"/>
				</xsl:call-template>
				<xsl:call-template name="simpleField">
					<xsl:with-param name="label" select="$i18n.agentRole"/>
					<xsl:with-param name="value" select="erms:role/text()"/>
				</xsl:call-template>
			</xsl:for-each>
		</xsl:if>

		<!-- ACCESS AND RESTRICTIONS -->
		<xsl:if test="erms:informationClass or erms:securityClass or erms:access or erms:restriction">
			<div class="form-separator"><xsl:value-of select="$i18n.accessSection"/></div>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.informationClass"/>
				<xsl:with-param name="value" select="erms:informationClass/text()"/>
			</xsl:call-template>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.securityClass"/>
				<xsl:with-param name="value" select="erms:securityClass/text()"/>
			</xsl:call-template>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.access"/>
				<xsl:with-param name="value" select="erms:access/text()"/>
			</xsl:call-template>
			<xsl:for-each select="erms:restriction">
				<div class="field">
					<div class="label">
						<xsl:value-of select="$i18n.restriction"/>
						<xsl:if test="@restrictionType">
							<xsl:text> (</xsl:text>
							<xsl:value-of select="@restrictionType"/>
							<xsl:text>)</xsl:text>
						</xsl:if>
					</div>
					<div class="value prewrap">
						<xsl:if test="erms:explanatoryText">
							<xsl:value-of select="erms:explanatoryText"/>
							<xsl:if test="erms:regulation"><xsl:text> — </xsl:text></xsl:if>
						</xsl:if>
						<xsl:if test="erms:regulation">
							<xsl:value-of select="$i18n.restrictionRegulation"/>
							<xsl:text>: </xsl:text>
							<xsl:value-of select="erms:regulation"/>
						</xsl:if>
					</div>
				</div>
			</xsl:for-each>
		</xsl:if>

		<!-- DISPOSAL -->
		<xsl:if test="erms:disposal">
			<div class="form-separator"><xsl:value-of select="$i18n.disposalSection"/></div>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.disposalScheduleId"/>
				<xsl:with-param name="value" select="erms:disposal/erms:disposalScheduleId/text()"/>
			</xsl:call-template>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.disposalAction"/>
				<xsl:with-param name="value" select="erms:disposal/erms:disposalAction/text()"/>
			</xsl:call-template>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.disposalPeriod"/>
				<xsl:with-param name="value" select="erms:disposal/erms:disposalPeriod/text()"/>
			</xsl:call-template>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.disposalMandate"/>
				<xsl:with-param name="value" select="erms:disposal/erms:disposalMandate/text()"/>
			</xsl:call-template>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.disposalDescription"/>
				<xsl:with-param name="value" select="erms:disposal/erms:disposalDescription/text()"/>
			</xsl:call-template>
		</xsl:if>

		<!-- PHYSICAL INFO -->
		<xsl:if test="erms:dispatchMode or erms:physicalLocations">
			<div class="form-separator"><xsl:value-of select="$i18n.physicalSection"/></div>
			<xsl:call-template name="simpleField">
				<xsl:with-param name="label" select="$i18n.dispatchMode"/>
				<xsl:with-param name="value" select="erms:dispatchMode/text()"/>
			</xsl:call-template>
			<xsl:for-each select="erms:physicalLocations/erms:physicalLocation">
				<xsl:call-template name="simpleField">
					<xsl:with-param name="label" select="$i18n.physicalLocation"/>
					<xsl:with-param name="value" select="(erms:currentLocation, erms:homeLocation)[normalize-space()][1]/text()"/>
				</xsl:call-template>
			</xsl:for-each>
		</xsl:if>

		<!-- HISTORY AND NOTES -->
		<xsl:if test="erms:archivalHistory or erms:notes">
			<div class="form-separator"><xsl:value-of select="$i18n.historySection"/></div>
			<xsl:for-each select="erms:archivalHistory/erms:historyLine">
				<xsl:call-template name="simpleField">
					<xsl:with-param name="label" select="$i18n.archivalHistory"/>
					<xsl:with-param name="value" select="text()"/>
				</xsl:call-template>
			</xsl:for-each>
			<xsl:for-each select="erms:notes/erms:note">
				<xsl:call-template name="labelledField">
					<xsl:with-param name="baseLabel" select="$i18n.note"/>
					<xsl:with-param name="qualifier" select="@noteType"/>
					<xsl:with-param name="value" select="text()"/>
				</xsl:call-template>
			</xsl:for-each>
		</xsl:if>
	</xsl:template>

	<!-- ==================== HELPER TEMPLATES ==================== -->

	<xsl:template name="simpleField">
		<xsl:param name="label"/>
		<xsl:param name="value"/>
		<xsl:if test="$value">
			<div class="field">
				<div class="label">
					<xsl:choose>
						<xsl:when test="$label"><xsl:value-of select="$label"/></xsl:when>
						<xsl:otherwise>i18n.key missing or not found</xsl:otherwise>
					</xsl:choose>
				</div>
				<div class="value prewrap"><xsl:value-of select="$value"/></div>
			</div>
		</xsl:if>
	</xsl:template>

	<!-- Field with optional qualifier in parentheses, e.g. "Datum (opened)" -->
	<xsl:template name="labelledField">
		<xsl:param name="baseLabel"/>
		<xsl:param name="qualifier"/>
		<xsl:param name="value"/>
		<xsl:if test="$value">
			<div class="field">
				<div class="label">
					<xsl:value-of select="$baseLabel"/>
					<xsl:if test="normalize-space($qualifier) != ''">
						<xsl:text> (</xsl:text>
						<xsl:value-of select="$qualifier"/>
						<xsl:text>)</xsl:text>
					</xsl:if>
				</div>
				<div class="value prewrap"><xsl:value-of select="$value"/></div>
			</div>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
