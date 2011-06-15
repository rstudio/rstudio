/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.jjs.impl.gflow.cfg;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JBreakStatement;
import com.google.gwt.dev.jjs.ast.JCaseStatement;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JContinueStatement;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JDoStatement;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JForStatement;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JLabeledStatement;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReboundEntryPoint;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JSwitchStatement;
import com.google.gwt.dev.jjs.ast.JThrowStatement;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.JUnaryOperation;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.JWhileStatement;
import com.google.gwt.dev.util.Preconditions;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Builder for CFG graph.
 * 
 * Resulting CFG graph contains as much information as needed by current
 * analysis set. The amount of detail in call graph can be shrink or extend over
 * time. Current CfgNode inheritance tree gives an accurate understanding of
 * current detail level.
 * 
 * To build an accurate representation of control flow graph in the presence of
 * exceptions, we maintain all possible exit reason from statement. (This is
 * called completion in JLS. We use exit name in code for brevity).
 * 
 * CfgBuilder also tries to assign cfg node parents to correspond to AST tree
 * structure. This is not always correct (yet). But it is always guaranteed that
 * you will always meet a cfg node corresponding to containing statements when
 * traversing cfg node parents.
 * 
 * Every statement always has the corresponding node in cfg graph.
 * 
 * TODO: such nodes as if, etc, should be parent of their expressions.
 */
public class CfgBuilder {
  /**
   * Visitor class which does all cfg building.
   */
  private static class BuilderVisitor extends JVisitor {

    /**
     * Representation of possible exits from statement. Emulates algebraic data
     * type to save memory.
     */
    private static class Exit {
      private enum Reason {
        /**
         * Exit with a break statement.
         */
        BREAK,
        /**
         * Exit with case else.
         */
        CASE_ELSE,
        /**
         * Exit with case then.
         */
        CASE_THEN,
        /**
         * Exit with continue statement.
         */
        CONTINUE,
        /**
         * Normal exit from statement.
         */
        NORMAL,
        /**
         * Exit with return statement.
         */
        RETURN,
        /**
         * Exit with exception throwing.
         */
        THROW
      }

      private static Exit createBreak(CfgNode<?> node, String label) {
        return new Exit(Reason.BREAK, node, null, label, null);
      }

      private static Exit createCaseElse(CfgNode<?> node) {
        return new Exit(Reason.CASE_ELSE, node, null, null, 
            CfgConditionalNode.ELSE);
      }

      private static Exit createCaseThen(CfgNode<?> node) {
        return new Exit(Reason.CASE_THEN, node, null, null, 
            CfgConditionalNode.THEN);
      }

      private static Exit createContinue(CfgNode<?> node, String label) {
        return new Exit(Reason.CONTINUE, node, null, label, null);
      }

      private static Exit createNormal(CfgNode<?> node, String role) {
        return new Exit(Reason.NORMAL, node, null, null, role);
      }

      private static Exit createReturn(CfgNode<?> node) {
        return new Exit(Reason.RETURN, node, null, null, null);
      }

      private static Exit createThrow(CfgNode<?> node,
          JType exceptionType, String role) {
        return new Exit(Reason.THROW, node, exceptionType, null, role);
      }

      /**
       * Exception type for <code>THROW</code> exit.
       */
      private final JType exceptionType;
      /**
       * Break/continue target label. Null if label wasn't set.
       */
      private final String label;
      /**
       * Cfg node which generated this exit.
       */
      private final CfgNode<?> node;
      /**
       * Exit reason.
       */
      private final Reason reason;
      /**
       * Role for all cfg edges generated from this exit.
       */
      private final String role;

      private Exit(Reason reason, CfgNode<?> source, 
          JType exceptionType, String label, String role) {
        if (source == null) {
          throw new IllegalArgumentException();
        }
        this.reason = reason;
        this.node = source;
        this.exceptionType = exceptionType;
        this.label = label;
        this.role = role;
      }

      public JType getExceptionType() {
        if (!isThrow()) {
          throw new IllegalArgumentException();
        }

        return exceptionType;
      }

      public String getLabel() {
        if (!isContinue() && !isBreak()) {
          throw new IllegalArgumentException();
        }
        return label;
      }

      public CfgNode<?> getNode() {
        return node;
      }

      public boolean isBreak() {
        return reason == Reason.BREAK;
      }

      public boolean isContinue() {
        return reason == Reason.CONTINUE;
      }

