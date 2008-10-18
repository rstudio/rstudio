<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" />
    <xsl:strip-space elements="*"/>
    
    <xsl:param name="id"/>
    <xsl:param name="name"/>
    <xsl:param name="synopsis"/>
    <xsl:param name="childIntro"/>

    <xsl:template match="*|node()">
		<!-- no echo -->
    </xsl:template>

    <xsl:template match="/">
        <xsl:apply-templates select="booklet"/>
    </xsl:template>

    <xsl:template match="booklet">
        <topic>
            <xsl:copy-of select="id"/>
            <title><xsl:value-of select="name"/></title>
            <synopsis><xsl:copy-of select="lead/node()"/></synopsis>
            <header><h1><xsl:copy-of select="name/node()"/></h1></header>
            <body>
                <!-- Lengthier description -->
                <xsl:copy-of select="description/node()"/>
            <xsl:apply-templates select="package">
                <xsl:sort select="name"/>
            </xsl:apply-templates>
            </body>
        </topic>
    </xsl:template>

    <xsl:template match="package">
		<xsl:call-template name="memberTable">
			<xsl:with-param name="members" select="class"/>
			<xsl:with-param name="title">Classes</xsl:with-param>
		</xsl:call-template>
<!--
		<xsl:call-template name="memberTable">
			<xsl:with-param name="members" select="interface"/>
			<xsl:with-param name="title">Interfaces</xsl:with-param>
		</xsl:call-template>
