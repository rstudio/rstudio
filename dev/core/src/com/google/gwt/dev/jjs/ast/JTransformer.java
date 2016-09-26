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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.js.JDebuggerStatement;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniClassLiteral;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.jjs.ast.js.JsonArray;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.List;

/**
 * A base class for transformers from JNode.
 *
 * @param <T> the type of the transformed node.
 */
@SuppressWarnings("unused")
public class JTransformer<T> {

  public T transformAbstractMethodBody(JAbstractMethodBody x) {
    return missing(x);
  }

  public T transformArrayLength(JArrayLength x) {
    return transformExpression(x);
  }

  public T transformArrayRef(JArrayRef x) {
    return transformExpression(x);
  }

  public T transformArrayType(JArrayType x) {
    return transformReferenceType(x);
  }

  public T transformAssertStatement(JAssertStatement x) {
    return transformStatement(x);
  }

  public T transformBinaryOperation(JBinaryOperation x) {
    return transformExpression(x);
  }

  public T transformBlock(JBlock x) {
    return transformStatement(x);
  }

  public T transformBooleanLiteral(JBooleanLiteral x) {
    return transformValueLiteral(x);
  }

  public T transformBreakStatement(JBreakStatement x) {
    return transformStatement(x);
  }

  public T transformCaseStatement(JCaseStatement x) {
    return transformStatement(x);
  }

  public T transformCastMap(JCastMap x) {
    return transformExpression(x);
  }

  public T transformCastOperation(JCastOperation x) {
    return transformExpression(x);
  }

  public T transformCharLiteral(JCharLiteral x) {
    return transformValueLiteral(x);
  }

  public T transformClassLiteral(JClassLiteral x) {
    return transformLiteral(x);
  }

  public T transformClassType(JClassType x) {
    return transformDeclaredType(x);
  }

  public T transformConditional(JConditional x) {
    return transformExpression(x);
  }

  public T transformConstructor(JConstructor x) {
    return transformMethod(x);
  }

  public T transformContinueStatement(JContinueStatement x) {
    return transformStatement(x);
  }

  public T transformDebuggerStatement(JDebuggerStatement x) {
    return transformStatement(x);
  }

  public T transformDeclarationStatement(JDeclarationStatement x) {
    return transformStatement(x);
  }

  public T transformDeclaredType(JDeclaredType x) {
    return transformReferenceType(x);
  }

  public T transformDoStatement(JDoStatement x) {
    return transformStatement(x);
  }

  public T transformDoubleLiteral(JDoubleLiteral x) {
    return transformValueLiteral(x);
  }

  public T transformExpression(JExpression x) {
    return missing(x);
  }

  public T transformExpressionStatement(JExpressionStatement x) {
    return transformStatement(x);
  }

  public T transformField(JField x) {
    return transformVariable(x);
  }

  public T transformFieldRef(JFieldRef x) {
    return transformVariableRef(x);
  }

  public T transformFloatLiteral(JFloatLiteral x) {
    return transformValueLiteral(x);
  }

  public T transformForStatement(JForStatement x) {
    return transformStatement(x);
  }

  public T transformIfStatement(JIfStatement x) {
    return transformStatement(x);
  }

  public T transformInstanceOf(JInstanceOf x) {
    return transformExpression(x);
  }

  public T transformInterfaceType(JInterfaceType x) {
    return transformDeclaredType(x);
  }

  public T transformIntLiteral(JIntLiteral x) {
    return transformValueLiteral(x);
  }

  public T transformLabel(JLabel x) {
    return missing(x);
  }

  public T transformLabeledStatement(JLabeledStatement x) {
    return transformStatement(x);
  }

  public T transformLiteral(JLiteral x) {
    return transformExpression(x);
  }

  public T transformLocal(JLocal x) {
    return transformVariable(x);
  }

  public T transformLocalRef(JLocalRef x) {
    return transformVariableRef(x);
  }

  public T transformLongLiteral(JLongLiteral x) {
    return transformValueLiteral(x);
  }

  public T transformMethod(JMethod x) {
    return missing(x);
  }

  public T transformMethodBody(JMethodBody x) {
    return transformAbstractMethodBody(x);
  }

  public T transformMethodCall(JMethodCall x) {
    return transformExpression(x);
  }

