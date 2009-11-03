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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.javac.CompilationUnit.State;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import com.google.gwt.dev.js.JsParserException.SourceDetail;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.util.Empty;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.util.Util;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Adapts compilation units containing JSNI-accessible code by rewriting the
 * source.
 */
public class JsniCollector {

  /**
   * Represents a logical interval of text.
   */
  public static class Interval {
    public final int end;
    public final int start;

    public Interval(int start, int end) {
      this.start = start;
      this.end = end;
    }
  }

  private static final class JsniMethodImpl extends JsniMethod {
    private JsFunction func;
    private final int line;
    private final String location;
    private final String name;
    private final String[] paramNames;
    private final JsProgram program;
    private char[] source;

    private JsniMethodImpl(String name, char[] source, String[] paramNames,
        int line, String location, JsProgram program) {
      this.name = name;
      this.source = source;
      this.paramNames = paramNames;
      this.line = line;
      this.location = location;
      this.program = program;
    }

    @Override
    public JsFunction function(TreeLogger logger) {
      if (func == null) {
        func = parseAsAnonymousFunction(logger, program,
            String.valueOf(source), paramNames, location, line);
        source = null;
      }
      return func;
    }

    @Override
    public int line() {
      return line;
    }

    @Override
    public String location() {
      return location;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String[] paramNames() {
      return paramNames;
    }

    @Override
    public JsProgram program() {
      return program;
    }

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("function ");
      sb.append(name);
      sb.append('(');
      boolean first = true;
      for (String paramName : paramNames) {
        if (first) {
          first = false;
        } else {
          sb.append(", ");
        }
        sb.append(paramName);
      }
      sb.append(") {\n");
      sb.append("  [...]");
      sb.append("}\n");
      return sb.toString();
    }
  }

  /**
   * Lazily grab source only for compilation units which actually contain native
   * methods, then cache it.
   * 
   * TODO: parse JSNI <i>during</i> JDT's cycle to avoiding having to read from
   * disk twice.
   */
  private static final class SourceCache {
    private String source;
    private final CompilationUnit unit;

    public SourceCache(CompilationUnit unit) {
      this.unit = unit;
    }

    public String get() {
      if (source == null) {
        source = unit.getSource();
      }
      return source;
    }
  }

  public static final String JSNI_BLOCK_END = "}-*/";

  public static final String JSNI_BLOCK_START = "/*-{";

  public static void collectJsniMethods(TreeLogger logger,
      Collection<CompilationUnit> units, JsProgram program) {
    for (CompilationUnit unit : units) {
      if (unit.getState() == State.COMPILED) {
        String loc = unit.getDisplayLocation();
        SourceCache sourceCache = new SourceCache(unit);
        assert unit.getJsniMethods() == null;
        List<JsniMethod> jsniMethods = new ArrayList<JsniMethod>();
        for (CompiledClass compiledClass : unit.getCompiledClasses()) {
          jsniMethods.addAll(collectJsniMethods(logger, loc, sourceCache,
              compiledClass, program));
        }
        unit.setJsniMethods(jsniMethods);
      }
    }
  }

  /**
   * TODO: log real errors, replacing GenerateJavaScriptAST?
   */
  private static List<JsniMethod> collectJsniMethods(TreeLogger logger,
      String loc, SourceCache sourceCache, CompiledClass compiledClass,
      JsProgram program) {
    TypeDeclaration typeDecl = compiledClass.getTypeDeclaration();
    int[] lineEnds = typeDecl.compilationResult.getLineSeparatorPositions();
    List<JsniMethod> jsniMethods = new ArrayList<JsniMethod>();
    String enclosingType = compiledClass.getBinaryName().replace('/', '.');
    AbstractMethodDeclaration[] methods = typeDecl.methods;
    if (methods != null) {
      for (AbstractMethodDeclaration method : methods) {
        if (!method.isNative()) {
          continue;
        }
        String source = sourceCache.get();
        Interval interval = findJsniSource(source, method);
        if (interval == null) {
          String msg = "No JavaScript body found for native method '" + method
              + "' in type '" + compiledClass.getSourceName() + "'";
          logger.log(TreeLogger.ERROR, msg, null);
          continue;
        }

        String js = source.substring(interval.start, interval.end);
        int startLine = Util.getLineNumber(interval.start, lineEnds, 0,
            lineEnds.length - 1);
        String jsniSignature = getJsniSignature(enclosingType, method);
        String[] paramNames = getParamNames(method);

        jsniMethods.add(new JsniMethodImpl(jsniSignature, js.toCharArray(),
            paramNames, startLine, loc, program));
      }
    }
    return jsniMethods;
  }

