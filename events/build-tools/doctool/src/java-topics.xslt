<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" />
    <xsl:strip-space elements="*"/>
    
    <xsl:param name="id"/>
    <xsl:param name="name"/>
    <xsl:param name="synopsis"/>
    <xsl:param name="childIntro"/>

    <xsl:key name="extendsInterface" match="//interface/superinterface" use="@ref"/>
    <xsl:key name="implementsInterface" match="//class/superinterface" use="@ref"/>
    <xsl:key name="extendsClass" match="//class/superclass" use="@ref"/>

    <xsl:template match="/">
        <topics>
           <xsl:apply-templates select="booklet"/>
        </topics>
    </xsl:template>

    <xsl:template match="booklet">
      <topic>
            <xsl:copy-of select="id"/>
            <title><xsl:value-of select="name"/></title>
           <location>
               <span class="selected item">
                  <xsl:value-of select="name"/>
               </span>
         </location>
            <synopsis><xsl:apply-templates select="lead/node()"/></synopsis>
            <header><h1><xsl:apply-templates select="name/node()"/></h1></header>
            <body>
                <!-- Lengthier description -->
                <xsl:apply-templates select="description/node()"/>

                <xsl:call-template name="memberTable">
                    <xsl:with-param name="members" select="package"/>
                    <xsl:with-param name="title">Packages</xsl:with-param>
                </xsl:call-template>
            </body>
            <xsl:apply-templates select="package">
                <xsl:sort select="name"/>
            </xsl:apply-templates>
      </topic>
    </xsl:template>

    <xsl:template name="emit-location">
       <xsl:if test="location">
           <location>
            <xsl:for-each select="location/link">
               <span class="item"><xsl:copy-of select="."/></span> &gt;
            </xsl:for-each>
            <span class="selected item">
                  <xsl:value-of select="name"/>
               </span>
           </location>
      </xsl:if>
    </xsl:template>

    <xsl:template match="package">
        <topic>
            <id><xsl:value-of select="id"/></id>
            <title><xsl:value-of select="name"/></title>
            <xsl:call-template name="emit-location"/>
            <index><xsl:value-of select="name"/> package</index>
            <xsl:if test="lead/node()">
                <synopsis><xsl:apply-templates select="lead/node()"/></synopsis>
            </xsl:if>
            <header>
                <xsl:call-template name="memberHeader">
                    <xsl:with-param name="member" select="."/>
                </xsl:call-template>
            </header>
            <body>
                <!-- Lengthier description -->
                <xsl:apply-templates select="description/node()"/>

                <xsl:call-template name="memberTable">
                    <xsl:with-param name="members" select="class"/>
                    <xsl:with-param name="title">Classes</xsl:with-param>
                </xsl:call-template>

                <xsl:call-template name="memberTable">
                    <xsl:with-param name="members" select="interface"/>
                    <xsl:with-param name="title">Interfaces</xsl:with-param>
                </xsl:call-template>
            </body>
            <xsl:apply-templates select="class">
                <xsl:sort select="name"/>
            </xsl:apply-templates>
            <xsl:apply-templates select="interface">
                <xsl:sort select="name"/>
            </xsl:apply-templates>
        </topic>        
    </xsl:template>

    <xsl:template match="class|interface">
        <topic>
            <xsl:copy-of select="id"/>
            <title><xsl:value-of select="name"/></title>
            <xsl:call-template name="emit-location"/>
            <index>
                <xsl:value-of select="name"/>
                <xsl:if test="name()='class'"> class</xsl:if>
                <xsl:if test="name()='interface'"> interface</xsl:if>
            </index>
            <synopsis><xsl:apply-templates select="lead/node()"/></synopsis>
            <header>
                <xsl:call-template name="memberHeader">
                    <xsl:with-param name="member" select="."/>
                </xsl:call-template>
            </header>
            <body>
                <!-- Lengthier description -->
                <xsl:apply-templates select="description/node()"/>
            
                <xsl:call-template name="memberTable">
                    <xsl:with-param name="members" select="class"/>
                    <xsl:with-param name="title">Nested Classes</xsl:with-param>
                </xsl:call-template>

                <xsl:call-template name="memberTable">
                    <xsl:with-param name="members" select="interface"/>
                    <xsl:with-param name="title">Nested Interfaces</xsl:with-param>
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
                    <h2 class="api">See Also</h2>
                     <xsl:for-each select="tags/link">
                           <xsl:copy-of select="."/>
                           <xsl:if test="position()!=last()">, </xsl:if> 
                     </xsl:for-each>
                </xsl:if>

               <xsl:for-each select="field[not(jre)]">
                   <xsl:sort select="name"/>
                   <xsl:if test="position() = 1">
                     <h2 class="api">Field Detail</h2>
                   </xsl:if>
                   <xsl:apply-templates select="."/>
                   <xsl:if test="position() != last()">
                     <hr/>
                   </xsl:if>
               </xsl:for-each>
   
               <xsl:for-each select="constructor[not(jre)]">
                   <xsl:sort select="name"/>
                   <xsl:if test="position() = 1">
                     <h2 class="api">Constructor Detail</h2>
                   </xsl:if>
                   <xsl:apply-templates select="."/>
                   <xsl:if test="position() != last()">
                     <hr/>
                   </xsl:if>
               </xsl:for-each>
   
               <xsl:for-each select="method[not(jre)]">
                   <xsl:sort select="name"/>
                   <xsl:if test="position() = 1">
                     <h2 class="api">Method Detail</h2>
                   </xsl:if>
                   <xsl:apply-templates select="."/>
                   <xsl:if test="position() != last()">
                     <hr/>
                   </xsl:if>
               </xsl:for-each>
                
            </body>

            <xsl:apply-templates select="interface">
                <xsl:sort select="name"/>
            </xsl:apply-templates>

            <xsl:apply-templates select="class">
                <xsl:sort select="name"/>
            </xsl:apply-templates>
        </topic>
    </xsl:template>

    <xsl:template match="method|constructor">
      <!-- Anchor -->
      <xsl:if test="string-length(substring-after(id, &quot;#&quot;)) != 0">
         <a name="{substring-after(id, &quot;#&quot;)}"/>
      </xsl:if>
      
      <!-- The simple name -->
      <h3 class="api"><xsl:value-of select="name"/></h3>    
    
      <!-- The signature -->
      <div class="memberSig">
         <xsl:call-template name="memberSig">
            <xsl:with-param name="member" select="."/>
         </xsl:call-template>
      </div>
   
      <!-- Lengthier description -->
      <xsl:apply-templates select="description/node()"/>

      <!-- Parameter description -->
      <xsl:if test="params/param">
         <h4 class="api">Parameters</h4>
         <dl class="memberDetail">
         <xsl:for-each select="params/param">
            <dt><xsl:value-of select="name"/></dt>
            <dd>
               <xsl:variable name="paramDesc" select="../../tags/param[name=current()/name]/description/node()"/>
               <xsl:apply-templates select="$paramDesc"/>
               <xsl:if test="not($paramDesc)"><xsl:comment>[Missing documentation]</xsl:comment></xsl:if>
            </dd>
         </xsl:for-each>
         </dl>
      </xsl:if>

      <!-- Return value description -->
      <xsl:if test="tags/return">
         <h4 class="api">Return Value</h4>
         <xsl:apply-templates select="tags/return/node()"/>
      </xsl:if>

      <!-- See also -->
      <xsl:if test="tags/link">
         <h4 class="api">See Also</h4>
         <xsl:for-each select="tags/link">
            <xsl:copy-of select="."/>
            <xsl:if test="position()!=last()">, </xsl:if> 
         </xsl:for-each>
      </xsl:if>
    </xsl:template>

    <xsl:template match="field">
      <!-- Anchor -->
      <xsl:if test="string-length(substring-after(id, &quot;#&quot;)) != 0">
         <a name="#{substring-after(id, &quot;#&quot;)}"/>
      </xsl:if>

      <!-- The simple name -->
      <h3 class="api"><xsl:value-of select="name"/></h3>    

      <!-- The signature -->
      <div class="memberSig">
         <xsl:call-template name="memberSig">
            <xsl:with-param name="member" select="."/>
         </xsl:call-template>
      </div>

      <!-- Lengthier description -->
      <xsl:apply-templates select="description/node()"/>
    </xsl:template>

    <xsl:template match="@*|node()">
      <xsl:copy>
         <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
    </xsl:template>

    <xsl:template name="memberTable">
        <xsl:param name="members"/>
        <xsl:param name="title"/>
        
        <xsl:if test="$members">
            <h2 class="api"><xsl:value-of select="$title"/></h2>
            <table cellspacing="1" cellpadding="1" class="members">
                    <xsl:apply-templates select="$members" mode="memberTableImpl">
                  <xsl:sort case-order="lower-first" data-type="text" order="ascending" select="name"/>
                    </xsl:apply-templates>
            </table>
        </xsl:if>
    </xsl:template>

   <xsl:template match="*" mode="memberTableImpl">
      <tr>
         <td>
            <xsl:choose>
               <xsl:when test="jre and (name()='method' or name()='constructor' or name()='field')">
                  <a href="{jre}"><xsl:value-of select="name"/><xsl:value-of select="flatSignature"/></a>
               </xsl:when>
               <xsl:otherwise>
                  <link ref="{id}"><xsl:value-of select="name"/><xsl:value-of select="flatSignature"/></link>
               </xsl:otherwise>
            </xsl:choose>
         </td>
         <td>
            <xsl:apply-templates select="lead/node()"/><xsl:text> </xsl:text>
         </td>
      </tr>
   </xsl:template>

    <xsl:template name="memberHeader">
        <xsl:param name="member"/>
        <xsl:variable name="parent" select="$member/.."/>

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
            <!-- A signature for the member -->
            <div class="classSig">
                <xsl:call-template name="memberSig">
                    <xsl:with-param name="member" select="."/>
                </xsl:call-template>
                
               <!-- Show the implementing classes and extending interfaces -->
               <xsl:if test="name()='interface'">
                  <xsl:variable name="id" select="id"/>
                  <xsl:for-each select="key('extendsInterface', $id)/..">
                     <xsl:if test="position() = 1">
                        <br/><br/>
                        <xsl:text>// Extended by </xsl:text>
                     </xsl:if>
                     <code><link ref="{id}"><xsl:value-of select="name"/></link></code>
                     <xsl:if test="position() != last()">, </xsl:if>
                  </xsl:for-each>

                  <xsl:for-each select="key('implementsInterface', $id)/..">
                     <xsl:if test="position() = 1">
                        <xsl:if test="not(key('extendsInterface', $id)/..)">
                           <!-- Extra break needed -->
                           <br/>
                        </xsl:if>
                        <br/>
                        <xsl:text>// Implemented by </xsl:text>
                     </xsl:if>
                     <code><link ref="{id}"><xsl:value-of select="name"/></link></code>
                     <xsl:if test="position() != last()">, </xsl:if>
                  </xsl:for-each>
               </xsl:if>

               <!-- Show the derived classes -->
               <xsl:if test="name()='class'">
                  <xsl:variable name="id" select="id"/>
                  <xsl:if test="name!='Object' and key('extendsClass', $id)">
                     <br/><br/>
                     <xsl:text>// Superclass of </xsl:text>
                     <xsl:for-each select="key('extendsClass', $id)/..">
                        <code><link ref="{id}"><xsl:value-of select="name"/></link></code>
                        <xsl:if test="position() != last()">, </xsl:if>
                     </xsl:for-each>
                  </xsl:if>
               </xsl:if>
            </div>
        </xsl:if>

    </xsl:template>
    
    <xsl:template name="memberSig">
        <xsl:param name="member"/>

        <xsl:if test="isPublic">public </xsl:if>
        <xsl:if test="isProtected">protected </xsl:if>
        <xsl:if test="isPrivate">private </xsl:if>
        <xsl:if test="isPackagePrivate">/*package*/ </xsl:if>
        <xsl:if test="isStatic">static </xsl:if>
        <xsl:if test="isFinal">final </xsl:if>
        <xsl:if test="isAbstract">abstract </xsl:if>
        <xsl:if test="isSynchronized">synchronized </xsl:if>

        <xsl:if test="name()='class'">
            class 
            <xsl:value-of select="name"/>
            <xsl:if test="superclass">
                <br/>extends
                <link ref="{superclass/@ref}"><xsl:value-of select="superclass"/></link>
            </xsl:if>
            <xsl:if test="superinterface">
                <br/>implements 
                <xsl:for-each select="superinterface">
                    <link ref="{@ref}"><xsl:value-of select="."/></link>
                    <xsl:if test="position() != last()">, </xsl:if>
                </xsl:for-each>
            </xsl:if>
        </xsl:if>

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
            <xsl:if test="type/@ref">
                <link ref="{type/@ref}"><xsl:value-of select="type"/></link>
            </xsl:if>
            <xsl:if test="not(type/@ref)">
                <xsl:value-of select="type"/>
            </xsl:if>
            <b><xsl:text> </xsl:text><xsl:value-of select="name"/></b><xsl:text>(</xsl:text>
            <xsl:if test="params/param">
               <xsl:for-each select="params/param">
                  <nobr>
                  <xsl:if test="type/@ref">
                     <link ref="{type/@ref}"><xsl:value-of select="type"/></link> 
                  </xsl:if>
                  <xsl:if test="not(type/@ref)">
                     <xsl:value-of select="type"/> 
                  </xsl:if>
                  <xsl:text> </xsl:text>
                  <xsl:value-of select="name"/> 
                  <xsl:if test="position() != last()">, </xsl:if>
                  </nobr>
               </xsl:for-each>
            </xsl:if><xsl:text>)</xsl:text>
            <xsl:if test="throws/throw">
               <br/>&#160;&#160;&#160;&#160;
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
</xsl:stylesheet>
