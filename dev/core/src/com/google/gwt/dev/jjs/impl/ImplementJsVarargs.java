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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayLength;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JForStatement;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JUnaryOperation;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;

import java.util.Collections;

/**
 * Implements JavaScript varargs calling convention by rewriting varargs calls and adding a
 * prolog to varargs JsMethods.
 * <p>
 * At the calls sites, inline array creation is replaced by array literals, which in turn will be
 * unwrapped as individual parameters at generation time.
 * <p>
 * To implement varargs methods, we analyze the usage of the varargs parameter to determine whether
 * it can be accessed directly (with possibly an offset in the index) or it has to be copied.
 */
public class ImplementJsVarargs {
  /**
   * Analyzes a method body to check whether the varargs parameter can be used directly or not.
   * <p>
   * The arguments variable cannot be used directly if is referenced without indexing or accessing
   * its length, or if it is written to.
   */
  private class NeedsArgumentsCopyAnalyzer extends JVisitor {

    private VarargsProcessingResult result = VarargsProcessingResult.SIMPLE_ACCESS;
    private JParameter varargsParameter;
    private int varargsParameterIndex;

    private NeedsArgumentsCopyAnalyzer(JMethod method) {
      assert method.isJsMethodVarargs();
      this.varargsParameter = Iterables.getLast(method.getParams());
      this.varargsParameterIndex = method.getParams().size() - 1;
      if (varargsParameterIndex != 0) {
        upgradeResult(VarargsProcessingResult.OFFSET_ACCESS);
      }
    }

    @Override
    public void endVisit(JParameterRef x, Context ctx) {
      // Any reference that is not .length or indexed means that we need to copy the varargs array.
      if (isVarargsReference(x)) {
        upgradeResult(VarargsProcessingResult.GENERAL_ACCESS);
      }
    }

    @Override
    public boolean visit(JArrayLength x, Context ctx) {
      if (isVarargsReference(x.getInstance())) {
        // This is a safe reference.
        return false;
      }
      return true;
    }

    @Override
    public boolean visit(JArrayRef x, Context ctx) {
      if (isVarargsReference(x.getInstance())) {
        // This is a safe reference, so only check the index expression.
        accept(x.getIndexExpr());
        return false;
      }

      return true;
    }

    @Override
    public boolean visit(JBinaryOperation x, Context ctx) {
      if (isModifyingVarargs(x)) {
          // The varargs parameter is written to, so upgrade to copy.
          upgradeResult(VarargsProcessingResult.GENERAL_ACCESS);
        return false;
      }
      return true;
    }

    @Override
    public boolean visit(JUnaryOperation x, Context ctx) {
      if (isModifyingVarargs(x)) {
        // The varargs parameter is written to, so upgrade to copy.
        upgradeResult(VarargsProcessingResult.GENERAL_ACCESS);
        return false;
      }
      return true;
    }

    @Override
    public boolean visit(JMethodCall x, Context ctx) {
      // Allow
      if (x.getTarget().isJsMethodVarargs() && x.getArgs().size() == 1
          && isVarargsReference(x.getArgs().get(0))) {
        // The varargs parameter is passed directly and it is the only parameter, so if it is the
        // only parameter in the current method it can be passed directly.
        upgradeResult(VarargsProcessingResult.PASS_WHOLE);
        if (x.getInstance() != null) {
          accept(x.getInstance());
        }
        for (JExpression arg : x.getArgs().subList(1, x.getArgs().size())) {
          accept(arg);
        }
        return false;
      }
      return true;
    }

    private boolean isModifyingVarargs(JBinaryOperation x) {
      if (!x.getOp().isAssignment()) {
        return false;
      }
      if (!(x.getLhs() instanceof JArrayRef)) {
        return false;
      }
      JArrayRef arrayRef = (JArrayRef) x.getLhs();
      JExpression instance = arrayRef.getInstance();
      return isVarargsReference(instance);
    }

    private boolean isModifyingVarargs(JUnaryOperation x) {
      if (!x.getOp().isModifying()) {
        return false;
      }
      if (!(x.getArg() instanceof JArrayRef)) {
        return false;
      }
      JArrayRef arrayRef = (JArrayRef) x.getArg();
      JExpression instance = arrayRef.getInstance();
      return isVarargsReference(instance);
    }