  public T transformMultiExpression(JMultiExpression x) {
    return transformExpression(x);
  }

  public T transformNameOf(JNameOf x) {
    return transformExpression(x);
  }

  public T transformNewArray(JNewArray x) {
    return transformExpression(x);
  }

  public T transformNewInstance(JNewInstance x) {
    return transformMethodCall(x);
  }

  public T missing(JNode x) {
    throw new InternalCompilerException(
        "Missing transform for " + x + " (" + x.getClass().getSimpleName() + ")");
  }

  public T transformNullLiteral(JNullLiteral x) {
    return transformValueLiteral(x);
  }

  public T transformNumericEntry(JNumericEntry x) {
    return transformExpression(x);
  }

  public T transformParameter(JParameter x) {
    return transformVariable(x);
  }

  public T transformParameterRef(JParameterRef x) {
    return transformVariableRef(x);
  }

  public T transformPermutationDependentValue(JPermutationDependentValue x) {
    return transformExpression(x);
  }

  public T transformPostfixOperation(JPostfixOperation x) {
    return transformUnaryOperation(x);
  }

  public T transformPrefixOperation(JPrefixOperation x) {
    return transformUnaryOperation(x);
  }

  public T transformPrimitiveType(JPrimitiveType x) {
    return transformType(x);
  }

  public T transformProgram(JProgram x) {
    return missing(x);
  }

  public T transformReferenceType(JReferenceType x) {
    return transformType(x);
  }

  public T transformReturnStatement(JReturnStatement x) {
    return transformStatement(x);
  }

  public T transformRunAsync(JRunAsync x) {
    return transformExpression(x);
  }

  public T transformRuntimeTypeReference(JRuntimeTypeReference x) {
    return transformExpression(x);
  }

  public T transformJsniClassLiteral(JsniClassLiteral x) {
    return transformClassLiteral(x);
  }

  public T transformJsniFieldRef(JsniFieldRef x) {
    return transformFieldRef(x);
  }

  public T transformJsniMethodBody(JsniMethodBody x) {
    return transformAbstractMethodBody(x);
  }

  public T transformJsniMethodRef(JsniMethodRef x) {
    return transformMethodCall(x);
  }

  public T transformJsonArray(JsonArray x) {
    return transformExpression(x);
  }

  public T transformStatement(JStatement x) {
    return missing(x);
  }

  public T transformStringLiteral(JStringLiteral x) {
    return transformValueLiteral(x);
  }

  public T transformSwitchStatement(JSwitchStatement x) {
    return transformStatement(x);
  }

  public T transformThisRef(JThisRef x) {
    return transformExpression(x);
  }

  public T transformThrowStatement(JThrowStatement x) {
    return transformStatement(x);
  }

  public T transformTryStatement(JTryStatement x) {
    return transformStatement(x);
  }

  public T transformType(JType x) {
    return missing(x);
  }

  public T transformUnaryOperation(JUnaryOperation x) {
    return transformExpression(x);
  }

  public T transformValueLiteral(JValueLiteral x) {
    return transformLiteral(x);
  }

  public T transformVariable(JVariable x) {
    return missing(x);
  }

  public T transformVariableRef(JVariableRef x) {
    return transformExpression(x);
  }

  public T transformUnsafeTypeCoercion(JUnsafeTypeCoercion x) {
    return transformExpression(x);
  }

  public T transformWhileStatement(JWhileStatement x) {
    return transformStatement(x);
  }

  private class JRewriterVisitor extends JVisitor {
    T result = null;

    public boolean didChange() {
      throw new UnsupportedOperationException();
    }

    public final void endVisit(JAbstractMethodBody x, Context ctx) {
    }

    public final void endVisit(JArrayLength x, Context ctx) {
    }

    public final void endVisit(JArrayRef x, Context ctx) {
    }

    public final void endVisit(JArrayType x, Context ctx) {
    }

    public final void endVisit(JAssertStatement x, Context ctx) {
    }

    public final void endVisit(JBinaryOperation x, Context ctx) {
    }

    public final void endVisit(JBlock x, Context ctx) {
    }

    public final void endVisit(JBooleanLiteral x, Context ctx) {
    }

    public final void endVisit(JBreakStatement x, Context ctx) {
    }

