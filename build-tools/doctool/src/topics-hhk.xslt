<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html"/>
    <xsl:template match="/">
        <html>
            <body>
                <ul>
                    <xsl:for-each select="//topic/index">
                        <xsl:sort select="."/>
                        <li>
                            <object type="text/sitemap">
                                <xsl:element name="param">
                                    <xsl:attribute name="name">Name</xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="." />
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="param">
                                    <xsl:attribute name="name">Local</xsl:attribute>
                                    <xsl:attribute name="value"><xsl:value-of select="../id" />.html</xsl:attribute>
                                </xsl:element>
                            </object>
                        </li>
                    </xsl:for-each>
                </ul>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
