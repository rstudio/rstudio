/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.dev.jjs.ast.CanBeStatic;
import com.google.gwt.dev.jjs.ast.HasEnclosingType;
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JAssertStatement;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JBreakStatement;
import com.google.gwt.dev.jjs.ast.JCaseStatement;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JCharLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JContinueStatement;
import com.google.gwt.dev.jjs.ast.JDoStatement;
import com.google.gwt.dev.jjs.ast.JDoubleLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JFloatLiteral;
import com.google.gwt.dev.jjs.ast.JForStatement;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JLabel;
import com.google.gwt.dev.jjs.ast.JLabeledStatement;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JLongLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JSourceInfo;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JSwitchStatement;
import com.google.gwt.dev.jjs.ast.JThrowStatement;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JWhileStatement;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethod;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.js.JsAbstractVisitorWithAllVisits;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsSourceInfo;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ArrayReference;
import org.eclipse.jdt.internal.compiler.ast.AssertStatement;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.BreakStatement;
import org.eclipse.jdt.internal.compiler.ast.CaseStatement;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ContinueStatement;
import org.eclipse.jdt.internal.compiler.ast.DoStatement;
import org.eclipse.jdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.jdt.internal.compiler.ast.EqualExpression;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldReference;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.jdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.jdt.internal.compiler.ast.PostfixExpression;
import org.eclipse.jdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedSuperReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedThisReference;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.SuperReference;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.SynchronizedStatement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.impl.BooleanConstant;
import org.eclipse.jdt.internal.compiler.impl.ByteConstant;
import org.eclipse.jdt.internal.compiler.impl.CharConstant;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.impl.DoubleConstant;
import org.eclipse.jdt.internal.compiler.impl.FloatConstant;
import org.eclipse.jdt.internal.compiler.impl.IntConstant;
import org.eclipse.jdt.internal.compiler.impl.LongConstant;
import org.eclipse.jdt.internal.compiler.impl.ShortConstant;
import org.eclipse.jdt.internal.compiler.impl.StringConstant;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.NestedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticArgumentBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jdt.internal.compiler.problem.ProblemHandler;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This is the big kahuna where most of the nitty gritty of creating our AST
 * happens. BuildTypeMap must have already run so we have valid mappings between
 * JDT nodes and our already-created AST nodes.
 */
public class GenerateJavaAST {

  /**
   * Comparator for <code>HasName</code> instances.
   */
  public static class HasNameSort implements Comparator {
    public int compare(Object o1, Object o2) {
      HasName h1 = (HasName) o1;
      HasName h2 = (HasName) o2;
      return h1.getName().compareTo(h2.getName());
    }
  }

  /**
   * Visit the JDT AST and produce our own AST into the passed-in TypeMap's
   * JProgram. By the end of this pass, the produced AST should contain every
   * piece of information we'll ever need about the code. The JDT nodes should
   * never again be referenced after this.
   * 
   * This is implemented as a reflective visitor for JDT's AST. The advantage of
   * doing it reflectively is that if we run into any JDT nodes we can't handle,
   * we'll automatically throw an exception. If we had subclassed
   * {@link org.eclipse.jdt.internal.compiler.ast.ASTNode} we'd have to override
   * every single method and explicitly throw an exception to get the same
   * behavior.
   * 
   * NOTE ON JDT FORCED OPTIMIZATIONS - If JDT statically determines that a
   * section of code in unreachable, it won't fully resolve that section of
   * code. This invalid-state code causes us major problems. As a result, we
   * have to optimize out those dead blocks early and never try to translate
   * them to our AST.
   */
  private static class JavaASTGenerationVisitor {

    private static String getJsniSig(JMethod method) {
      StringBuffer sb = new StringBuffer();
      sb.append(method.getName());
      sb.append("(");
      for (int i = 0; i < method.getOriginalParamTypes().size(); ++i) {
        JType type = (JType) method.getOriginalParamTypes().get(i);
        sb.append(type.getJsniSignatureName());
      }
      sb.append(")");
      return sb.toString();
    }

    private static InternalCompilerException translateException(JNode node,
        Throwable e) {
      InternalCompilerException ice;
      if (e instanceof InternalCompilerException) {
        ice = (InternalCompilerException) e;
        ice.addNode(node);
      } else {
        ice = new InternalCompilerException(node,
            "Error constructing Java AST", e);
      }
      return ice;
    }

    private Object[] args = new Object[1];

    private JReferenceType currentClass;

    private ClassScope currentClassScope;

    private String currentFileName;

    private JMethod currentMethod;

    private MethodScope currentMethodScope;

    private int[] currentSeparatorPositions;

    private final Map/* <JMethod, Map<String, JLabel>> */labelMap = new IdentityHashMap();

    private Class[] params = new Class[1];

    private final JProgram program;

    private final TypeMap typeMap;

    public JavaASTGenerationVisitor(TypeMap typeMap) {
      this.typeMap = typeMap;
      program = this.typeMap.getProgram();
    }

    /**
     * We emulate static initializers and intance initializers as methods. As in
     * other cases, this gives us: simpler AST, easier to optimize, more like
     * output JavaScript.
     */
    public void processType(TypeDeclaration x) {
      currentClass = (JReferenceType) typeMap.get(x.binding);
      try {
        currentClassScope = x.scope;
        currentSeparatorPositions = x.compilationResult.lineSeparatorPositions;
        currentFileName = String.valueOf(x.compilationResult.fileName);

        if (x.fields != null) {
          // Process fields
          for (int i = 0, n = x.fields.length; i < n; ++i) {
            FieldDeclaration fieldDeclaration = x.fields[i];
            if (fieldDeclaration.isStatic()) {
              // clinit
              currentMethod = (JMethod) currentClass.methods.get(0);
              currentMethodScope = x.staticInitializerScope;
            } else {
              // init
              currentMethod = (JMethod) currentClass.methods.get(1);
              currentMethodScope = x.initializerScope;
            }

            if (fieldDeclaration instanceof Initializer) {
              assert (currentClass instanceof JClassType);
              processInitializer((Initializer) fieldDeclaration);
            } else {
              processField(fieldDeclaration);
            }
          }
        }

        currentMethodScope = null;
        currentMethod = null;

        if (x.methods != null) {
          // Process methods
          for (int i = 0, n = x.methods.length; i < n; ++i) {
            if (x.methods[i].isConstructor()) {
              assert (currentClass instanceof JClassType);
              processConstructor((ConstructorDeclaration) x.methods[i]);
            } else if (x.methods[i].isClinit()) {
              // nothing to do
            } else {
              processMethod(x.methods[i]);
            }
          }
        }

        currentClassScope = null;
        currentClass = null;
        currentSeparatorPositions = null;
        currentFileName = null;
      } catch (Throwable e) {
        throw translateException(currentClass, e);
      }
    }

    /**
     * This is the guts of the "reflective" part of this visitor. Try to find a
     * "process" method that exactly matches the run-time type of the argument.
     */
    protected JNode dispatch(String name, Object child) {
      if (child == null) {
        return null;
      }

      try {
        params[0] = child.getClass();
        Method method = getClass().getDeclaredMethod(name, params);
        args[0] = child;
        return (JNode) method.invoke(this, args);
      } catch (Throwable e) {
        if (e instanceof InvocationTargetException) {
          e = ((InvocationTargetException) e).getTargetException();
        }
        throw translateException(child, e);
      }
    }

    /**
     * Process an Expression type node reflectively; must return a JExpression.
     */
    protected JExpression dispProcessExpression(Expression x) {
      /*
       * Note that we always prefer a JDT-computed constant value to the actual
       * written expression. (Let's hope JDT is always right.) This means we
       * don't have to write processExpression methods for the numerous JDT
       * literal nodes because they ALWAYS have a constant value.
       */
      if (x != null && x.constant != null
          && x.constant != Constant.NotAConstant) {
        return (JExpression) dispatch("processConstant", x.constant);
      }
      return (JExpression) dispatch("processExpression", x);
    }

    /**
     * Process an Statement type node reflectively; must return a JStatement.
     */
    protected JStatement dispProcessStatement(Statement x) {
      JStatement stmt;
      if (x instanceof Expression) {
        JExpression expr = dispProcessExpression((Expression) x);
        if (expr == null) {
          return null;
        }
        stmt = new JExpressionStatement(program, makeSourceInfo(x), expr);
      } else {
        stmt = (JStatement) dispatch("processStatement", x);
      }
      return stmt;
    }

    JBooleanLiteral processConstant(BooleanConstant x) {
      return program.getLiteralBoolean(x.booleanValue());
    }

    JIntLiteral processConstant(ByteConstant x) {
      return program.getLiteralInt(x.byteValue());
    }

    JCharLiteral processConstant(CharConstant x) {
      return program.getLiteralChar(x.charValue());
    }

