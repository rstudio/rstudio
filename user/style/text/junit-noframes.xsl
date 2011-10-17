<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns:lxslt="http://xml.apache.org/xslt"
        xmlns:stringutils="xalan://org.apache.tools.ant.util.StringUtils">
<xsl:output method="text" indent="no" encoding="UTF-8" />
<xsl:decimal-format decimal-separator="." grouping-separator="," />
<!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 -->

<xsl:param name="TITLE">JSR-303 TCK report</xsl:param>

<!--

 Modified from the default stylesheet.
 Changed so counts are relative to the TOTAL tests in the TCK

-->

<xsl:param name="markedFailing" />
<xsl:param name="markedNonTckTest" />
<xsl:param name="markedNotSupported" />
<xsl:param name="markedTestNotCompatible" />

<xsl:template match="testsuites">

	<xsl:variable name="rawTestCount" select="258"/>
    <!--
      rawTestCount from
      jar -xf jsr303-tck-1.0.3.GA-sources.jar
      grep -r \@Test org/hibernate/jsr303/tck/tests/| grep -v "enabled = false"  | wc -l
    -->
    <xsl:variable name="testCount" select="($rawTestCount - $markedNotSupported -$markedTestNotCompatible)"/>
    <xsl:variable name="testExecutedCount" select="sum(testsuite/@tests) - $markedNonTckTest"/>
    <xsl:variable name="errorCount" select="sum(testsuite/@errors)"/>
    <xsl:variable name="failureCount" select="sum(testsuite/@failures)"/>
    <xsl:variable name="timeCount" select="sum(testsuite/@time)"/>
    <xsl:variable name="passedCount" select="($testExecutedCount - $failureCount - $errorCount)"/>
    <xsl:variable name="successRate" select="($passedCount) div $testCount"/>
    <xsl:variable name="coveredCount" select="$testExecutedCount + $markedNotSupported + $markedTestNotCompatible"/>
    <xsl:if test="($failureCount + $errorCount) != ($markedFailing)">
      <xsl:text>WARNING expected Failures + Errors to match the </xsl:text>
      <xsl:value-of select="$markedFailing" />
      <xsl:text> test marked @Failing
</xsl:text>
    </xsl:if>
    <xsl:if test="($rawTestCount) != ($coveredCount)">
      <xsl:text>WARNING only </xsl:text>
      <xsl:value-of select="$coveredCount" /> <xsl:text> of </xsl:text>
      <xsl:value-of select="$rawTestCount"/> <xsl:text> (</xsl:text>
      <xsl:call-template name="display-percent">
         <xsl:with-param name="value" select="$coveredCount div $rawTestCount"/>
      </xsl:call-template>
      <xsl:text>) TCK Tests Covered.
</xsl:text>
    </xsl:if> 
    <xsl:value-of select="$passedCount" /> <xsl:text> of </xsl:text>
    <xsl:value-of select="$testCount"/> <xsl:text> (</xsl:text>
    <xsl:call-template name="display-percent">
         <xsl:with-param name="value" select="$successRate"/>
    </xsl:call-template>
    <xsl:text>) Pass with </xsl:text>
    <xsl:value-of select="$failureCount"/> <xsl:text> Failures and </xsl:text>
    <xsl:value-of select="$errorCount" /> <xsl:text> Errors.
</xsl:text>
 
</xsl:template>

<xsl:template name="display-percent">
    <xsl:param name="value"/>
    <xsl:value-of select="format-number($value,'0.00%')"/>
</xsl:template>

</xsl:stylesheet>
