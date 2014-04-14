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

import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.ScriptReference;
import com.google.gwt.core.ext.linker.StylesheetReference;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utility class to help linkers do resource injection.
 */
public class ResourceInjectionUtil {
  /**
   * Installs stylesheets and scripts.
   */
  public static StringBuffer injectResources(StringBuffer selectionScript,
      ArtifactSet artifacts) {
    // Add external dependencies
    int startPos = selectionScript.indexOf("// __MODULE_STYLES_END__");
    if (startPos != -1) {
      for (StylesheetReference resource : artifacts.find(StylesheetReference.class)) {
        String text = generateStylesheetInjector(resource.getSrc());
        selectionScript.insert(startPos, text);
        startPos += text.length();
      }
    }

    startPos = selectionScript.indexOf("// __MODULE_SCRIPTS_END__");
    if (startPos != -1) {
      for (ScriptReference resource : artifacts.find(ScriptReference.class)) {
        String text = generateScriptInjector(resource.getSrc());
        selectionScript.insert(startPos, text);
        startPos += text.length();
      }
    }
    return selectionScript;
  }

  /**
   * Installs stylesheets using the installOneStylesheet method, which is
   * assumed to be defined on the page.  The installOneStylesheet()
   * helper function is invoked as follows:
   *
   * <pre>
   * installOneStylesheet(URL);
   * </pre>
   */
  public static StringBuffer injectStylesheets(StringBuffer selectionScript,
      ArtifactSet artifacts) {
    int startPos = selectionScript.indexOf("// __MODULE_STYLES__");
    if (startPos != -1) {
      for (StylesheetReference resource : artifacts.find(StylesheetReference.class)) {
        String text = "installOneStylesheet('" + resource.getSrc() + "');\n";
        selectionScript.insert(startPos, text);
        startPos += text.length();
      }
    }
    return selectionScript;
  }

  private static String generateScriptInjector(String scriptUrl) {
    if (isRelativeURL(scriptUrl)) {
      return "  if (!__gwt_scriptsLoaded['"
          + scriptUrl
          + "']) {\n"
          + "    __gwt_scriptsLoaded['"
          + scriptUrl
          + "'] = true;\n"
          + "    document.write('<script language=\\\"javascript\\\" src=\\\"'+base+'"
          + scriptUrl + "\\\"></script>');\n" + "  }\n";
    } else {
      return "  if (!__gwt_scriptsLoaded['" + scriptUrl + "']) {\n"
          + "    __gwt_scriptsLoaded['" + scriptUrl + "'] = true;\n"
          + "    document.write('<script language=\\\"javascript\\\" src=\\\""
          + scriptUrl + "\\\"></script>');\n" + "  }\n";
    }
  }

  /**
   * Generate a Snippet of JavaScript to inject an external stylesheet.
   *
   * <pre>
   * if (!__gwt_stylesLoaded['URL']) {
   *   var l = $doc.createElement('link');
   *   __gwt_styleLoaded['URL'] = l;
   *   l.setAttribute('rel', 'stylesheet');
   *   l.setAttribute('href', HREF_EXPR);
   *   $doc.getElementsByTagName('head')[0].appendChild(l);
   * }
   * </pre>
   */
  private static String generateStylesheetInjector(String stylesheetUrl) {
    String hrefExpr = "'" + stylesheetUrl + "'";
    if (isRelativeURL(stylesheetUrl)) {
      hrefExpr = "base + " + hrefExpr;
    }

    return "if (!__gwt_stylesLoaded['" + stylesheetUrl + "']) {\n           "
        + "  var l = $doc.createElement('link');\n                          "
        + "  __gwt_stylesLoaded['" + stylesheetUrl + "'] = l;\n             "
        + "  l.setAttribute('rel', 'stylesheet');\n                         "
        + "  l.setAttribute('href', " + hrefExpr + ");\n                    "
        + "  $doc.getElementsByTagName('head')[0].appendChild(l);\n         "
        + "}\n";
  }

  /**
   * Determines whether or not the URL is relative.
   *
   * @param src the test url
   * @return <code>true</code> if the URL is relative, <code>false</code> if not
   */
  private static boolean isRelativeURL(String src) {
    // A straight absolute url for the same domain, server, and protocol.
    if (src.startsWith("/")) {
      return false;
    }

    // If it can be parsed as a URL, then it's probably absolute.
    try {
      // Just check to see if it can be parsed, no need to store the result.
      new URL(src);

      // Let's guess that it is absolute (thus, not relative).
      return false;
    } catch (MalformedURLException e) {
      // Do nothing, since it was a speculative parse.
    }

    // Since none of the above matched, let's guess that it's relative.
    return true;
  }

}