    JDoubleLiteral processConstant(DoubleConstant x) {
      return program.getLiteralDouble(x.doubleValue());
    }

    JFloatLiteral processConstant(FloatConstant x) {
      return program.getLiteralFloat(x.floatValue());
    }

    JIntLiteral processConstant(IntConstant x) {
      return program.getLiteralInt(x.intValue());
    }

    JLongLiteral processConstant(LongConstant x) {
      return program.getLiteralLong(x.longValue());
    }

    JIntLiteral processConstant(ShortConstant x) {
      return program.getLiteralInt(x.shortValue());
    }

    JStringLiteral processConstant(StringConstant x) {
      return program.getLiteralString(x.stringValue().toCharArray());
    }

    /**
     * Weird: we used to have JConstructor (and JConstructorCall) in our AST,
     * but we got rid of them completely and instead model them as instance
     * methods whose qualifier is a naked no-argument new operation.
     * 
     * There are several reasons we do it this way:
     * 
     * 1) When spitting our AST back to Java code (for verification purposes),
     * we found it was impossible to correctly emulate nested classes as
     * non-nested classes using traditional constructor syntax. It boiled down
     * to the fact that you really HAVE to assign your synthetic arguments to
     * your synthetic fields BEFORE calling your superclass constructor (because
     * it might call you back polymorphically). And trying to do that in
     * straight Java is a semantic error, a super call must be the first
     * statement of your constructor.
     * 
     * 2) It's a lot more like how we'll be generating JavaScript eventually.
     * 
     * 3) It's a lot easier to optimize; the same optimizations work on our
     * synthetic fields as work on any user fields. In fact, once we're past AST
     * generation, we throw away all information about what's synthetic.
     * 
     * The order of emulation is: - assign all synthetic fields from synthetic
     * args - call our super constructor emulation method - call our instance
     * initializer emulation method - run user code - return this
     */
    void processConstructor(ConstructorDeclaration x) {
      JMethod ctor = (JMethod) typeMap.get(x.binding);
      try {
        JSourceInfo info = ctor.body.getSourceInfo();

        currentMethod = ctor;
        currentMethodScope = x.scope;

        JMethodCall call = null;
        ExplicitConstructorCall ctorCall = x.constructorCall;
        if (ctorCall != null) {
          call = (JMethodCall) dispatch("processExpression", ctorCall);
        }

        /*
         * Determine if we have an explicit this call. The presence of an
         * explicit this call indicates we can skip certain initialization steps
         * (as the callee will perform those steps for us). These skippable
         * steps are 1) assigning synthetic args to fields and 2) running
         * initializers.
         */
        boolean hasExplicitThis = (ctorCall != null)
            && !ctorCall.isSuperAccess();

        JClassType enclosingType = (JClassType) ctor.getEnclosingType();

        // Call clinit; $clinit is always in position 0.
        JMethod clinitMethod = (JMethod) enclosingType.methods.get(0);
        JMethodCall clinitCall = new JMethodCall(program, info, null,
            clinitMethod);
        ctor.body.statements.add(new JExpressionStatement(program, info,
            clinitCall));

        /*
         * All synthetic fields must be assigned, unless we have an explicit
         * this constructor call, in which case the callee will assign them for
         * us.
         */
        if (!hasExplicitThis) {
          ReferenceBinding declaringClass = x.binding.declaringClass;
          if (declaringClass instanceof NestedTypeBinding) {
            Iterator/* <JParameter> */paramIt = ctor.params.iterator();
            NestedTypeBinding nestedBinding = (NestedTypeBinding) declaringClass;
            if (nestedBinding.enclosingInstances != null) {
              for (int i = 0; i < nestedBinding.enclosingInstances.length; ++i) {
                SyntheticArgumentBinding arg = nestedBinding.enclosingInstances[i];
                JParameter param = (JParameter) paramIt.next();
                if (arg.matchingField != null) {
                  JField field = (JField) typeMap.get(arg);
                  ctor.body.statements.add(program.createAssignmentStmt(info,
                      createVariableRef(info, field), createVariableRef(info,
                          param)));
                }
              }
            }

            if (nestedBinding.outerLocalVariables != null) {
              for (int i = 0; i < nestedBinding.outerLocalVariables.length; ++i) {
                SyntheticArgumentBinding arg = nestedBinding.outerLocalVariables[i];
                JParameter param = (JParameter) paramIt.next();
                JField field = (JField) typeMap.get(arg);
                ctor.body.statements.add(program.createAssignmentStmt(info,
                    createVariableRef(info, field), createVariableRef(info,
                        param)));
              }
            }
          }
        }

        // optional this or super constructor call
        if (call != null) {
          ctor.body.statements.add(new JExpressionStatement(program,
              makeSourceInfo(ctorCall), call));
        }

        JExpression thisRef = createThisRef(info, enclosingType);

        /*
         * Call the synthetic instance initializer method, unless we have an
         * explicit this constructor call, in which case the callee will.
         */
        if (!hasExplicitThis) {
          // $init is always in position 1 (clinit is in 0)
          JMethod initMethod = (JMethod) enclosingType.methods.get(1);
          JMethodCall initCall = new JMethodCall(program, info, thisRef,
              initMethod);
          ctor.body.statements.add(new JExpressionStatement(program, info,
              initCall));
        }

        // user code (finally!)
        if (x.statements != null) {
          for (int i = 0, n = x.statements.length; i < n; ++i) {
            Statement origStmt = x.statements[i];
            JStatement jstmt = dispProcessStatement(origStmt);
            if (jstmt != null) {
              ctor.body.statements.add(jstmt);
            }
          }
        }

        currentMethodScope = null;
        currentMethod = null;

        // synthesize a return statement to emulate returning the new object
        ctor.body.statements.add(new JReturnStatement(program, null, thisRef));
      } catch (Throwable e) {
        throw translateException(ctor, e);
      }
    }

    JExpression processExpression(AllocationExpression x) {
      JSourceInfo info = makeSourceInfo(x);
      SourceTypeBinding typeBinding = (SourceTypeBinding) x.resolvedType;
      if (typeBinding.constantPoolName() == null) {
        /*
         * Weird case: if JDT determines that this local class is totally
         * uninstantiable, it won't bother allocating a local name.
         */
        return program.getLiteralNull();
      }
      JClassType newType = (JClassType) typeMap.get(typeBinding);
      MethodBinding b = x.binding;
      JMethod ctor = (JMethod) typeMap.get(b);
      JMethodCall call;
      JClassType javaLangString = program.getTypeJavaLangString();
      if (newType == javaLangString) {
        /*
         * MAGIC: java.lang.String is implemented as a JavaScript String
         * primitive with a modified prototype. This requires funky handling of
         * constructor calls. We find a method named _String() whose signature
         * matches the requested constructor
         */
        int ctorArgc = ctor.params.size();
        JMethod targetMethod = null;
        outer : for (int j = 0; j < javaLangString.methods.size(); ++j) {
          JMethod method = (JMethod) javaLangString.methods.get(j);
          if (method.getName().equals("_String")
              && method.params.size() == ctorArgc) {
            for (int i = 0; i < ctorArgc; ++i) {
              JParameter mparam = (JParameter) method.params.get(i);
              JParameter cparam = (JParameter) ctor.params.get(i);
              if (mparam.getType() != cparam.getType()) {
                continue outer;
              }
            }
            targetMethod = method;
            break;
          }
        }
        if (targetMethod == null) {
          throw new InternalCompilerException(
              "String constructor error; no matching implementation.");
        }
        call = new JMethodCall(program, makeSourceInfo(x), null, targetMethod);
      } else {
        JNewInstance newInstance = new JNewInstance(program, info, newType);
        call = new JMethodCall(program, info, newInstance, ctor);
      }

      // Synthetic args for inner classes
      ReferenceBinding targetBinding = b.declaringClass;
      if (targetBinding.isNestedType() && !targetBinding.isStatic()) {
        NestedTypeBinding nestedBinding = (NestedTypeBinding) targetBinding;
        // Synthetic this args for inner classes
        if (nestedBinding.enclosingInstances != null) {
          for (int i = 0; i < nestedBinding.enclosingInstances.length; ++i) {
            SyntheticArgumentBinding arg = nestedBinding.enclosingInstances[i];
            JClassType syntheticThisType = (JClassType) typeMap.get(arg.type);
            call.getArgs().add(createThisRef(info, syntheticThisType));
          }
        }
        // Synthetic locals for local classes
        if (nestedBinding.outerLocalVariables != null) {
          for (int i = 0; i < nestedBinding.outerLocalVariables.length; ++i) {
            SyntheticArgumentBinding arg = nestedBinding.outerLocalVariables[i];
            JVariable variable = (JVariable) typeMap.get(arg.actualOuterLocalVariable);
            call.getArgs().add(
                createVariableRef(info, variable, arg.actualOuterLocalVariable));
          }
        }
      }

      // Plain old regular user arguments
      if (x.arguments != null) {
        for (int i = 0, n = x.arguments.length; i < n; ++i) {
          call.getArgs().add(dispProcessExpression(x.arguments[i]));
        }
      }

      return call;
    }

