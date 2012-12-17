<?xml version="1.0"?>

<!--
Copyright 2007 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
-->
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html" />
  <xsl:strip-space elements="*" />
  <xsl:key name="topicSearch" match="//topic" use="id" />
  <xsl:param name="css" />
  <xsl:param name="title" />
  <xsl:template match="/">
    @FILE
    <xsl:apply-templates select="//topic" />
  </xsl:template>
  <xsl:template name="gen-side">
    <div id="side">

      <div id="menu">

        <h4>
          <a href="http://code.google.com/webtoolkit/">
            <xsl:text>Google Web Toolkit</xsl:text>
          </a>
        </h4>

        <ul>
          <li>
            <a href="http://code.google.com/webtoolkit/download.html">
              <xsl:text>Download GWT</xsl:text>
            </a>
          </li>
        </ul>

        <!-- Section: Product Overview -->
        <ul>
          <li>
            <a href="http://code.google.com/webtoolkit/overview.html">
              <xsl:text>Product Overview</xsl:text>
            </a>
          </li>
          <li>
            <a
              href="./gettingstarted.html">
              <xsl:text>Getting Started Guide</xsl:text>
            </a>
          </li>
          <li>
            <a
              href="http://code.google.com/webtoolkit/documentation/examples/">
              <xsl:text>Example Projects</xsl:text>
            </a>
          </li>
        </ul>

        <!-- Section: Developer Guide -->
        <ul>

          <!-- If the page is inside the dev guide, show the link as selected -->
          <li>
            <xsl:element name="a">
              <xsl:if test="contains(id, 'com.google.gwt.doc')">
                <xsl:attribute name="class">
                  <xsl:text>selected</xsl:text>
                </xsl:attribute>
              </xsl:if>
              <xsl:variable name="href">
                ./com.google.gwt.doc.DeveloperGuide.html
              </xsl:variable>
              <xsl:attribute name="href">
                <xsl:value-of select="normalize-space($href)" />
              </xsl:attribute>
              <xsl:text>Developer Guide</xsl:text>
            </xsl:element>
          </li>

          <!-- If the page is inside the GWT class ref, show the link as selected -->
          <li>
            <xsl:element name="a">
              <xsl:if
                test="not(contains(id, 'com.google.gwt.doc.')) and (contains(id, 'com.google.gwt.') or contains(id, 'java.') or id = 'gwt' or id = 'jre')">
                <xsl:attribute name="class">
                  <xsl:text>selected</xsl:text>
                </xsl:attribute>
              </xsl:if>
              <xsl:variable name="href">./gwt.html</xsl:variable>
              <xsl:attribute name="href">
                <xsl:value-of select="normalize-space($href)" />
              </xsl:attribute>
              <xsl:text>Class Reference</xsl:text>
            </xsl:element>
          </li>
          <li>
            <a href="http://code.google.com/webtoolkit/issues/">
              <xsl:text>Issue Tracking</xsl:text>
            </a>
          </li>
          <li>
            <a
              href="http://groups.google.com/group/Google-Web-Toolkit">
              <xsl:text>Developer Forum</xsl:text>
            </a>
          </li>
        </ul>

        <!-- Section: Important uncategorized links -->
        <ul>
          <li>
            <a href="http://googlewebtoolkit.blogspot.com/">
              <xsl:text>GWT Blog</xsl:text>
            </a>
          </li>
          <li>
            <a href="http://code.google.com/webtoolkit/faq.html">
              <xsl:text>GWT FAQ</xsl:text>
            </a>
          </li>
          <li>
            <a
              href="http://code.google.com/webtoolkit/makinggwtbetter.html">
              <xsl:text>Making GWT Better</xsl:text>
            </a>
          </li>
        </ul>

        <ul>
          <li>
            <a
              href="http://code.google.com/webtoolkit/thirdparty.html">
              <xsl:text>Third Party Tools</xsl:text>
            </a>
          </li>
        </ul>

      </div>

      <div id="search">
        <form id="searchbox_015986126177484454297:pfmwlvdl42y" action="http://www.google.com/cse">
          <input type="hidden" name="cx" value="015986126177484454297:pfmwlvdl42y" />
          <input type="hidden" name="cof" value="FORID:0" />
          <div class="header">Search Google Code:</div>
          <input name="q" type="text" size="20" />
          <input type="submit" name="sa" value="Search" /><br/>
        </form>
        <script type="text/javascript" src="http://google.com/coop/cse/brand?form=searchbox_015986126177484454297:pfmwlvdl42y"></script>
      </div>
      
    </div>
  </xsl:template>

  <xsl:template name="gen-header">
    <div id="gaia">&#160;</div>
    <div id="header">
      <div id="logo">
        <a href="http://code.google.com/">
          <img src="http://code.google.com/images/code_sm.png"
            alt="Google" />
        </a>
      </div>
      <!-- The title for the entire docset -->
      <div id="title">
        <xsl:value-of select="$title" />
      </div>
      <div id="breadcrumbs">
        <div id="nextprev">
          <xsl:call-template name="emitPrevTopic">
            <xsl:with-param name="start" select="." />
          </xsl:call-template>
          <xsl:text></xsl:text>
          <xsl:call-template name="emitNextTopic">
            <xsl:with-param name="start" select="." />
          </xsl:call-template>
        </div>
        <span class="item">
          <a href="http://code.google.com/">
            <xsl:text>Google Code Home</xsl:text>
          </a>
        </span>
        &gt;
        <span class="item">
          <a href="http://code.google.com/webtoolkit/">
            <xsl:text>Google Web Toolkit</xsl:text>
          </a>
        </span>
        &gt;
        <!-- The topic location -->
        <xsl:if test="location">
          <xsl:apply-templates select="location/@*|location/node()" />
        </xsl:if>
      </div>
    </div>
  </xsl:template>

  <xsl:template name="emitPrevTopic">
    <xsl:param name="start" />
    <xsl:variable name="prev"
      select="$start/preceding-sibling::topic[position()=1]" />
    <xsl:variable name="parentLastChild" select="$prev" />
    <xsl:variable name="parent" select="$start/parent::topic" />
    <xsl:choose>
      <!-- if there's a previous sibling, use "last deepest child" algorithm -->
      <xsl:when test="$prev/topic">
        <xsl:call-template name="emitDeepestPriorChild">
          <xsl:with-param name="start" select="$prev" />
        </xsl:call-template>
      </xsl:when>
      <!-- if there's a previous sibling without a child, use it -->
      <xsl:when test="$prev">
        <nobr>
          <a href="{$prev/id}.html">&#171; prev</a>
        </nobr>
      </xsl:when>
      <!-- if there's a parent, use it -->
      <xsl:when test="$parent">
        <nobr>
          <a href="{$parent/id}.html">&#171; prev</a>
        </nobr>
      </xsl:when>
      <xsl:otherwise>
        <nobr style="visibility:hidden">&#171; prev</nobr>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="emitDeepestPriorChild">
    <xsl:param name="start" />
    <xsl:choose>
      <xsl:when test="$start/topic">
        <xsl:call-template name="emitDeepestPriorChild">
          <xsl:with-param name="start"
            select="$start/topic[position()=last()]" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <nobr>
          <a href="{$start/id}.html">&#171; prev</a>
        </nobr>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="emitNextTopic">
    <xsl:param name="start" />
    <xsl:variable name="child" select="$start/topic[position()=1]" />
    <xsl:variable name="next"
      select="$start/following-sibling::topic[position()=1]" />
    <xsl:variable name="parentNext"
      select="$start/ancestor::topic[following-sibling::topic]/following-sibling::topic" />
    <xsl:choose>
      <!-- if there's a first child, use it -->
      <xsl:when test="$child">
        <nobr>
          <a href="{$child/id}.html">next &#187;</a>
        </nobr>
      </xsl:when>
      <!-- if there's a next sibling, use it -->
      <xsl:when test="$next">
        <nobr>
          <a href="{$next/id}.html">next &#187;</a>
        </nobr>
      </xsl:when>
      <!-- find the first parent that has a next sibling -->
      <xsl:when test="$parentNext">
        <nobr>
          <a href="{$parentNext/id}.html">next &#187;</a>
        </nobr>
      </xsl:when>
      <xsl:otherwise>
        <nobr style="visibility:hidden">next &#187;</nobr>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Only topics that have titles get pulled in -->
  <xsl:template match="topic[title]">
    <xsl:variable name="filedelim">
      <xsl:text>&#10;</xsl:text>
      <xsl:text>@FILE &#32;</xsl:text>
      <xsl:value-of select="normalize-space(id)" />
      <xsl:text>.html</xsl:text>
      <xsl:text>&#10;</xsl:text>
    </xsl:variable>
    <xsl:value-of select="$filedelim" />
    <html xmlns="http://www.w3.org/1999/xhtml">
      <head>
        <title>
          Google Web Toolkit
          <xsl:if test="title">
            -
            <xsl:value-of select="title" />
          </xsl:if>
        </title>
        <meta http-equiv="content-type"
          content="text/html; charset=utf-8" />
        <link href="../css/base.css" rel="stylesheet" type="text/css" />
        <link href="./doc.css" rel="stylesheet" type="text/css" />
        <link href="../css/print.css" rel="stylesheet" media="print"
          type="text/css" />
      </head>
      <body>
        <xsl:call-template name="gen-header" />
        <xsl:call-template name="gen-side" />
        <div id="body">
          <!-- If there is a header, prefer it to the title -->
          <xsl:choose>
            <xsl:when test="header">
              <xsl:apply-templates select="header/@*|header/node()" />
            </xsl:when>
            <xsl:otherwise>
              <!-- The title for this topic -->
              <h1>
                <xsl:value-of select="title" />
              </h1>
            </xsl:otherwise>
          </xsl:choose>
          <!-- The topic body -->
          <xsl:if test="body">
            <xsl:apply-templates select="body/@*|body/node()" />
          </xsl:if>
          <!-- Tips -->
          <xsl:for-each select="tip">
            <div class="tipContainer">
              <div class="tipCallout">Tip</div>
              <div class="tipBody">
                <xsl:apply-templates select="node()" />
              </div>
            </div>
          </xsl:for-each>
          
          <!-- Links to child topics -->
		  <xsl:if test="childIntro">
		    <xsl:if test="childIntro/text()">
		      <h2>
		        <xsl:value-of select="childIntro/text()" />
		      </h2>
		    </xsl:if>
		    
		    <xsl:if test="topic">
		    	<ul class="childToc">
		            <xsl:for-each select="topic">
		            	<li>
		            	
				          <div class="heading">
				            <xsl:call-template name="makeLink">
				              <xsl:with-param name="linkText"
				                select="title/node()" />
				              <xsl:with-param name="linkTarget" select="." />
				            </xsl:call-template>
				          </div>
				          
				          <xsl:if test="synopsis">
				            <div class="synopsis">
				              <xsl:apply-templates select="synopsis/node()" />
				            </div>
				          </xsl:if>

				          <xsl:if test="topic">
				            <ul>
			                  <xsl:for-each select="topic">
			                  	<li>
						            <xsl:call-template name="makeLink">
						              <xsl:with-param name="linkText"
						                select="title/node()" />
						              <xsl:with-param name="linkTarget" select="." />
						            </xsl:call-template>
						            <xsl:if test="position() != last()">,</xsl:if>
						      	</li>
			  	              </xsl:for-each>
				            </ul>
				          </xsl:if>

						</li>
			          
		            </xsl:for-each>
				</ul>
			</xsl:if>
		  </xsl:if>
          
          <!-- See also links -->
          <xsl:if test="seeAlso/link">
            <div class="topicSeeAlso">
              <h2>Related topics</h2>
              <xsl:for-each select="seeAlso/link">
                <xsl:apply-templates select="." />
                <xsl:if test="position()!=last()">, </xsl:if>
              </xsl:for-each>
            </div>
          </xsl:if>
        </div>

        <div id="footer">
          &#169;2007 Google
          <span class="noprint">
            -
            <a href="http://www.google.com/">
              <xsl:text>Google Home</xsl:text>
            </a>
            -
            <a href="http://www.google.com/jobs/">
              <xsl:text>We're Hiring</xsl:text>
            </a>
            -
            <a href="http://www.google.com/privacy.html">
              <xsl:text>Privacy Policy</xsl:text>
            </a>
            -
            <a href="http://www.google.com/terms_of_service.html">
              <xsl:text>Terms of Service</xsl:text>
            </a>
            -
            <a href="mailto:code@google.com">
              <xsl:text>Contact Us</xsl:text>
            </a>
          </span>

          <div id="license"
            style="text-align: center; margin: 1em 0em 1em 0em">
            Except as otherwise
            <a
              href="http://code.google.com/policies.html#restrictions">
              <xsl:text>noted</xsl:text>
            </a>
            <xsl:text>, the content of this &#32;</xsl:text>

            <xsl:text>page is licensed under the &#32;</xsl:text>
            <a rel="license"
              href="http://creativecommons.org/licenses/by/2.5/">
              <xsl:variable name="cclicense">
                Creative Commons Attribution 2.5 License
              </xsl:variable>
              <xsl:value-of select="normalize-space($cclicense)" />
            </a>
            <xsl:text>.</xsl:text>
            <xsl:text disable-output-escaping="yes">
              <![CDATA[<!--]]>
            </xsl:text>
            <rdf:RDF xmlns="http://web.resource.org/cc/"
              xmlns:dc="http://purl.org/dc/elements/1.1/"
              xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <Work rdf:about="">
                <license
                  rdf:resource="http://creativecommons.org/licenses/by/2.5/" />
              </Work>
              <License
                rdf:about="http://creativecommons.org/licenses/by/2.5/">
                <permits
                  rdf:resource="http://web.resource.org/cc/Reproduction" />
                <permits
                  rdf:resource="http://web.resource.org/cc/Distribution" />
                <requires
                  rdf:resource="http://web.resource.org/cc/Notice" />
                <requires
                  rdf:resource="http://web.resource.org/cc/Attribution" />
                <permits
                  rdf:resource="http://web.resource.org/cc/DerivativeWorks" />
              </License>
            </rdf:RDF>
            <xsl:text disable-output-escaping="yes">
              <![CDATA[-->]]>
            </xsl:text>
          </div>
        </div>

        <!-- analytics -->
        <script src="https://ssl.google-analytics.com/urchin.js"
          type="text/javascript" />
        <script type="text/javascript">
          _uacct="UA-18071-1"; _uanchor=1; urchinTracker();
        </script>
      </body>
    </html>
  </xsl:template>
  
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="link">
    <xsl:variable name="testLinkBase"
      select="substring-before(@ref, &quot;#&quot;)" />
    <xsl:variable name="linkBase">
      <xsl:if test="string-length($testLinkBase) != 0">
        <xsl:value-of select="$testLinkBase" />
      </xsl:if>
      <xsl:if test="string-length($testLinkBase) = 0">
        <xsl:value-of select="@ref" />
      </xsl:if>
    </xsl:variable>
    <xsl:call-template name="makeLink">
      <xsl:with-param name="linkRef" select="@ref" />
      <xsl:with-param name="linkText" select="node()" />
      <xsl:with-param name="linkTarget"
        select="key('topicSearch', $linkBase)" />
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="makeLink">
    <!-- This only becomes a hyperlink if the cross-ref can be resolved -->
    <xsl:param name="linkRef" />
    <xsl:param name="linkText" />
    <xsl:param name="linkTarget" />
    <xsl:choose>
      <xsl:when test="$linkTarget">
        <xsl:variable name="extra"
          select="substring-after($linkRef, &quot;#&quot;)" />
        <xsl:element name="a">
          <xsl:attribute name="href">
            <xsl:value-of select="$linkTarget/id" />
            <xsl:text>.html</xsl:text>
            <xsl:if test="string-length($extra) != 0">
              <xsl:text>#</xsl:text>
              <xsl:value-of select="$extra" />
            </xsl:if>
          </xsl:attribute>
          <xsl:apply-templates select="$linkText" />
        </xsl:element>
      </xsl:when>
      <xsl:otherwise>
        <!-- plain text -->
        <xsl:apply-templates select="$linkText" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
