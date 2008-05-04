/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jdt.CompilationUnitProviderWithAlternateSource;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.shell.JsniMethods.JsniMethod;
import com.google.gwt.dev.util.Jsni;
import com.google.gwt.dev.util.StringCopier;
import com.google.gwt.dev.util.Util;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapts compilation units containing JSNI-accessible code by rewriting the
 * source.
 */
public class JsniInjector {

  /**
   * A chunk of replacement text and where to put it.
   */
  private static class Replacement implements Comparable<Replacement> {
    public final int end;

    public final int start;

    public final char[] text;

    public Replacement(int start, int end, char[] text) {
      this.start = start;
      this.end = end;
      this.text = text;
    }

    public int compareTo(Replacement other) {
      if (start < other.start) {
        assert (end <= other.start) : "Overlapping changes not supported";
        return -1;
      } else if (start > other.start) {
        assert (start >= other.end) : "Overlapping changes not supported";
        return 1;
      } else {
        return 0;
      }
    }
  }

  private static final int BLOCK_SIZE = 1024;

  private static final String JSNIMETHOD_NAME = JsniMethod.class.getName().replace(
      '$', '.');

  private static final String JSNIMETHODS_NAME = JsniMethods.class.getName();

  private final Map<JClassType, List<JsniMethod>> jsniMethodMap = new IdentityHashMap<JClassType, List<JsniMethod>>();
  private final TypeOracle oracle;

  public JsniInjector(TypeOracle oracle) {
    this.oracle = oracle;
  }

  public CompilationUnitProvider inject(TreeLogger logger,
      CompilationUnitProvider cup, File jsniSaveDirectory)
      throws UnableToCompleteException {

    logger = logger.branch(TreeLogger.SPAM,
        "Checking for JavaScript native methods", null);

    // Analyze the source and build a list of changes.
    char[] source = cup.getSource();
    List<Replacement> changes = new ArrayList<Replacement>();
    rewriteCompilationUnit(logger, source, changes, cup, false);

    // Sort and apply the changes.
    int n = changes.size();
    if (n > 0) {
      Replacement[] repls = changes.toArray(new Replacement[n]);
      Arrays.sort(repls);
      StringCopier copier = new StringCopier(source);
      for (int i = 0; i < n; ++i) {
        Replacement repl = repls[i];
        copier.commit(repl.text, repl.start, repl.end);
      }

      char[] results = copier.finish();

      if (jsniSaveDirectory != null) {
        String originalPath = cup.getLocation().replace(File.separatorChar, '/');
        String suffix = cup.getPackageName().replace('.', '/');
        int pos = originalPath.indexOf(suffix);
        if (pos >= 0) {
          String filePath = originalPath.substring(pos);
          File out = new File(jsniSaveDirectory, filePath);
          Util.writeCharsAsFile(logger, out, results);
        }
      }

      return new CompilationUnitProviderWithAlternateSource(cup, results);
    } else {
      // No changes were made, so we return the original.
      logger.log(TreeLogger.SPAM, "No JavaScript native methods were found",
          null);
      return cup;
    }
  }

  private void collectJsniMethods(TreeLogger logger, char[] source,
      JClassType type) throws UnableToCompleteException {

    // Locate the nearest non-local type; don't try to annotate local types.
    JClassType targetType = type;
    while (targetType.isLocalType()) {
      targetType = targetType.getEnclosingType();
    }
    List<JsniMethod> jsniMethods = jsniMethodMap.get(targetType);
    String loc = type.getCompilationUnit().getLocation();

    for (JMethod method : type.getMethods()) {
      if (!method.isNative()) {
        continue;
      }
      Jsni.Interval interval = Jsni.findJsniSource(method);
      if (interval == null) {
        String msg = "No JavaScript body found for native method '" + method
            + "' in type '" + type + "'";
        logger.log(TreeLogger.ERROR, msg, null);
        throw new UnableToCompleteException();
      }
      // Parse it.
      String js = String.valueOf(source, interval.start, interval.end
          - interval.start);
      int startLine = Jsni.countNewlines(source, 0, interval.start) + 1;
      JsBlock body = Jsni.parseAsFunctionBody(logger, js, loc, startLine);

      // Add JsniMethod annotations to the target type.
      if (jsniMethods == null) {
        jsniMethods = new ArrayList<JsniMethod>();
        jsniMethodMap.put(targetType, jsniMethods);
      }
      jsniMethods.add(createJsniMethod(method, body, loc, source));
    }
  }