    JExpression processExpression(AND_AND_Expression x) {
      JType type = (JType) typeMap.get(x.resolvedType);
      JSourceInfo info = makeSourceInfo(x);
      return processBinaryOperation(info, JBinaryOperator.AND, type, x.left,
          x.right);
    }

    JExpression processExpression(ArrayAllocationExpression x) {
      JSourceInfo info = makeSourceInfo(x);
      JArrayType type = (JArrayType) typeMap.get(x.resolvedType);
      JNewArray newArray = new JNewArray(program, info, type);

      if (x.initializer != null) {
        newArray.initializers = new ArrayList();
        if (x.initializer.expressions != null) {
          for (int i = 0; i < x.initializer.expressions.length; i++) {
            Expression expression = x.initializer.expressions[i];
            newArray.initializers.add(dispProcessExpression(expression));
          }
        }
      } else {
        newArray.dims = new ArrayList();
        for (int i = 0; i < x.dimensions.length; i++) {
          Expression dimension = x.dimensions[i];
          // can be null if index expression was empty
          if (dimension == null) {
            newArray.dims.add(program.getLiteralAbsentArrayDimension());
          } else {
            newArray.dims.add(dispProcessExpression(dimension));
          }
        }
      }

      return newArray;
    }

    JExpression processExpression(ArrayInitializer x) {
      JSourceInfo info = makeSourceInfo(x);
      JArrayType type = (JArrayType) typeMap.get(x.resolvedType);
      JNewArray newArray = new JNewArray(program, info, type);

      newArray.initializers = new ArrayList();
      if (x.expressions != null) {
        for (int i = 0; i < x.expressions.length; i++) {
          Expression expression = x.expressions[i];
          newArray.initializers.add(dispProcessExpression(expression));
        }
      }

      return newArray;
    }

    JExpression processExpression(ArrayReference x) {
      JSourceInfo info = makeSourceInfo(x);
      JArrayRef arrayRef = new JArrayRef(program, info,
          dispProcessExpression(x.receiver), dispProcessExpression(x.position));
      return arrayRef;
    }

    JExpression processExpression(Assignment x) {
      JType type = (JType) typeMap.get(x.resolvedType);
      JSourceInfo info = makeSourceInfo(x);
      return processBinaryOperation(info, JBinaryOperator.ASG, type, x.lhs,
          x.expression);
    }

    JExpression processExpression(BinaryExpression x) {
      JBinaryOperator op;

      int binOp = (x.bits & BinaryExpression.OperatorMASK) >> BinaryExpression.OperatorSHIFT;
      switch (binOp) {
        case BinaryExpression.LEFT_SHIFT:
          op = JBinaryOperator.SHL;
          break;
        case BinaryExpression.RIGHT_SHIFT:
          op = JBinaryOperator.SHR;
          break;
        case BinaryExpression.UNSIGNED_RIGHT_SHIFT:
          op = JBinaryOperator.SHRU;
          break;
        case BinaryExpression.PLUS:
          op = JBinaryOperator.ADD;
          break;
        case BinaryExpression.MINUS:
          op = JBinaryOperator.SUB;
          break;
        case BinaryExpression.REMAINDER:
          op = JBinaryOperator.MOD;
          break;
        case BinaryExpression.XOR:
          op = JBinaryOperator.BIT_XOR;
          break;
        case BinaryExpression.AND:
          op = JBinaryOperator.BIT_AND;
          break;
        case BinaryExpression.MULTIPLY:
          op = JBinaryOperator.MUL;
          break;
        case BinaryExpression.OR:
          op = JBinaryOperator.BIT_OR;
          break;
        case BinaryExpression.DIVIDE:
          op = JBinaryOperator.DIV;
          break;
        case BinaryExpression.LESS_EQUAL:
          op = JBinaryOperator.LTE;
          break;
        case BinaryExpression.GREATER_EQUAL:
          op = JBinaryOperator.GTE;
          break;
        case BinaryExpression.GREATER:
          op = JBinaryOperator.GT;
          break;
        case BinaryExpression.LESS:
          op = JBinaryOperator.LT;
          break;
        default:
          throw new InternalCompilerException(
              "Unexpected operator for BinaryExpression");
      }

      JType type = (JType) typeMap.get(x.resolvedType);
      JSourceInfo info = makeSourceInfo(x);
      return processBinaryOperation(info, op, type, x.left, x.right);
    }

    JExpression processExpression(CastExpression x) {
      JSourceInfo info = makeSourceInfo(x);
      JType type = (JType) typeMap.get(x.resolvedType);
      JCastOperation cast = new JCastOperation(program, info, type,
          dispProcessExpression(x.expression));
      return cast;
    }

    JExpression processExpression(ClassLiteralAccess x) {
      JType type = (JType) typeMap.get(x.targetType);
      return program.getLiteralClass(type);
    }

    JExpression processExpression(CompoundAssignment x) {
      JBinaryOperator op;

      switch (x.operator) {
        case CompoundAssignment.PLUS:
          op = JBinaryOperator.ASG_ADD;
          break;
        case CompoundAssignment.MINUS:
          op = JBinaryOperator.ASG_SUB;
          break;
        case CompoundAssignment.MULTIPLY:
          op = JBinaryOperator.ASG_MUL;
          break;
        case CompoundAssignment.DIVIDE:
          op = JBinaryOperator.ASG_DIV;
          break;
        case CompoundAssignment.AND:
          op = JBinaryOperator.ASG_BIT_AND;
          break;
        case CompoundAssignment.OR:
          op = JBinaryOperator.ASG_BIT_OR;
          break;
        case CompoundAssignment.XOR:
          op = JBinaryOperator.ASG_BIT_XOR;
          break;
        case CompoundAssignment.REMAINDER:
          op = JBinaryOperator.ASG_MOD;
          break;
        case CompoundAssignment.LEFT_SHIFT:
          op = JBinaryOperator.ASG_SHL;
          break;
        case CompoundAssignment.RIGHT_SHIFT:
          op = JBinaryOperator.ASG_SHR;
          break;
        case CompoundAssignment.UNSIGNED_RIGHT_SHIFT:
          op = JBinaryOperator.ASG_SHRU;
          break;
        default:
          throw new InternalCompilerException(
              "Unexpected operator for CompoundAssignment");
      }

      JType type = (JType) typeMap.get(x.resolvedType);
      JSourceInfo info = makeSourceInfo(x);
      return processBinaryOperation(info, op, type, x.lhs, x.expression);
    }

    JExpression processExpression(ConditionalExpression x) {
      JSourceInfo info = makeSourceInfo(x);
      JType type = (JType) typeMap.get(x.resolvedType);
      JExpression ifTest = dispProcessExpression(x.condition);
      JExpression thenExpr = dispProcessExpression(x.valueIfTrue);
      JExpression elseExpr = dispProcessExpression(x.valueIfFalse);
      JConditional conditional = new JConditional(program, info, type, ifTest,
          thenExpr, elseExpr);
      return conditional;
    }

    JExpression processExpression(EqualExpression x) {
      JBinaryOperator op;
      switch ((x.bits & BinaryExpression.OperatorMASK) >> BinaryExpression.OperatorSHIFT) {
        case BinaryExpression.EQUAL_EQUAL:
          op = JBinaryOperator.EQ;
          break;
        case BinaryExpression.NOT_EQUAL:
          op = JBinaryOperator.NEQ;
          break;
        default:
          throw new InternalCompilerException(
              "Unexpected operator for EqualExpression");
      }

      JType type = (JType) typeMap.get(x.resolvedType);
      JSourceInfo info = makeSourceInfo(x);
      return processBinaryOperation(info, op, type, x.left, x.right);
    }

    /**
     * How we have to treat super calls vs. this calls is so different, they may
     * as well have been two different JDT nodes.
     */
    JMethodCall processExpression(ExplicitConstructorCall x) {
      if (x.isSuperAccess()) {
        return processSuperConstructorCall(x);
      } else {
        return processThisConstructorCall(x);
      }
    }

    JExpression processExpression(FieldReference x) {
      JSourceInfo info = makeSourceInfo(x);
      FieldBinding fieldBinding = x.binding;
      JField field;
      if (fieldBinding.declaringClass == null) {
        // probably array.length
        field = program.getSpecialField("Array.length");
        if (!field.getName().equals(String.valueOf(fieldBinding.name))) {
          throw new InternalCompilerException("Error matching fieldBinding.");
        }
      } else {
        field = (JField) typeMap.get(fieldBinding);
      }
      JExpression instance = dispProcessExpression(x.receiver);
      JExpression fieldRef = new JFieldRef(program, info, instance, field,
          currentClass);
      return fieldRef;
    }

