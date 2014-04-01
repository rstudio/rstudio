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
package com.google.gwt.dev.jjs.impl.codesplitter;

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
import com.google.gwt.dev.jjs.ast.JNumericEntry;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.ArrayList;
import java.util.Collections;
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
  private class AsyncCreateVisitor extends JModVisitor {
    private JMethod currentMethod;
    private final JMethod runAsyncOnsuccess = program
        .getIndexedMethod("RunAsyncCallback.onSuccess");

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      if (method == runAsyncOnsuccess
          && (currentMethod != null && currentMethod.getEnclosingType() == program
              .getIndexedType("AsyncFragmentLoader"))) {
        /*
         * Note: The volatile marker on the method flags it so that we don't
         * optimize calls from AsyncFragmentLoader to implementations of
         * RunAsyncCallback.onSuccess(). This can defeat code splitting.
         */
        x.setVolatile();
        return;
      }
      boolean explicitClassLiteral = false;
      if (isRunAsyncMethod(method)) {
        JExpression asyncCallback;
        String name;
        switch (x.getArgs().size()) {
          case 1:
            name = getImplicitName(currentMethod);
            asyncCallback = x.getArgs().get(0);
            break;
          case 2:
            JExpression arg0 = x.getArgs().get(0);
            if (!(arg0 instanceof JClassLiteral)) {
              error(arg0.getSourceInfo(),
                  "Only class literals may be used to name a call to GWT.runAsync()");
              return;
            }
            name = nameFromClassLiteral((JClassLiteral) arg0);
            explicitClassLiteral = true;
            asyncCallback = x.getArgs().get(1);
            break;
          default:
            throw new InternalCompilerException(
                "runAsync call found with neither 1 nor 2 arguments: " + x);
        }

        int splitPoint = runAsyncs.size() + 1;
        SourceInfo info = x.getSourceInfo();

        JMethod runAsyncMethod = program.getIndexedMethod("AsyncFragmentLoader.runAsync");
        assert runAsyncMethod != null;
        JMethodCall runAsyncCall = new JMethodCall(info, null, runAsyncMethod);
        runAsyncCall.addArg(new JNumericEntry(info, "RunAsyncFragmentIndex", splitPoint));
        runAsyncCall.addArg(asyncCallback);

        JReferenceType callbackType = (JReferenceType) asyncCallback.getType();
        callbackType = callbackType.getUnderlyingType();
        JMethod callbackMethod;
        if (callbackType instanceof JClassType) {
          callbackMethod =
              program.typeOracle.getMethodBySignature((JClassType) callbackType, "onSuccess()V");
        } else {
          callbackMethod = program.getIndexedMethod("RunAsyncCallback.onSuccess");
        }
        if (callbackMethod == null) {
          error(x.getSourceInfo(), "Only a RunAsyncCallback with a defined onSuccess() can "
              + "be passed to runAsync().");
          return;
        }
        JMethodCall onSuccessCall = new JMethodCall(info, asyncCallback, callbackMethod);

        JRunAsync runAsyncNode = new JRunAsync(info, splitPoint, name, explicitClassLiteral,
            runAsyncCall, onSuccessCall);
        runAsyncs.add(runAsyncNode);
        ctx.replaceMe(runAsyncNode);
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
    private final Map<String, List<JRunAsync>> replacementsByName;
    private final JMethod runAsyncCode;

    public ReplaceRunAsyncResources() {
      replacementsByName = new HashMap<String, List<JRunAsync>>();
      runAsyncCode = program.getIndexedMethod("RunAsyncCode.runAsyncCode");
      for (JRunAsync replacement : runAsyncs) {
        String name = replacement.getName();
        if (name != null) {
          List<JRunAsync> list = replacementsByName.get(name);
          if (list == null) {
            list = new ArrayList<JRunAsync>();
            replacementsByName.put(name, list);
          }
          list.add(replacement);
        }
      }
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      if (x.getTarget() == runAsyncCode) {
        JExpression arg0 = x.getArgs().get(0);
        if (!(arg0 instanceof JClassLiteral)) {
          error(arg0.getSourceInfo(), "Only a class literal may be passed to runAsyncCode");
          return;
        }
        JClassLiteral lit = (JClassLiteral) arg0;
        String name = nameFromClassLiteral(lit);
        List<JRunAsync> matches = replacementsByName.get(name);
        SourceInfo info = x.getSourceInfo();
        if (matches == null || matches.size() == 0) {
          error(info, "No runAsync call is named " + name);
          return;
        }
        if (matches.size() > 1) {
          TreeLogger branch = error(info, "Multiple runAsync calls are named " + name);
          List<String> errors = new ArrayList<String>();
          for (JRunAsync match : matches) {
            errors.add("One call is at '" + match.getSourceInfo().getFileName() + ':'
                + match.getSourceInfo().getStartLine() + "'");
          }
          Collections.sort(errors);
          for (String error : errors) {
            branch.log(TreeLogger.ERROR, error);
          }
          return;
        }
        int splitPoint = matches.get(0).getRunAsyncId();
        JMethodCall newCall =
            new JMethodCall(info, null, program
                .getIndexedMethod("RunAsyncCode.forSplitPointNumber"));
        newCall.addArg(new JNumericEntry(info, "RunAsyncFragmentIndex", splitPoint));
        ctx.replaceMe(newCall);
      }
    }
  }

  public static void exec(TreeLogger logger, JProgram program)
      throws UnableToCompleteException {
    Event codeSplitterEvent =
        SpeedTracerLogger.start(CompilerEventType.CODE_SPLITTER, "phase", "ReplaceRunAsyncs");
    TreeLogger branch =
        logger.branch(TreeLogger.TRACE, "Replacing GWT.runAsync with island loader calls");
    new ReplaceRunAsyncs(branch, program).execImpl();
    codeSplitterEvent.end();
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

  static String getImplicitName(JMethod method) {
    String name;
    StringBuilder sb = new StringBuilder();
    sb.append('@');
    sb.append(method.getEnclosingType().getName());
    sb.append("::");
    sb.append(JProgram.getJsniSig(method, false));
    name = sb.toString();
    return name;
  }

  /**
   * Convert a class literal to a runAsync name.
   */
  private static String nameFromClassLiteral(JClassLiteral classLiteral) {
    return classLiteral.getRefType().getName();
  }

  private boolean errorsFound = false;
  private final TreeLogger logger;
  private final JProgram program;

  private final List<JRunAsync> runAsyncs = new ArrayList<JRunAsync>();

  private ReplaceRunAsyncs(TreeLogger logger, JProgram program) {
    this.logger = logger;
    this.program = program;
  }

  private TreeLogger error(SourceInfo info, String message) {
    errorsFound = true;
    TreeLogger fileLogger =
        logger.branch(TreeLogger.ERROR, "Errors in '" + info.getFileName() + "'");
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
    setNumEntriesInAsyncFragmentLoader(runAsyncs.size() + 1);
    program.setRunAsyncs(runAsyncs);
    new ReplaceRunAsyncResources().accept(program);
    if (errorsFound) {
      throw new UnableToCompleteException();
    }
  }

  private void setNumEntriesInAsyncFragmentLoader(int entryCount) {
    JMethodCall constructorCall = getBrowserLoaderConstructor(program);
    assert constructorCall.getArgs().get(0).getType() == JPrimitiveType.INT;
    constructorCall.setArg(0,
        new JNumericEntry(constructorCall.getSourceInfo(), "RunAsyncFragmentCount", entryCount));
  }
}