  private static Interval findJsniSource(String source,
      AbstractMethodDeclaration method) {
    assert (method.isNative());
    int bodyStart = method.bodyStart;
    int bodyEnd = method.bodyEnd;
    String js = source.substring(bodyStart, bodyEnd + 1);

    int jsniStart = js.indexOf(JSNI_BLOCK_START);
    if (jsniStart == -1) {
      return null;
    }

    int jsniEnd = js.indexOf(JSNI_BLOCK_END, jsniStart);
    if (jsniEnd == -1) {
      // Suspicious, but maybe this is just a weird comment, so let it slide.
      //
      return null;
    }

    int srcStart = bodyStart + jsniStart + JSNI_BLOCK_START.length();
    int srcEnd = bodyStart + jsniEnd;
    return new Interval(srcStart, srcEnd);
  }

  /**
   * Gets a unique name for this method and its signature (this is used to
   * determine whether one method overrides another).
   */
  private static String getJsniSignature(String enclosingType,
      AbstractMethodDeclaration method) {
    return '@' + enclosingType + "::" + getMemberSignature(method);
  }

  /**
   * Gets a unique name for this method and its signature (this is used to
   * determine whether one method overrides another).
   */
  private static String getMemberSignature(AbstractMethodDeclaration method) {
    String name = String.valueOf(method.selector);
    StringBuilder sb = new StringBuilder();
    sb.append(name);
    sb.append("(");
    if (method.arguments != null) {
      for (Argument param : method.arguments) {
        sb.append(param.binding.type.signature());
      }
    }
    sb.append(")");
    return sb.toString();
  }

  private static String[] getParamNames(AbstractMethodDeclaration method) {
    if (method.arguments != null) {
      String[] paramNames = new String[method.arguments.length];
      for (int i = 0; i < paramNames.length; ++i) {
        paramNames[i] = String.valueOf(method.arguments[i].name);
      }
      return paramNames;
    }
    return Empty.STRINGS;
  }

  /**
   * TODO: rip out problem reporting code from BuildTypeMap and attach errors to
   * the compilation units.
   */
  private static JsFunction parseAsAnonymousFunction(TreeLogger logger,
      JsProgram program, String js, String[] paramNames, String location,
      int startLine) {

    // Wrap the code in an anonymous function and parse it.
    StringReader r;
    {
      StringBuilder sb = new StringBuilder();
      sb.append("function (");
      boolean first = true;
      for (String paramName : paramNames) {
        if (first) {
          first = false;
        } else {
          sb.append(',');
        }
        sb.append(paramName);
      }
      sb.append(") {");
      sb.append(js);
      sb.append('}');
      r = new StringReader(sb.toString());
    }

    try {
      List<JsStatement> stmts = JsParser.parse(program.createSourceInfo(
          startLine, location), program.getScope(), r);

      return (JsFunction) ((JsExprStmt) stmts.get(0)).getExpression();
    } catch (IOException e) {
      // Should never happen.
      throw new RuntimeException(e);
    } catch (JsParserException e) {
      SourceDetail dtl = e.getSourceDetail();
      if (dtl != null) {
        StringBuilder sb = new StringBuilder();
        sb.append(location);
        sb.append("(");
        sb.append(dtl.getLine());
        sb.append(", ");
        sb.append(dtl.getLineOffset());
        sb.append("): ");
        sb.append(e.getMessage());
        logger.log(TreeLogger.ERROR, sb.toString(), e);
        return null;
      } else {
        logger.log(TreeLogger.ERROR, "Error parsing JSNI source", e);
        return null;
      }
    }
  }

  private JsniCollector() {
  }
}