    JExpression processExpression(InstanceOfExpression x) {
      JSourceInfo info = makeSourceInfo(x);
      JExpression expr = dispProcessExpression(x.expression);
      JReferenceType testType = (JReferenceType) typeMap.get(x.type.resolvedType);
      return new JInstanceOf(program, info, testType, expr);
    }

    JExpression processExpression(MessageSend x) {
      JSourceInfo info = makeSourceInfo(x);
      JType type = (JType) typeMap.get(x.resolvedType);
      JMethod method = (JMethod) typeMap.get(x.binding);
      assert (type == method.getType());

      JExpression qualifier = dispProcessExpression(x.receiver);
      if (x.receiver instanceof ThisReference) {
        if (method.isStatic()) {
          // don't bother qualifying it, it's a no-op
          // TODO(???): this may be handled by later optimizations now
          qualifier = null;
        } else if (x.receiver instanceof QualifiedThisReference) {
          // do nothing, the qualifier is correct
        } else {
          /*
           * In cases where JDT had to synthesize a this ref for us, it could
           * actually be the wrong type, if the target method is in an enclosing
           * class. We have to sythensize our own ref of the correct type.
           */
          qualifier = createThisRef(info, method.getEnclosingType());
        }
      }

      JMethodCall call = new JMethodCall(program, info, qualifier, method);
      boolean isSuperRef = x.receiver instanceof SuperReference;
      if (isSuperRef) {
        call.setStaticDispatchOnly();
      }

      // The arguments come first...
      if (x.arguments != null) {
        for (int i = 0, n = x.arguments.length; i < n; ++i) {
          call.getArgs().add(dispProcessExpression(x.arguments[i]));
        }
      }

      return call;
    }

    JExpression processExpression(NullLiteral x) {
      return program.getLiteralNull();
    }

    JExpression processExpression(OR_OR_Expression x) {
      JType type = (JType) typeMap.get(x.resolvedType);
      JSourceInfo info = makeSourceInfo(x);
      return processBinaryOperation(info, JBinaryOperator.OR, type, x.left,
          x.right);
    }

    JExpression processExpression(PostfixExpression x) {
      JSourceInfo info = makeSourceInfo(x);
      JUnaryOperator op;

      switch (x.operator) {
        case PostfixExpression.MINUS:
          op = JUnaryOperator.DEC;
          break;

        case PostfixExpression.PLUS:
          op = JUnaryOperator.INC;
          break;

        default:
          throw new InternalCompilerException("Unexpected postfix operator");
      }

      JPostfixOperation postOp = new JPostfixOperation(program, info, op,
          dispProcessExpression(x.lhs));
      return postOp;
    }

    JExpression processExpression(PrefixExpression x) {
      JSourceInfo info = makeSourceInfo(x);
      JUnaryOperator op;

      switch (x.operator) {
        case PrefixExpression.MINUS:
          op = JUnaryOperator.DEC;
          break;

        case PrefixExpression.PLUS:
          op = JUnaryOperator.INC;
          break;

        default:
          throw new InternalCompilerException("Unexpected prefix operator");
      }

      JPrefixOperation preOp = new JPrefixOperation(program, info, op,
          dispProcessExpression(x.lhs));
      return preOp;
    }

    JExpression processExpression(QualifiedAllocationExpression x) {
      /*
       * Weird: sometimes JDT will create a QualifiedAllocationExpression with
       * no qualifier. I guess this is supposed to let us know that we need to
       * synthesize a synthetic this arg based on our own current "this"? But
       * plain old regular AllocationExpression also must be treated as if it
       * might be be implicitly qualified, so I'm not sure what the point is.
       * Let's just defer to the AllocationExpression logic if there's no
       * qualifier.
       */
      if (x.enclosingInstance() == null) {
        return processExpression((AllocationExpression) x);
      }

      JSourceInfo info = makeSourceInfo(x);
      MethodBinding b = x.binding;
      JMethod ctor = (JMethod) typeMap.get(b);
      JClassType enclosingType = (JClassType) ctor.getEnclosingType();
      JNewInstance newInstance = new JNewInstance(program, info, enclosingType);
      JMethodCall call = new JMethodCall(program, info, newInstance, ctor);
      JExpression qualifier = dispProcessExpression(x.enclosingInstance);

      // Synthetic args for inner classes
      ReferenceBinding targetBinding = b.declaringClass;
      if (targetBinding.isNestedType() && !targetBinding.isStatic()) {
        NestedTypeBinding nestedBinding = (NestedTypeBinding) targetBinding;
        // Synthetic this args for inner classes
        if (nestedBinding.enclosingInstances != null) {
          for (int i = 0; i < nestedBinding.enclosingInstances.length; ++i) {
            SyntheticArgumentBinding arg = nestedBinding.enclosingInstances[i];
            JClassType syntheticThisType = (JClassType) typeMap.get(arg.type);
            call.getArgs().add(createThisRef(syntheticThisType, qualifier));
          }
        }
        // Synthetic locals for local classes
        if (nestedBinding.outerLocalVariables != null) {
          for (int i = 0; i < nestedBinding.outerLocalVariables.length; ++i) {
            SyntheticArgumentBinding arg = nestedBinding.outerLocalVariables[i];
            JVariable variable = (JVariable) typeMap.get(arg.actualOuterLocalVariable);
            call.getArgs().add(
                createVariableRef(info, variable, arg.actualOuterLocalVariable));
          }
        }
      }

      // Plain old regular arguments
      if (x.arguments != null) {
        for (int i = 0, n = x.arguments.length; i < n; ++i) {
          call.getArgs().add(dispProcessExpression(x.arguments[i]));
        }
      }

      return call;
    }

    JExpression processExpression(QualifiedNameReference x) {
      JSourceInfo info = makeSourceInfo(x);
      Binding binding = x.binding;
      JNode node = typeMap.get(binding);
      if (!(node instanceof JVariable)) {
        return null;
      }
      JVariable variable = (JVariable) node;

      JExpression curRef = createVariableRef(info, variable, binding);

      /*
       * Wackiness: JDT represents multiple field access as an array of fields,
       * each qualified by everything to the left. So each subsequent item in
       * otherBindings takes the current expression as a qualifier.
       */
      if (x.otherBindings != null) {
        for (int i = 0; i < x.otherBindings.length; i++) {
          FieldBinding fieldBinding = x.otherBindings[i];
          JField field;
          if (fieldBinding.declaringClass == null) {
            // probably array.length
            field = program.getSpecialField("Array.length");
            if (!field.getName().equals(String.valueOf(fieldBinding.name))) {
              throw new InternalCompilerException(
                  "Error matching fieldBinding.");
            }
          } else {
            field = (JField) typeMap.get(fieldBinding);
          }
          curRef = new JFieldRef(program, info, curRef, field, currentClass);
        }
      }

      return curRef;
    }

    JExpression processExpression(QualifiedSuperReference x) {
      JClassType type = (JClassType) typeMap.get(x.resolvedType);
      JSourceInfo info = makeSourceInfo(x);
      /*
       * WEIRD: If a superref is qualified with the EXACT type of the innermost
       * type (in other words, a needless qualifier), it must refer to that
       * innermost type, because a class can never be nested inside of itself.
       * In this case, we must treat it as an unqualified superref.
       * 
       * In all other cases, the qualified superref cannot possibily refer to
       * the innermost type (even if the innermost type could be cast to a
       * compatible type), so we must create a reference to some outer type.
       */
      if (type == currentClass) {
        return createSuperRef(info, type);
      } else {
        return createQualifiedSuperRef(info, type);
      }
    }

    JExpression processExpression(QualifiedThisReference x) {
      JClassType type = (JClassType) typeMap.get(x.resolvedType);
      JSourceInfo info = makeSourceInfo(x);
      /*
       * WEIRD: If a thisref is qualified with the EXACT type of the innermost
       * type (in other words, a needless qualifier), it must refer to that
       * innermost type, because a class can never be nested inside of itself.
       * In this case, we must treat it as an unqualified thisref.
       * 
       * In all other cases, the qualified thisref cannot possibily refer to the
       * innermost type (even if the innermost type could be cast to a
       * compatible type), so we must create a reference to some outer type.
       */
      if (type == currentClass) {
        return createThisRef(info, type);
      } else {
        return createQualifiedThisRef(info, type);
      }
    }

