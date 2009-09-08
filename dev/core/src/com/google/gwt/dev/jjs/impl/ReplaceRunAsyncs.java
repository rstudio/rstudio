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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Replaces calls to
 * {@link com.google.gwt.core.client.GWT#runAsync(com.google.gwt.core.client.RunAsyncCallback)}
 * and
 * {@link com.google.gwt.core.client.GWT#runAsync(Class, com.google.gwt.core.client.RunAsyncCallback)
 * by calls to a fragment loader.
 */
public class ReplaceRunAsyncs {
  /**
   * Information about the replacement of one runAsync call by a call to a
   * generated code-loading method.
   */
  public static class RunAsyncReplacement implements Serializable {
    private final JMethod enclosingMethod;
    private final JMethod loadMethod;
    private final String name;
    private final int number;

    RunAsyncReplacement(int number, JMethod enclosingMethod,
        JMethod loadMethod, String name) {
      this.number = number;
      this.enclosingMethod = enclosingMethod;
      this.loadMethod = loadMethod;
      this.name = name;
    }

    /**
     * Can be null if the enclosing method cannot be designated with a JSNI
     * reference.
     */
    public JMethod getEnclosingMethod() {
      return enclosingMethod;
    }

    /**
     * The load method to request loading the code for this method.
     */
    public JMethod getLoadMethod() {
      return loadMethod;
    }

    /**
     * Return the name of this runAsync, which is specified by a class literal
     * in the two-argument version of runAsync(). Returns <code>null</code> if
     * there is no name for the call.
     */
    public String getName() {
      return name;
    }

    /**
     * The index of this runAsync, numbered from 1 to n.
     */
    public int getNumber() {
      return number;
    }

    @Override
    public String toString() {
      return "#" + number + ": " + enclosingMethod.toString();
    }
  }

  private class AsyncCreateVisitor extends JModVisitor {
    private JMethod currentMethod;
    private int entryCount = 1;

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      if (isRunAsyncMethod(method)) {
        JExpression asyncCallback;
        String name;
        switch (x.getArgs().size()) {
          case 1:
            name = null;
            asyncCallback = x.getArgs().get(0);
            break;
          case 2:
            name = ((JClassLiteral) x.getArgs().get(0)).getRefType().getName();
            asyncCallback = x.getArgs().get(1);
            break;
          default:
            throw new InternalCompilerException(
                "runAsync call found with neither 1 nor 2 arguments: " + x);
        }

        int entryNumber = entryCount++;
        JClassType loader = getFragmentLoader(entryNumber);
        JMethod loadMethod = getRunAsyncMethod(loader);
        assert loadMethod != null;
        runAsyncReplacements.put(entryNumber, new RunAsyncReplacement(
            entryNumber, currentMethod, loadMethod, name));

        JMethodCall methodCall = new JMethodCall(x.getSourceInfo(), null,
            loadMethod);
        methodCall.addArg(asyncCallback);

        program.addEntryMethod(getOnLoadMethod(loader), entryNumber);

        ctx.replaceMe(methodCall);
      }
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      currentMethod = x;
      return true;
    }

    private boolean isRunAsyncMethod(JMethod method) {
      /*
       * The method is overloaded, so check the enclosing type plus the name.
       */
      return method.getEnclosingType() == program.getIndexedType("GWT")
          && method.getName().equals("runAsync");
    }
  }

  public static void exec(TreeLogger logger, JProgram program) {
    logger.log(TreeLogger.TRACE,
        "Replacing GWT.runAsync with island loader calls");
    new ReplaceRunAsyncs(program).execImpl();
  }

  /**
   * Extract the initializer of AsyncFragmentLoader.BROWSER_LOADER. A couple of
   * parts of the compiler modify this constructor call.
   */
  static JMethodCall getBrowserLoaderConstructor(JProgram program) {
    JField field = program.getIndexedField("AsyncFragmentLoader.BROWSER_LOADER");
    JMethodCall constructorCall = (JMethodCall) field.getDeclarationStatement().getInitializer();
    assert constructorCall.getArgs().size() == 4;
    return constructorCall;
  }

  private JProgram program;

  private Map<Integer, RunAsyncReplacement> runAsyncReplacements = new HashMap<Integer, RunAsyncReplacement>();

  private ReplaceRunAsyncs(JProgram program) {
    this.program = program;
  }

  private void execImpl() {
    AsyncCreateVisitor visitor = new AsyncCreateVisitor();
    visitor.accept(program);
    setNumEntriesInAsyncFragmentLoader(visitor.entryCount);
    program.setRunAsyncReplacements(runAsyncReplacements);
  }

  private JClassType getFragmentLoader(int fragmentNumber) {
    String fragmentLoaderClassName = FragmentLoaderCreator.ASYNC_LOADER_PACKAGE
        + "." + FragmentLoaderCreator.ASYNC_LOADER_CLASS_PREFIX
        + fragmentNumber;
    JType result = program.getFromTypeMap(fragmentLoaderClassName);
    assert (result != null);
    assert (result instanceof JClassType);
    return (JClassType) result;
  }

  private JMethod getOnLoadMethod(JClassType loaderType) {
    assert loaderType != null;
    assert loaderType.getMethods() != null;
    for (JMethod method : loaderType.getMethods()) {
      if (method.getName().equals("onLoad")) {
        assert (method.isStatic());
        assert (method.getParams().size() == 0);
        return method;
      }
    }
    assert false;
    return null;
  }

  private JMethod getRunAsyncMethod(JClassType loaderType) {
    assert loaderType != null;
    assert loaderType.getMethods() != null;
    for (JMethod method : loaderType.getMethods()) {
      if (method.getName().equals("runAsync")) {
        assert (method.isStatic());
        assert (method.getParams().size() == 1);
        assert (method.getParams().get(0).getType().getName().equals(FragmentLoaderCreator.RUN_ASYNC_CALLBACK));
        return method;
      }
    }
    return null;
  }

  private void setNumEntriesInAsyncFragmentLoader(int entryCount) {
    JMethodCall constructorCall = getBrowserLoaderConstructor(program);
    assert constructorCall.getArgs().get(0).getType() == JPrimitiveType.INT;
    constructorCall.setArg(0, program.getLiteralInt(entryCount));
  }
}