      public boolean isNormal() {
        return reason == Reason.NORMAL;
      }

      public boolean isThrow() {
        return reason == Reason.THROW;
      }

      @Override
      public String toString() {
        return reason.toString();
      }
    }

    /**
     * All exits at this point of interpretation tabulated by their reason.
     */
    private final EnumMap<Exit.Reason, ArrayList<Exit>> currentExitsByReason =
      new EnumMap<Exit.Reason, ArrayList<Exit>>(Exit.Reason.class);

    /**
     * Artificial cfg end node.
     */
    private final CfgEndNode endNode = new CfgEndNode();

    /**
     * Resulting graph.
     */
    private final Cfg graph = new Cfg();

    /**
     * Map from statement to its label.
     */
    private final Map<JStatement, String> labels = 
      new HashMap<JStatement, String>();

    /**
     * All nodes in the graph.
     */
    private final List<CfgNode<?>> nodes = new ArrayList<CfgNode<?>>();

    /**
     * Parent for newly created cfg nodes.
     */
    private CfgNode<?> parent = null;

    private final JProgram program;

    private JSwitchStatement switchStatement;
    
    private final JTypeOracle typeOracle;

    public BuilderVisitor(JProgram program) {
      this.program = program;
      this.typeOracle = program.typeOracle;

      for (Exit.Reason reason : Exit.Reason.values()) {
        this.currentExitsByReason.put(reason, new ArrayList<Exit>());
      }
    }

    /**
     * Build cfg for codeblock. Resulting graph will have one incoming edge
     * and no outgoing edges.
     */
    public Cfg build(JBlock codeBlock) {
      CfgEdge methodIn = new CfgEdge();
      graph.addGraphInEdge(methodIn);
      accept(codeBlock);
      graph.addIn(nodes.get(0), methodIn);
      addNode(endNode);

      // Wire all remaining exits to end node.
      for (Exit.Reason reason : Exit.Reason.values()) {
        switch (reason) {
          case CONTINUE:
          case BREAK:
          case CASE_ELSE:
          case CASE_THEN:
            if (!currentExitsByReason.get(reason).isEmpty()) {
              // This shouldn't happen.
              throw new IllegalArgumentException("Unhandled exit: " + reason);
            }
            break;
          case NORMAL:
          case RETURN:
          case THROW: {
            for (Exit exit : currentExitsByReason.get(reason)) {
              addEdge(exit, endNode);
            }
            break;
          }
        }
      }

      return graph;
    }

    /**
     * Build cfg for codeblock. Resulting graph will have one incoming edge
     * and no outgoing edges.
     */
    public Cfg build(JExpression expression) {
      accept(expression);
      addNode(endNode);

      for (Exit.Reason reason : Exit.Reason.values()) {
        Preconditions.checkArgument(currentExitsByReason.get(reason).isEmpty(),
          "Unhandled exits %s", reason);
      }

      return graph;
    }

    @Override
    public boolean visit(JBinaryOperation x, Context ctx) {
      if (x.isAssignment()) {
        // Generate writes.
        accept(x.getRhs());
        acceptExpressionSubreads(x.getLhs());
        if (x.getOp() == JBinaryOperator.ASG) {
          addNode(new CfgWriteNode(parent, x, x.getLhs(), x.getRhs()));
        } else {
          addNode(new CfgReadWriteNode(parent, x, x.getLhs(), null));
        }
        return false;
      } else if (x.getOp() == JBinaryOperator.AND
          || x.getOp() == JBinaryOperator.OR) {
        // generate conditionals.
        accept(x.getLhs());

        CfgBinaryConditionalOperationNode node = 
          pushNode(new CfgBinaryConditionalOperationNode(parent, x));

        if (x.getOp() == JBinaryOperator.AND) {
          addNormalExit(node, CfgConditionalNode.THEN);
          accept(x.getRhs());
          List<Exit> thenExits = removeNormalExits();
          addNormalExit(node, CfgConditionalNode.ELSE);
          addExits(thenExits);
        } else {
          addNormalExit(node, CfgConditionalNode.ELSE);
          accept(x.getRhs());
          List<Exit> elseExits = removeNormalExits();
          addNormalExit(node, CfgConditionalNode.THEN);
          addExits(elseExits);
        }

        popNode();

        return false;
      }

      return true;
    }