    JExpression processExpression(SingleNameReference x) {
      JSourceInfo info = makeSourceInfo(x);
      Binding binding = x.binding;
      Object target = typeMap.get(binding);
      if (!(target instanceof JVariable)) {
        return null;
      }
      JVariable variable = (JVariable) target;

      /*
       * Wackiness: if a field happens to have synthetic accessors (only fields
       * can have them, apparently), this is a ref to a field in an enclosing
       * instance. CreateThisRef should compute a "this" access of the
       * appropriate type, unless the field is static.
       */
      if (x.syntheticAccessors != null) {
        JField field = (JField) variable;
        if (!field.isStatic()) {
          JExpression instance = createThisRef(info, field.getEnclosingType());
          return new JFieldRef(program, info, instance, field, currentClass);
        }
      }

      return createVariableRef(info, variable, binding);
    }

    JExpression processExpression(SuperReference x) {
      JClassType type = (JClassType) typeMap.get(x.resolvedType);
      assert (type == currentClass.extnds);
      JSourceInfo info = makeSourceInfo(x);
      JExpression superRef = createSuperRef(info, type);
      return superRef;
    }

    JExpression processExpression(ThisReference x) {
      JClassType type = (JClassType) typeMap.get(x.resolvedType);
      assert (type == currentClass);
      JSourceInfo info = makeSourceInfo(x);
      JExpression thisRef = createThisRef(info, type);
      return thisRef;
    }

    JExpression processExpression(UnaryExpression x) {
      JSourceInfo info = makeSourceInfo(x);
      JUnaryOperator op;
      int operator = ((x.bits & UnaryExpression.OperatorMASK) >> UnaryExpression.OperatorSHIFT);

      switch (operator) {
        case UnaryExpression.MINUS:
          op = JUnaryOperator.NEG;
          break;

        case UnaryExpression.NOT:
          op = JUnaryOperator.NOT;
          break;

        case UnaryExpression.PLUS:
          // Odd case.. a useless + operator; just return the operand
          return dispProcessExpression(x.expression);

        case UnaryExpression.TWIDDLE:
          op = JUnaryOperator.BIT_NOT;
          break;

        default:
          throw new InternalCompilerException(
              "Unexpected operator for unary expression");
      }

      JPrefixOperation preOp = new JPrefixOperation(program, info, op,
          dispProcessExpression(x.expression));
      return preOp;
    }

    void processField(FieldDeclaration declaration) {
      JField field = (JField) typeMap.tryGet(declaration.binding);
      if (field == null) {
        /*
         * When anonymous classes declare constant fields, the field declaration
         * is not visited by JDT. Just bail since any references to that field
         * are guaranteed to be replaced with literals.
         */
        return;
      }
      try {
        JExpression initializer = null;
        if (declaration.initialization != null) {
          initializer = dispProcessExpression(declaration.initialization);
        }

        if (initializer instanceof JLiteral) {
          field.constInitializer = (JLiteral) initializer;
        } else if (initializer != null) {
          JSourceInfo info = makeSourceInfo(declaration);
          JStatement assignStmt = program.createAssignmentStmt(info,
              createVariableRef(info, field), initializer);

          // will either be init or clinit
          currentMethod.body.statements.add(assignStmt);
        }
      } catch (Throwable e) {
        throw translateException(field, e);
      }
    }

    void processInitializer(Initializer initializer) {
      JBlock block = (JBlock) dispProcessStatement(initializer.block);
      try {
        // will either be init or clinit
        currentMethod.body.statements.add(block);
      } catch (Throwable e) {
        throw translateException(initializer, e);
      }
    }

    void processMethod(AbstractMethodDeclaration x) {
      MethodBinding b = x.binding;
      JMethod method = (JMethod) typeMap.get(b);
      try {
        if (b.isImplementing() || b.isOverriding()) {
          tryFindUpRefs(method, b);
        }

        if (x.isNative()) {
          processNativeMethod(x, (JsniMethod) method);
          return;
        }

        currentMethod = method;
        currentMethodScope = x.scope;

        if (x.statements != null) {
          for (int i = 0, n = x.statements.length; i < n; ++i) {
            Statement origStmt = x.statements[i];
            JStatement jstmt = dispProcessStatement(origStmt);
            if (jstmt != null) {
              method.body.statements.add(jstmt);
            }
          }
        }
        currentMethodScope = null;
        currentMethod = null;
      } catch (Throwable e) {
        throw translateException(method, e);
      }
    }

    void processNativeMethod(AbstractMethodDeclaration x,
        JsniMethod nativeMethod) {

      JsFunction func = nativeMethod.getFunc();
      if (func == null) {
        return;
      }

      // resolve jsni refs
      final List/* <JsNameRef> */nameRefs = new ArrayList/* <JsNameRef> */();
      func.traverse(new JsAbstractVisitorWithAllVisits() {
        // @Override
        public void endVisit(JsNameRef x) {
          String ident = x.getName().getIdent();
          if (ident.charAt(0) == '@') {
            nameRefs.add(x);
          }
        }
      });

      for (int i = 0; i < nameRefs.size(); ++i) {
        JsNameRef nameRef = (JsNameRef) nameRefs.get(i);
        JSourceInfo info = translateInfo(nameRef.getInfo());
        String ident = nameRef.getName().getIdent();
        HasEnclosingType node = (HasEnclosingType) program.jsniMap.get(ident);
        if (node == null) {
          node = parseJsniRef(info, x, ident);
          if (node == null) {
            continue; // already reported error
          }
          program.jsniMap.put(ident, node);
        }
        assert (node != null);
        CanBeStatic canBeStatic = (CanBeStatic) node;
        HasName hasName = (HasName) node;
        boolean isField = node instanceof JField;
        assert (isField || node instanceof JMethod);
        if (canBeStatic.isStatic() && nameRef.getQualifier() != null) {
          reportJsniError(info, x,
              "Cannot make a qualified reference to the static "
                  + (isField ? "field " : "method ") + hasName.getName());
        } else if (!canBeStatic.isStatic() && nameRef.getQualifier() == null) {
          reportJsniError(info, x,
              "Cannot make an unqualified reference to the instance "
                  + (isField ? "field " : "method ") + hasName.getName());
        }

        if (isField) {
          /*
           * TODO FIXME HACK: We should be replacing compile-time constant refs
           * from JSNI with the literal value of the field.
           */
          JField field = (JField) node;
          JsniFieldRef fieldRef = new JsniFieldRef(program, info, field,
              currentClass);
          nativeMethod.jsniFieldRefs.add(fieldRef);
        } else {
          JMethod method = (JMethod) node;
          JsniMethodRef methodRef = new JsniMethodRef(program, info, method);
          nativeMethod.jsniMethodRefs.add(methodRef);
        }
      }
    }

    // // 5.0
    // JStatement processStatement(ForeachStatement x) {
    // return null;
    // }

    JStatement processStatement(AssertStatement x) {
      JSourceInfo info = makeSourceInfo(x);
      JExpression expr = dispProcessExpression(x.assertExpression);
      JExpression arg = dispProcessExpression(x.exceptionArgument);
      return new JAssertStatement(program, info, expr, arg);
    }

    JBlock processStatement(Block x) {
      if (x == null) {
        return null;
      }

      JSourceInfo info = makeSourceInfo(x);
      JBlock block = new JBlock(program, info);
      if (x.statements != null) {
        for (int i = 0, n = x.statements.length; i < n; ++i) {
          JStatement jstmt = dispProcessStatement(x.statements[i]);
          if (jstmt != null) {
            block.statements.add(jstmt);
          }
        }
      }
      return block;
    }

    JStatement processStatement(BreakStatement x) {
      JSourceInfo info = makeSourceInfo(x);
      return new JBreakStatement(program, info, getOrCreateLabel(info,
          currentMethod, x.label));
    }

    JStatement processStatement(CaseStatement x) {
      JSourceInfo info = makeSourceInfo(x);
      JExpression expression = dispProcessExpression(x.constantExpression);
      return new JCaseStatement(program, info, (JLiteral) expression);
    }

    JStatement processStatement(ContinueStatement x) {
      JSourceInfo info = makeSourceInfo(x);
      return new JContinueStatement(program, info, getOrCreateLabel(info,
          currentMethod, x.label));
    }

    JStatement processStatement(DoStatement x) {
      JSourceInfo info = makeSourceInfo(x);
      JExpression loopTest = dispProcessExpression(x.condition);
      JStatement loopBody = dispProcessStatement(x.action);
      JDoStatement stmt = new JDoStatement(program, info, loopTest, loopBody);
      return stmt;
    }

    JStatement processStatement(EmptyStatement x) {
      return null;
    }

    JStatement processStatement(ForStatement x) {
      JSourceInfo info = makeSourceInfo(x);
      // SEE NOTE ON JDT FORCED OPTIMIZATIONS
      // If false, just return the initializers
      // for (init; false; inc) { x } => { init }
      if (x.condition != null) {
        Constant cst = x.condition.optimizedBooleanConstant();
        if (cst != Constant.NotAConstant) {
          if (!cst.booleanValue()) {
            JBlock block = new JBlock(program, info);
            block.statements = processStatements(x.initializations);
            return block;
          }
        }
      }

      List/* <? extends JStatement> */init = processStatements(x.initializations);
      JExpression expr = dispProcessExpression(x.condition);
      List/* <JExpressionStatement> */incr = processStatements(x.increments);
      JStatement body = dispProcessStatement(x.action);
      return new JForStatement(program, info, init, expr, incr, body);
    }