    private boolean isVarargsReference(JExpression instance) {
      if (!(instance instanceof JParameterRef)) {
        return false;
      }

      return (((JParameterRef) instance).getTarget() == varargsParameter);
    }

    private void upgradeResult(VarargsProcessingResult upgradeTo) {
      result = VarargsProcessingResult.join(result, upgradeTo);
    }
  }

  // Defines the analysis lattice
  //                            SIMPLE_ACCESS   (only arguments[i] or arguments.length, no writing)
  //                               /        \
  //  (complete passing of        /          \
  //   arguments)           PASS_WHOLE  OFFSET_ACCESS   (Simple access but needs to offset index)
  //                              \         /
  //                               \       /
  //                             GENERAL_ACCESS
  //
  private enum VarargsProcessingResult {
    SIMPLE_ACCESS, PASS_WHOLE, OFFSET_ACCESS, GENERAL_ACCESS;

    private static VarargsProcessingResult join(
        VarargsProcessingResult thisResult, VarargsProcessingResult thatResult) {
      if (thisResult.ordinal() > thatResult.ordinal()) {
        VarargsProcessingResult swap = thisResult;
        thisResult = thatResult;
        thatResult = swap;
      }

      if ((thisResult == PASS_WHOLE &&  thatResult == OFFSET_ACCESS)) {
        return GENERAL_ACCESS;
      }
      return thatResult;
    }
  }

  private VarargsProcessingResult needsVarargsProcessing(JMethod method) {
    NeedsArgumentsCopyAnalyzer analyzer =  new NeedsArgumentsCopyAnalyzer(method);
    analyzer.accept(method);
    return analyzer.result;
  }

  private abstract class VarargsReplacer {
    abstract JExpression replace(JParameterRef expression);

    JExpression replace(JArrayRef expression) {
      return new JArrayRef(expression.getSourceInfo(),
          replace((JParameterRef)  expression.getInstance()),
          expression.getIndexExpr());
    }

    JExpression replace(JArrayLength expression) {
      return new JArrayLength(expression.getSourceInfo(),
          replace((JParameterRef)  expression.getInstance()));
    }
  }

  /**
   * Replaces varargs parameter accesses with accesses to the copy.
   */
  private class ReplaceVarargsVariable extends VarargsReplacer {
    private JLocal localVariable;
    ReplaceVarargsVariable(JLocal localVariable) {
      this.localVariable = localVariable;
    }

    @Override
    public JExpression replace(JParameterRef expression) {
      return localVariable.createRef(expression.getSourceInfo());
    }
  }

  /**
   * Fixes this indexing of vararg accesses.
   */
  private class ReindexAccess extends VarargsReplacer {
    private int varargsIndex;
    ReindexAccess(int varargsIndex) {
      this.varargsIndex = varargsIndex;
    }

    @Override
    public JExpression replace(JParameterRef expression) {
      return expression;
    }

    JExpression replace(JArrayRef expression) {
      SourceInfo sourceInfo = expression.getSourceInfo();
      return new JArrayRef(expression.getSourceInfo(),
          expression.getInstance(),
          new JBinaryOperation(sourceInfo, JPrimitiveType.INT, JBinaryOperator.ADD,
              expression.getIndexExpr(), new JIntLiteral(sourceInfo, varargsIndex)));
    }

    JExpression replace(JArrayLength expression) {
      SourceInfo sourceInfo = expression.getSourceInfo();
      return new JBinaryOperation(sourceInfo, JPrimitiveType.INT, JBinaryOperator.SUB,
              expression, new JIntLiteral(sourceInfo, varargsIndex));
    }
  }

  private class VarargsMethodNormalizer extends JModVisitor {
    private JParameter varargsParameter;
    private int varargsIndex;
    private VarargsReplacer replacer;
    private JLocal argumentsCopyVariable;