    @Override
    public boolean visit(JBlock x, Context ctx) {
      pushNode(new CfgBlockNode(parent, x));
      accept(x.getStatements());
      popNode();
      return false;
    }

    @Override
    public boolean visit(JBreakStatement x, Context ctx) {
      pushNode(new CfgStatementNode<JStatement>(parent, x));
      String label = null;
      if (x.getLabel() != null) {
        label = x.getLabel().getName();
      }
      CfgBreakNode node = addNode(new CfgBreakNode(parent, x));
      addExit(Exit.createBreak(node, label));
      popNode();
      return false;
    }

    @Override
    public boolean visit(JCaseStatement x, Context ctx) {
      pushNode(new CfgStatementNode<JStatement>(parent, x));
      if (x.getExpr() != null) {
        // case label
        JExpression condition = new JBinaryOperation(x.getSourceInfo(), 
            program.getTypePrimitiveBoolean(), 
            JBinaryOperator.EQ, switchStatement.getExpr(), x.getExpr());
        CfgCaseNode node = addNode(new CfgCaseNode(parent, x, condition));
        addExit(Exit.createCaseThen(node));
        addExit(Exit.createCaseElse(node));
      } else {
        // default label
      }
      popNode();
      return false;
    }

    @Override
    public boolean visit(JConditional x, Context ctx) {
      accept(x.getIfTest());

      CfgConditionalExpressionNode node = 
        pushNode(new CfgConditionalExpressionNode(parent, x));

      addNormalExit(node, CfgConditionalNode.THEN);
      accept(x.getThenExpr());
      List<Exit> thenExits = removeNormalExits();

      addNormalExit(node, CfgConditionalNode.ELSE);
      accept(x.getElseExpr());

      addExits(thenExits);
      popNode();
      return false;
    }

    @Override
    public boolean visit(JContinueStatement x, Context ctx) {
      pushNode(new CfgStatementNode<JStatement>(parent, x));
      String label = null;
      if (x.getLabel() != null) {
        label = x.getLabel().getName();
      }
      CfgContinueNode node = addNode(new CfgContinueNode(parent, x));
      addExit(Exit.createContinue(node, label));
      popNode();
      return false;
    }

    @Override
    public boolean visit(JDeclarationStatement x, Context ctx) {
      pushNode(new CfgStatementNode<JStatement>(parent, x));
      if (x.getInitializer() != null) {
        accept(x.getInitializer());
        addNode(new CfgWriteNode(parent, x, x.getVariableRef(),
            x.getInitializer()));
      }
      popNode();
      return false;
    }

    @Override
    public boolean visit(JDoStatement x, Context ctx) {
      List<Exit> unlabeledExits = removeUnlabeledExits();
      pushNode(new CfgStatementNode<JStatement>(parent, x));
      int pos = nodes.size();

      if (x.getBody() != null) {
        accept(x.getBody());
      }

      if (x.getTestExpr() != null) {
        accept(x.getTestExpr());
      }

      CfgDoNode node = addNode(new CfgDoNode(parent, x));

      addEdge(node, nodes.get(pos), new CfgEdge(CfgConditionalNode.THEN));
      
      String label = labels.get(x);
      addContinueEdges(nodes.get(pos), label);
      addBreakExits(label);
      addNormalExit(node, CfgConditionalNode.ELSE);

      popNode();
      addExits(unlabeledExits);
      return false;
    }

    @Override
    public boolean visit(JExpressionStatement x, Context ctx) {
      pushNode(new CfgStatementNode<JStatement>(parent, x));
      accept(x.getExpr());
      popNode();
      return false;
    }

    @Override
    public boolean visit(JForStatement x, Context ctx) {
      List<Exit> unlabeledExits = removeUnlabeledExits();
      pushNode(new CfgStatementNode<JStatement>(parent, x));
      accept(x.getInitializers());

      CfgForNode cond = null;
      int testPos = nodes.size();
      
      if (x.getTestExpr() != null) {
        accept(x.getTestExpr());
        cond = addNode(new CfgForNode(parent, x));
        addNormalExit(cond, CfgConditionalNode.THEN);
      }

      if (x.getBody() != null) {
        accept(x.getBody());
      }
      int incrementsPos = nodes.size();
      accept(x.getIncrements());

      List<Exit> thenExits = removeNormalExits();
      for (Exit e : thenExits) {
        addEdge(e, nodes.get(testPos));
      }

      String label = labels.get(x);
      // If there's no increments, continue goes straight to test.
      int continuePos = incrementsPos != nodes.size() ? incrementsPos : testPos;
      addContinueEdges(nodes.get(continuePos), label);
      addBreakExits(label);
      if (cond != null) {
        addNormalExit(cond, CfgConditionalNode.ELSE);
      }

      popNode();
      addExits(unlabeledExits);
      return false;
    }