    JStatement processStatement(IfStatement x) {
      // SEE NOTE ON JDT FORCED OPTIMIZATIONS
      // If constant, just return the appropriate case
      // if (true) x; else y; => x
      // if (false) x; else y; => y
      // if (true) x; => x
      // if (false) x; => ;
      Constant cst = x.condition.optimizedBooleanConstant();
      if (cst != Constant.NotAConstant) {
        if (cst.booleanValue()) {
          return dispProcessStatement(x.thenStatement);
        } else {
          return dispProcessStatement(x.elseStatement);
        }
      }

      JSourceInfo info = makeSourceInfo(x);
      JExpression expr = dispProcessExpression(x.condition);
      JStatement thenStmt = dispProcessStatement(x.thenStatement);
      JStatement elseStmt = dispProcessStatement(x.elseStatement);
      JIfStatement ifStmt = new JIfStatement(program, info, expr, thenStmt,
          elseStmt);
      return ifStmt;
    }

    JStatement processStatement(LabeledStatement x) {
      JStatement body = dispProcessStatement(x.statement);
      if (body == null) {
        return null;
      }
      JSourceInfo info = makeSourceInfo(x);
      return new JLabeledStatement(program, info, getOrCreateLabel(info,
          currentMethod, x.label), body);
    }

    JStatement processStatement(LocalDeclaration x) {
      JSourceInfo info = makeSourceInfo(x);
      JLocal local = (JLocal) typeMap.get(x.binding);
      JLocalRef localRef = new JLocalRef(program, info, local);
      JExpression initializer = dispProcessExpression(x.initialization);
      return new JLocalDeclarationStatement(program, info, localRef,
          initializer);
    }

    JStatement processStatement(ReturnStatement x) {
      JSourceInfo info = makeSourceInfo(x);
      if (currentMethodScope.referenceContext instanceof ConstructorDeclaration) {
        /*
         * Special: constructors are implemented as instance methods that return
         * their this object, so any embedded return statements have to be fixed
         * up.
         */
        JClassType enclosingType = (JClassType) currentMethod.getEnclosingType();
        assert (x.expression == null);
        return new JReturnStatement(program, info, createThisRef(info,
            enclosingType));
      } else {
        return new JReturnStatement(program, info,
            dispProcessExpression(x.expression));
      }
    }

    JStatement processStatement(SwitchStatement x) {
      JSourceInfo info = makeSourceInfo(x);
      JExpression expression = dispProcessExpression(x.expression);
      JBlock block = new JBlock(program, info);
      block.statements = processStatements(x.statements);
      return new JSwitchStatement(program, info, expression, block);
    }

    JStatement processStatement(SynchronizedStatement x) {
      JSourceInfo info = makeSourceInfo(x);
      JBlock block = (JBlock) dispProcessStatement(x.block);
      JExpression expr = dispProcessExpression(x.expression);
      block.statements.add(0, new JExpressionStatement(program, info, expr));
      return block;
    }

    JStatement processStatement(ThrowStatement x) {
      JSourceInfo info = makeSourceInfo(x);
      JExpression toThrow = dispProcessExpression(x.exception);
      return new JThrowStatement(program, info, toThrow);
    }

    JStatement processStatement(TryStatement x) {
      JSourceInfo info = makeSourceInfo(x);
      JBlock tryBlock = (JBlock) dispProcessStatement(x.tryBlock);
      List/* <JLocalRef> */catchArgs = new ArrayList/* <JLocalRef> */();
      List/* <JBlock> */catchBlocks = new ArrayList/* <JBlock> */();
      if (x.catchBlocks != null) {
        for (int i = 0, c = x.catchArguments.length; i < c; ++i) {
          JLocal local = (JLocal) typeMap.get(x.catchArguments[i].binding);
          catchArgs.add(createVariableRef(info, local));
        }
        for (int i = 0, c = x.catchBlocks.length; i < c; ++i) {
          catchBlocks.add(dispProcessStatement(x.catchBlocks[i]));
        }
      }
      JBlock finallyBlock = (JBlock) dispProcessStatement(x.finallyBlock);
      return new JTryStatement(program, info, tryBlock, catchArgs, catchBlocks,
          finallyBlock);
    }

    JStatement processStatement(TypeDeclaration x) {
      // do nothing -- the local class is treated at the program level
      return null;
    }

    JStatement processStatement(WhileStatement x) {
      // SEE NOTE ON JDT FORCED OPTIMIZATIONS
      // If false, just return nothing
      // while (false) { x } => ;
      Constant cst = x.condition.optimizedBooleanConstant();
      if (cst != Constant.NotAConstant) {
        if (!cst.booleanValue()) {
          return null;
        }
      }
      JSourceInfo info = makeSourceInfo(x);
      JExpression loopTest = dispProcessExpression(x.condition);
      JStatement loopBody = dispProcessStatement(x.action);
      JWhileStatement stmt = new JWhileStatement(program, info, loopTest,
          loopBody);
      return stmt;
    }

    List/* <? extends JStatement> */processStatements(Statement[] statements) {
      List/* <JStatement> */jstatements = new ArrayList/* <JStatement> */();
      if (statements != null) {
        for (int i = 0, n = statements.length; i < n; ++i) {
          JStatement jstmt = dispProcessStatement(statements[i]);
          if (jstmt != null) {
            jstatements.add(jstmt);
          }
        }
      }
      return jstatements;
    }

    JMethodCall processSuperConstructorCall(ExplicitConstructorCall x) {
      JSourceInfo info = makeSourceInfo(x);
      JMethod ctor = (JMethod) typeMap.get(x.binding);
      JExpression trueQualifier = createThisRef(info, currentClass);
      JMethodCall call = new JMethodCall(program, info, trueQualifier, ctor);

      // We have to find and pass through any synthetics our supertype needs
      ReferenceBinding superClass = x.binding.declaringClass;
      if (superClass instanceof NestedTypeBinding && !superClass.isStatic()) {
        NestedTypeBinding myBinding = (NestedTypeBinding) currentClassScope.referenceType().binding;
        NestedTypeBinding superBinding = (NestedTypeBinding) superClass;

        // enclosing types
        if (superBinding.enclosingInstances != null) {
          JExpression qualifier = dispProcessExpression(x.qualification);
          for (int j = 0; j < superBinding.enclosingInstances.length; ++j) {
            SyntheticArgumentBinding arg = superBinding.enclosingInstances[j];
            JClassType classType = (JClassType) typeMap.get(arg.type);
            if (qualifier == null) {
              /*
               * Got to be one of my params; it would be illegal to use a this
               * ref at this moment-- we would most likely be passing in a
               * supertype field that HASN'T BEEN INITIALIZED YET.
               * 
               * Unfortunately, my params might not work as-is, so we have to
               * check each one to see if any will make a suitable this ref.
               */
              List/* <JExpression> */workList = new ArrayList/* <JExpression> */();
              Iterator/* <JParameter> */paramIt = currentMethod.params.iterator();
              for (int i = 0; i < myBinding.enclosingInstances.length; ++i) {
                workList.add(createVariableRef(info,
                    (JParameter) paramIt.next()));
              }
              call.getArgs().add(createThisRef(classType, workList));
            } else {
              call.getArgs().add(createThisRef(classType, qualifier));
            }
          }
        }

        // outer locals
        if (superBinding.outerLocalVariables != null) {
          for (int j = 0; j < superBinding.outerLocalVariables.length; ++j) {
            SyntheticArgumentBinding arg = superBinding.outerLocalVariables[j];
            // Got to be one of my params
            JType varType = (JType) typeMap.get(arg.type);
            String varName = String.valueOf(arg.name);
            JParameter param = null;
            for (int i = 0; i < currentMethod.params.size(); ++i) {
              JParameter paramIt = (JParameter) currentMethod.params.get(i);
              if (varType == paramIt.getType()
                  && varName.equals(paramIt.getName())) {
                param = paramIt;
              }
            }
            if (param == null) {
              throw new InternalCompilerException(
                  "Could not find matching local arg for explicit super ctor call.");
            }
            call.getArgs().add(createVariableRef(info, param));
          }
        }
      }

      if (x.arguments != null) {
        for (int i = 0, n = x.arguments.length; i < n; ++i) {
          call.getArgs().add(dispProcessExpression(x.arguments[i]));
        }
      }

      return call;
    }

