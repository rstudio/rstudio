<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html"/>
    <xsl:template match="/">
        <html>
            <body>
                <ul>
                    <xsl:apply-templates select="topic">
                        <xsl:sort select="order" data-type="number" order="descending"/>
                    </xsl:apply-templates>
                </ul>
            </body>
        </html>
    </xsl:template>
    
    <xsl:template match="topic">
        <li>
            <object type="text/sitemap">
                <xsl:element name="param">
                    <xsl:attribute name="name">Name</xsl:attribute>
                    <xsl:attribute name="value"><xsl:value-of select="title" /></xsl:attribute>
                </xsl:element>
                <xsl:element name="param">
                    <xsl:attribute name="name">Local</xsl:attribute>
                    <xsl:attribute name="value"><xsl:value-of select="id" />.html</xsl:attribute>
                </xsl:element>
            </object>
        </li>
        <xsl:if test="topic">
            <ul>
                <xsl:apply-templates select="topic" />
            </ul>
        </xsl:if>
    </xsl:template>

    <!-- If a topic doesn't have a title, we pretend it isn't there and hoist its children up in its place -->
    <xsl:template match="topic[not(title)]">
        <xsl:apply-templates select="topic">
            <xsl:sort select="order" data-type="number" order="descending"/>        
        </xsl:apply-templates>
    </xsl:template>
</xsl:stylesheet>
