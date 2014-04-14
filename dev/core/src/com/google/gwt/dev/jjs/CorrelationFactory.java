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
package com.google.gwt.dev.jjs;

import com.google.gwt.dev.jjs.Correlation.Axis;
import com.google.gwt.dev.jjs.Correlation.Literal;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.thirdparty.guava.common.collect.MapMaker;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

/**
 * A {@link Correlation} factory.
 */
public abstract class CorrelationFactory implements Serializable {

  /**
   * A dummy factory that always returns <code>null</code>.
   */
  public static final class DummyCorrelationFactory extends CorrelationFactory {
    public static final CorrelationFactory INSTANCE = new DummyCorrelationFactory();

    private DummyCorrelationFactory() {
    }

    @Override
    public Correlation by(JDeclaredType type) {
      return null;
    }

    @Override
    public Correlation by(JField field) {
      return null;
    }

    @Override
    public Correlation by(JMethod method) {
      return null;
    }

    @Override
    public Correlation by(Literal type) {
      return null;
    }

    @Override
    public SourceInfo makeSourceInfo(SourceOrigin origin) {
      return origin;
    }
  }

  /**
   * A real factory that returns new {@link Correlation Correlations}.
   */
  public static final class RealCorrelationFactory extends CorrelationFactory {
    /*
     * NB: The Correlation type uses AST nodes in its factory methods to make it
     * easier to extract whatever information we want to include in the Compile
     * Reports without having to update call sites with additional parameters.
     *
     * In the general case, references to AST nodes should not be exposed to any
     * public-API consumers of the Correlation.
     */

    public static final CorrelationFactory INSTANCE = new RealCorrelationFactory();

    /**
     * Correlations based on Literals are all the same, so we'll just cook up a
     * Map to make {@link #by(Literal)} fast.
     */
    private static final Map<Literal, Correlation> LITERAL_CORRELATIONS =
        new EnumMap<Literal, Correlation>(Literal.class);

    static {
      for (Literal l : Literal.values()) {
        LITERAL_CORRELATIONS.put(l, new Correlation(Axis.LITERAL, l.getDescription(), l));
      }
    }

    public static String getMethodIdent(JMethod method) {
      StringBuilder sb = new StringBuilder();
      sb.append(method.getEnclosingType().getName()).append("::");
      sb.append(method.getName()).append("(");
      for (JType type : method.getOriginalParamTypes()) {
        sb.append(type.getJsniSignatureName());
      }
      sb.append(")");
      sb.append(method.getOriginalReturnType().getJsniSignatureName());
      return sb.toString();
    }

    /**
     * This cuts down on the total number of Correlation objects allocated.
     */
    private final Map<Object, Correlation> canonicalMap = new MapMaker().weakKeys().weakValues().makeMap();

    private RealCorrelationFactory() {
    }

    @Override
    public Correlation by(JDeclaredType type) {
      Correlation toReturn = canonicalMap.get(type);
      if (toReturn == null) {
        toReturn = new Correlation(Axis.CLASS, type.getName(), type);
        canonicalMap.put(type, toReturn);
      }
      return toReturn;
    }

    @Override
    public Correlation by(JField field) {
      Correlation toReturn = canonicalMap.get(field);
      if (toReturn == null) {
        toReturn =
            new Correlation(Axis.FIELD,
                field.getEnclosingType().getName() + "::" + field.getName(), field);
        canonicalMap.put(field, toReturn);
      }
      return toReturn;
    }

    @Override
    public Correlation by(JMethod method) {
      Correlation toReturn = canonicalMap.get(method);
      if (toReturn == null) {

        toReturn = new Correlation(Axis.METHOD, getMethodIdent(method), method);
        canonicalMap.put(method, toReturn);
      }
      return toReturn;
    }

    @Override
    public Correlation by(Literal type) {
      assert LITERAL_CORRELATIONS.containsKey(type);
      return LITERAL_CORRELATIONS.get(type);
    }

    @Override
    public SourceInfo makeSourceInfo(SourceOrigin origin) {
      return new SourceInfoCorrelation(origin);
    }
  }

  public abstract Correlation by(JDeclaredType type);

  public abstract Correlation by(JField field);

  public abstract Correlation by(JMethod method);

  public abstract Correlation by(Literal type);

  public abstract SourceInfo makeSourceInfo(SourceOrigin origin);
}