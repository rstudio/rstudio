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

import com.google.gwt.dev.PropertyAndBindingInfo;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.PermutationProperties;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.AccessModifier;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JCaseStatement;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JPermutationDependentValue;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JSwitchStatement;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Replaces any "GWT.create()" calls with a new expression for the actual result of the deferred
 * binding decision and System.getProperty() with the actual value for that property.
 * <p>
 * When properties are collapsed (e.g. in soft permutations) a method is create that resolves
 * the value at runtime.
 * </p>
 *
 */
public class ResolvePermutationDependentValues {

  private class ValueReplacer extends JModVisitor {
    @Override
    public void endVisit(JPermutationDependentValue x, Context ctx) {

      if (x.isTypeRebind()) {
        ctx.replaceMe(rebindClassExpression(x));
        return;
      }

      assert x.isProperty();
      ctx.replaceMe(propertyValueExpression(x));
    }

    private JExpression propertyValueExpression(JPermutationDependentValue x) {
      List<String> propertyValues = props.getConfigurationProperties().getStrings(x.getRequestedValue());

      String propertyValue = propertyValues.isEmpty() ? null : Joiner.on(",").join(propertyValues);

      if (propertyValue != null) {
        // It is a configuration property.
        return program.getLiteral(x.getSourceInfo(), propertyValue);
      }

      if (isSoftPermutationProperty(x.getRequestedValue())) {
        JMethod method = getOrCreateSoftPropertyMethod(x.getSourceInfo(), x.getRequestedValue());
        return new JMethodCall(x.getSourceInfo(), null, method);
      }

      propertyValue = commonPropertyAndBindingInfo.getPropertyValue(x.getRequestedValue());

      if (propertyValue != null) {
        return program.getLiteral(x.getSourceInfo(), propertyValue);
      }

      return x.getDefaultValueExpression();
    }

    private JExpression rebindClassExpression(JPermutationDependentValue x) {
      if (isSoftTypeRebind(x.getRequestedValue())) {
        JMethod method = getOrCreateTypeRebindMethod(x.getSourceInfo(), x.getRequestedValue(),
                x.getResultValues(), x.getResultExpressions());
        return new JMethodCall(x.getSourceInfo(), null, method);
      }

      return computeInstantiationExpression(x);
    }
  }

  public static boolean exec(JProgram program, PermutationProperties props,
      List<PropertyAndBindingInfo> propertyAndBindingInfo) {
    return new ResolvePermutationDependentValues(program, props, propertyAndBindingInfo).execImpl();
  }

  private final JProgram program;
  private final PermutationProperties props;
  private final Set<String> bindingPropertyNames;
  private final List<PropertyAndBindingInfo> permutationPropertyAndBindingInfo;
  private final PropertyAndBindingInfo commonPropertyAndBindingInfo;
  private final JClassType holderType;
  private final JMethod permutationIdMethod;
  private final Map<String, JMethod> softPermutationMethods = Maps.newHashMap();

  private ResolvePermutationDependentValues(JProgram program, PermutationProperties props,
      List<PropertyAndBindingInfo> gwtCreateAnswers) {
    this.program = program;
    this.permutationPropertyAndBindingInfo = gwtCreateAnswers;
    this.props = props;
    this.bindingPropertyNames = FluentIterable.from(props.getBindingProperties()).transform(
        new Function<BindingProperty, String>() {
          @Override
          public String apply(BindingProperty bindingProperty) {
            return bindingProperty.getName();
          }
        }).toSet();
    this.commonPropertyAndBindingInfo = PropertyAndBindingInfo.getCommonAnswers(gwtCreateAnswers);
    this.holderType = (JClassType) program.getIndexedType("CollapsedPropertyHolder");
    this.permutationIdMethod = program.getIndexedMethod(
        RuntimeConstants.COLLAPSED_PROPERTY_HOLDER_GET_PERMUTATION_ID);
  }

  public JExpression computeInstantiationExpression(JPermutationDependentValue x) {
    String reqType = x.getRequestedValue();
    // Rebinds are always on a source type name.
    String reboundClassName = commonPropertyAndBindingInfo.getReboundType(reqType);
    if (reboundClassName == null) {
      // The fact that we already compute every rebind permutation before
      // compiling should prevent this case from ever happening in real life.
      //
      throw new InternalCompilerException("Unexpected failure to rebind '" + reqType + "'");
    }
    assert program.getFromTypeMap(reboundClassName) != null;
    int index = x.getResultValues().indexOf(reboundClassName);
    if (index == -1) {
      throw new InternalCompilerException("No matching rebind result in all rebind results!");
    }
    return x.getResultExpressions().get(index);
  }

  private boolean execImpl() {
    ValueReplacer valueReplacer = new ValueReplacer();
    valueReplacer.accept(program);
    return valueReplacer.didChange();
  }