    @Override
    public boolean visit(JMethod x, Context ctx) {
      if (!x.isJsMethodVarargs()) {
        return false;
      }
      varargsParameter = Iterables.getLast(x.getParams());
      varargsIndex = x.getParams().size() - 1;

      // JsVarargs parameter can be assumend not null in the implementing method
      varargsParameter.setType(varargsParameter.getType().strengthenToNonNull());

      argumentsCopyVariable = null;
      switch (needsVarargsProcessing(x)) {
        case GENERAL_ACCESS:
          argumentsCopyVariable = JProgram.createLocal(varargsParameter.getSourceInfo(),
              varargsParameter.getName(), varargsParameter.getType(), false,
              (JMethodBody) x.getBody());
          replacer = new ReplaceVarargsVariable(argumentsCopyVariable);
          return true;
        case OFFSET_ACCESS:
          replacer = new ReindexAccess(varargsIndex);
          return true;
       default:
          return false;
      }
    }

    @Override
    public void endVisit(JParameterRef x, Context ctx) {
      if (x.getTarget() == varargsParameter) {
        maybeReplace(x, replacer.replace(x), ctx);
      }
    }

    @Override
    public void endVisit(JArrayRef x, Context ctx) {
      if (x.getInstance() instanceof JParameterRef
          && ((JParameterRef) x.getInstance()).getTarget() == varargsParameter) {
        maybeReplace(x, replacer.replace(x), ctx);
      }
    }

    @Override
    public void endVisit(JArrayLength x, Context ctx) {
      if (x.getInstance() instanceof JParameterRef
          && ((JParameterRef) x.getInstance()).getTarget() == varargsParameter) {
        maybeReplace(x, replacer.replace(x), ctx);
      }
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      if (!x.isJsMethodVarargs()) {
        return;
      }
      // rename the varargs variable to _arguments_.
      varargsParameter.setName("_arguments_");
    }

    private void maybeReplace(JExpression x, JExpression replacement, Context ctx) {
      if (replacement != x) {
        ctx.replaceMe(replacement);
      }
    }

    @Override
    public void endVisit(JMethodBody x, Context ctx) {
      if (argumentsCopyVariable == null) {
        return;
      }

      // Needs to populate the copy; add preamble.
      //
      // {
      //   <Type>[] args = new <Type>[arguments.length - offset];
      //   for (int $i = 0; $i < arguments.length - offset; i++) {
      //     args[i] = arguments[i + offset];
      //   }
      // }

      SourceInfo sourceInfo = varargsParameter.getSourceInfo();
      JBlock preamble = new JBlock(sourceInfo);
      JArrayType varargsArrayType = (JArrayType) varargsParameter.getType().getUnderlyingType();

      // (1) varargs_ = new VarArgsType[varargs.length - varArgsParameterIndex]
      JExpression lengthMinusVarargsIndex = varargsIndex == 0
          ? new JArrayLength(sourceInfo, varargsParameter.createRef(sourceInfo))
          : new JBinaryOperation(sourceInfo, JPrimitiveType.INT, JBinaryOperator.SUB,
              new JArrayLength(sourceInfo, varargsParameter.createRef(sourceInfo)),
              new JIntLiteral(sourceInfo, varargsIndex));
      JNewArray arrayVariable = JNewArray.createArrayWithDimensionExpressions(sourceInfo,
          varargsArrayType, Collections.singletonList(lengthMinusVarargsIndex));
      preamble.addStmt(new JDeclarationStatement(
          sourceInfo, argumentsCopyVariable.createRef(sourceInfo), arrayVariable));

      JLocal index = JProgram.createLocal(sourceInfo, "$i", JPrimitiveType.INT, false, x);

      // (2) (copy loop body)  varargs_[i] = varargs[i + varargsIndex];
      JExpression iPlusVarargsIndex = varargsIndex == 0 ? index.createRef(sourceInfo)
          : new JBinaryOperation(sourceInfo, JPrimitiveType.INT, JBinaryOperator.ADD,
              index.createRef(sourceInfo), new JIntLiteral(sourceInfo, varargsIndex));

      JBlock block = new JBlock(sourceInfo);
      block.addStmt(new JBinaryOperation(
          sourceInfo,
          varargsArrayType.getElementType(),
          JBinaryOperator.ASG,
          new JArrayRef(sourceInfo, replacer.replace(varargsParameter.createRef(sourceInfo)),
              index.createRef(sourceInfo)),
          new JArrayRef(sourceInfo, varargsParameter.createRef(sourceInfo), iPlusVarargsIndex))
              .makeStatement());
      // (3) for (int $i = 0 ; i < arguments.length - index; i++) {
      //       varargs_[i] = varargs[i + varargsIndex];
      //     }
      preamble.addStmt(new JForStatement(sourceInfo, Collections.<JStatement>singletonList(
          new JDeclarationStatement(sourceInfo, index.createRef(sourceInfo), JIntLiteral.ZERO)),
          new JBinaryOperation(sourceInfo, JPrimitiveType.INT,JBinaryOperator.LT,
                  index.createRef(sourceInfo),
              new CloneExpressionVisitor().cloneExpression(lengthMinusVarargsIndex)),
          new JPostfixOperation(sourceInfo, JUnaryOperator.INC, index.createRef(sourceInfo)),
          block));
      x.getStatements().add(0, preamble);
    }
  }