    @Override
    public boolean visit(JIfStatement x, Context ctx) {
      pushNode(new CfgStatementNode<JStatement>(parent, x));
      accept(x.getIfExpr());

      CfgIfNode node = addNode(new CfgIfNode(parent, x));

      addNormalExit(node, CfgConditionalNode.THEN);
      if (x.getThenStmt() != null) {
        accept(x.getThenStmt());
      }
      List<Exit> thenExits = removeNormalExits();

      addNormalExit(node, CfgConditionalNode.ELSE);
      if (x.getElseStmt() != null) {
        accept(x.getElseStmt());
      }

      addExits(thenExits);

      popNode();
      return false;
    }

    @Override
    public boolean visit(JLabeledStatement x, Context ctx) {
      String label = x.getLabel().getName();
      labels.put(x.getBody(), label);
      accept(x.getBody());
      addBreakExits(label);
      return false;
    }

    /**
     * Each method call generates optional throw.
     */
    @Override
    public boolean visit(JMethodCall x, Context ctx) {
      // TODO: join optthrow + call
      if (x.getInstance() != null) {
        // TODO: add optional NPE exception
        accept(x.getInstance());
      }
      accept(x.getArgs());

      CfgOptionalThrowNode node = addNode(new CfgOptionalThrowNode(parent, x));

      addNormalExit(node, CfgOptionalThrowNode.NO_THROW);
      for (JClassType exceptionType : x.getTarget().getThrownExceptions()) {
        addExit(Exit.createThrow(node, exceptionType, null));
      }
      JDeclaredType runtimeExceptionType = 
        program.getFromTypeMap("java.lang.RuntimeException");
      if (runtimeExceptionType != null) {
        addExit(Exit.createThrow(node, runtimeExceptionType,
            CfgOptionalThrowNode.RUNTIME_EXCEPTION));
      }
      JDeclaredType errorExceptionType = 
        program.getFromTypeMap("java.lang.Error");
      if (errorExceptionType != null) {
        addExit(Exit.createThrow(node, errorExceptionType,
            CfgOptionalThrowNode.ERROR));
      }

      addNode(new CfgMethodCallNode(parent, x));
      
      return false;
    }

    @Override
    public boolean visit(JReboundEntryPoint x, Context ctx) {
      pushNode(new CfgStatementNode<JReboundEntryPoint>(parent, x));
      accept(x.getEntryCalls());
      popNode();
      return false;
    }

    @Override
    public boolean visit(JReturnStatement x, Context ctx) {
      pushNode(new CfgStatementNode<JStatement>(parent, x));
      if (x.getExpr() != null) {
        accept(x.getExpr());
      }

      CfgReturnNode node = addNode(new CfgReturnNode(parent, x));
      addExit(Exit.createReturn(node));
      popNode();
      return false;
    }

    @Override
    public boolean visit(JStatement x, Context ctx) {
      // The statement isn't supported.
      throw new UnsupportedNodeException(x.getClass().toString());
    }

