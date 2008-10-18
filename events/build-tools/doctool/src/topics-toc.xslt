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
				<xsl:apply-templates select="/topics/topic">
					<xsl:sort select="order" data-type="number" order="descending"/>
				</xsl:apply-templates>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="topic">
		<div style="margin-left: 2%">
			<xsl:element name="a">
				<xsl:attribute name="href"><xsl:value-of select="id"/>.html</xsl:attribute>
				<xsl:attribute name="class">tocLink</xsl:attribute>
				<xsl:value-of select="title"/>
			</xsl:element>
			<xsl:apply-templates select="topic"/>
		</div>
	</xsl:template>
</xsl:stylesheet>
