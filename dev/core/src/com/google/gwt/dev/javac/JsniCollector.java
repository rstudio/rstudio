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

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.ext.TreeLogger.HelpInfo;
import com.google.gwt.dev.jjs.CorrelationFactory;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import com.google.gwt.dev.js.JsParserException.SourceDetail;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.util.collect.IdentityHashMap;
import com.google.gwt.dev.util.collect.IdentityMaps;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.compiler.util.Util;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

/**
 * Adapts compilation units containing JSNI-accessible code by rewriting the
 * source.
 */
public class JsniCollector {

  private static final class JsniMethodImpl extends JsniMethod implements
      Serializable {
    private final JsFunction func;
    private boolean isScriptOnly;
    private final String name;

    public JsniMethodImpl(String name, JsFunction func, boolean isScriptOnly) {
      this.name = name;
      this.func = func;
      this.isScriptOnly = isScriptOnly;
    }

    @Override
    public JsFunction function() {
      return func;
    }

    @Override
    public boolean isScriptOnly() {
      return isScriptOnly;
    }

    @Override
    public int line() {
      return func.getSourceInfo().getStartLine();
    }

    @Override
    public String location() {
      return func.getSourceInfo().getFileName();
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String[] paramNames() {
      List<JsParameter> params = func.getParameters();
      String[] result = new String[params.size()];
      for (int i = 0; i < result.length; ++i) {
        result[i] = params.get(i).getName().getIdent();
      }
      return result;
    }

    @Override
    public String toString() {
      return func.toString();
    }
  }

  private static class Visitor extends MethodVisitor {
    private static boolean isScriptOnly(AbstractMethodDeclaration method) {
      if (method.annotations == null) {
        return false;
      }
      for (Annotation a : method.annotations) {
        ReferenceBinding binding = (ReferenceBinding) a.resolvedType;
        String name = CharOperation.toString(binding.compoundName);
        if (name.equals(GwtScriptOnly.class.getName())) {
          return true;
        }
      }
      return false;
    }

    private final CorrelationFactory correlator;
    private final Map<MethodDeclaration, JsniMethod> jsniMethods;
    private final JsScope scope;
    private final String source;
    private SourceInfo cudInfo;

    public Visitor(String source, JsScope scope, CorrelationFactory correlator,
        Map<MethodDeclaration, JsniMethod> jsniMethods) {
      this.jsniMethods = jsniMethods;
      this.source = source;
      this.scope = scope;
      this.correlator = correlator;
    }

    @Override
    public void collect(CompilationUnitDeclaration cud, String sourceMapPath) {
      cudInfo = correlator.makeSourceInfo(SourceOrigin.create(0, sourceMapPath));
      super.collect(cud, sourceMapPath);
    }

    @Override
    protected boolean interestingMethod(AbstractMethodDeclaration method) {
      return method.isNative();
    }

    @Override
    protected void processMethod(TypeDeclaration typeDecl,
        AbstractMethodDeclaration method, String enclosingType) {
      JsFunction jsFunction = parseJsniFunction(method, source, enclosingType,
          cudInfo, scope);
      if (jsFunction != null) {
        String jsniSignature = getJsniSignature(enclosingType, method);
        jsniMethods.put((MethodDeclaration) method, new JsniMethodImpl(
            jsniSignature, jsFunction, isScriptOnly(method)));
      }
    }
  }

  public static final String JSNI_BLOCK_END = "}-*/";

  public static final String JSNI_BLOCK_START = "/*-{";

  public static Map<MethodDeclaration, JsniMethod> collectJsniMethods(
      CompilationUnitDeclaration cud, String sourceMapPath,
      String source, JsScope scope,
      CorrelationFactory correlator) {
    Map<MethodDeclaration, JsniMethod> jsniMethods = new IdentityHashMap<MethodDeclaration, JsniMethod>();
    new Visitor(source, scope, correlator, jsniMethods).collect(cud, sourceMapPath);
    return IdentityMaps.normalizeUnmodifiable(jsniMethods);
  }

  public static JsFunction parseJsniFunction(AbstractMethodDeclaration method,
      String unitSource, String enclosingType, SourceInfo baseInfo,
      JsScope scope) {
    CompilationResult compResult = method.compilationResult;
    int[] indexes = compResult.lineSeparatorPositions;
    int startLine = Util.getLineNumber(method.sourceStart, indexes, 0,
        indexes.length - 1);
    SourceInfo info = baseInfo.makeChild(SourceOrigin.create(
        method.sourceStart, method.bodyEnd, startLine, baseInfo.getFileName()));

    // Handle JSNI block
    String jsniCode = unitSource
        .substring(method.bodyStart, method.bodyEnd + 1);
    int startPos = jsniCode.indexOf("/*-{");
    int endPos = jsniCode.lastIndexOf("}-*/");
    if (startPos < 0 && endPos < 0) {
      reportJsniError(
          info,
          method,
          "Native methods require a JavaScript implementation enclosed with /*-{ and }-*/");
      return null;
    }
    if (startPos < 0) {
      reportJsniError(info, method,
          "Unable to find start of native block; begin your JavaScript block with: /*-{");
      return null;
    }
    if (endPos < 0) {
      reportJsniError(
          info,
          method,
          "Unable to find end of native block; terminate your JavaScript block with: }-*/");
      return null;
    }

    startPos += 3; // move up to open brace
    endPos += 1; // move past close brace

    jsniCode = jsniCode.substring(startPos, endPos);

    // Here we parse it as an anonymous function, but we will give it a
    // name later when we generate the JavaScript during code generation.
    //
    StringBuilder functionSource = new StringBuilder("function (");
    boolean first = true;
    if (method.arguments != null) {
      for (Argument arg : method.arguments) {
        if (first) {
          first = false;
        } else {
          functionSource.append(',');
        }
        functionSource.append(arg.binding.name);
      }
    }
    functionSource.append(") ");
    int functionHeaderLength = functionSource.length();
    functionSource.append(jsniCode);
    StringReader sr = new StringReader(functionSource.toString());

    // Absolute start and end position of braces in original source.
    int absoluteJsStartPos = method.bodyStart + startPos;
    int absoluteJsEndPos = absoluteJsStartPos + jsniCode.length();

    // Adjust the points the JS parser sees to account for the synth header.
    int jsStartPos = absoluteJsStartPos - functionHeaderLength;
    int jsEndPos = absoluteJsEndPos - functionHeaderLength;

    // To compute the start line, count lines from point to point.
    int jsLine = info.getStartLine()
        + countLines(indexes, info.getStartPos(), absoluteJsStartPos);

    SourceInfo jsInfo = baseInfo.makeChild(SourceOrigin.create(jsStartPos,
        jsEndPos, jsLine, baseInfo.getFileName()));
    try {
      List<JsStatement> result = JsParser.parse(jsInfo, scope, sr);
      JsExprStmt jsExprStmt = (JsExprStmt) result.get(0);
      return (JsFunction) jsExprStmt.getExpression();
    } catch (IOException e) {
      throw new InternalCompilerException("Internal error parsing JSNI in '"
          + enclosingType + '.' + method.toString() + '\'', e);
    } catch (JsParserException e) {
      int problemCharPos = computeAbsoluteProblemPosition(indexes, e
          .getSourceDetail());
      SourceInfo errorInfo = SourceOrigin.create(problemCharPos,
          problemCharPos, e.getSourceDetail().getLine(), info.getFileName());
      // Strip the file/line header because reportJsniError will add that.
      String msg = e.getMessage();
      int pos = msg.indexOf(": ");
      msg = msg.substring(pos + 2);
      reportJsniError(errorInfo, method, msg);
      return null;
    }
  }

  public static void reportJsniError(SourceInfo info,
      AbstractMethodDeclaration method, String msg) {
    reportJsniProblem(info, method, msg, ProblemSeverities.Error);
  }

  public static void reportJsniWarning(SourceInfo info,
      MethodDeclaration method, String msg) {
    reportJsniProblem(info, method, msg, ProblemSeverities.Warning);
  }

  /**
   * JS reports the error as a line number, to find the absolute position in the
   * real source stream, we have to walk from the absolute JS start position
   * until we have counted down enough lines. Then we use the column position to
   * find the exact spot.
   */
  private static int computeAbsoluteProblemPosition(int[] indexes,
      SourceDetail detail) {
    // Convert 1-based to -1 - based.
    int line = detail.getLine() - 1;
    if (line == 0) {
      return detail.getLineOffset() - 1;
    }

    int result = indexes[line - 1] + detail.getLineOffset();
    /*
     * In other words, make sure our result is actually on this line (less than
     * the start position of the next line), but make sure we don't overflow if
     * this is the last line in the file.
     */
    assert line >= indexes.length || result < indexes[line];
    return result;
  }

  private static int countLines(int[] indexes, int p1, int p2) {
    assert p1 >= 0;
    assert p2 >= 0;
    assert p1 <= p2;
    int p1line = findLine(p1, indexes, 0, indexes.length);
    int p2line = findLine(p2, indexes, 0, indexes.length);
    return p2line - p1line;
  }

  private static int findLine(int pos, int[] indexes, int lo, int tooHi) {
    assert (lo < tooHi);
    if (lo == tooHi - 1) {
      return lo;
    }
    int mid = lo + (tooHi - lo) / 2;
    assert (lo < mid);
    if (pos < indexes[mid]) {
      return findLine(pos, indexes, lo, mid);
    } else {
      return findLine(pos, indexes, mid, tooHi);
    }
  }

  /**
   * Gets a unique name for this method and its signature (this is used to
   * determine whether one method overrides another).
   */
  private static String getJsniSignature(String enclosingType,
      AbstractMethodDeclaration method) {
    return '@' + enclosingType + "::"
        + MethodVisitor.getMemberSignature(method);
  }

  private static void reportJsniProblem(SourceInfo info,
      AbstractMethodDeclaration methodDeclaration, String message,
      int problemSeverity) {
    // TODO: provide helpInfo for how to write JSNI methods?
    HelpInfo jsniHelpInfo = null;
    CompilationResult compResult = methodDeclaration.compilationResult();
    // recalculate startColumn, because SourceInfo does not hold it
    int startColumn = Util.searchColumnNumber(compResult
        .getLineSeparatorPositions(), info.getStartLine(), info.getStartPos());
    GWTProblem.recordProblem(info, startColumn, compResult, message,
        jsniHelpInfo, problemSeverity);
  }

  private JsniCollector() {
  }
}
