<?xml version="1.0"?>
<!-- edited with XMLSpy v2005 rel. 3 U (http://www.altova.com) by Bruce Johnson (private) -->
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
        <div class="header item">
          <a href="http://code.google.com/webtoolkit/">Google Web Toolkit</a>
        </div>
        <div class="group">
          <div class="item">
            <a href="http://code.google.com/webtoolkit/overview.html">
              Product Overview
            </a>
          </div>
          <div class="item">
            <a href="http://code.google.com/webtoolkit/gettingstarted.html">
              Getting Started Guide
            </a>
          </div>
          <div class="item">
            <a href="http://code.google.com/webtoolkit/download.html">
              Download SDK
            </a>
          </div>
        </div>
        <div class="group">
          <xsl:element name="div">
            <xsl:attribute name="class">
              <xsl:text>item</xsl:text>
              <xsl:if test="contains(id, 'com.google.gwt.doc')">
                <xsl:text></xsl:text>
                <xsl:text>selected</xsl:text>
              </xsl:if>
            </xsl:attribute>
            <a href="./com.google.gwt.doc.DeveloperGuide.html">
              Developer Guide
            </a>
          </xsl:element>
          <div class="group">
            <div class="item">
              <a
                href="http://code.google.com/webtoolkit/documentation/examples/">
                Example Projects
              </a>
            </div>
            <xsl:element name="div">
              <xsl:attribute name="class">
                <xsl:text>item</xsl:text>
                <xsl:if
                  test="(contains(id, 'com.google.gwt.') and not(contains(id, 'com.google.gwt.doc.'))) or id = 'gwt'">
                  <xsl:text></xsl:text>
                  <xsl:text>selected</xsl:text>
                </xsl:if>
              </xsl:attribute>
              <a href="./gwt.html">GWT Class Reference</a>
            </xsl:element>
            <xsl:element name="div">
              <xsl:attribute name="class">
                <xsl:text>item</xsl:text>
                <xsl:if test="contains(id, 'java.') or id='jre'">
                  <xsl:text></xsl:text>
                  <xsl:text>selected</xsl:text>
                </xsl:if>
              </xsl:attribute>
              <a href="./jre.html">JRE Emulation Library</a>
            </xsl:element>
          </div>
        </div>
        <div class="group">
          <div class="item">
            <a href="http://code.google.com/webtoolkit/faq.html">
              Web Toolkit FAQ
            </a>
          </div>
          <div class="item">
            <a href="http://googlewebtoolkit.blogspot.com/">Web Toolkit Blog</a>
          </div>
          <div class="item">
            <a href="http://code.google.com/webtoolkit/thirdparty.html">
              Third Party Tools
            </a>
          </div>
          <div class="item">
            <a href="http://code.google.com/webtoolkit/issues/">
              Issue Tracking
            </a>
          </div>
          <div class="item">
            <a href="http://groups.google.com/group/Google-Web-Toolkit">
              Developer Forum
            </a>
          </div>
        </div>
      </div>
      <div id="search">
        <form action="http://www.google.com/search" method="get">
          <div>
            <input name="domains" value="code.google.com" type="hidden" />
            <input name="sitesearch" value="code.google.com" type="hidden" />
            <div class="header">Search this site:</div>
            <div class="input">
              <input name="q" size="10" />
            </div>
            <div class="button">
              <input value="Search" type="submit" />
            </div>
          </div>
        </form>
      </div>
    </div>
  </xsl:template>
  <xsl:template name="gen-header">
    <div id="gaia">&#160;</div>
    <div id="header">
      <div id="logo">
        <a href="http://code.google.com/">
          <img src="http://code.google.com/images/code_sm.png" alt="Google" />
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
          <a href="http://code.google.com/">Google Code Home</a>
        </span>
        &gt;
        <span class="item">
          <a href="http://code.google.com/webtoolkit/">Google Web Toolkit</a>
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
          <xsl:with-param name="start" select="$start/topic[position()=last()]" />
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
        <meta http-equiv="content-type" content="text/html; charset=utf-8" />
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
            <ul>
              <xsl:for-each select="topic">
                <li>
                  <div class="heading">
                    <xsl:call-template name="makeLink">
                      <xsl:with-param name="linkText" select="title/node()" />
                      <xsl:with-param name="linkTarget" select="." />
                    </xsl:call-template>
                  </div>
                  <xsl:if test="synopsis">
                    <div>
                      <xsl:apply-templates select="synopsis" />
                    </div>
                  </xsl:if>
                </li>
              </xsl:for-each>
            </ul>
          </xsl:if>
          <!-- See also links -->
          <xsl:if test="seeAlso/link">
            <div class="topicSeeAlso">
              <h2>Related topics</h2>
              <xsl:for-each select="seeAlso/link">
                <xsl:apply-templates select="." />
                <xsl:if test="position()!=last()">,</xsl:if>
              </xsl:for-each>
            </div>
          </xsl:if>
        </div>

        <div id="footer">
          &#169;2006 Google
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
            <a href="http://code.google.com/policies.html#restrictions">
              <xsl:text>noted</xsl:text>
            </a>
            <xsl:text>, the content of this &#32;</xsl:text>

            <xsl:text>page is licensed under the &#32;</xsl:text>
            <a rel="license"
              href="http://creativecommons.org/licenses/by/2.5/">
              <xsl:text>Creative Commons Attribution 2.5 License</xsl:text>
            </a>
            <xsl:text>.</xsl:text>
            <xsl:comment>
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
                  <requires rdf:resource="http://web.resource.org/cc/Notice" />
                  <requires
                    rdf:resource="http://web.resource.org/cc/Attribution" />
                  <permits
                    rdf:resource="http://web.resource.org/cc/DerivativeWorks" />
                </License>
              </rdf:RDF>
            </xsl:comment>
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
      <xsl:with-param name="linkTarget" select="key('topicSearch', $linkBase)" />
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
