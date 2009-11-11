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
import com.google.gwt.dev.javac.JsniMethod;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.js.JsSourceGenerationVisitor;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Helper methods working with JSNI.
 */
public class Jsni {

  /**
   * Generate source code, fixing up any JSNI references for hosted mode.
   * 
   * <p/><table>
   * <tr>
   * <td>Original</td>
   * <td>Becomes</td>
   * </tr>
   * <tr>
   * <td><code>obj.@class::method(params)(args)</code></td>
   * 
   * <td><code>__gwt_makeJavaInvoke(paramCount)(obj, dispId, args)</code></td>
   * </tr>
   * <tr>
   * <td><code>@class::method(params)(args)</code></td>
   * 
   * <td><code>__gwt_makeJavaInvoke(paramCount)(null, dispId, args)</code></td>
   * </tr>
   * <tr>
   * <td><code>obj.@class::method(params)</code></td>
   * 
   * <td><code>__gwt_makeTearOff(obj, dispId, paramCount)</code></td>
   * </tr>
   * <tr>
   * <td><code>@class::method(params)</code></td>
   * 
   * <td><code>__gwt_makeTearOff(null, dispId, paramCount)</code></td>
   * </tr>
   * <tr>
   * <td><code>obj.@class::field</code></td>
   * 
   * <td><code>obj[dispId]</code></td>
   * </tr>
   * <tr>
   * <td><code>@class::field</code></td>
   * 
   * <td><code>__static[dispId]</code></td>
   * </tr>
   * </table>
   */
  private static class JsSourceGenWithJsniIdentFixup extends
      JsSourceGenerationVisitor {
    private final DispatchIdOracle dispatchInfo;
    private final TextOutput out;
    private final JsProgram program;

    public JsSourceGenWithJsniIdentFixup(TextOutput out, DispatchIdOracle ccl,
        JsProgram program) {
      super(out);
      this.dispatchInfo = ccl;
      this.out = out;
      this.program = program;
    }

    /**
     * This will handle references to fields or tear-offs of Java methods.
     */
    @Override
    public boolean visit(JsNameRef x, JsContext<JsExpression> ctx) {
      String ident = x.getIdent();
      JsExpression q = x.getQualifier();
      if (ident.startsWith("@")) {
        int dispId = dispatchInfo.getDispId(ident);

        Member member;
        if (dispId < 0) {
          // We've already emitted a warning from getDispId; just fake the jsni
          member = null;
        } else {
          member = dispatchInfo.getClassInfoByDispId(dispId).getMember(dispId);
        }

        if (member == null || member instanceof Field
            || member instanceof SyntheticClassMember) {
          if (q != null) {
            accept(q);
            out.print("[");
            out.print(String.valueOf(dispId));
            out.print("]");
          } else {
            out.print("__static[");
            out.print(String.valueOf(dispId));
            out.print("]");
          }

          return false;
        }

        int paramCount = 0;
        if (member instanceof Method) {
          paramCount = ((Method) member).getParameterTypes().length;
        } else if (member instanceof Constructor) {
          paramCount = ((Constructor<?>) member).getParameterTypes().length;
        }

        // Use a clone instead of modifying the original JSNI
        // __gwt_makeTearOff(obj, dispId, paramCount)
        SourceInfo newSourceInfo = x.getSourceInfo().makeChild(getClass(),
            "Replace JSNI ref for hosted mode");
        JsInvocation rewritten = new JsInvocation(newSourceInfo);
        rewritten.setQualifier(new JsNameRef(newSourceInfo, "__gwt_makeTearOff"));

        List<JsExpression> arguments = rewritten.getArguments();
        if (q == null) {
          q = program.getNullLiteral();
        }
        arguments.add(q);
        arguments.add(program.getNumberLiteral(dispId));
        arguments.add(program.getNumberLiteral(paramCount));

        accept(rewritten);
        return false;
      }
      return super.visit(x, ctx);
    }

    /**
     * Handles immediate invocations of JSNI method references. This has to be
     * done through a wonky method "__gwt_makeJavaInvoke" to handle exceptions
     * correctly on some browsers.
     */
    @Override
    public boolean visit(JsInvocation x, JsContext<JsExpression> ctx) {
      if (x.getQualifier() instanceof JsNameRef) {
        JsNameRef ref = (JsNameRef) x.getQualifier();
        String ident = ref.getIdent();
        if (ident.startsWith("@")) {
          int dispId = dispatchInfo.getDispId(ident);

          Member member;
          if (dispId < 0) {
            member = null;
          } else {
            member = dispatchInfo.getClassInfoByDispId(dispId).getMember(dispId);
          }

          /*
           * Make sure the ident is a reference to a method or constructor and
           * not a reference to a field whose contents (e.g. a Function) we
           * intend to immediately invoke.
           * 
           * p.C::method()(); versus p.C::field();
           * 
           * Also, if the reference was to a non-existent field, we'll go ahead
           * and rewrite the call site as though -1 is a valid dispid.
           */
          if (member == null || member instanceof Method
              || member instanceof Constructor) {

            // Use a clone instead of modifying the original JSNI
            // __gwt_makeJavaInvoke(paramCount)(obj, dispId, args)
            int paramCount = 0;
            if (member instanceof Method) {
              paramCount = ((Method) member).getParameterTypes().length;
            } else if (member instanceof Constructor) {
              paramCount = ((Constructor<?>) member).getParameterTypes().length;
            }

            SourceInfo newSourceInfo = x.getSourceInfo().makeChild(getClass(),
                "Replace JSNI ref for hosted mode");
            JsInvocation inner = new JsInvocation(newSourceInfo);
            inner.setQualifier(new JsNameRef(newSourceInfo,
                "__gwt_makeJavaInvoke"));
            inner.getArguments().add(program.getNumberLiteral(paramCount));

            JsInvocation outer = new JsInvocation(newSourceInfo);
            outer.setQualifier(inner);
            JsExpression q = ref.getQualifier();
            if (q == null) {
              q = program.getNullLiteral();
            }
            List<JsExpression> arguments = outer.getArguments();
            arguments.add(q);
            arguments.add(program.getNumberLiteral(dispId));
            arguments.addAll(x.getArguments());

            accept(outer);
            return false;
          }
        }
      }
      return super.visit(x, ctx);
    }
  }

  public static final String JAVASCRIPTHOST_NAME = JavaScriptHost.class.getName();

  /**
   * Gets the body of a JSNI method, with Java refs escaped for hosted mode
   * injection.
   */
  public static String getJavaScriptForHostedMode(TreeLogger logger,
      DispatchIdOracle dispatchInfo, JsniMethod jsniMethod) {
    JsFunction func = jsniMethod.function();
    if (func == null) {
      return null;
    }
    return generateJavaScriptForHostedMode(dispatchInfo, jsniMethod.program(),
        func.getBody());
  }

  /**
   * Returns a string representing the source output of the JsNode, where all
   * JSNI idents have been replaced with legal JavaScript for hosted mode.
   */
  private static String generateJavaScriptForHostedMode(
      DispatchIdOracle dispatchInfo, JsProgram program, JsNode<?> node) {
    DefaultTextOutput out = new DefaultTextOutput(false);
    JsSourceGenWithJsniIdentFixup vi = new JsSourceGenWithJsniIdentFixup(out,
        dispatchInfo, program);
    vi.accept(node);
    return out.toString();
  }

}