  private boolean isSoftTypeRebind(String requestType) {
    return !commonPropertyAndBindingInfo.containsType(requestType);
  }

  private boolean isSoftPermutationProperty(String propertyName) {
    return bindingPropertyNames.contains(propertyName) &&
        !commonPropertyAndBindingInfo.containsProperty(propertyName);
  }

  private JMethod getOrCreateSoftPropertyMethod(final SourceInfo info, String propertyName) {
    JMethod toReturn = softPermutationMethods.get(propertyName);
    if (toReturn != null) {
      return toReturn;
    }
    Multimap<String, Integer> resultsToPermutations = PropertyAndBindingInfo
        .getPermutationIdsByPropertyName(permutationPropertyAndBindingInfo, propertyName);

    List<String> propertyValues = Lists.newArrayList(resultsToPermutations.keySet());
    return createReboundValueSelectorMethod(info, "property_", propertyName,
        program.getTypeJavaLangString(), propertyValues,
        FluentIterable.from(propertyValues).transform(
            new Function<String, JExpression>() {
              @Override
              public JExpression apply(String s) {
                return program.getLiteral(info, s);
              }
            }).toList(), resultsToPermutations);
  }

  private JMethod getOrCreateTypeRebindMethod(SourceInfo info, String requestType,
      List<String> resultTypes, List<JExpression> instantiationExpressions) {
    assert resultTypes.size() == instantiationExpressions.size();

    JMethod toReturn = softPermutationMethods.get(requestType);
    if (toReturn != null) {
      return toReturn;
    }
    Multimap<String, Integer> resultsToPermutations = PropertyAndBindingInfo
        .getPermutationIdsByRequestTypes(permutationPropertyAndBindingInfo, requestType);

    return createReboundValueSelectorMethod(info, "create_", requestType,
        program.getTypeJavaLangObject().strengthenToNonNull(), resultTypes,
        instantiationExpressions, resultsToPermutations);
  }

  private JMethod createReboundValueSelectorMethod(SourceInfo info, String prefix,
      String parameterType, JReferenceType returntype, List<String> results,
      List<JExpression> resultExpressions, Multimap<String, Integer> resultsToPermutations) {

    // Pick the most-used result type to emit less code
    String mostUsed = mostUsedValue(resultsToPermutations);
    assert mostUsed != null;
    JMethod toReturn;
    info = info.makeChild(SourceOrigin.UNKNOWN);
    // c_g_g_d_c_i_DOMImpl
    toReturn =
        new JMethod(info, prefix + parameterType.replace("_", "_1").replace('.', '_'), holderType,
            returntype, false, true, true, AccessModifier.PUBLIC);
    toReturn.setBody(new JMethodBody(info));
    holderType.addMethod(toReturn);
    toReturn.freezeParamTypes();
    info.addCorrelation(info.getCorrelator().by(toReturn));
    softPermutationMethods.put(parameterType, toReturn);

    // Used in the return statement at the end
    JExpression mostUsedExpression = null;

    JBlock switchBody = new JBlock(info);
    for (int i = 0, j = results.size(); i < j; i++) {
      String resultType = results.get(i);
      JExpression expression = resultExpressions.get(i);

      Collection<Integer> permutations = resultsToPermutations.get(resultType);
      if (permutations.isEmpty()) {
        // This rebind result is unused in this permutation
        continue;
      } else if (resultType.equals(mostUsed)) {
        // Save off the fallback expression and go onto the next type
        mostUsedExpression = expression;
        continue;
      }

      for (int permutationId : permutations) {
        // case 33:
        switchBody.addStmt(new JCaseStatement(info, program.getLiteralInt(permutationId)));
      }

      // return new FooImpl();
      switchBody.addStmt(expression.makeReturnStatement());
    }

    assert switchBody.getStatements().size() > 0 : "No case statement emitted "
        + "for supposedly soft-rebind " + parameterType;

    // switch (CollapsedPropertyHolder.getPermutationId()) { ... }
    JSwitchStatement sw =
        new JSwitchStatement(info, new JMethodCall(info, null, permutationIdMethod), switchBody);

    // return new FallbackImpl(); at the very end.
    assert mostUsedExpression != null : "No most-used expression";
    JReturnStatement fallbackReturn = mostUsedExpression.makeReturnStatement();

    JMethodBody body = (JMethodBody) toReturn.getBody();
    body.getBlock().addStmt(sw);
    body.getBlock().addStmt(fallbackReturn);

    return toReturn;
  }

  private String mostUsedValue(Multimap<String, Integer> resultsToPermutations) {
    String mostUsed = null;
    int max = 0;
    for (String key : resultsToPermutations.keySet()) {

      int size = resultsToPermutations.get(key).size();
      if (size > max) {
        max = size;
        mostUsed = key;
      }
    }
    return mostUsed;
  }
}
