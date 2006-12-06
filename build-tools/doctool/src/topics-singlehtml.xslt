<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html"/>
    <xsl:strip-space elements="*"/>

    <xsl:key name="topicSearch" match="//topic" use="id"/>

    <xsl:param name="css"/>

    <xsl:template match="/">
        <html>
            <head>
                <title>Innuvo Dynamic Client System 1.1 Documentation</title>
                <link rel="stylesheet" href="help.css" type="text/css" />
            </head>
            <body>
                <xsl:apply-templates select="//topic"/> 
            </body>
        </html>
    </xsl:template>

    <!-- Only topics that have titles get pulled in -->
    <xsl:template match="topic[title]">
        <xsl:element name="a">
            <xsl:attribute name="name"><xsl:value-of select="id"/></xsl:attribute>
        </xsl:element>

<!--
        <xsl:if test="header">
            <div class="topicHeader">
                <xsl:apply-templates select="header/@*|header/node()" />
            </div>
        </xsl:if>
-->

        <xsl:if test="body">
            <div class="topicBody">
                <xsl:apply-templates select="body/@*|body/node()" />
            </div>
        </xsl:if>
        
        <!-- Links to child topics -->
<!--        
        <xsl:if test="childIntro">
            <div class="topicChildren">
                <xsl:apply-templates select="childIntro"/>
                <ul>
                    <xsl:for-each select="topic">
                        <li>
                            <xsl:call-template name="makeLink">
                                <xsl:with-param name="linkText" select="title/node()"/>
                                <xsl:with-param name="linkTarget" select="."/>
                            </xsl:call-template>
                            <xsl:if test="synopsis">
                                - <xsl:apply-templates select="synopsis"/>
                            </xsl:if>
                        </li>
                    </xsl:for-each>
                </ul>
            </div>
        </xsl:if>
-->

        <!-- See also links -->
<!--        
        <xsl:if test="seeAlso">
            <div class="topicSeeAlso">
                <h2>Related topics</h2>
                <xsl:for-each select="seeAlso">
                    <xsl:apply-templates select="link"/>
                    <xsl:if test="position()!=last()">, </xsl:if> 
                </xsl:for-each>
            </div>
        </xsl:if>
-->        
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy>
    </xsl:template>
    
    <xsl:template match="pre">
        <!-- Do not copy, wastes time during spelling/grammar checks -->
        <div>CODE SAMPLE REMOVED</div>
    </xsl:template>
    
    <xsl:template match="table[@class='members']">
        <!-- Do not copy, wastes time during spelling/grammar checks -->
        <div>TABLE OF MEMBERS REMOVED</div>
    </xsl:template>

    <xsl:template match="img">
        <!-- Do not copy, wastes time during spelling/grammar checks -->
        <div>IMAGE REMOVED</div>
    </xsl:template>

    <xsl:template match="link">
        <xsl:call-template name="makeLink">
            <xsl:with-param name="linkText" select="node()"/>
            <xsl:with-param name="linkTarget" select="key('topicSearch', @ref)"/>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template name="makeLink">
        <xsl:param name="linkText"/>
        <xsl:param name="linkTarget"/>
        <!-- Always emit plain text -->
        <xsl:apply-templates select="$linkText"/>
    </xsl:template>
    
</xsl:stylesheet>