-->
		<xsl:apply-templates select="class">
			<xsl:sort select="name"/>
		</xsl:apply-templates>
    </xsl:template>

    <xsl:template match="class|interface">
        <topic>
            <xsl:copy-of select="id"/>
            <title><xsl:value-of select="name"/></title>
            <index>
                <xsl:value-of select="name"/>
                <xsl:if test="name()='class'"> class</xsl:if>
                <xsl:if test="name()='interface'"> interface</xsl:if>
            </index>
            <synopsis><xsl:copy-of select="lead/node()"/></synopsis>
            <header>
                <xsl:call-template name="memberHeader">
                    <xsl:with-param name="member" select="."/>
                </xsl:call-template>
            </header>
            <body>
                <!-- Lengthier description -->
                <xsl:copy-of select="description/node()"/>
            
                <xsl:call-template name="memberTable">
                    <xsl:with-param name="members" select="class"/>
                    <xsl:with-param name="title">Nested classes</xsl:with-param>
                </xsl:call-template>

                <xsl:call-template name="memberTable">
                    <xsl:with-param name="members" select="interface"/>
                    <xsl:with-param name="title">Nested interfaces</xsl:with-param>
                </xsl:call-template>

                <xsl:call-template name="memberTable">
                    <xsl:with-param name="members" select="field"/>
                    <xsl:with-param name="title">Fields</xsl:with-param>
                </xsl:call-template>

                <xsl:call-template name="memberTable">
                    <xsl:with-param name="members" select="constructor"/>
                    <xsl:with-param name="title">Constructors</xsl:with-param>
                </xsl:call-template>

                <xsl:call-template name="memberTable">
                    <xsl:with-param name="members" select="method"/>
                    <xsl:with-param name="title">Methods</xsl:with-param>
                </xsl:call-template>

                <!-- See also -->
                <xsl:if test="tags/link">
                    <h2>See Also</h2>
                    <div class="apiSection">
                        <xsl:for-each select="tags/link">
                            <xsl:copy-of select="."/>
                            <xsl:if test="position()!=last()">, </xsl:if> 
                        </xsl:for-each>
                    </div>
                </xsl:if>
            </body>

            <xsl:apply-templates select="interface">
                <xsl:sort select="name"/>
            </xsl:apply-templates>

            <xsl:apply-templates select="class">
                <xsl:sort select="name"/>
            </xsl:apply-templates>

            <xsl:apply-templates select="field">
                <xsl:sort select="name"/>
            </xsl:apply-templates>

            <xsl:apply-templates select="constructor">
                <xsl:sort select="name"/>
            </xsl:apply-templates>

            <xsl:apply-templates select="method">
                <xsl:sort select="name"/>
            </xsl:apply-templates>
        </topic>
    </xsl:template>

    <xsl:template match="method|constructor">
        <topic>
            <id><xsl:value-of select="id"/></id>
            <title>
				<xsl:call-template name="emitFlatSig">
					<xsl:with-param name="methodOrCtor" select="current()"/>
				</xsl:call-template>
			</title>
            <index>
                <xsl:value-of select="name"/>
                <xsl:if test="name()='method'"> method</xsl:if>
                <xsl:if test="name()='constructor'"> constructor</xsl:if>
            </index>
            <synopsis><xsl:copy-of select="lead/node()"/></synopsis>
            <header>
                <xsl:call-template name="memberHeader">
                    <xsl:with-param name="member" select="."/>
                </xsl:call-template>
            </header>
            <body>
                <!-- Lengthier description -->
                <xsl:copy-of select="description/node()"/>

                <!-- Parameter description -->
                <xsl:if test="params/param">
                    <h2>Parameters</h2>
                    <div class="apiSection">
                        <dl>
                        <xsl:for-each select="params/param">
                            <dt>							
								<xsl:if test="starts-with(name, '_')">
									<xsl:value-of select="substring(name,2)"/><xsl:text>?</xsl:text>
								</xsl:if>
								<xsl:if test="not(starts-with(name, '_'))">
									<xsl:value-of select="name"/> 
								</xsl:if>
                            </dt>
                            <dd>
								<xsl:if test="starts-with(name, '_')">
									<em>[Optional] </em>
								</xsl:if>
                                <xsl:variable name="paramDesc" select="../../tags/param[name=current()/name]/description/node()"/>
                                <xsl:copy-of select="$paramDesc"/>
                                <xsl:if test="not($paramDesc)"><xsl:comment>[Missing documentation]</xsl:comment></xsl:if>
                            </dd>
                        </xsl:for-each>
                        </dl>
                    </div>
                </xsl:if>

                <!-- Return value description -->
                <xsl:if test="tags/return">
                    <h2>Return Value</h2>
                    <div class="apiSection">
                        <xsl:copy-of select="tags/return/node()"/>
                    </div>
                </xsl:if>

                <!-- See also -->
                <xsl:if test="tags/link">
                    <h2>See Also</h2>
                    <div class="apiSection">
                        <xsl:for-each select="tags/link">
                            <xsl:copy-of select="."/>
                            <xsl:if test="position()!=last()">, </xsl:if> 
                        </xsl:for-each>
                    </div>
                </xsl:if>
            </body>
        </topic>
    </xsl:template>

    <xsl:template match="field">
        <topic>
            <id><xsl:value-of select="id"/></id>
            <title><xsl:value-of select="name"/></title>
            <index><xsl:value-of select="name"/> field</index>
            <synopsis><xsl:copy-of select="lead/node()"/></synopsis>
            <header>
                <xsl:call-template name="memberHeader">
                    <xsl:with-param name="member" select="."/>
                </xsl:call-template>
            </header>
            <body>
                <!-- Lengthier description -->
                <xsl:copy-of select="description/node()"/>
            </body>
        </topic>
    </xsl:template>

    <xsl:template match="*|node()">
        <!-- quiet -->
    </xsl:template>
    
    <xsl:template name="memberTable">
        <xsl:param name="members"/>
        <xsl:param name="title"/>
        
        <xsl:if test="$members">
            <h2><xsl:value-of select="$title"/></h2>
            <table class="members" cellspacing="1" cellpadding="0">
                    <xsl:apply-templates select="$members" mode="memberTableImpl">
						<xsl:sort case-order="lower-first" data-type="text" order="ascending" select="."/>
                    </xsl:apply-templates>
            </table>
        </xsl:if>
    </xsl:template>

	<xsl:template match="*" mode="memberTableImpl">
		<tr>
			<td width="1%">
				<nobr>
					<link ref="{id}">
						<xsl:call-template name="emitFlatSig">
							<xsl:with-param name="methodOrCtor" select="current()"/>
						</xsl:call-template>
					</link>
				</nobr>
			</td>
			<td><xsl:copy-of select="lead/node()"/><xsl:text> </xsl:text></td>
		</tr>
	</xsl:template>

    <xsl:template name="memberHeader">
        <xsl:param name="member"/>
        <xsl:variable name="parent" select="$member/.."/>

        <div class="memberHeader">

            <!-- The header announces the simple name of the member -->
            <h1>
                <xsl:if test="name()='package'">Package </xsl:if>
                <xsl:if test="name()='class'">Class </xsl:if>
                <xsl:if test="name()='interface'">Interface </xsl:if>
                <xsl:if test="name()='method'">Method </xsl:if>
                <xsl:if test="name()='constructor'">Constructor </xsl:if>
                <xsl:if test="name()='field'">Field </xsl:if>
                <xsl:value-of select="name"/>
            </h1>

            <xsl:if test="name()!='package'">
                <!-- Tell where this member lives -->
<!--
                <div class="memberOf">
                    Member of <link ref="{$parent/id}"><xsl:value-of select="$parent/name"/></link>
                </div>
-->            
                <!-- A signature for the member -->
                <code class="signature">
                    <xsl:call-template name="memberSig">
                        <xsl:with-param name="member" select="."/>
                    </xsl:call-template>
                </code>
            </xsl:if>
        </div>
        
    </xsl:template>
    
    <xsl:template name="memberSig">
        <xsl:param name="member"/>

