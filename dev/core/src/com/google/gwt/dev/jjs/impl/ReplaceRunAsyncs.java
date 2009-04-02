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
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Replaces calls to
 * {@link com.google.gwt.core.client.GWT#runAsync(com.google.gwt.core.client.RunAsyncCallback)}"
 * by calls to a fragment loader.
 */
public class ReplaceRunAsyncs {
  private class AsyncCreateVisitor extends JModVisitor {
    private JMethod currentMethod;
    private Map<Integer, String> splitPointMap = new TreeMap<Integer, String>();
    private Map<String, Integer> methodCount = new HashMap<String, Integer>();
    private int entryCount = 1;

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      if (method == program.getIndexedMethod("GWT.runAsync")) {
        assert (x.getArgs().size() == 1);
        JExpression asyncCallback = x.getArgs().get(0);

        int entryNumber = entryCount++;
        logger.log(TreeLogger.DEBUG, "Assigning split point #" + entryNumber
            + " in method " + fullMethodDescription(currentMethod));

        String methodDescription = fullMethodDescription(currentMethod);
        if (methodCount.containsKey(methodDescription)) {
          methodCount.put(methodDescription,
              methodCount.get(methodDescription) + 1);
          methodDescription += "#"
              + Integer.toString(methodCount.get(methodDescription));
        } else {
          methodCount.put(methodDescription, 1);
        }
        splitPointMap.put(entryNumber, methodDescription);

        JClassType loader = getFragmentLoader(entryNumber);
        JMethod loadMethod = getRunAsyncMethod(loader);
        assert loadMethod != null;

        JMethodCall methodCall = new JMethodCall(program, x.getSourceInfo(),
            null, loadMethod);
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
  }

  public static int exec(TreeLogger logger, JProgram program) {
    return new ReplaceRunAsyncs(logger, program).execImpl();
  }

  private static String fullMethodDescription(JMethod method) {
    return (method.getEnclosingType().getName() + "." + JProgram.getJsniSig(method));
  }

  private final TreeLogger logger;
  private JProgram program;

  private ReplaceRunAsyncs(TreeLogger logger, JProgram program) {
    this.logger = logger.branch(TreeLogger.TRACE,
        "Replacing GWT.runAsync with island loader calls");
    this.program = program;
  }

  private int execImpl() {
    AsyncCreateVisitor visitor = new AsyncCreateVisitor();
    visitor.accept(program);
    setNumEntriesInAsyncFragmentLoader(visitor.entryCount);
    program.setSplitPointMap(visitor.splitPointMap);

    return visitor.entryCount;
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
    assert loaderType.methods != null;
    for (JMethod method : loaderType.methods) {
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
    assert loaderType.methods != null;
    for (JMethod method : loaderType.methods) {
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
    JField field = program.getIndexedField("AsyncFragmentLoader.numEntries");
    field.getDeclarationStatement().initializer = program.getLiteralInt(entryCount);
  }
}