    public final void endVisit(JCaseStatement x, Context ctx) {
    }

    public final void endVisit(JCastMap x, Context ctx) {
    }

    public final void endVisit(JCastOperation x, Context ctx) {
    }

    public final void endVisit(JCharLiteral x, Context ctx) {
    }

    public final void endVisit(JClassLiteral x, Context ctx) {
    }

    public final void endVisit(JClassType x, Context ctx) {
    }

    public final void endVisit(JConditional x, Context ctx) {
    }

    public final void endVisit(JConstructor x, Context ctx) {
    }

    public final void endVisit(JContinueStatement x, Context ctx) {
    }

    public final void endVisit(JDebuggerStatement x, Context ctx) {
    }

    public final void endVisit(JDeclarationStatement x, Context ctx) {
    }

    public final void endVisit(JDeclaredType x, Context ctx) {
    }

    public final void endVisit(JDoStatement x, Context ctx) {
    }

    public final void endVisit(JDoubleLiteral x, Context ctx) {
    }

    public final void endVisit(JExpression x, Context ctx) {
    }

    public final void endVisit(JExpressionStatement x, Context ctx) {
    }

    public final void endVisit(JField x, Context ctx) {
    }

    public final void endVisit(JFieldRef x, Context ctx) {
    }

    public final void endVisit(JFloatLiteral x, Context ctx) {
    }

    public final void endVisit(JForStatement x, Context ctx) {
    }

    public final void endVisit(JIfStatement x, Context ctx) {
    }

    public final void endVisit(JInstanceOf x, Context ctx) {
    }

    public final void endVisit(JInterfaceType x, Context ctx) {
    }

    public final void endVisit(JIntLiteral x, Context ctx) {
    }

    public final void endVisit(JLabel x, Context ctx) {
    }

    public final void endVisit(JLabeledStatement x, Context ctx) {
    }

    public final void endVisit(JLiteral x, Context ctx) {
    }

    public final void endVisit(JLocal x, Context ctx) {
    }

    public final void endVisit(JLocalRef x, Context ctx) {
    }

    public final void endVisit(JLongLiteral x, Context ctx) {
    }

    public final void endVisit(JMethod x, Context ctx) {
    }

    public final void endVisit(JMethodBody x, Context ctx) {
    }

    public final void endVisit(JMethodCall x, Context ctx) {
    }

    public final void endVisit(JMultiExpression x, Context ctx) {
    }

    public final void endVisit(JNameOf x, Context ctx) {
    }

    public final void endVisit(JNewArray x, Context ctx) {
    }

    public final void endVisit(JNewInstance x, Context ctx) {
    }

    public final void endVisit(JNode x, Context ctx) {
    }

    public final void endVisit(JNullLiteral x, Context ctx) {
    }

    public final void endVisit(JNumericEntry x, Context ctx) {
    }

    public final void endVisit(JParameter x, Context ctx) {
    }

    public final void endVisit(JParameterRef x, Context ctx) {
    }

    public final void endVisit(JPermutationDependentValue x, Context ctx) {
    }

    public final void endVisit(JPostfixOperation x, Context ctx) {
    }

    public final void endVisit(JPrefixOperation x, Context ctx) {
    }

    public final void endVisit(JPrimitiveType x, Context ctx) {
    }

    public final void endVisit(JProgram x, Context ctx) {
    }

    public final void endVisit(JReferenceType x, Context ctx) {
    }

    public final void endVisit(JReturnStatement x, Context ctx) {
    }

    public final void endVisit(JRunAsync x, Context ctx) {
    }

    public final void endVisit(JRuntimeTypeReference x, Context ctx) {
    }

    public final void endVisit(JsniClassLiteral x, Context ctx) {
    }

    public final void endVisit(JsniFieldRef x, Context ctx) {
    }

    public final void endVisit(JsniMethodBody x, Context ctx) {
    }

    public final void endVisit(JsniMethodRef x, Context ctx) {
    }

    public final void endVisit(JsonArray x, Context ctx) {
    }

    public final void endVisit(JStatement x, Context ctx) {
    }

    public final void endVisit(JStringLiteral x, Context ctx) {
    }

    public final void endVisit(JSwitchStatement x, Context ctx) {
    }

