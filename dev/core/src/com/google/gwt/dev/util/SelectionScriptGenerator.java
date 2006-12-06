/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.util;

import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.cfg.PropertyProvider;
import com.google.gwt.dev.cfg.Script;
import com.google.gwt.dev.cfg.Scripts;
import com.google.gwt.dev.cfg.Styles;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * Generates the "module.nocache.html" file for use in both hosted and web mode.
 * It is able to generate JavaScript with the knowledge of a module's settings.
 * This class is used by {@link com.google.gwt.dev.GWTCompiler} and
 * {@link com.google.gwt.dev.shell.GWTShellServlet}.
 */
public class SelectionScriptGenerator {

  /**
   * A constructor for creating a selection script that will work in either
   * hosted or web mode.
   * 
   * @param moduleDef the module for which the selection script will be
   *          generated
   * @param props the module's property objects, arranged in the same order in
   *          which sets of property values should be interpreted by the
   *          {@link #recordSelection(String[], String)} method
   */
  public SelectionScriptGenerator(ModuleDef moduleDef, Property[] props) {
    this.moduleName = moduleDef.getName();
    this.scripts = moduleDef.getScripts();
    this.styles = moduleDef.getStyles();
    this.moduleProps = moduleDef.getProperties();
    this.orderedProps = (Property[]) props.clone();
  }

  /**
   * A constructor for creating a selection script that will work only in hosted
   * mode.
   * 
   * @param moduleDef the module for which the selection script will be
   *          generated
   */
  public SelectionScriptGenerator(ModuleDef moduleDef) {
    this.moduleName = moduleDef.getName();
    this.scripts = moduleDef.getScripts();
    this.styles = moduleDef.getStyles();
    this.moduleProps = moduleDef.getProperties();
    this.orderedProps = null;
  }

  /**
   * Generates a selection script based on the current settings.
   * 
   * @return an html document whose contents are the definition of a
   *         module.nocache.html file
   */
  public String generateSelectionScript() {
    StringWriter src = new StringWriter();
    PrintWriter pw = new PrintWriter(src, true);

    pw.println("<html>");

    // Emit the head and script.
    //
    pw.println("<head><script>");
    String onloadExpr = genScript(pw);
    pw.println("</script></head>");

    // Emit the body.
    //
    pw.print("<body onload='");
    pw.print(onloadExpr);
    pw.println("'>");

    // This body text won't be seen unless you open the html alone.
    pw.print("<font face='arial' size='-1'>");
    pw.print("This script is part of module</font> <code>");
    pw.print(moduleName);
    pw.println("</code>");

    pw.println("</body>");

    pw.println("</html>");

    pw.close();
    String html = src.toString();
    return html;
  }

  /**
   * Emits all the script required to set up the module and, in web mode, select
   * a compilation.
   * 
   * @param pw
   * @return an expression that should be called as the body's onload handler
   */
  private String genScript(PrintWriter pw) {
    // Emit $wnd and $doc for dynamic property providers.
    pw.println("var $wnd = parent;");
    pw.println("var $doc = $wnd.document;");
    pw.println("var $moduleName = null;");

    // Emit property providers; these are used in both modes.
    genPropProviders(pw);

    // If the ordered props are specified, then we're generating for both modes.
    if (orderedProps != null) {
      // Web mode or hosted mode.
      if (orderedProps.length > 0) {
        pw.println();
        genAnswerFunction(pw);
        pw.println();
        genSrcSetFunction(pw, null);
      } else {
        // Rare case of no properties; happens if you inherit from Core alone.
        assert (orderedProps.length == 0);
        Set entrySet = propertyValuesSetByStrongName.entrySet();
        assert (entrySet.size() == 1);
        Map.Entry entry = (Entry) entrySet.iterator().next();
        String strongName = (String) entry.getKey();
        genSrcSetFunction(pw, strongName);
      }
    } else {
      // Hosted mode only, so there is no strong name selection (i.e. because
      // there is no compiled JavaScript); do nothing
    }

    // Emit dynamic file injection logic; same logic is used in both modes.
    if (hasExternalFiles()) {
      genInjectExternalFiles(pw);
    }

    genOnLoad(pw);

    return "onLoad()";
  }

  private boolean hasExternalFiles() {
    return !scripts.isEmpty() || !styles.isEmpty();
  }

  private void genOnLoad(PrintWriter pw) {
    // Emit the onload() function.
    pw.println();
    pw.println("function onLoad() {");

    // Early out (or fall through below) if the page is loaded out of context.
    pw.println("  if (!$wnd.__gwt_isHosted) return;");

    // Maybe inject scripts.
    if (hasExternalFiles()) {
      pw.println("  injectExternalFiles();");
    }

    // If we're in web mode, run the compilation selector logic.
    // The compilation will call mcb.compilationLoaded() itself.
    pw.println("  if (!$wnd.__gwt_isHosted()) {");
    pw.println("    selectScript();");
    pw.println("  }");

    // If we're in hosted mode, notify $wnd that we're ready to go.
    // Requires that we get the module control block.
    pw.println("  else {");
    pw.println("    var mcb = $wnd.__gwt_tryGetModuleControlBlock(location.search);");
    pw.println("    if (mcb) {");
    pw.println("      $moduleName = mcb.getName();");
    pw.println("      mcb.compilationLoaded(window);");
    pw.println("    }");
    pw.println("  }");
    pw.println("}");
  }

