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
package com.google.gwt.dev.util;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import com.google.gwt.dev.js.JsSourceGenerationVisitor;
import com.google.gwt.dev.js.JsParserException.SourceDetail;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.shell.JavaScriptHost;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * Helper methods working with JSNI.
 */
public class Jsni {

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

  /**
   * Generate source code, fixing up any JSNI references for hosted mode.
   * 
   * <p/><table>
   * <tr>
   * <td>Original</td>
   * <td>Becomes</td>
   * </tr>
   * <tr>
   * <td><code>.@class::method(params)(args)</code></td>
   * 
   * <td><code>["@class::method(params)"](args)</code></td>
   * </tr>
   * <tr>
   * <td><code>@class::method(params)(args)</code></td>
   * 
   * <td><code>__static["@class::method(params)"](args)</code></td>
   * </tr>
   * <tr>
   * <td><code>.@class::field</code></td>
   * 
   * <td><code>["@class::field"]</code></td>
   * </tr>
   * <tr>
   * <td><code>@class::field</code></td>
   * 
   * <td><code>__static["@class::field"]</code></td>
   * </tr>
   * </table>
   */
  private static class JsSourceGenWithJsniIdentFixup extends
      JsSourceGenerationVisitor {
    private final TextOutput out;

    public JsSourceGenWithJsniIdentFixup(TextOutput out) {
      super(out);
      this.out = out;
    }

    @Override
    public boolean visit(JsNameRef x, JsContext<JsExpression> ctx) {
      String ident = x.getIdent();
      if (ident.startsWith("@")) {
        JsExpression q = x.getQualifier();
        if (q != null) {
          accept(q);
          out.print("[\"");
          out.print(ident);
          out.print("\"]");
        } else {
          out.print("__static[\"");
          out.print(ident);
          out.print("\"]");
        }
        return false;
      } else {
        return super.visit(x, ctx);
      }
    }
  }

  public static final String JAVASCRIPTHOST_NAME = JavaScriptHost.class.getName();

  public static final String JSNI_BLOCK_END = "}-*/";

  public static final String JSNI_BLOCK_START = "/*-{";

  /**
   * Generates the code to wrap a set of parameters as an object array. In Java
   * 1.5 we can take advantage of autoboxing to not have to wrap primitives.
   */
  public static String buildArgList(JMethod method) {
    StringBuilder sb = new StringBuilder();
    sb.append("new Object[]{");

    JParameter[] params = method.getParameters();
    for (int i = 0; i < params.length; ++i) {
      sb.append(params[i].getName());
      sb.append(", ");
    }

    sb.append("}");
    String args = sb.toString();
    return args;
  }

  /**
   * Generates the code to pass the exact types associated with each argument of
   * this method.
   */
  public static String buildTypeList(JMethod method) {
    StringBuilder sb = new StringBuilder();
    sb.append("new Class[]{");

    JParameter[] params = method.getParameters();
    for (int i = 0; i < params.length; ++i) {
      JType type = params[i].getType();
      String typeName = type.getErasedType().getQualifiedSourceName();
      sb.append(typeName);
      sb.append(".class, ");
    }

    sb.append("}");
    String classes = sb.toString();
    return classes;
  }

  public static int countNewlines(char[] buf, int start, int end) {
    int total = 0;
    while (start < end) {
      switch (buf[start]) {
        case '\r':
          ++total;
          // if the next character is a line feed, eat it too
          if (start + 1 < end && buf[start + 1] == '\n') {
            ++start;
          }
          break;
        case '\n':
          ++total;
          break;
      }
      ++start;
    }
    return total;
  }

  public static int countNewlines(String src, int start, int end) {
    return countNewlines(src.toCharArray(), start, end);
  }

  /**
   * Replaces double-quotes, backslashes, and newlines in native JS code with
   * their appropriate escaped form (so they can be encoded in a java string).
   */
  public static String escapedJavaScriptForStringLiteral(String js) {
    StringBuilder sb = new StringBuilder(js);
    for (int i = 0; i < sb.length(); ++i) {
      char c = sb.charAt(i);
      switch (c) {
        case '\"':
        case '\\':
          sb.insert(i, '\\');
          ++i;
          break;
        case '\r':
          sb.setCharAt(i, 'r');
          sb.insert(i, '\\');
          ++i;
          break;
        case '\n':
          sb.setCharAt(i, 'n');
          sb.insert(i, '\\');
          ++i;
          break;
      }
    }
    return sb.toString();
  }

  public static Interval findJsniSource(JMethod method)
      throws UnableToCompleteException {
    assert (method.isNative());
    int bodyStart = method.getBodyStart();
    int bodyEnd = method.getBodyEnd();
    int bodyLen = bodyEnd - bodyStart + 1;
    char[] source = method.getEnclosingType().getCompilationUnit().getSource();
    String js = String.valueOf(source, bodyStart, bodyLen);

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
   * Returns a string representing the source output of the JsNode, where all
   * JSNI idents have been replaced with legal JavaScript for hosted mode.
   */
  public static String generateJavaScriptForHostedMode(JsNode<?> node) {
    DefaultTextOutput out = new DefaultTextOutput(false);
    JsSourceGenWithJsniIdentFixup vi = new JsSourceGenWithJsniIdentFixup(out);
    vi.accept(node);
    return out.toString();
  }

  /**
   * Gets a unique name for this method and its signature (this is used to
   * determine whether one method overrides another).
   */
  public static String getJsniSignature(JMethod method) {
    return method.getEnclosingType().getQualifiedSourceName() + "::"
        + getMemberSignature(method);
  }

  /**
   * Gets a unique name for this method and its signature (this is used to
   * determine whether one method overrides another).
   */
  public static String getMemberSignature(JMethod method) {
    String name = method.getName();
    StringBuilder sb = new StringBuilder();
    sb.append(name);
    sb.append("(");
    JParameter[] params = method.getParameters();
    for (int i = 0; i < params.length; ++i) {
      JParameter param = params[i];
      String typeSig = param.getType().getJNISignature();
      sb.append(typeSig);
    }
    sb.append(")");
    String result = sb.toString();
    return result;
  }

  /**
   * In other words, it can have <code>return</code> statements.
   */
  public static JsBlock parseAsFunctionBody(TreeLogger logger, String js,
      String location, int startLine) throws UnableToCompleteException {
    // Wrap it in fake function and parse it.
    js = "function(){ " + js + " }";

    JsParser jsParser = new JsParser();
    JsProgram jsPgm = new JsProgram();
    StringReader r = new StringReader(js);

    try {
      List<JsStatement> stmts = jsParser.parse(jsPgm.getScope(), r, startLine);

      // Rip the body out of the parsed function and attach the JavaScript
      // AST to the method.
      //
      JsFunction fn = (JsFunction) ((JsExprStmt) stmts.get(0)).getExpression();
      return fn.getBody();
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Error reading JavaScript source", e);
      throw new UnableToCompleteException();
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
        throw new UnableToCompleteException();
      } else {
        logger.log(TreeLogger.ERROR, "Error parsing JSNI source", e);
        throw new UnableToCompleteException();
      }
    }
  }

}
