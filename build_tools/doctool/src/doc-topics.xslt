<?xml version="1.0"?>
<!-- Transforms booklet expositive documentation into Topics -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml"/>

    <xsl:template match="/">
    	<topics>
			<xsl:apply-templates select="booklet"/>
		</topics>
    </xsl:template>

    <xsl:template match="booklet">
        <xsl:apply-templates select="package/*"/>
    </xsl:template>

    <xsl:template match="class | interface">
        <topic>
            <id><xsl:value-of select="id"/></id>

            <xsl:if test="tags/order">
                <order><xsl:value-of select="tags/order"/></order>
            </xsl:if>

            <xsl:variable name="title">
                <xsl:value-of select="tags/title"/>
                <xsl:if test="not(tags/title)">
                    <xsl:comment>[Missing title]</xsl:comment>
                </xsl:if>
			</xsl:variable>

            <title>
            	<xsl:value-of select="$title"/>
            </title>

            <xsl:if test="location/link">
	            <location>
                    <xsl:for-each select="location/link">
	                	<xsl:if test="position() > 2">
							<span class="item"><xsl:copy-of select="."/></span> &gt;
						</xsl:if>
                    </xsl:for-each>
					<span class="selected item">
	                    <xsl:value-of select="$title"/>
	                </span>
	            </location>
            </xsl:if>

            <xsl:for-each select="tags/index">
                <index><xsl:value-of select="."/></index>
            </xsl:for-each>

            <xsl:for-each select="tags/tip">
                <xsl:copy-of select="."/>
            </xsl:for-each>

            <xsl:if test="tags/synopsis">
                <synopsis><xsl:copy-of select="tags/synopsis/node()"/></synopsis>
            </xsl:if>
    
            <body><xsl:copy-of select="description/node()"/></body>

            <!-- See also -->
            <xsl:if test="tags/link">
				<seeAlso>
					<xsl:for-each select="tags/link">
						<xsl:copy-of select="."/>
					</xsl:for-each>
				</seeAlso>
            </xsl:if>
        
            <xsl:if test="tags/childIntro">
                <childIntro><xsl:copy-of select="tags/childIntro/node()"/></childIntro>
            </xsl:if>

            <xsl:apply-templates select="class | interface"/>
        </topic>        
    </xsl:template>

    <xsl:template match="*|node()">
        <!-- quiet -->
    </xsl:template>
</xsl:stylesheet>