  // Normalizes JsVarargsCalls so that
  //  (1) inline new array expressions resulting from a "regular" varargs invocation are replaced
  //      by plain array literals.
  //  (2) side effecting instances in JsVarargs instance method calls with array calling convention
  //      are hoited into temporary variables.
  private class VarargsCallsNormalizer extends JModVisitor {
    private JMethodBody currentMethodBody;

    @Override
    public boolean visit(JMethodBody x, Context ctx) {
      currentMethodBody = x;
      return true;
    }

    @Override
    public void endVisit(JMethodBody x, Context ctx) {
      currentMethodBody = null;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      if (!method.isJsMethodVarargs()) {
        return;
      }

      int varargIndex = method.getParams().size() - 1;
      JExpression varargArgument = x.getArgs().get(varargIndex);
      if (varargArgument instanceof JNewArray) {
        JNewArray varargArray = (JNewArray) varargArgument;
        if (varargArray.getInitializers() != null) {
          x.setArg(varargIndex, ArrayNormalizer.getInitializerArray(varargArray));
          madeChanges();
          return;
        }
      }

      SourceInfo varargsArgumentsourceInfo = varargArgument.getSourceInfo();
      if (varargArgument.getType().canBeNull()) {
        // varargsArgument => Array.ensureNotNull(varargsArgument)
        x.setArg(varargIndex, new JMethodCall(varargsArgumentsourceInfo, null,
            program.getIndexedMethod(RuntimeConstants.ARRAY_ENSURE_NOT_NULL), varargArgument));
      }

      // Passed as an array to varargs method will result in an apply call, in which case hoist the
      // qualifier to make sure it is only evaluated once.
      JExpression instance = x.getInstance();
      if (x.getTarget().needsDynamicDispatch() && !x.isStaticDispatchOnly()
          && instance != null && !(instance instanceof JVariableRef)) {
        // Move the potentially sideffecting qualifier to a temporary variable so that
        // the code generation for calls that need .apply don't need to hande the case.
        SourceInfo sourceInfo = x.getSourceInfo();
        JLocal tempInstance = JProgram.createLocal(sourceInfo, "$instance",
            instance.getType(), false, currentMethodBody);
        // (tempInstance = instance,
        //  tempInstance.method(pars);
        ctx.replaceMe(JjsUtils.createOptimizedMultiExpression(
            new JBinaryOperation(sourceInfo, instance.getType(),
                JBinaryOperator.ASG, tempInstance.createRef(sourceInfo), instance),
            new JMethodCall(x, tempInstance.createRef(sourceInfo), x.getArgs())));
      }
    }
  }

  public static void exec(JProgram program) {
    new ImplementJsVarargs(program).execImpl();
  }

  private final JProgram program;

  private ImplementJsVarargs(JProgram program) {
    this.program = program;
  }

  private void execImpl() {
    new VarargsMethodNormalizer().accept(program);
    new VarargsCallsNormalizer().accept(program);
  }
}
