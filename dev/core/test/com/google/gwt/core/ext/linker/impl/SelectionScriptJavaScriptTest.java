/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.util.tools.Utility;

import com.gargoylesoftware.htmlunit.AlertHandler;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.MockWebConnection;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tests the JavaScript code in the selection script test using HtmlUnit.
 */
public class SelectionScriptJavaScriptTest extends TestCase {
  private static String TEST_MODULE_NAME = "test.Module";

  /**
   * Return script code that includes the definition of computeScriptBase(),
   * most of the variables it needs to run, and a call to the function. It
   * assumes that the metaProps variable is already in scope.
   */
  private static String loadComputeScriptBase() throws IOException {
    StringBuffer code = new StringBuffer();
    code.append("var base = \"\", $doc=document;\n");
    code.append("__gwt_getMetaProperty = function(name) { "
                 + "var value = metaProps[name];"
                 + "return (value == null) ? null : value; }");
    code.append(Utility.getFileFromClassPath(SelectionScriptLinker.COMPUTE_SCRIPT_BASE_JS));
    code.append("computeScriptBase();\n");
    return code.toString();
  }

  /**
   * Return script code that includes the definition of processMetas(), all
   * variables it needs to run, and a call to the function.
   */
  private static String loadProcessMetas() throws IOException {
    StringBuffer code = new StringBuffer();
    code.append("var metaProps = { }, propertyErrorFunc, onLoadErrorFunc;\n");
    code.append(Utility.getFileFromClassPath(SelectionScriptLinker.PROCESS_METAS_JS));
    code.append("processMetas();\n");
    return code.toString().replaceAll("__MODULE_NAME__", TEST_MODULE_NAME);
  }

  /**
   * Test a meta tag specifying a base for this module
   */
  public void testModuleSpecificMetas1() throws IOException {
    StringBuffer metas = new StringBuffer();
    metas.append("<meta name=\"" + TEST_MODULE_NAME
        + "::gwt:property\" content=\"baseUrl=http://new/base\">\n");

    StringBuffer testCode = new StringBuffer();
    testCode.append(loadProcessMetas());
    testCode.append("alert('baseUrl='+metaProps['baseUrl']);\n");

    List<String> alerts = loadPage(makeHostPage(metas), testCode);

    assertEquals(1, alerts.size());
    assertEquals("baseUrl=http://new/base", alerts.get(0));
  }

  /**
   * Test a meta tag specifying a base for a different module.
   */
  public void testModuleSpecificMetas2() throws IOException {
    StringBuffer metas = new StringBuffer();
    metas.append("<meta name=\"some.other.module::gwt:property\" content=\"baseUrl=http://new/base\">\n");

    StringBuffer testCode = new StringBuffer();
    testCode.append(loadProcessMetas());
    testCode.append("alert('baseUrl='+metaProps['baseUrl']);\n");

    List<String> alerts = loadPage(makeHostPage(metas), testCode);

    assertEquals(1, alerts.size());
    assertEquals("baseUrl=undefined", alerts.get(0));
  }

  /**
   * Test that all the meta tags are extracted.
   */
  public void testProcessMetas() throws FailingHttpStatusCodeException,
      MalformedURLException, IOException {
    StringBuffer metas = new StringBuffer();
    metas.append("<meta name=\"gwt:property\" content=\"baseUrl=http://new/base\">\n");
    metas.append("<meta name=\"gwt:property\" content=\"novalue\">\n");
    metas.append("<meta name=\"gwt:onPropertyErrorFn\" content=\"function() { alert('custom prop error called');}\"}>\n");
    metas.append("<meta name=\"gwt:onLoadErrorFn\" content=\"function() { alert('custom onLoad error called');}\">\n");

    StringBuffer testCode = new StringBuffer();
    testCode.append(loadProcessMetas());
    testCode.append("alert('baseUrl='+metaProps['baseUrl']);\n");
    testCode.append("alert('novalue='+metaProps['novalue']);\n");
    testCode.append("alert('absent='+metaProps['absent']);\n");
    testCode.append("propertyErrorFunc();\n");
    testCode.append("onLoadErrorFunc();\n");

    List<String> alerts = loadPage(makeHostPage(metas), testCode);

    assertEquals(5, alerts.size());
    Iterator<String> alertsIter = alerts.iterator();
    assertEquals("baseUrl=http://new/base", alertsIter.next());
    assertEquals("novalue=", alertsIter.next());
    assertEquals("absent=undefined", alertsIter.next());
    assertEquals("custom prop error called", alertsIter.next());
    assertEquals("custom onLoad error called", alertsIter.next());
  }

  /**
   * Test the default href
   */
  public void testDefault() throws IOException {
    StringBuffer testCode = new StringBuffer();
    testCode.append("var metaProps = { };\n");
    testCode.append("function isBodyLoaded() { return true; }\n");
    testCode.append(loadComputeScriptBase());
    testCode.append("alert('base='+base);\n");

    List<String> alerts = loadPage(makeHostPage(""), testCode);

    assertEquals(1, alerts.size());
    assertEquals("base=http://foo.test/", alerts.get(0));
  }

