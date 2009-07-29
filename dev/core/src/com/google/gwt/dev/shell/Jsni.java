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
import com.google.gwt.dev.js.JsSourceGenerationVisitor;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;

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

  /**
   * Gets the body of a JSNI method, with Java refs escaped for hosted mode
   * injection.
   */
  public static String getJavaScriptForHostedMode(TreeLogger logger,
      DispatchIdOracle dispatchInfo, JsniMethod jsniMethod) {
    /*
     * Surround the original JS body statements with a try/catch so that we can
     * map JavaScript exceptions back into Java. Note that the method body
     * itself will print curly braces, so we don't need them around the
     * try/catch.
     */
    String jsTry = "try ";
    String jsCatch = " catch (e) {\n  __static[\"@" + Jsni.JAVASCRIPTHOST_NAME
        + "::exceptionCaught(Ljava/lang/Object;)\"](e);\n" + "}\n";
    JsFunction func = jsniMethod.function(logger);
    if (func == null) {
      return null;
    }
    return jsTry + generateJavaScriptForHostedMode(func.getBody()) + jsCatch;
  }

  /**
   * Returns a string representing the source output of the JsNode, where all
   * JSNI idents have been replaced with legal JavaScript for hosted mode.
   */
  private static String generateJavaScriptForHostedMode(JsNode<?> node) {
    DefaultTextOutput out = new DefaultTextOutput(false);
    JsSourceGenWithJsniIdentFixup vi = new JsSourceGenWithJsniIdentFixup(out);
    vi.accept(node);
    return out.toString();
  }

}