  private JsniMethod createJsniMethod(JMethod method, JsBlock jsniBody,
      final String file, char[] source) {

    final int line = Jsni.countNewlines(source, 0, method.getBodyStart()) + 1;

    final String name = Jsni.getJsniSignature(method);

    JParameter[] params = method.getParameters();
    final String[] paramNames = new String[params.length];
    for (int i = 0; i < params.length; ++i) {
      paramNames[i] = params[i].getName();
    }

    /*
     * Surround the original JS body statements with a try/catch so that we can
     * map JavaScript exceptions back into Java. Note that the method body
     * itself will print curly braces, so we don't need them around the
     * try/catch.
     */
    String jsTry = "try ";
    String jsCatch = " catch (e) {\n  __static[\"@" + Jsni.JAVASCRIPTHOST_NAME
        + "::exceptionCaught(Ljava/lang/Object;)\"](e == null ? null : e);\n"
        + "}\n";
    String body = jsTry + Jsni.generateJavaScriptForHostedMode(jsniBody)
        + jsCatch;

    /*
     * Break up the body into 1k strings; this ensures we don't blow up any
     * class file limits.
     */
    int length = body.length();
    final String[] bodyParts = new String[(length + BLOCK_SIZE - 1)
        / BLOCK_SIZE];
    for (int i = 0; i < bodyParts.length; ++i) {
      int startIndex = i * BLOCK_SIZE;
      int endIndex = Math.min(startIndex + BLOCK_SIZE, length);
      bodyParts[i] = body.substring(startIndex, endIndex);
    }

    return new JsniMethod() {

      public Class<? extends Annotation> annotationType() {
        return JsniMethod.class;
      }

      public String[] body() {
        return bodyParts;
      }

      public String file() {
        return file;
      }

      public int line() {
        return line;
      }

      public String name() {
        return name;
      }

      public String[] paramNames() {
        return paramNames;
      }

      @Override
      public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("@" + JSNIMETHOD_NAME + "(file=\"");
        sb.append(Jsni.escapedJavaScriptForStringLiteral(file));
        sb.append("\",line=");
        sb.append(line);
        sb.append(",name=\"@");
        sb.append(name);
        sb.append("\",paramNames={");
        for (String paramName : paramNames) {
          sb.append('\"');
          sb.append(paramName);
          sb.append('\"');
          sb.append(',');
        }
        sb.append("},body={");
        for (String bodyPart : bodyParts) {
          sb.append('"');
          sb.append(Jsni.escapedJavaScriptForStringLiteral(bodyPart));
          sb.append('"');
          sb.append(',');
        }
        sb.append("})");
        return sb.toString();
      }
    };
  }

  /**
   * Generate annotation metadata for all the JSNI methods in a list.
   */
  private char[] genJsniMethodsAnnotation(List<JsniMethod> jsniMethods,
      boolean pretty) {
    StringBuffer sb = new StringBuffer();
    String nl = pretty ? "\n " : "";
    sb.append("@" + JSNIMETHODS_NAME + "({");
    for (JsniMethod jsniMethod : jsniMethods) {
      sb.append(jsniMethod.toString());
      sb.append(',');
      sb.append(nl);
    }
    sb.append("})");
    return sb.toString().toCharArray();
  }

  private void rewriteCompilationUnit(TreeLogger logger, char[] source,
      List<Replacement> changes, CompilationUnitProvider cup, boolean pretty)
      throws UnableToCompleteException {

    // Collect all JSNI methods in the compilation unit.
    JClassType[] types = oracle.getTypesInCompilationUnit(cup);
    for (JClassType type : types) {
      if (!type.getQualifiedSourceName().startsWith("java.")) {
        collectJsniMethods(logger, source, type);
      }
    }

    // Annotate the appropriate types with JsniMethod annotations.
    for (JClassType type : types) {
      List<JsniMethod> jsniMethods = jsniMethodMap.get(type);
      if (jsniMethods != null && jsniMethods.size() > 0) {
        char[] annotation = genJsniMethodsAnnotation(jsniMethods, pretty);
        int declStart = type.getDeclStart();
        changes.add(new Replacement(declStart, declStart, annotation));
      }
    }
  }
}
