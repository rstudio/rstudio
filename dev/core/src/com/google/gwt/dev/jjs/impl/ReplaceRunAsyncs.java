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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Replaces calls to
 * {@link com.google.gwt.core.client.GWT#runAsync(com.google.gwt.core.client.RunAsyncCallback)}
 * and
 * {@link com.google.gwt.core.client.GWT#runAsync(Class, com.google.gwt.core.client.RunAsyncCallback)}
 * by calls to a fragment loader. Additionally, replaces access to
 * {@link com.google.gwt.core.client.prefetch.RunAsyncCode#runAsyncCode(Class)}
 * by an equivalent call using an integer rather than a class literal.
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
            name = nameFromClassLiteral((JClassLiteral) x.getArgs().get(0));
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
  private class ReplaceRunAsyncResources extends JModVisitor {
    private Map<String, List<RunAsyncReplacement>> replacementsByName;

    public ReplaceRunAsyncResources() {
      replacementsByName = new HashMap<String, List<RunAsyncReplacement>>();
      for (RunAsyncReplacement replacement : runAsyncReplacements.values()) {
        String name = replacement.getName();
        if (name != null) {
          List<RunAsyncReplacement> list = replacementsByName.get(name);
          if (list == null) {
            list = new ArrayList<RunAsyncReplacement>();
            replacementsByName.put(name, list);
          }
          list.add(replacement);
        }
      }
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      if (x.getTarget() == program.getIndexedMethod("RunAsyncCode.runAsyncCode")) {
        JExpression arg0 = x.getArgs().get(0);
        if (!(arg0 instanceof JClassLiteral)) {
          error(arg0.getSourceInfo(),
              "Only a class literal may be passed to runAsyncCode");
          return;
        }
        JClassLiteral lit = (JClassLiteral) arg0;
        String name = nameFromClassLiteral(lit);
        List<RunAsyncReplacement> matches = replacementsByName.get(name);
        if (matches == null || matches.size() == 0) {
          error(x.getSourceInfo(), "No runAsync call is named " + name);
          return;
        }
        if (matches.size() > 1) {
          TreeLogger branch = error(x.getSourceInfo(),
              "Multiple runAsync calls are named " + name);
          for (RunAsyncReplacement match : matches) {
            branch.log(TreeLogger.ERROR, "One call is in "
                + methodDescription(match.getEnclosingMethod()));
          }
          return;
        }
        Integer splitPoint = matches.get(0).getNumber();

        JMethodCall newCall = new JMethodCall(x.getSourceInfo(), null,
            program.getIndexedMethod("RunAsyncCode.forSplitPointNumber"));
        newCall.addArg(program.getLiteralInt(splitPoint));
        ctx.replaceMe(newCall);
      }
    }

    private String methodDescription(JMethod method) {
      StringBuilder desc = new StringBuilder();
      desc.append(method.getEnclosingType().getName());
      desc.append(".");
      desc.append(method.getName());
      desc.append(" (");
      desc.append(method.getSourceInfo().getFileName());
      desc.append(':');
      desc.append(method.getSourceInfo().getStartLine());
      desc.append(")");

      return desc.toString();
    }
  }

  public static void exec(TreeLogger logger, JProgram program)
      throws UnableToCompleteException {
    TreeLogger branch = logger.branch(TreeLogger.TRACE,
        "Replacing GWT.runAsync with island loader calls");
    new ReplaceRunAsyncs(branch, program).execImpl();
  }

  /**
   * Extract the initializer of AsyncFragmentLoader.BROWSER_LOADER. A couple of
   * parts of the compiler modify this initializer call.
   */
  static JMethodCall getBrowserLoaderConstructor(JProgram program) {
    JField field = program.getIndexedField("AsyncFragmentLoader.BROWSER_LOADER");
    JMethodCall initializerCall = (JMethodCall) field.getDeclarationStatement().getInitializer();
    assert initializerCall.getArgs().size() == 2;
    return initializerCall;
  }

  /**
   * Convert a class literal to a runAsync name.
   */
  private static String nameFromClassLiteral(JClassLiteral classLiteral) {
    return classLiteral.getRefType().getName();
  }

  private boolean errorsFound = false;
  private final TreeLogger logger;
  private JProgram program;

  private Map<Integer, RunAsyncReplacement> runAsyncReplacements = new HashMap<Integer, RunAsyncReplacement>();

  private ReplaceRunAsyncs(TreeLogger logger, JProgram program) {
    this.logger = logger;
    this.program = program;
  }

  private TreeLogger error(SourceInfo info, String message) {
    errorsFound = true;
    TreeLogger fileLogger = logger.branch(TreeLogger.ERROR, "Error in '"
        + info.getFileName() + "'");
    String linePrefix = "";
    if (info.getStartLine() > 0) {
      linePrefix = "Line " + info.getStartLine() + ": ";
    }
    fileLogger.log(TreeLogger.ERROR, linePrefix + message);
    return fileLogger;
  }

  private void execImpl() throws UnableToCompleteException {
    AsyncCreateVisitor visitor = new AsyncCreateVisitor();
    visitor.accept(program);
    setNumEntriesInAsyncFragmentLoader(visitor.entryCount);
    program.setRunAsyncReplacements(runAsyncReplacements);
    new ReplaceRunAsyncResources().accept(program);
    if (errorsFound) {
      throw new UnableToCompleteException();
    }
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
