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
package com.google.gwt.dev.jdt;

import com.google.gwt.dev.javac.JsniCollector;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.js.rhino.Context;
import com.google.gwt.dev.js.rhino.ErrorReporter;
import com.google.gwt.dev.js.rhino.EvaluatorException;
import com.google.gwt.dev.js.rhino.TokenStream;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Walks the AST to find references to Java identifiers from within JSNI blocks.
 * By default, it does a full JavaScript parse to accurately find JSNI
 * references. If {@link #beSloppy()} is called, then it will run much more
 * quickly but it will return a superset of the actual JSNI references.
 */
public class FindJsniRefVisitor extends ASTVisitor {
  /**
   * A Rhino error reporter that discards any errors it sees.
   */
  private static ErrorReporter NullErrorReporter = new ErrorReporter() {
    public void error(String message, String sourceName, int line,
        String lineSource, int lineOffset) {
      // ignore it
    }

    public EvaluatorException runtimeError(String message, String sourceName,
        int line, String lineSource, int lineOffset) {
      throw new InternalCompilerException("Rhino run-time error: " + message);
    }

    public void warning(String message, String sourceName, int line,
        String lineSource, int lineOffset) {
      // ignore it
    }
  };

  private final Set<String> jsniRefs = new LinkedHashSet<String>();
  private boolean sloppy = false;

  public void beSloppy() {
    sloppy = true;
  }

  public Set<String> getJsniRefs() {
    return Collections.unmodifiableSet(jsniRefs);
  }

  public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
    if (!methodDeclaration.isNative()) {
      return false;
    }

    // Handle JSNI block
    String jsniCode = getJSNICode(methodDeclaration);
    if (jsniCode == null) {
      return false;
    }
    if (jsniCode.indexOf('@') < 0) {
      // short cut: if there are no at signs, there are no JSNI refs
      return false;
    }

    if (sloppy) {
      findJsniRefsSloppily(jsniCode);
    } else {
      findJsniRefsAccurately(methodDeclaration, jsniCode);
    }

    return false;
  }

  private void findJsniRefsAccurately(MethodDeclaration methodDeclaration,
      String jsniCode) throws InternalCompilerException {
    JsProgram jsProgram = new JsProgram();

    String syntheticFnHeader = "function(";
    boolean first = true;
    if (methodDeclaration.arguments != null) {
      for (int i = 0, c = methodDeclaration.arguments.length; i < c; ++i) {
        Argument arg = methodDeclaration.arguments[i];
        if (first) {
          first = false;
        } else {
          syntheticFnHeader += ',';
        }
        syntheticFnHeader += String.valueOf(arg.name);
      }
    }
    syntheticFnHeader += ')';
    StringReader sr = new StringReader(syntheticFnHeader + '\n' + jsniCode);
    try {
      // start at -1 to avoid counting our synthetic header
      List<JsStatement> result = JsParser.parse(SourceOrigin.UNKNOWN,
          jsProgram.getScope(), sr);
      new JsVisitor() {
        public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
          String ident = x.getIdent();
          if (ident.charAt(0) == '@') {
            jsniRefs.add(ident.substring(1));
          }
        }
      }.acceptList(result);
    } catch (IOException e) {
      throw new InternalCompilerException(e.getMessage(), e);
    } catch (JsParserException e) {
      // ignore, we only care about finding valid references
    }
  }

  private void findJsniRefsSloppily(String jsniCode) {
    StringReader reader = new StringReader(jsniCode);
    int idx = 0;
    while (true) {
      idx = jsniCode.indexOf('@', idx);
      if (idx < 0) {
        break;
      }
      try {
        reader.reset();
        // Ignore return value, since we know the index is valid.
        reader.skip(idx);
      } catch (IOException e) {
        throw new InternalCompilerException(e.getMessage(), e);
      }
      String jsniRefString = readJsIdentifier(reader);
      if (jsniRefString == null) {
        // badly formatted identifier; skip to the next @ sign
        idx++;
      } else {
        assert (jsniRefString.charAt(0) == '@');
        jsniRefs.add(jsniRefString.substring(1));
        idx += jsniRefString.length();
      }
    }
  }

  private String getJSNICode(MethodDeclaration methodDeclaration) {
    char[] source = methodDeclaration.compilationResult().getCompilationUnit().getContents();
    String jsniCode = String.valueOf(source, methodDeclaration.bodyStart,
        methodDeclaration.bodyEnd - methodDeclaration.bodyStart + 1);
    int startPos = jsniCode.indexOf(JsniCollector.JSNI_BLOCK_START);
    int endPos = jsniCode.lastIndexOf(JsniCollector.JSNI_BLOCK_END);
    if (startPos < 0 || endPos < 0) {
      return null; // ignore the error
    }

    startPos += JsniCollector.JSNI_BLOCK_START.length() - 1; // move up to open brace
    endPos += 1; // move past close brace

    jsniCode = jsniCode.substring(startPos, endPos);
    return jsniCode;
  }

  /**
   * Read a JavaScript identifier using Rhino. If the parse fails, returns null.
   */
  private String readJsIdentifier(Reader reader)
      throws InternalCompilerException {
    try {
      Context.enter().setErrorReporter(NullErrorReporter);
      TokenStream tokStr = new TokenStream(reader, "(memory)", 0);
      if (tokStr.getToken() == TokenStream.NAME) {
        return tokStr.getString();
      } else {
        return null;
      }
    } catch (IOException e) {
      throw new InternalCompilerException(e.getMessage(), e);
    } finally {
      Context.exit();
    }
  }

}