    public final void endVisit(JThisRef x, Context ctx) {
    }

    public final void endVisit(JThrowStatement x, Context ctx) {
    }

    public final void endVisit(JTryStatement x, Context ctx) {
    }

    public final void endVisit(JType x, Context ctx) {
    }

    public final void endVisit(JUnaryOperation x, Context ctx) {
    }

    public final void endVisit(JValueLiteral x, Context ctx) {
    }

    public final void endVisit(JVariable x, Context ctx) {
    }

    public final void endVisit(JVariableRef x, Context ctx) {
    }

    public final void endVisit(JWhileStatement x, Context ctx) {
    }

    public final boolean visit(JAbstractMethodBody x, Context ctx) {
      assert result == null;
      result = transformAbstractMethodBody(x);
      return false;
    }

    public final boolean visit(JArrayLength x, Context ctx) {
      assert result == null;
      result = transformArrayLength(x);
      return false;
    }

    public final boolean visit(JArrayRef x, Context ctx) {
      assert result == null;
      result = transformArrayRef(x);
      return false;
    }

    public final boolean visit(JArrayType x, Context ctx) {
      assert result == null;
      result = transformArrayType(x);
      return false;
    }

    public final boolean visit(JAssertStatement x, Context ctx) {
      assert result == null;
      result = transformAssertStatement(x);
      return false;
    }

    public final boolean visit(JBinaryOperation x, Context ctx) {
      assert result == null;
      result = transformBinaryOperation(x);
      return false;
    }

    public final boolean visit(JBlock x, Context ctx) {
      assert result == null;
      result = transformBlock(x);
      return false;
    }

    public final boolean visit(JBooleanLiteral x, Context ctx) {
      assert result == null;
      result = transformBooleanLiteral(x);
      return false;
    }

    public final boolean visit(JBreakStatement x, Context ctx) {
      assert result == null;
      result = transformBreakStatement(x);
      return false;
    }

    public final boolean visit(JCaseStatement x, Context ctx) {
      assert result == null;
      result = transformCaseStatement(x);
      return false;
    }

    public final boolean visit(JCastMap x, Context ctx) {
      assert result == null;
      result = transformCastMap(x);
      return false;
    }

    public final boolean visit(JCastOperation x, Context ctx) {
      assert result == null;
      result = transformCastOperation(x);
      return false;
    }

    public final boolean visit(JCharLiteral x, Context ctx) {
      assert result == null;
      result = transformCharLiteral(x);
      return false;
    }

    public final boolean visit(JClassLiteral x, Context ctx) {
      assert result == null;
      result = transformClassLiteral(x);
      return false;
    }

    public final boolean visit(JClassType x, Context ctx) {
      assert result == null;
      result = transformClassType(x);
      return false;
    }

    public final boolean visit(JConditional x, Context ctx) {
      assert result == null;
      result = transformConditional(x);
      return false;
    }

    public final boolean visit(JConstructor x, Context ctx) {
      assert result == null;
      result = transformConstructor(x);
      return false;
    }

    public final boolean visit(JContinueStatement x, Context ctx) {
      assert result == null;
      result = transformContinueStatement(x);
      return false;
    }

    public final boolean visit(JDebuggerStatement x, Context ctx) {
      assert result == null;
      result = transformDebuggerStatement(x);
      return false;
    }

    public final boolean visit(JDeclarationStatement x, Context ctx) {
      assert result == null;
      result = transformDeclarationStatement(x);
      return false;
    }

    public final boolean visit(JDeclaredType x, Context ctx) {
      assert result == null;
      result = transformDeclaredType(x);
      return false;
    }

    public final boolean visit(JDoStatement x, Context ctx) {
      assert result == null;
      result = transformDoStatement(x);
      return false;
    }

    public final boolean visit(JDoubleLiteral x, Context ctx) {
      assert result == null;
      result = transformDoubleLiteral(x);
      return false;
    }

    public final boolean visit(JExpression x, Context ctx) {
      assert result == null;
      result = transformExpression(x);
      return false;
    }

    public final boolean visit(JExpressionStatement x, Context ctx) {
      assert result == null;
      result = transformExpressionStatement(x);
      return false;
    }