    @Override
    public boolean visit(JSwitchStatement x, Context ctx) {
      pushNode(new CfgStatementNode<JStatement>(parent, x));
      accept(x.getExpr());
      
      JSwitchStatement oldSwitchStatement = switchStatement;
      // We don't want to mess with other case exits here
      List<Exit> oldCaseElseExits = removeExits(Exit.Reason.CASE_ELSE);
      List<Exit> oldCaseThenExits = removeExits(Exit.Reason.CASE_THEN);
      List<Exit> oldBreakExits = removeUnlabeledBreaks();
      switchStatement = x;

      // Goto to the first non-default node.
      CfgSwitchGotoNode gotoNode = addNode(new CfgSwitchGotoNode(parent, x));
      Exit gotoExit = Exit.createNormal(gotoNode, null);
      
      int defaultPos = -1;
      
      List<Exit> breakExits = new ArrayList<Exit>();
      List<Exit> fallThroughExits = new ArrayList<Exit>();

      List<JStatement> statements = x.getBody().getStatements();
      
      for (JStatement s : statements) {
        if (s instanceof JCaseStatement) {
          if (((JCaseStatement) s).getExpr() != null) {
            // case label

            fallThroughExits.addAll(removeExits(Exit.Reason.NORMAL));
            if (gotoExit != null) {
              // This is first non-default case.
              addExit(gotoExit);
              gotoExit = null;
            }
            List<Exit> elseExits = removeExits(Exit.Reason.CASE_ELSE);
            for (Exit e : elseExits) {
              addNormalExit(e.getNode(), e.role);
            }
          } else {
            // default label
            defaultPos = nodes.size();
          }
        } else {
          List<Exit> thenExits = removeExits(Exit.Reason.CASE_THEN);
          for (Exit e : thenExits) {
            addNormalExit(e.getNode(), e.role);
          }
          if (!fallThroughExits.isEmpty()) {
            for (Exit e : fallThroughExits) {
              addExit(e);
            }
            fallThroughExits.clear();
          }
        }
        accept(s);
        breakExits.addAll(removeUnlabeledBreaks());
      }

      if (gotoExit != null) {
        // Happens when there are no case statements.
        if (defaultPos >= 0) {
          addEdge(gotoExit, nodes.get(defaultPos));
        } else {
          addExit(gotoExit);
        }
        gotoExit = null;
      }

      List<Exit> thenExits = removeExits(Exit.Reason.CASE_THEN);
      for (Exit e : thenExits) {
        addNormalExit(e.getNode(), e.role);
      }

      List<Exit> elseExits = removeExits(Exit.Reason.CASE_ELSE);

      if (defaultPos >= 0) {
        for (Exit e : elseExits) {
          addEdge(e, nodes.get(defaultPos));
        }
      } else {
        for (Exit e : elseExits) {
          addExit(Exit.createNormal(e.getNode(), e.role));
        }
      }
      
      for (Exit e : breakExits) {
        addNormalExit(e.getNode(), e.role);
      }

      switchStatement = oldSwitchStatement;
      addExits(oldCaseElseExits);
      addExits(oldCaseThenExits);
      addExits(oldBreakExits);
      popNode();
      return false;
    }

    @Override
    public boolean visit(JThrowStatement x, Context ctx) {
      pushNode(new CfgStatementNode<JStatement>(parent, x));
      accept(x.getExpr());
      CfgThrowNode node = addNode(new CfgThrowNode(parent, x));
      addExit(Exit.createThrow(node, x.getExpr().getType(), null));
      popNode();
      return false;
    }

