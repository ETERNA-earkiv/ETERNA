<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:pmo="http://www.cgm.com/sv/pmo/v1"
    exclude-result-prefixes="pmo">
    <xsl:output method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes" />

    <xsl:param name="i18n.patient" select="'Patient'" />
    <xsl:param name="i18n.name" select="'Namn'" />
    <xsl:param name="i18n.codeNumber" select="'Personnummer'" />
    <xsl:param name="i18n.birthDate" select="'Födelsedatum'" />
    <xsl:param name="i18n.sex" select="'Kön'" />
    <xsl:param name="i18n.language" select="'Språk'" />
    <xsl:param name="i18n.phone" select="'Telefon'" />
    <xsl:param name="i18n.email" select="'E-post'" />
    <xsl:param name="i18n.address" select="'Adress'" />
    <xsl:param name="i18n.deceased" select="'Avliden'" />
    <xsl:param name="i18n.confidential" select="'Sekretessmarkering'" />
    <xsl:param name="i18n.interpreterNeeded" select="'Tolkbehov'" />
    <xsl:param name="i18n.relatives" select="'Anhöriga'" />
    <xsl:param name="i18n.relativeName" select="'Namn'" />
    <xsl:param name="i18n.relativeType" select="'Relation'" />
    <xsl:param name="i18n.custodian" select="'Vårdnadshavare'" />
    <xsl:param name="i18n.schoolAttendance" select="'Skolplacering'" />
    <xsl:param name="i18n.school" select="'Skola'" />
    <xsl:param name="i18n.class" select="'Klass'" />
    <xsl:param name="i18n.schoolYear" select="'Läsår'" />
    <xsl:param name="i18n.period" select="'Period'" />
    <xsl:param name="i18n.dataOrigin" select="'Dataursprung'" />
    <xsl:param name="i18n.system" select="'System'" />
    <xsl:param name="i18n.organization" select="'Organisation'" />
    <xsl:param name="i18n.created" select="'Skapad'" />
    <xsl:param name="i18n.caseBookName" select="'Journaltyp'" />
    <xsl:param name="i18n.growthInfo" select="'Tillväxtinformation'" />
    <xsl:param name="i18n.gaWeeks" select="'Gestationsålder (veckor)'" />
    <xsl:param name="i18n.gaDays" select="'Gestationsålder (dagar)'" />
    <xsl:param name="i18n.yes" select="'Ja'" />
    <xsl:param name="i18n.no" select="'Nej'" />

    <xsl:template name="bool">
        <xsl:param name="val" />
        <xsl:choose>
            <xsl:when test="$val = 'true'"><xsl:value-of select="$i18n.yes" /></xsl:when>
            <xsl:otherwise><xsl:value-of select="$i18n.no" /></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="/">
        <div class="descriptiveMetadata">
            <xsl:apply-templates select="pmo:JournalExport" />
        </div>
    </xsl:template>

    <xsl:template match="pmo:JournalExport">
        <!-- Case book name -->
        <xsl:if test="normalize-space(pmo:CaseBookName) != ''">
            <div class="field">
                <div class="label"><xsl:value-of select="$i18n.caseBookName" /></div>
                <div class="value"><xsl:value-of select="pmo:CaseBookName" /></div>
            </div>
        </xsl:if>

        <!-- Data Origin -->
        <xsl:apply-templates select="pmo:DataOrigin" />

        <!-- Patient Data -->
        <xsl:apply-templates select="pmo:PatientData/pmo:Patient" />
    </xsl:template>

    <!-- Data Origin section -->
    <xsl:template match="pmo:DataOrigin">
        <div class="field">
            <div class="label sectionHeader"><xsl:value-of select="$i18n.dataOrigin" /></div>
            <div class="value">
                <div class="field">
                    <div class="label"><xsl:value-of select="$i18n.system" /></div>
                    <div class="value"><xsl:value-of select="pmo:System" /><xsl:text> </xsl:text><xsl:value-of select="pmo:SystemVersion" /></div>
                </div>
                <div class="field">
                    <div class="label"><xsl:value-of select="$i18n.organization" /></div>
                    <div class="value"><xsl:value-of select="pmo:OrganizationName" /></div>
                </div>
                <xsl:if test="normalize-space(pmo:TopOrganizationName) != ''">
                    <div class="field">
                        <div class="label">Huvudorganisation</div>
                        <div class="value"><xsl:value-of select="pmo:TopOrganizationName" /></div>
                    </div>
                </xsl:if>
                <div class="field">
                    <div class="label"><xsl:value-of select="$i18n.created" /></div>
                    <div class="value"><xsl:value-of select="substring(pmo:Created, 1, 10)" /></div>
                </div>
            </div>
        </div>
    </xsl:template>

    <!-- Patient section -->
    <xsl:template match="pmo:Patient">
        <div class="field">
            <div class="label sectionHeader"><xsl:value-of select="$i18n.patient" /></div>
            <div class="value">
                <div class="field">
                    <div class="label"><xsl:value-of select="$i18n.name" /></div>
                    <div class="value"><xsl:value-of select="pmo:Name" /></div>
                </div>
                <div class="field">
                    <div class="label"><xsl:value-of select="$i18n.codeNumber" /></div>
                    <div class="value"><xsl:value-of select="pmo:CodeNumber" /></div>
                </div>
                <div class="field">
                    <div class="label"><xsl:value-of select="$i18n.birthDate" /></div>
                    <div class="value"><xsl:value-of select="pmo:BirthDate" /></div>
                </div>
                <div class="field">
                    <div class="label"><xsl:value-of select="$i18n.sex" /></div>
                    <div class="value">
                        <xsl:choose>
                            <xsl:when test="pmo:Sex = 'F'">Kvinna</xsl:when>
                            <xsl:when test="pmo:Sex = 'M'">Man</xsl:when>
                            <xsl:otherwise><xsl:value-of select="pmo:Sex" /></xsl:otherwise>
                        </xsl:choose>
                    </div>
                </div>
                <xsl:if test="normalize-space(pmo:Language) != ''">
                    <div class="field">
                        <div class="label"><xsl:value-of select="$i18n.language" /></div>
                        <div class="value"><xsl:value-of select="pmo:Language" /></div>
                    </div>
                </xsl:if>
                <xsl:if test="normalize-space(pmo:MobilePhone) != ''">
                    <div class="field">
                        <div class="label"><xsl:value-of select="$i18n.phone" /></div>
                        <div class="value"><xsl:value-of select="pmo:MobilePhone" /></div>
                    </div>
                </xsl:if>
                <xsl:if test="normalize-space(pmo:Email) != ''">
                    <div class="field">
                        <div class="label"><xsl:value-of select="$i18n.email" /></div>
                        <div class="value"><xsl:value-of select="pmo:Email" /></div>
                    </div>
                </xsl:if>
                <div class="field">
                    <div class="label"><xsl:value-of select="$i18n.deceased" /></div>
                    <div class="value">
                        <xsl:call-template name="bool"><xsl:with-param name="val" select="pmo:Deceased" /></xsl:call-template>
                    </div>
                </div>
                <xsl:if test="pmo:ConfidentialData = 'true'">
                    <div class="field">
                        <div class="label"><xsl:value-of select="$i18n.confidential" /></div>
                        <div class="value"><xsl:value-of select="$i18n.yes" /></div>
                    </div>
                </xsl:if>
                <xsl:if test="pmo:InterpreterNeeded = 'true'">
                    <div class="field">
                        <div class="label"><xsl:value-of select="$i18n.interpreterNeeded" /></div>
                        <div class="value"><xsl:value-of select="$i18n.yes" /></div>
                    </div>
                </xsl:if>

                <!-- Address -->
                <xsl:apply-templates select="pmo:PatientAddress" />

                <!-- Relatives -->
                <xsl:if test="pmo:Relative">
                    <div class="field">
                        <div class="label sectionHeader"><xsl:value-of select="$i18n.relatives" /></div>
                        <div class="value">
                            <xsl:for-each select="pmo:Relative">
                                <div class="field">
                                    <div class="label"><xsl:value-of select="pmo:RelativeType" /></div>
                                    <div class="value">
                                        <xsl:value-of select="pmo:Name" />
                                        <xsl:if test="pmo:Custodian = 'true'">
                                            <xsl:text> (</xsl:text><xsl:value-of select="$i18n.custodian" /><xsl:text>)</xsl:text>
                                        </xsl:if>
                                        <xsl:if test="pmo:Deceased = 'true'">
                                            <xsl:text> — </xsl:text><xsl:value-of select="$i18n.deceased" />
                                            <xsl:if test="normalize-space(pmo:DeceasedDate) != ''">
                                                <xsl:text> </xsl:text><xsl:value-of select="pmo:DeceasedDate" />
                                            </xsl:if>
                                        </xsl:if>
                                    </div>
                                </div>
                            </xsl:for-each>
                        </div>
                    </div>
                </xsl:if>

                <!-- Growth Info -->
                <xsl:if test="pmo:GrowthPatInfo">
                    <div class="field">
                        <div class="label sectionHeader"><xsl:value-of select="$i18n.growthInfo" /></div>
                        <div class="value">
                            <div class="field">
                                <div class="label"><xsl:value-of select="$i18n.gaWeeks" /></div>
                                <div class="value"><xsl:value-of select="pmo:GrowthPatInfo/pmo:GAWeeks" /></div>
                            </div>
                            <div class="field">
                                <div class="label"><xsl:value-of select="$i18n.gaDays" /></div>
                                <div class="value"><xsl:value-of select="pmo:GrowthPatInfo/pmo:GADays" /></div>
                            </div>
                        </div>
                    </div>
                </xsl:if>

                <!-- School Attendance -->
                <xsl:if test="pmo:SchoolAttendance">
                    <div class="field">
                        <div class="label sectionHeader"><xsl:value-of select="$i18n.schoolAttendance" /></div>
                        <div class="value">
                            <xsl:for-each select="pmo:SchoolAttendance">
                                <div class="field">
                                    <div class="label"><xsl:value-of select="pmo:SchoolYear" /></div>
                                    <div class="value">
                                        <xsl:value-of select="pmo:School" />
                                        <xsl:if test="normalize-space(pmo:Class) != ''">
                                            <xsl:text>, </xsl:text><xsl:value-of select="$i18n.class" /><xsl:text> </xsl:text><xsl:value-of select="pmo:Class" />
                                        </xsl:if>
                                        <xsl:text> (</xsl:text>
                                        <xsl:value-of select="pmo:StartDate" />
                                        <xsl:text> – </xsl:text>
                                        <xsl:value-of select="pmo:EndDate" />
                                        <xsl:text>)</xsl:text>
                                    </div>
                                </div>
                            </xsl:for-each>
                        </div>
                    </div>
                </xsl:if>
            </div>
        </div>
    </xsl:template>

    <!-- Address -->
    <xsl:template match="pmo:PatientAddress">
        <div class="field">
            <div class="label"><xsl:value-of select="$i18n.address" /></div>
            <div class="value">
                <xsl:value-of select="pmo:Address1" />
                <xsl:if test="normalize-space(pmo:ZipCode) != '' or normalize-space(pmo:City) != ''">
                    <xsl:text>, </xsl:text>
                    <xsl:value-of select="pmo:ZipCode" />
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="pmo:City" />
                </xsl:if>
            </div>
        </div>
    </xsl:template>

</xsl:stylesheet>