    public final boolean visit(JField x, Context ctx) {
      assert result == null;
      result = transformField(x);
      return false;
    }

    /**
     * NOTE: not called from JsniFieldRef.
     */
    public final boolean visit(JFieldRef x, Context ctx) {
      assert result == null;
      result = transformFieldRef(x);
      return false;
    }

    public final boolean visit(JFloatLiteral x, Context ctx) {
      assert result == null;
      result = transformFloatLiteral(x);
      return false;
    }

    public final boolean visit(JForStatement x, Context ctx) {
      assert result == null;
      result = transformForStatement(x);
      return false;
    }

    public final boolean visit(JIfStatement x, Context ctx) {
      assert result == null;
      result = transformIfStatement(x);
      return false;
    }

    public final boolean visit(JInstanceOf x, Context ctx) {
      assert result == null;
      result = transformInstanceOf(x);
      return false;
    }

    public final boolean visit(JInterfaceType x, Context ctx) {
      assert result == null;
      result = transformInterfaceType(x);
      return false;
    }

    public final boolean visit(JIntLiteral x, Context ctx) {
      assert result == null;
      result = transformIntLiteral(x);
      return false;
    }

    public final boolean visit(JLabel x, Context ctx) {
      assert result == null;
      result = transformLabel(x);
      return false;
    }

    public final boolean visit(JLabeledStatement x, Context ctx) {
      assert result == null;
      result = transformLabeledStatement(x);
      return false;
    }

    public final boolean visit(JLiteral x, Context ctx) {
      assert result == null;
      result = transformLiteral(x);
      return false;
    }

    public final boolean visit(JLocal x, Context ctx) {
      assert result == null;
      result = transformLocal(x);
      return false;
    }

    public final boolean visit(JLocalRef x, Context ctx) {
      assert result == null;
      result = transformLocalRef(x);
      return false;
    }

    public final boolean visit(JLongLiteral x, Context ctx) {
      assert result == null;
      result = transformLongLiteral(x);
      return false;
    }

    public final boolean visit(JMethod x, Context ctx) {
      assert result == null;
      result = transformMethod(x);
      return false;
    }

    public final boolean visit(JMethodBody x, Context ctx) {
      assert result == null;
      result = transformMethodBody(x);
      return false;
    }

    public final boolean visit(JMethodCall x, Context ctx) {
      assert result == null;
      result = transformMethodCall(x);
      return false;
    }

    public final boolean visit(JMultiExpression x, Context ctx) {
      assert result == null;
      result = transformMultiExpression(x);
      return false;
    }

    public final boolean visit(JNameOf x, Context ctx) {
      assert result == null;
      result = transformNameOf(x);
      return false;
    }

    public final boolean visit(JNewArray x, Context ctx) {
      assert result == null;
      result = transformNewArray(x);
      return false;
    }

    public final boolean visit(JNewInstance x, Context ctx) {
      assert result == null;
      result = transformNewInstance(x);
      return false;
    }

    public final boolean visit(JNode x, Context ctx) {
      missing(x);
      return false;
    }

    public final boolean visit(JNullLiteral x, Context ctx) {
      assert result == null;
      result = transformNullLiteral(x);
      return false;
    }

    public final boolean visit(JNumericEntry x, Context ctx) {
      assert result == null;
      result = transformNumericEntry(x);
      return false;
    }

    public final boolean visit(JParameter x, Context ctx) {
      assert result == null;
      result = transformParameter(x);
      return false;
    }

    public final boolean visit(JParameterRef x, Context ctx) {
      assert result == null;
      result = transformParameterRef(x);
      return false;
    }

    public final boolean visit(JPermutationDependentValue x, Context ctx) {
      assert result == null;
      result = transformPermutationDependentValue(x);
      return false;
    }

    public final boolean visit(JPostfixOperation x, Context ctx) {
      assert result == null;
      result = transformPostfixOperation(x);
      return false;
    }

    public final boolean visit(JPrefixOperation x, Context ctx) {
      assert result == null;
      result = transformPrefixOperation(x);
      return false;
    }

    public final boolean visit(JPrimitiveType x, Context ctx) {
      assert result == null;
      result = transformPrimitiveType(x);
      return false;
    }

    public final boolean visit(JProgram x, Context ctx) {
      assert result == null;
      result = transformProgram(x);
      return false;
    }