    @Override
    public boolean visit(JTryStatement x, Context ctx) {
      pushNode(new CfgTryNode(parent, x));
      accept(x.getTryBlock());

      // Process all blocks and determine their exits

      List<Exit> tryBlockExits = removeCurrentExits();

      List<Integer> catchBlockPos = new ArrayList<Integer>();
      List<List<Exit>> catchExits = new ArrayList<List<Exit>>();

      for (JBlock b : x.getCatchBlocks()) {
        catchBlockPos.add(nodes.size());
        accept(b);
        catchExits.add(removeCurrentExits());
      }

      int finallyPos = nodes.size();
      if (x.getFinallyBlock() != null) {
        accept(x.getFinallyBlock());
      }
      List<Exit> finallyExits = removeCurrentExits();

      // Actual work goes here. We are citing JLS to make it easier to follow.
      // We prefer to have duplicated code for finally block handling just
      // to make it easier to follow.

      // A try statement without a finally block is executed by first executing
      // the try block. Then there is a choice:
      if (x.getFinallyBlock() == null) {
        // If execution of the try block completes normally, then no further
        // action is taken and the try statement completes normally.
        addExits(removeNormalExits(tryBlockExits));

        nextExit : for (Exit e : tryBlockExits) {
          if (e.isThrow()) {
            // If execution of the try block completes abruptly because of a
            // throw of a value V, then there is a choice:
            nextCatchBlock : for (int i = 0; i < x.getCatchArgs().size(); ++i) {
              // If the run-time type of V is assignable (�5.2) to the
              // Parameter of any catch clause of the try statement, then
              // the first (leftmost) such catch clause is selected.
              JClassType catchType = 
                (JClassType) x.getCatchArgs().get(i).getType();
              JType exceptionType = e.getExceptionType();

              boolean canCatch = false;
              boolean fullCatch = false;

              // It's not that simple in static analysis though.
              if (typeOracle.canTriviallyCast(exceptionType, catchType)) {
                // Catch clause fully covers exception type. We'll land
                // here for sure.
                canCatch = true;
                fullCatch = true;
              } else if (typeOracle.canTriviallyCast(catchType, exceptionType)) {
                // We can land here if we throw some subclass of
                // exceptionType
                canCatch = true;
                fullCatch = false;
              }

              if (canCatch) {
                addEdge(e, nodes.get(catchBlockPos.get(i)));
                if (fullCatch) {
                  continue nextExit;
                }
                continue nextCatchBlock;
              }
            }

            // If the run-time type of V is not assignable to the parameter of
            // any catch clause of the try statement, then the try statement
            // completes abruptly because of a throw of the value V.
            addExit(e);
          } else {
            // If execution of the try block completes abruptly for any other
            // reason, then the try statement completes abruptly for the same
            // reason.
            addExit(e);
          }
        }

        // Continuing catch case here:
        // If that block completes normally, then the try statement
        // completes normally; if that block completes abruptly for any reason,
        // then the try statement completes abruptly for the same reason.
        for (List<Exit> exits : catchExits) {
          addExits(exits);
        }
      } else {
        // A try statement with a finally block is executed by first
        // executing the try block. Then there is a choice:

        // If execution of the try block completes normally, then the finally
        // block is executed,
        CfgNode<?> finallyNode = nodes.get(finallyPos);
        for (Exit e : removeNormalExits(tryBlockExits)) {
          addEdge(e, finallyNode);
        }

        // and then there is a choice: If the finally block completes normally,
        // then the try statement completes normally.
        // If the finally block completes abruptly for reason S, then the
        // try statement completes abruptly for reason S.
        addExits(finallyExits);

        nextExit : for (Exit e : tryBlockExits) {
          if (e.isThrow()) {
            // If execution of the try block completes abruptly because of a
            // throw of a value V, then there is a choice:

            nextCatchBlock : for (int i = 0; i < x.getCatchArgs().size(); ++i) {
              // If the run-time type of V is assignable to the parameter of any
              // catch clause of the try statement, then the first
              // (leftmost) such catch clause is selected.
              JClassType catchType = 
                (JClassType) x.getCatchArgs().get(i).getType();
              JType exceptionType = e.getExceptionType();

              boolean canCatch = false;
              boolean fullCatch = false;

              // It's not that simple in static analysis though.
              if (typeOracle.canTriviallyCast(exceptionType, catchType)) {
                // Catch clause fully covers exception type. We'll land
                // here for sure.
                canCatch = true;
                fullCatch = true;
              } else if (typeOracle.canTriviallyCast(catchType, exceptionType)) {
                // We can land here if we throw some subclass of
                // exceptionType
                canCatch = true;
                fullCatch = false;
              }

              if (canCatch) {
                addEdge(e, nodes.get(catchBlockPos.get(i)));
                if (fullCatch) {
                  continue nextExit;
                }
                continue nextCatchBlock;
              }
            }

            // If the run-time type of V is not assignable to the parameter of
            // any catch clause of the try statement, then the finally block is
            // executed.
            addEdge(e, finallyNode);
            // Then there is a choice:
            for (Exit finallyExit : finallyExits) {
              if (finallyExit.isNormal()) {
                // If the finally block completes normally, then the try
                // statement completes abruptly because of a throw of the
                // value V.
                addExit(new Exit(e.reason, finallyExit.node, e.exceptionType,
                    e.label, e.role));
              } else {
                // If the finally block completes abruptly for reason S, then
                // the try statement completes abruptly for reason S
                // (and reason R is discarded).
                // This is already covered earlier:
                // addExits(finallyExits)
              }
            }

          } else {
            // If execution of the try block completes abruptly for any other
            // reason R, then the finally block is executed.
            addEdge(e, finallyNode);
            // Then there is a choice:
            for (Exit finallyExit : finallyExits) {
              if (finallyExit.isNormal()) {
                // If the finally block completes normally, then the try
                // statement completes abruptly for reason R.
                addExit(new Exit(e.reason, finallyExit.node, e.exceptionType,
                    e.label, e.role));
              } else {
                // If the finally block completes abruptly for reason S, then
                // the try statement completes abruptly for reason S
                // (and reason R is discarded).
                // This is already covered earlier:
                // addExits(finallyExits)
              }
            }
          }
        }

        // Continuing catch case here:
        // If that block completes normally, then the try statement
        // completes normally; if that block completes abruptly for any reason,
        // then the try statement completes abruptly for the same reason.
        for (List<Exit> exits : catchExits) {
          for (Exit e : exits) {
            // If the catch block completes normally, then the finally block is
            // executed.
            if (e.isNormal()) {
              addEdge(e, finallyNode);
            } else {
              // If the catch block completes abruptly for reason R, then the
              // finally block is executed. Then there is a choice:
              for (Exit finallyExit : finallyExits) {
                if (finallyExit.isNormal()) {
                  // If the finally block completes normally, then the try
                  // statement completes abruptly for reason R.
                  addExit(new Exit(e.reason, finallyExit.node, e.exceptionType,
                      e.label, e.role));
                } else {
                  // If the finally block completes abruptly for reason S, then
                  // the try statement completes abruptly for reason S
                  // (and reason R is discarded).
                  // This is already covered earlier:
                  // addExits(finallyExits)
                }
              }
            }
          }
        }
      }

      popNode();
      return false;
    }