    JMethodCall processThisConstructorCall(ExplicitConstructorCall x) {
      JSourceInfo info = makeSourceInfo(x);
      JMethod ctor = (JMethod) typeMap.get(x.binding);
      JExpression trueQualifier = createThisRef(info, currentClass);
      JMethodCall call = new JMethodCall(program, info, trueQualifier, ctor);

      // All synthetics must be passed through to the target ctor
      ReferenceBinding declaringClass = x.binding.declaringClass;
      if (declaringClass instanceof NestedTypeBinding) {
        Iterator/* <JParameter> */paramIt = currentMethod.params.iterator();
        NestedTypeBinding nestedBinding = (NestedTypeBinding) declaringClass;
        if (nestedBinding.enclosingInstances != null) {
          for (int i = 0; i < nestedBinding.enclosingInstances.length; ++i) {
            call.getArgs().add(
                createVariableRef(info, (JParameter) paramIt.next()));
          }
        }
        if (nestedBinding.outerLocalVariables != null) {
          for (int i = 0; i < nestedBinding.outerLocalVariables.length; ++i) {
            call.getArgs().add(
                createVariableRef(info, (JParameter) paramIt.next()));
          }
        }
      }

      assert (x.qualification == null);
      if (x.arguments != null) {
        for (int i = 0, n = x.arguments.length; i < n; ++i) {
          call.getArgs().add(dispProcessExpression(x.arguments[i]));
        }
      }

      return call;
    }

    private void addAllOuterThisRefs(List list, JExpression expr,
        JClassType classType) {
      if (classType.fields.size() > 0) {
        JField field = (JField) classType.fields.get(0);
        if (field.getName().startsWith("this$")) {
          list.add(new JFieldRef(program, expr.getSourceInfo(), expr, field,
              currentClass));
        }
      }
    }

    private void addAllOuterThisRefsPlusSuperChain(List workList,
        JExpression expr, JClassType classType) {
      for (; classType != null; classType = classType.extnds) {
        addAllOuterThisRefs(workList, expr, classType);
      }
    }

    private boolean areParametersIdentical(MethodBinding a, MethodBinding b) {
      TypeBinding[] params1 = a.parameters;
      TypeBinding[] params2 = b.parameters;
      if (params1.length != params2.length) {
        return false;
      }

      for (int i = 0; i < params1.length; ++i) {
        if (params1[i] != params2[i]) {
          return false;
        }
      }

      return true;
    }

    /**
     * Helper to create a qualified "super" ref (really a synthetic this field
     * access) of the appropriate type. Always use this method instead of
     * creating a naked JThisRef or you won't get the synthetic accesses right.
     */
    private JExpression createQualifiedSuperRef(JSourceInfo info,
        JClassType targetType) {
      assert (currentClass instanceof JClassType);
      JExpression expr = program.getExprThisRef(info, (JClassType) currentClass);
      List/* <JExpression> */list = new ArrayList();
      addAllOuterThisRefsPlusSuperChain(list, expr, (JClassType) currentClass);
      return createSuperRef(targetType, list);
    }

    /**
     * Helper to create a qualified "this" ref (really a synthetic this field
     * access) of the appropriate type. Always use this method instead of
     * creating a naked JThisRef or you won't get the synthetic accesses right.
     */
    private JExpression createQualifiedThisRef(JSourceInfo info,
        JClassType targetType) {
      assert (currentClass instanceof JClassType);
      JExpression expr = program.getExprThisRef(info, (JClassType) currentClass);
      List/* <JExpression> */list = new ArrayList();
      addAllOuterThisRefsPlusSuperChain(list, expr, (JClassType) currentClass);
      return createThisRef(targetType, list);
    }

    /**
     * Helper to create a "super" ref (really a this ref or synthetic this field
     * access) of the appropriate type. Always use this method instead of
     * creating a naked JThisRef or you won't get the synthetic accesses right.
     */
    private JExpression createSuperRef(JClassType targetType,
        List/* <JExpression> */list) {
      assert (currentClass instanceof JClassType);
      LinkedList/* <JExpression> */workList = new LinkedList/* <JExpression> */();
      workList.addAll(list);

      while (!workList.isEmpty()) {
        JExpression expr = (JExpression) workList.removeFirst();
        JClassType classType = (JClassType) expr.getType();
        if (classType.extnds == targetType) {
          return expr;
        }
        addAllOuterThisRefsPlusSuperChain(workList, expr, classType);
      }

      throw new InternalCompilerException(
          "Cannot create a SuperRef of the appropriate type.");
    }

    /**
     * Helper to create a "super" ref (really a this ref or synthetic this field
     * access) of the appropriate type. Always use this method instead of
     * creating a naked JThisRef or you won't get the synthetic accesses right.
     */
    private JExpression createSuperRef(JSourceInfo info, JClassType targetType) {
      assert (currentClass instanceof JClassType);
      JExpression expr = program.getExprThisRef(info, (JClassType) currentClass);
      List/* <JExpression> */list = new ArrayList();
      list.add(expr);
      return createSuperRef(targetType, list);
    }

    /**
     * Helper to creates an expression of the target type, possibly by accessing
     * synthetic this fields on the passed-in expression. This is needed by a
     * QualifiedAllocationExpression, because the qualifier may not be the
     * correct type, and we may need use one of its fields.
     */
    private JExpression createThisRef(JReferenceType targetType,
        JExpression expr) {
      List/* <JExpression> */list = new ArrayList();
      list.add(expr);
      return createThisRef(targetType, list);
    }

    /**
     * Helper to creates an expression of the target type, possibly by accessing
     * synthetic this fields on ANY of several passed-in expressions. Why in the
     * world would we need to do this? It turns out that when making an
     * unqualified explicit super constructor call to something that needs a
     * synthetic outer this arg, the correct value to pass in can be one of
     * several of the calling constructor's own synthetic ags. The catch is,
     * it's possible none of the args are exactly the right type-- we have to
     * make one of them the right type by following each of their synthetic this
     * refs up an arbitrarily big tree of enclosing classes and
     * supertypes-with-enclosing-classes until we find something that's the
     * right type.
     * 
     * We have this implemented as a Breadth-First Search to minimize the number
     * of derefs required, and this seems to be correct. Note that we explicitly
     * prefer the current expression as one of its supertypes over a synthetic
     * this ref rooted off the current expression that happens to be the correct
     * type. We have observed this to be consistent with how Java handles it.
     */
    private JExpression createThisRef(JReferenceType targetType,
        List/* <JExpression> */list) {
      LinkedList/* <JExpression> */workList = new LinkedList/* <JExpression> */();
      workList.addAll(list);
      while (!workList.isEmpty()) {
        JExpression expr = (JExpression) workList.removeFirst();
        JClassType classType = (JClassType) expr.getType();
        for (; classType != null; classType = classType.extnds) {
          // prefer myself or myself-as-supertype over any of my this$ fields
          // that may have already been added to the work list
          if (program.typeOracle.canTriviallyCast(classType, targetType)) {
            return expr;
          }
          addAllOuterThisRefs(workList, expr, classType);
        }
      }

      throw new InternalCompilerException(
          "Cannot create a ThisRef of the appropriate type.");
    }

    /**
     * Helper to creates this ref (or maybe a synthetic this field access) of
     * the appropriate type. Always use this method instead of creating a naked
     * JThisRef or you won't get the synthetic accesses right.
     */
    private JExpression createThisRef(JSourceInfo info,
        JReferenceType targetType) {
      assert (currentClass instanceof JClassType);
      return createThisRef(targetType, program.getExprThisRef(info,
          (JClassType) currentClass));
    }

    /**
     * Creates an appropriate JVariableRef for the polymorphic type of the
     * requested JVariable.
     */
    private JVariableRef createVariableRef(JSourceInfo info, JVariable variable) {
      if (variable instanceof JLocal) {
        JLocal local = (JLocal) variable;
        if (local.getEnclosingMethod() != currentMethod) {
          throw new InternalCompilerException(
              "LocalRef referencing local in a different method.");
        }
        return new JLocalRef(program, info, local);
      } else if (variable instanceof JParameter) {
        JParameter parameter = (JParameter) variable;
        if (parameter.getEnclosingMethod() != currentMethod) {
          throw new InternalCompilerException(
              "ParameterRef referencing param in a different method.");
        }
        return new JParameterRef(program, info, parameter);
      } else if (variable instanceof JField) {
        JField field = (JField) variable;
        JExpression instance = null;
        if (!field.isStatic()) {
          JClassType fieldEnclosingType = (JClassType) field.getEnclosingType();
          instance = createThisRef(info, fieldEnclosingType);
          if (!program.typeOracle.canTriviallyCast(
              (JClassType) instance.getType(), fieldEnclosingType)) {
            throw new InternalCompilerException(
                "FieldRef referencing field in a different type.");
          }
        }
        return new JFieldRef(program, info, instance, field, currentClass);
      }
      throw new InternalCompilerException("Unknown JVariable subclass.");
    }