  /**
   * Test the script base with no meta properties.
   */
  public void testScriptBase() throws IOException {
    StringBuffer testCode = new StringBuffer();
    testCode.append("var metaProps = { };\n");
    testCode.append("function isBodyLoaded() { return false; }\n");
    testCode.append(loadComputeScriptBase());
    testCode.append("alert('base='+base);\n");

    List<String> alerts = loadPage(makeHostPage(""), testCode);

    assertEquals(1, alerts.size());
    assertEquals("base=http://foo.test/foo/", alerts.get(0));
  }

  /**
   * Test getting the base URL from a meta property with an absolute URL
   */
  public void testScriptBaseFromMetas() throws IOException {
    StringBuffer testCode = new StringBuffer();
    testCode.append("var metaProps = { baseUrl : \"http://static.test/fromMetaTag/\" };\n");
    testCode.append(loadComputeScriptBase());
    testCode.append("alert('base='+base);\n");

    List<String> alerts = loadPage(makeHostPage(""), testCode);

    assertEquals(1, alerts.size());
    assertEquals("base=http://static.test/fromMetaTag/", alerts.get(0));
  }

  /**
   * Test getting the base URL from a meta property with a relative URL
   */
  public void testRelativeScriptBaseFromMetas() throws IOException {
    StringBuffer testCode = new StringBuffer();
    testCode.append("var metaProps = { baseUrl : \"fromMeta/tag/\" };\n");
    testCode.append(loadComputeScriptBase());
    testCode.append("alert('base='+base);\n");

    List<String> alerts = loadPage(makeHostPage(""), testCode);

    assertEquals(1, alerts.size());
    assertEquals("base=http://foo.test/fromMeta/tag/", alerts.get(0));
  }

  /**
   * Test the script base logic for an inlined selection script.
   */
  public void testScriptBaseForInlined() throws IOException {
    StringBuffer hostPage = new StringBuffer();
    hostPage.append("<html><head>\n");
    hostPage.append("<script lang=\"javascript\"><!--\n");
    hostPage.append("var metaProps = { }\n");
    hostPage.append(loadComputeScriptBase());
    hostPage.append("alert('base='+base);\n");
    hostPage.append("--></script>\n");

    List<String> alerts = loadPage(hostPage.toString(), "");

    assertEquals(1, alerts.size());
    assertEquals("base=http://foo.test/", alerts.get(0));
  }

  /**
   * Test getting a the base URL from the HTML base tag
   */
  public void testScriptBaseFromBaseTag() throws IOException {
    StringBuffer hostPage = new StringBuffer();
    hostPage.append("<html><head>\n");
    hostPage.append("<base href=\"http://static.test/fromBaseTag/\">\n");
    hostPage.append("<script lang=\"javascript\"><!--\n");
    hostPage.append("var metaProps = { }\n");
    hostPage.append(loadComputeScriptBase());
    hostPage.append("alert('base='+base);\n");
    hostPage.append("--></script>\n");

    List<String> alerts = loadPage(hostPage.toString(), "");

    assertEquals(1, alerts.size());
    assertEquals("base=http://static.test/fromBaseTag/", alerts.get(0));
  }

  /**
   * Test the script base logic for an inlined selection script.
   */
  public void testNocacheJsTag() throws IOException {
    StringBuffer hostPage = new StringBuffer();
    hostPage.append("<html><head>\n");
    hostPage.append("<script lang='javascript' type='application/javascript' ");
    hostPage.append("src='from/nocache/__MODULE_NAME__.nocache.js'></script>\n");
    hostPage.append("<script lang=\"javascript\"><!--\n");
    hostPage.append("var metaProps = { }\n");
    hostPage.append(loadComputeScriptBase());
    hostPage.append("alert('base='+base);\n");
    hostPage.append("--></script>\n");

    List<String> alerts = loadPage(hostPage.toString(), "");

    assertEquals(1, alerts.size());
    assertEquals("base=http://foo.test/from/nocache/", alerts.get(0));
  }

  /**
   * Load a page and return all alerts that it generates.
   */
  private List<String> loadPage(String hostPage, CharSequence testScript)
      throws FailingHttpStatusCodeException, MalformedURLException, IOException {
    WebClient webClient = new WebClient();

    // Make a mock web connection that can return the host page and the test
    // script
    MockWebConnection webConnection = new MockWebConnection();
    webConnection.setDefaultResponse(hostPage, "text/html");
    webConnection.setResponse(new URL(
        "http://foo.test/foo/test.Module.nocache.js"), testScript.toString(),
        "application/javascript");
    webClient.setWebConnection(webConnection);

    final List<String> alerts = new ArrayList<String>();
    webClient.setAlertHandler(new AlertHandler() {
      @Override
      public void handleAlert(Page page, String msg) {
        alerts.add(msg);
      }
    });

    webClient.getPage("http://foo.test/");
    return alerts;
  }

  private String makeHostPage(CharSequence metas) {
    StringBuffer buf = new StringBuffer();
    buf.append("<html><head>\n");
    buf.append(metas);
    buf.append("<script src=\"/foo/test.Module.nocache.js\">\n");
    buf.append("</head></html>");
    return buf.toString();
  }
}