    @Override
    public boolean visit(JUnaryOperation x, Context ctx) {
      if (x.getOp().isModifying()) {
        acceptExpressionSubreads(x.getArg());
        addNode(new CfgReadWriteNode(parent, x, x.getArg(), null));
        return false;
      }
      // read will be added by normal flow
      return true;
    }

    @Override
    public boolean visit(JVariableRef x, Context ctx) {
      // TODO: add NPE exceptions for field references.
      addNode(new CfgReadNode(parent, x));
      return true;
    }

    @Override
    public boolean visit(JWhileStatement x, Context ctx) {
      List<Exit> unlabeledExits = removeUnlabeledExits();
      pushNode(new CfgStatementNode<JStatement>(parent, x));
      int pos = nodes.size();
      accept(x.getTestExpr());

      CfgWhileNode node = addNode(new CfgWhileNode(parent, x));

      addNormalExit(node, CfgConditionalNode.THEN);
      if (x.getBody() != null) {
        accept(x.getBody());
      }

      List<Exit> thenExits = removeNormalExits();
      for (Exit e : thenExits) {
        addEdge(e, nodes.get(pos));
      }

      String label = labels.get(x);
      addContinueEdges(nodes.get(pos), label);
      addBreakExits(label);
      addNormalExit(node, CfgConditionalNode.ELSE);

      popNode();
      addExits(unlabeledExits);
      return false;
    }

    /**
     * Detect all reads which are performed before evaluating expression.
     */
    private void acceptExpressionSubreads(JExpression expression) {
      if (expression instanceof JFieldRef) {
        JExpression instance = ((JFieldRef) expression).getInstance();
        if (instance != null) {
          accept(instance);
        }
      } else if (expression instanceof JArrayRef) {
        JArrayRef arrayRef = (JArrayRef) expression;
        accept(arrayRef.getInstance());
        accept(arrayRef.getIndexExpr());
      } else if (!(expression instanceof JVariableRef)) {
        throw new IllegalArgumentException("Unexpeted lhs: " + expression);
      }
    }

    /**
     * Transform all break exits into normal exits, thus making sure that
     * next node will get edges from them.
     */
    private void addBreakExits(String label) {
      List<Exit> exits = removeLoopExits(Exit.Reason.BREAK, label);
      for (Exit e : exits) {
        addNormalExit(e.getNode());
      }
    }

    /**
     * Transform all continue exits into normal exits, thus making sure that
     * next node will get edges from them.
     */
    private void addContinueEdges(CfgNode<?> node, String label) {
      List<Exit> continueExits = removeLoopExits(Exit.Reason.CONTINUE, label);
      for (Exit e : continueExits) {
        addEdge(e, node);
      }
    }

    private CfgEdge addEdge(CfgNode<?> from, CfgNode<?> to, CfgEdge edge) {
      graph.addOut(from, edge);
      graph.addIn(to, edge);
      return edge;
    }

    private CfgEdge addEdge(Exit e, CfgNode<?> to) {
      CfgEdge edge = new CfgEdge(e.role);
      return addEdge(e.node, to, edge);
    }

    private void addExit(Exit exit) {
      currentExitsByReason.get(exit.reason).add(exit);
    }

    private void addExits(List<Exit> exits) {
      for (Exit exit : exits) {
        currentExitsByReason.get(exit.reason).add(exit);
      }
    }

