<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html"/>
	<xsl:param name="title"/>
	<xsl:template match="/">
		<html>
			<head>
				<link rel="stylesheet" href="help.css" type="text/css"/>
			</head>
			<body style="margin: 0.5em">
				<h1>
					<xsl:value-of select="$title"/>
				</h1>
				<xsl:apply-templates select="//topic[index]">
					<xsl:sort select="index" order="ascending"/>
				</xsl:apply-templates>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="topic[index]">
		<div>
			<xsl:value-of select="index"/>
			<xsl:text> - </xsl:text>
			<xsl:element name="a">
				<xsl:attribute name="href"><xsl:value-of select="id"/>.html</xsl:attribute>
				<xsl:attribute name="class">tocLink</xsl:attribute>
				<xsl:value-of select="title"/>
			</xsl:element>
		</div>
	</xsl:template>
</xsl:stylesheet>