<!--
        <xsl:if test="isPublic">public </xsl:if>
        <xsl:if test="isProtected">protected </xsl:if>
        <xsl:if test="isPrivate">private </xsl:if>
        <xsl:if test="isPackagePrivate">/*package*/ </xsl:if>
        <xsl:if test="isStatic">static </xsl:if>
        <xsl:if test="isFinal">final </xsl:if>
        <xsl:if test="isAbstract">abstract </xsl:if>
        <xsl:if test="isSynchronized">synchronized </xsl:if>
-->
        <xsl:if test="name()='class'">
            class 
            <b><xsl:value-of select="name"/></b>
<!--
            <xsl:if test="superclass[@ref != 'java.lang.Object']">
                <br/>derives from 
                <link ref="{superclass/@ref}"><xsl:value-of select="superclass"/></link>
            </xsl:if>
-->
<!--            
            <xsl:if test="superinterface">
                <br/>can be used as 
                <xsl:for-each select="superinterface">
                    <link ref="{@ref}"><xsl:value-of select="."/></link>
                    <xsl:if test="position() != last()">, </xsl:if>
                </xsl:for-each>
            </xsl:if>
-->
        </xsl:if>
<!--        
        <xsl:if test="name()='interface'">
            interface 
            <b><xsl:value-of select="name"/></b>
            <xsl:if test="superinterface">
                <br/>extends 
                <xsl:for-each select="superinterface">
                    <link ref="{@ref}"><xsl:value-of select="."/></link>
                    <xsl:if test="position() != last()">, </xsl:if>
                </xsl:for-each>
            </xsl:if>
        </xsl:if>
-->
        <xsl:if test="name()='field'">
            <xsl:if test="@ref">
                <link ref="{@ref}"><xsl:value-of select="type"/></link>
            </xsl:if>
            <xsl:if test="not(@ref)">
                <xsl:value-of select="type"/>
            </xsl:if>
            <xsl:text> </xsl:text>
            <b><xsl:value-of select="name"/></b>
        </xsl:if>

        <xsl:if test="name()='method' or name()='constructor'">
<!--        
            <xsl:if test="type/@ref">
                <link ref="{type/@ref}"><xsl:value-of select="type"/></link>
            </xsl:if>
            <xsl:if test="not(type/@ref)">
                <xsl:value-of select="type"/>
            </xsl:if>
-->
            <b><xsl:value-of select="name"/></b><xsl:text>(</xsl:text>
            <xsl:if test="params/param">
					<xsl:for-each select="params/param">
<!--						   
						<div style="margin-left: 2em">
						   <nobr>
							<xsl:if test="type/@ref">
								<link ref="{type/@ref}"><xsl:value-of select="type"/></link> 
							</xsl:if>
							<xsl:if test="not(type/@ref)">
								<xsl:value-of select="type"/> 
							</xsl:if>
							<xsl:text> </xsl:text>
-->
							<xsl:if test="starts-with(name, '_')">
								<xsl:value-of select="substring(name,2)"/><xsl:text>?</xsl:text>
							</xsl:if>
							<xsl:if test="not(starts-with(name, '_'))">
								<xsl:value-of select="name"/> 
							</xsl:if>
							<xsl:if test="position() != last()">, </xsl:if>
<!--							
							</nobr>
						</div>
-->
					</xsl:for-each>
            </xsl:if>
            <xsl:text>)</xsl:text>
            <xsl:if test="throws/throw">
                <br/>
                throws
                <xsl:for-each select="throws/throw">
                    <xsl:text> </xsl:text>
                    <xsl:if test="@ref">
                        <link ref="{@ref}"><xsl:value-of select="."/></link>
                    </xsl:if>
                    <xsl:if test="not(@ref)">
                        <xsl:value-of select="."/>
                    </xsl:if>
                    <xsl:if test="position() != last()">,</xsl:if>
                </xsl:for-each>
            </xsl:if>            
        </xsl:if>
    </xsl:template>

	<xsl:template name="emitFlatSig">
		<xsl:param name="methodOrCtor"/>
		<xsl:value-of select="$methodOrCtor/name"/>
		<xsl:text>(</xsl:text>
			<xsl:for-each select="$methodOrCtor/params/param">
				<xsl:call-template name="emitParamName">
					<xsl:with-param name="param" select="."/>
				</xsl:call-template>
                    <xsl:if test="position() != last()">, </xsl:if>
			</xsl:for-each>
		<xsl:text>)</xsl:text>
	</xsl:template>

	<xsl:template name="emitParamName">
		<xsl:param name="param"/>
		<xsl:if test="starts-with($param/name, '_')">
			<xsl:value-of select="substring($param/name,2)"/><xsl:text>?</xsl:text>
		</xsl:if>
		<xsl:if test="not(starts-with($param/name, '_'))">
			<xsl:value-of select="$param/name"/> 
		</xsl:if>
	</xsl:template>
    
</xsl:stylesheet>