    /**
     * Add new node to cfg. Wire all current normal exits to it.
     */
    private <N extends CfgNode<?>> N addNode(N node) {
      nodes.add(node);
      graph.addNode(node);

      ArrayList<Exit> normalExits =
        currentExitsByReason.put(Exit.Reason.NORMAL, new ArrayList<Exit>());
      for (Exit exit : normalExits) {
        addEdge(exit, node);
      }

      // Simplify all the code, add normal exit for CfgSimplNode automatically.
      if (node instanceof CfgSimpleNode<?>) {
        addNormalExit(node);
      }

      return node;
    }

    private void addNormalExit(CfgNode<?> node) {
      addNormalExit(node, null);
    }

    private void addNormalExit(CfgNode<?> node, String role) {
      addExit(Exit.createNormal(node, role));
    }

    private void popNode() {
      parent = parent.getParent();
    }

    private <N extends CfgNode<?>> N pushNode(N node) {
      addNode(node);
      parent = node;
      return node;
    }

    private List<Exit> removeCurrentExits() {
      ArrayList<Exit> result = new ArrayList<Exit>();
      for (Exit.Reason reason : Exit.Reason.values()) {
        result.addAll(currentExitsByReason.put(reason, new ArrayList<Exit>()));
      }
      return result;
    }

    private List<Exit> removeExits(Exit.Reason reason) {
      return currentExitsByReason.put(reason, new ArrayList<Exit>());
    }
    
    private List<Exit> removeExits(List<Exit> exits, Exit.Reason reason) {
      List<Exit> result = new ArrayList<Exit>();
      for (Iterator<Exit> i = exits.iterator(); i.hasNext();) {
        Exit exit = i.next();
        if (exit.reason == reason) {
          i.remove();
          result.add(exit);
        }
      }
      return result;
    }

    /**
     * Remove all loop exits to specified label.
     */
    private List<Exit> removeLoopExits(Exit.Reason reason, String label) {
      ArrayList<Exit> removedExits = new ArrayList<Exit>();
      ArrayList<Exit> remainingExits = new ArrayList<Exit>();

      for (Exit exit : currentExitsByReason.get(reason)) {
        if (exit.getLabel() == null || exit.getLabel().equals(label)) {
          removedExits.add(exit);
        } else {
          remainingExits.add(exit);
        }
      }
      currentExitsByReason.put(reason, remainingExits);

      return removedExits;
    }

    /**
     * Remove all normal exits from current exit list.
     */
    private List<Exit> removeNormalExits() {
      return currentExitsByReason.put(Exit.Reason.NORMAL, new ArrayList<Exit>());
    }

    private List<Exit> removeNormalExits(List<Exit> exits) {
      return removeExits(exits, Exit.Reason.NORMAL);
    }

    private List<Exit> removeUnlabeledBreaks() {
      List<Exit> breakExits = removeExits(Exit.Reason.BREAK);
      List<Exit> labeledBreaks = new ArrayList<Exit>();
      
      for (Iterator<Exit> i = breakExits.iterator(); i.hasNext();) {
        Exit exit = i.next();
        if (exit.getLabel() != null) {
          i.remove();
          labeledBreaks.add(exit);
        }
      }
      addExits(labeledBreaks);
      return breakExits;
    }

    private List<Exit> removeUnlabeledExits() {
      List<Exit> unlabeledExits = new ArrayList<Exit>();
      Exit.Reason reasons[] = { Exit.Reason.BREAK, Exit.Reason.CONTINUE };
      for (Exit.Reason reason : reasons) {
        for (Iterator<Exit> i = currentExitsByReason.get(reason).iterator(); i.hasNext();) {
          Exit exit = i.next();
          if (exit.getLabel() == null) {
            i.remove();
            unlabeledExits.add(exit);
          }
        }
      }
      return unlabeledExits;
    }
  }

  /**
   * Special exception which is thrown when we encounter some syntactic 
   * construction which is not yet supported by CfgBuilder. 
   */
  private static class UnsupportedNodeException extends RuntimeException {
    public UnsupportedNodeException(String message) {
      super(message);
    }
  }

  /**
   * Build Cfg for code block. 
   */
  public static Cfg build(JProgram program, JBlock codeblock) {
    return new BuilderVisitor(program).build(codeblock);
  }

  public static Cfg buildExpressionCfg(JProgram program, JExpression value) {
    return new BuilderVisitor(program).build(value);
  }
}