    /**
     * Creates an appropriate JVariableRef for the polymorphic type of the
     * requested JVariable.
     */
    private JVariableRef createVariableRef(JSourceInfo info,
        JVariable variable, Binding binding) {
      // Fix up the reference if it's to an outer local/param
      variable = possiblyReferenceOuterLocal(variable, binding);
      if (variable == null) {
        /*
         * Strange case: in certain circumstances, JDT will fail to provide an
         * emulation path to an outer local variable. In the case I know of, the
         * reference is a spurious qualifier to a static method call. Let's just
         * return null and ditch the expression.
         */
        return null;
      }
      return createVariableRef(info, variable);
    }

    /**
     * Get a new label of a particular name, or create a new one if it doesn't
     * exist already.
     */
    private JLabel getOrCreateLabel(JSourceInfo info, JMethod enclosingMethod,
        char[] name) {
      if (name == null) {
        return null;
      }
      String sname = String.valueOf(name);
      Map/* <String, JLabel> */lblMap = (Map) this.labelMap.get(enclosingMethod);
      if (lblMap == null) {
        lblMap = new HashMap();
        this.labelMap.put(enclosingMethod, lblMap);
      }
      JLabel jlabel = (JLabel) lblMap.get(sname);
      if (jlabel == null) {
        jlabel = new JLabel(program, info, sname);
        lblMap.put(sname, jlabel);
      }
      return jlabel;
    }

    private JSourceInfo makeSourceInfo(Statement x) {
      int startLine = ProblemHandler.searchLineNumber(
          currentSeparatorPositions, x.sourceStart);
      return new JSourceInfo(x.sourceStart, x.sourceEnd, startLine,
          currentFileName);
    }

    private HasEnclosingType parseJsniRef(JSourceInfo info,
        AbstractMethodDeclaration x, String ident) {
      String[] parts = ident.substring(1).split("::");
      assert (parts.length == 2);
      String className = parts[0];
      JReferenceType type = program.getFromTypeMap(className);
      if (type == null) {
        reportJsniError(info, x, "Unresolvable native reference to type '"
            + className + "'");
        return null;
      }
      String rhs = parts[1];
      int parenPos = rhs.indexOf('(');
      if (parenPos < 0) {
        // look for a field
        for (int i = 0; i < type.fields.size(); ++i) {
          JField field = (JField) type.fields.get(i);
          if (field.getName().equals(rhs)) {
            return field;
          }
        }

        reportJsniError(info, x, "Unresolvable native reference to field '"
            + rhs + "' in type '" + className + "'");
      } else {
        // look for a method
        String methodName = rhs.substring(0, parenPos);
        String almostMatches = null;
        for (int i = 0; i < type.methods.size(); ++i) {
          JMethod method = (JMethod) type.methods.get(i);
          if (method.getName().equals(methodName)) {
            String jsniSig = getJsniSig(method);
            if (jsniSig.equals(rhs)) {
              return method;
            } else if (almostMatches == null) {
              almostMatches = "'" + jsniSig + "'";
            } else {
              almostMatches += ", '" + jsniSig + "'";
            }
          }
        }

        if (almostMatches == null) {
          reportJsniError(info, x, "Unresolvable native reference to method '"
              + methodName + "' in type '" + className + "'");
        } else {
          reportJsniError(info, x, "Unresolvable native reference to method '"
              + methodName + "' in type '" + className + "' (did you mean "
              + almostMatches + "?)");
        }
      }
      return null;
    }

    /**
     * Sometimes a variable reference can be to a local or parameter in an an
     * enclosing method. This is a tricky situation to detect. There's no
     * obvious way to tell, but the clue we can get from JDT is that the local's
     * containing method won't be the same as the method we're currently
     * processing.
     * 
     * Once we have this clue, we can use getEmulationPath to compute the
     * current class's binding for that field.
     */
    private JVariable possiblyReferenceOuterLocal(JVariable variable,
        Binding binding) {

      if (variable instanceof JLocal || variable instanceof JParameter) {
        LocalVariableBinding localBinding = (LocalVariableBinding) binding;
        if (localBinding.declaringScope.methodScope() != currentMethodScope) {
          variable = null;
          VariableBinding[] vars = currentMethodScope.getEmulationPath(localBinding);
          if (vars == null) {
            return null;
          }
          assert (vars.length == 1);
          VariableBinding varBinding = vars[0];

          // See if there's an available parameter
          if (varBinding instanceof SyntheticArgumentBinding) {
            JType type = (JType) typeMap.get(varBinding.type);
            String name = String.valueOf(varBinding.name);
            for (int i = 0; i < currentMethod.params.size(); ++i) {
              JParameter param = (JParameter) currentMethod.params.get(i);
              if (type == param.getType() && name.equals(param.getName())) {
                variable = param;
                break;
              }
            }
          }

          // just use the field
          if (variable == null) {
            variable = (JField) typeMap.get(varBinding);
          }

          // now we have an updated variable that we can create our ref from
        }
      }
      return variable;
    }

    /**
     * Helper for creating all JBinaryOperation. Several different JDT nodes can
     * result in binary operations: AND_AND_Expression, Assignment,
     * BinaryExpresion, CompoundAssignment, EqualExpression, and
     * OR_OR_Expression. Hopefully the specific operators that can result in
     * each different JDT type won't change between releases, because we only
     * look for the specific operators that we think should match each JDT node,
     * and throw an error if there's a mismatch.
     */
    private JExpression processBinaryOperation(JSourceInfo info,
        JBinaryOperator op, JType type, Expression arg1, Expression arg2) {
      JExpression exprArg1 = dispProcessExpression(arg1);
      JExpression exprArg2 = dispProcessExpression(arg2);
      JBinaryOperation binaryOperation = new JBinaryOperation(program, info,
          type, op, exprArg1, exprArg2);
      return binaryOperation;
    }

    private InternalCompilerException translateException(Object node,
        Throwable e) {
      InternalCompilerException ice;
      if (e instanceof InternalCompilerException) {
        ice = (InternalCompilerException) e;
      } else {
        ice = new InternalCompilerException("Error constructing Java AST", e);
      }
      String className = node.getClass().getName();
      String description = node.toString();
      JSourceInfo sourceInfo = null;
      if (node instanceof Statement) {
        sourceInfo = makeSourceInfo((Statement) node);
      }
      ice.addNode(className, description, sourceInfo);
      return ice;
    }

    /**
     * For a given method(and method binding), try to find all methods that it
     * overrides/implements.
     */
    private void tryFindUpRefs(JMethod method, MethodBinding binding) {
      tryFindUpRefsRecursive(method, binding, binding.declaringClass);
    }

    /**
     * For a given method(and method binding), recursively try to find all
     * methods that it overrides/implements.
     */
    private void tryFindUpRefsRecursive(JMethod method, MethodBinding binding,
        ReferenceBinding searchThisType) {

      // See if this class has any uprefs, unless this class is myself
      if (binding.declaringClass != searchThisType) {
        MethodBinding result = searchThisType.getExactMethod(binding.selector,
            binding.parameters, null);

        if (result != null) {
          if (areParametersIdentical(binding, result)) {
            JMethod upRef = (JMethod) typeMap.get(result);
            if (!method.overrides.contains(upRef)) {
              method.overrides.add(upRef);
            }
          }
        }
      }

      // recurse super class
      if (searchThisType.superclass() != null) {
        tryFindUpRefsRecursive(method, binding, searchThisType.superclass());
      }

      // recurse super interfaces
      if (searchThisType.superInterfaces() != null) {
        for (int i = 0; i < searchThisType.superInterfaces().length; i++) {
          ReferenceBinding intf = searchThisType.superInterfaces()[i];
          tryFindUpRefsRecursive(method, binding, intf);
        }
      }
    }
  }

  /**
   * Combines the information from the JDT type nodes and the type map to create
   * a JProgram structure.
   */
  public static void exec(TypeDeclaration[] types, TypeMap typeMap,
      JProgram jprogram) {
    JavaASTGenerationVisitor v = new JavaASTGenerationVisitor(typeMap);
    for (int i = 0; i < types.length; ++i) {
      v.processType(types[i]);
    }
    Collections.sort(jprogram.getDeclaredTypes(), new HasNameSort());
  }

  public static void reportJsniError(JSourceInfo info,
      AbstractMethodDeclaration methodDeclaration, String message) {
    CompilationResult compResult = methodDeclaration.compilationResult();
    DefaultProblem problem = new DefaultProblem(
        info.getFileName().toCharArray(), message, IProblem.Unclassified, null,
        ProblemSeverities.Error, info.getStartPos(), info.getEndPos(),
        info.getStartLine());
    compResult.record(problem, methodDeclaration);
  }

  public static JSourceInfo translateInfo(JsSourceInfo info) {
    // TODO Auto-generated method stub
    return null;
  }

}