    public final boolean visit(JReferenceType x, Context ctx) {
      assert result == null;
      result = transformReferenceType(x);
      return false;
    }

    public final boolean visit(JReturnStatement x, Context ctx) {
      assert result == null;
      result = transformReturnStatement(x);
      return false;
    }

    public final boolean visit(JRunAsync x, Context ctx) {
      assert result == null;
      result = transformRunAsync(x);
      return false;
    }

    public final boolean visit(JRuntimeTypeReference x, Context ctx) {
      assert result == null;
      result = transformRuntimeTypeReference(x);
      return false;
    }

    public final boolean visit(JsniClassLiteral x, Context ctx) {
      assert result == null;
      result = transformJsniClassLiteral(x);
      return false;
    }

    public final boolean visit(JsniFieldRef x, Context ctx) {
      /* NOTE: Skip JFieldRef */
      assert result == null;
      result = transformJsniFieldRef(x);
      return false;
    }

    public final boolean visit(JsniMethodBody x, Context ctx) {
      assert result == null;
      result = transformJsniMethodBody(x);
      return false;
    }

    public final boolean visit(JsniMethodRef x, Context ctx) {
      /* NOTE: Skip JMethodCall */
      assert result == null;
      result = transformJsniMethodRef(x);
      return false;
    }

    public final boolean visit(JsonArray x, Context ctx) {
      assert result == null;
      result = transformJsonArray(x);
      return false;
    }

    public final boolean visit(JStatement x, Context ctx) {
      assert result == null;
      result = transformStatement(x);
      return false;
    }

    public final boolean visit(JStringLiteral x, Context ctx) {
      assert result == null;
      result = transformStringLiteral(x);
      return false;
    }

    public final boolean visit(JSwitchStatement x, Context ctx) {
      assert result == null;
      result = transformSwitchStatement(x);
      return false;
    }

    public final boolean visit(JThisRef x, Context ctx) {
      assert result == null;
      result = transformThisRef(x);
      return false;
    }

    public final boolean visit(JThrowStatement x, Context ctx) {
      assert result == null;
      result = transformThrowStatement(x);
      return false;
    }

    public final boolean visit(JTryStatement x, Context ctx) {
      assert result == null;
      result = transformTryStatement(x);
      return false;
    }

    public final boolean visit(JType x, Context ctx) {
      assert result == null;
      result = transformType(x);
      return false;
    }

    public final boolean visit(JUnaryOperation x, Context ctx) {
      assert result == null;
      result = transformUnaryOperation(x);
      return false;
    }

    public final boolean visit(JUnsafeTypeCoercion x, Context ctx) {
      assert result == null;
      result = transformUnsafeTypeCoercion(x);
      return false;
    }

    public final boolean visit(JValueLiteral x, Context ctx) {
      assert result == null;
      result = transformValueLiteral(x);
      return false;
    }

    public final boolean visit(JVariable x, Context ctx) {
      assert result == null;
      result = transformVariable(x);
      return false;
    }

    public final boolean visit(JVariableRef x, Context ctx) {
      assert result == null;
      result = transformVariableRef(x);
      return false;
    }

    public final boolean visit(JWhileStatement x, Context ctx) {
      assert result == null;
      result = transformWhileStatement(x);
      return false;
    }
  }

  public final <T> T transform(JNode node) {
    if (node == null) {
      return null;
    }
    JRewriterVisitor visitor = new JRewriterVisitor();
    visitor.accept(node);
    return (T) visitor.result;
  }

  public final <T,U extends JNode> List<T> transform(List<U> nodes) {
    List<T> result = Lists.newArrayListWithCapacity(nodes.size());
    transformInto(nodes, result);
    return result;
  }

  public final <T,U extends JNode> void transformInto(List<U> nodes,
      List<T> transformedNodes) {
    for (JNode node: nodes) {
      transformedNodes.add((T) transform(node));
    }
  }

  public final <T,U extends JNode> void transformIntoExcludingNulls(List<U> nodes,
      List<T> transformedNodes) {
    for (JNode node: nodes) {
      T transformed = transform(node);
      if (transformed != null) {
        transformedNodes.add(transformed);
      }
    }
  }
}
