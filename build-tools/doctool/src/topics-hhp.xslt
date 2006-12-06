<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text"/>

    <xsl:param name="title"/>
    <xsl:param name="filebase"/>
    <xsl:param name="chm"><xsl:value-of select="$filebase"/>.chm</xsl:param>
    <xsl:param name="hhc"><xsl:value-of select="$filebase"/>.hhc</xsl:param>
    <xsl:param name="hhk"><xsl:value-of select="$filebase"/>.hhk</xsl:param>
    <xsl:param name="css"><xsl:value-of select="$filebase"/>.css</xsl:param>
    <xsl:template match="/">
[Options]
Compatibility = 1.1 Or later
Compiled file = <xsl:value-of select="$chm"/>
Contents file = <xsl:value-of select="$hhc"/>
Default Font = Arial, 9, 0
Default Window=Main
Display compile notes=No
Display compile progress=No
Error log file=error.log
Full-text search=Yes
Index file = <xsl:value-of select="$hhk"/>
Language=0x409 English (United States)
Title=<xsl:value-of select="$title"/>

[Windows]
Main=&quot;<xsl:value-of select="$title"/>&quot;,&quot;<xsl:value-of select="$hhc"/>&quot;,&quot;<xsl:value-of select="$hhk"/>&quot;,,,,,,,0x62520,,0x304c,[10,10,610,510],,,,,,,0

[Files]
<xsl:for-each select="//topic[title]">
<xsl:sort select="order" data-type="number" order="descending"/>        
<xsl:value-of select="id"/>.html
</xsl:for-each>
    </xsl:template>

</xsl:stylesheet>