  /**
   * Records a mapping from a unique set of client property values onto a strong
   * name (that is, a compilation).
   * 
   * @param values a set of client property values ordered such that the i'th
   *          value corresponds with the i'th property in {@link #props}
   * @param strongName the base name of a compiled <code>.cache.html</code>
   *          file
   */
  public void recordSelection(String[] values, String strongName) {
    Set valuesSet = (Set) propertyValuesSetByStrongName.get(strongName);
    if (valuesSet == null) {
      valuesSet = new HashSet();
      propertyValuesSetByStrongName.put(strongName, valuesSet);
    }
    valuesSet.add(values.clone());
  }

  private void genAnswerFunction(PrintWriter pw) {
    pw.println("function O(a,v) {");
    pw.println("  var answer = O.answers;");
    pw.println("  var i = -1;");
    pw.println("  var n = a.length - 1;");
    pw.println("  while (++i < n) {");
    pw.println("    if (!(a[i] in answer)) {");
    pw.println("      answer[a[i]] = [];");
    pw.println("    }");
    pw.println("    answer = answer[a[i]];");
    pw.println("  }");
    pw.println("  answer[a[n]] = v;");
    pw.println("}");
    pw.println("O.answers = [];");
  }

  private void genAnswers(PrintWriter pw) {
    for (Iterator iter = propertyValuesSetByStrongName.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Entry) iter.next();
      String strongName = (String) entry.getKey();
      Set propValuesSet = (Set) entry.getValue();

      // Create one answers entry for each string array in the set.
      //
      for (Iterator iterator = propValuesSet.iterator(); iterator.hasNext();) {
        String[] propValues = (String[]) iterator.next();

        pw.print("    O([");
        for (int i = 0; i < orderedProps.length; i++) {
          if (i > 0) {
            pw.print(",");
          }
          pw.print(literal(propValues[i]));
        }
        pw.print("]");
        pw.print(",");
        pw.print(literal(strongName));
        pw.println(");");
      }
    }
  }

  private void genPropProviders(PrintWriter pw) {
    pw.println();

    for (Iterator iter = moduleProps.iterator(); iter.hasNext();) {
      Property prop = (Property) iter.next();
      String activeValue = prop.getActiveValue();
      if (activeValue == null) {
        // Emit a provider function, defined by the user in module config.
        pw.println();
        PropertyProvider provider = prop.getProvider();
        assert (provider != null) : "expecting a default property provider to have been set";
        String js = Jsni.generateJavaScript(provider.getBody());
        pw.print("window[\"provider$" + prop.getName() + "\"] = function() ");
        pw.print(js);
        pw.println(";");

        // Emit a map of allowed property values as an object literal.
        pw.println();
        pw.println("window[\"values$" + prop.getName() + "\"] = {");
        String[] knownValues = prop.getKnownValues();
        for (int i = 0; i < knownValues.length; i++) {
          if (i > 0) {
            pw.println(", ");
          }
          // Each entry is of the form: "propName":<index>.
          // Note that we depend here on the known values being already
          // enclosed in quotes (because property names can have dots which
          // aren't allowed unquoted as keys in the object literal).
          pw.print(literal(knownValues[i]) + ": ");
          pw.print(i);
        }
        pw.println();
        pw.println("};");

        // Emit a wrapper that verifies that the value is valid.
        // It is this function that is called directly to get the propery.
        pw.println();
        pw.println("window[\"prop$" + prop.getName() + "\"] = function() {");
        pw.println("  var v = window[\"provider$" + prop.getName() + "\"]();");
        pw.println("  var ok = window[\"values$" + prop.getName() + "\"];");
        // Make sure this is an allowed value; if so, return.
        pw.println("  if (v in ok)");
        pw.println("    return v;");
        // Not an allowed value, so build a nice message and call the handler.
        pw.println("  var a = new Array(" + knownValues.length + ");");
        pw.println("  for (var k in ok)");
        pw.println("    a[ok[k]] = k;");
        pw.print("  $wnd.__gwt_onBadProperty(");
        pw.print(literal(moduleName));
        pw.print(", ");
        pw.print(literal(prop.getName()));
        pw.println(", a, v);");
        pw.println("  if (arguments.length > 0) throw null; else return null;");
        pw.println("};");
      }
    }
  }

  private void genPropValues(PrintWriter pw) {
    pw.println("    var F;");
    pw.print("    var I = [");
    for (int i = 0; i < orderedProps.length; i++) {
      if (i > 0) {
        pw.print(", ");
      }

      Property prop = orderedProps[i];
      String activeValue = prop.getActiveValue();
      if (activeValue == null) {
        // This is a call to a property provider function.
        //
        PropertyProvider provider = prop.getProvider();
        assert (provider != null) : "expecting a default property provider to have been set";
        // When we call the provider, we supply a bogus argument to indicate
        // that it should throw an exception if the property is a bad value.
        // The absence of arguments (as in hosted mode) tells it to return null.
        pw.print("(F=window[\"prop$" + prop.getName() + "\"],F(1))");
      } else {
        // This property was explicitly set at compile-time.
        //
        pw.print(literal(activeValue));
      }
    }
    pw.println("];");
  }

  /**
   * Generates a function that injects calls to a shared file-injection
   * functions.
   * 
   * @param pw generate source onto this writer
   */
  private void genInjectExternalFiles(PrintWriter pw) {
    pw.println();
    pw.println("function injectExternalFiles() {");
    pw.println("  var mcb = $wnd.__gwt_tryGetModuleControlBlock(location.search);");
    pw.println("  if (!mcb) return;");
    pw.println("  var base = mcb.getBaseURL();");  

    // Styles come first to give them a little more time to load.
    pw.println("  mcb.addStyles([");
    boolean needComma = false;
    for (Iterator iter = styles.iterator(); iter.hasNext();) {
      String src = (String) iter.next();
      if (needComma) {
        pw.println(",");
      }
      needComma = true;
      
      pw.print("    ");
      if (isRelativeURL(src)) {
        pw.print("base+"); 
      }
      pw.print("'");
      pw.print(src);
      pw.print("'");
    }
    pw.println();
    pw.println("    ]);");

    // Scripts
    pw.println("  mcb.addScripts([");
    needComma = false;
    for (Iterator iter = scripts.iterator(); iter.hasNext();) {
      Script script = (Script) iter.next();
      if (needComma) {
        pw.println(",");
      }
      needComma = true;

      // Emit the src followed by the module-ready function.
      // Note that the module-ready function is a string because it gets
      // eval'ed in the context of the host html window. This is absolutely
      // required because otherwise in web mode (IE) you get an
      // "cannot execute code from a freed script" error.
      String src = script.getSrc();
      pw.print("    ");
      if (isRelativeURL(src)) {
        pw.print("base+");
      }
      pw.print("'");
      pw.print(src);
      pw.print("', \"");
      String readyFnJs = Jsni.generateEscapedJavaScript(script.getJsReadyFunction());
      pw.print(readyFnJs);
      pw.print("\"");
    }
    pw.println();
    pw.println("    ]);");

    pw.println("}");
  }

  /**
   * Determines whether or not the URL is relative. 
   * @param src the test url
   * @return <code>true</code> if the URL is relative, <code>false</code> if not
   */
  private boolean isRelativeURL(String src) {
    // A straight absolute url for the same domain, server, and protocol.
    if (src.startsWith("/")) {
      return false;
    } 

    // If it can be parsed as a URL, then it's probably absolute.
    try {
      URL testUrl = new URL(src);
      
      // Let's guess that it is absolute (thus, not relative).
      return false;
    } catch (MalformedURLException e) {
      // Do nothing, since it was a speculative parse.
    }
    
    // Since none of the above matched, let's guess that it's relative.
    return true;
  }

  /**
   * @param pw generate source onto this writer
   * @param oneAndOnlyStrongName if <code>null</code>, use the normal logic;
   *          otherwise, there are no client properties and thus there is
   *          exactly one permutation, specified by this parameter
   */
  private void genSrcSetFunction(PrintWriter pw, String oneAndOnlyStrongName) {
    pw.println();
    pw.println("function selectScript() {");
    if (oneAndOnlyStrongName == null) {
      pw.println("  try {");
      genPropValues(pw);
      pw.println();
      genAnswers(pw);
      pw.println();
      pw.print("    var strongName = O.answers");
      for (int i = 0; i < orderedProps.length; i++) {
        pw.print("[I[" + i + "]]");
      }
      pw.println(";");
      pw.println("    var query = location.search;");
      pw.println("    query = query.substring(0, query.indexOf('&'));");
      pw.println("    var newUrl = strongName + '.cache.html' + query;");
      pw.println("    location.replace(newUrl);");
      pw.println("  } catch (e) {");
      pw.println("    // intentionally silent on property failure");
      pw.println("  }");
    } else {
      // There is exactly one compilation, so it is unconditionally selected.
      //
      String scriptToLoad = oneAndOnlyStrongName + ".cache.html";
      pw.println("  location.replace('" + scriptToLoad + "');");
    }
    pw.println("}");
  }

  private String literal(String lit) {
    return "\"" + lit + "\"";
  }

  /**
   * Maps compilation strong name onto a <code>Set</code> of
   * <code>String[]</code>. We use a <code>TreeMap</code> to produce the
   * same generated code for the same set of compilations.
   */
  private final Map propertyValuesSetByStrongName = new TreeMap();
  private final Property[] orderedProps;
  private final Properties moduleProps;
  private final String moduleName;
  private final Scripts scripts;
  private final Styles styles;
}
