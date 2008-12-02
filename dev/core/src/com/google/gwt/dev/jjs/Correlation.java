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
package com.google.gwt.dev.jjs;

import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsName;

import org.apache.commons.collections.map.ReferenceMap;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

/**
 * Each SourceInfo may define one or more axes by which it can be correlated
 * with other SourceInfo objects. Correlation has set and map-key semantics.
 */
public final class Correlation implements Serializable {
  /*
   * NB: The Correlation type uses AST nodes in its factory methods to make it
   * easier to extract whatever information we want to include in the SOYC
   * reports without having to update call sites with additional parameters.
   * 
   * In the general case, references to AST nodes should not be exposed to any
   * public-API consumers of the Correlation.
   */

  /**
   * The axes on which we'll want to pivot the SourceInfo data-set.
   */
  public enum Axis {
    /*
     * Note to implementors: Possibly the switch statement in
     * StandardCompilationArtifact if additional member-type enum values are
     * added.
     */

    /**
     * A Java class or interface type.
     */
    CLASS(true, false),

    /**
     * A field defined within a Java type.
     */
    FIELD(true, false),

    /**
     * A JavaScript function derived from a class or method.
     */
    FUNCTION(false, true),

    /**
     * Objects with global names may be aliased (e.g. polymorphic method
     * dispatch).
     */
    JS_ALIAS(false, true),

    /**
     * The globally-unique identifier used to represent the Member in the
     * compiled output.
     */
    JS_NAME(false, true),

    /**
     * Indicates a literal value in the original source.
     */
    LITERAL(true, true),

    /**
     * A Java method.
     */
    METHOD(true, false),

    /**
     * Represents a physical source file.
     */
    ORIGIN(true, true);

    private final boolean isJava;
    private final boolean isJs;

    /**
     * Arguments indicate which AST the axis is relevant to.
     */
    private Axis(boolean isJava, boolean isJs) {
      this.isJava = isJava;
      this.isJs = isJs;
    }

    public boolean isJava() {
      return isJava;
    }

    public boolean isJs() {
      return isJs;
    }
  }

  /**
   * Specifies the type of literal value.
   */
  public enum Literal {
    VOID("void"), NULL("null"), BYTE("byte"), SHORT("short"), INT("int"), LONG(
        "long"), FLOAT("float"), DOUBLE("double"), BOOLEAN("boolean"), CHAR(
        "char"), STRING("string"), CLASS("class"), JS_BOOLEAN("boolean", true), JS_NUMBER(
        "number", true), JS_NULL("null", true), JS_STRING("string", true),
    /**
     * undefined isn't actually a literal in JS, but we more-or-less treat it as
     * though it were.
     */
    JS_UNDEFINED("undefined", true);

    private final String description;
    private final boolean isJava;
    private final boolean isJs;

    private Literal(String description) {
      this.description = description;
      isJava = true;
      isJs = false;
    }

    private Literal(String description, boolean isJs) {
      this.description = description;
      isJava = !isJs;
      this.isJs = isJs;
    }

    public String getDescription() {
      return description;
    }

    public boolean isJava() {
      return isJava;
    }

    public boolean isJs() {
      return isJs;
    }
  }

  /**
   * Compares Correlations based on axis and idents. Note that due to inherent
   * limitations of mapping AST nodes into Strings, this Comparator may not
   * always agree with {@link Correlation#equals(Object)}.
   */
  public static final Comparator<Correlation> AXIS_IDENT_COMPARATOR = new Comparator<Correlation>() {
    public int compare(Correlation a, Correlation b) {
      int r = a.axis.compareTo(b.axis);
      if (r != 0) {
        return r;
      }

      return a.ident.compareTo(b.ident);
    }
  };

  /**
   * This cuts down on the total number of Correlation objects allocated.
   */
  @SuppressWarnings("unchecked")
  private static final Map<Object, Correlation> CANONICAL_MAP = Collections.synchronizedMap(new ReferenceMap(
      ReferenceMap.WEAK, ReferenceMap.WEAK));

  /**
   * Correlations based on Literals are all the same, so we'll just cook up a
   * Map to make {@link #by(Literal)} fast.
   */
  private static final Map<Literal, Correlation> LITERAL_CORRELATIONS = new EnumMap<Literal, Correlation>(
      Literal.class);

  static {
    for (Literal l : Literal.values()) {
      LITERAL_CORRELATIONS.put(l, new Correlation(Axis.LITERAL,
          l.getDescription(), l));
    }
  }

  public static Correlation by(JField field) {
    Correlation toReturn = CANONICAL_MAP.get(field);
    if (toReturn == null) {
      toReturn = new Correlation(Axis.FIELD, field.getEnclosingType().getName()
          + "::" + field.getName(), field);
      CANONICAL_MAP.put(field, toReturn);
    }
    return toReturn;
  }

  public static Correlation by(JMethod method) {
    Correlation toReturn = CANONICAL_MAP.get(method);
    if (toReturn == null) {

      toReturn = new Correlation(Axis.METHOD, getMethodIdent(method), method);
      CANONICAL_MAP.put(method, toReturn);
    }
    return toReturn;
  }

  public static Correlation by(JReferenceType type) {
    Correlation toReturn = CANONICAL_MAP.get(type);
    if (toReturn == null) {
      toReturn = new Correlation(Axis.CLASS, type.getName(), type);
      CANONICAL_MAP.put(type, toReturn);
    }
    return toReturn;
  }

  public static Correlation by(JsFunction function) {
    Correlation toReturn = CANONICAL_MAP.get(function);
    if (toReturn == null) {
      toReturn = new Correlation(Axis.FUNCTION, function.getName().getIdent(),
          function);
      CANONICAL_MAP.put(function, toReturn);
    }
    return toReturn;
  }

  /**
   * Creates a JS_NAME Correlation.
   */
  public static Correlation by(JsName name) {
    return by(name, false);
  }

  /**
   * Creates either a JS_NAME or JS_ALIAS correlation, based on the value of
   * <code>isAlias</code>.
   */
  public static Correlation by(JsName name, boolean isAlias) {
    Correlation toReturn = CANONICAL_MAP.get(name);
    if (toReturn == null) {
      toReturn = new Correlation(isAlias ? Axis.JS_ALIAS : Axis.JS_NAME,
          name.getIdent(), name);
      CANONICAL_MAP.put(name, toReturn);
    }
    return toReturn;
  }

  public static Correlation by(Literal type) {
    assert LITERAL_CORRELATIONS.containsKey(type);
    return LITERAL_CORRELATIONS.get(type);
  }

  public static Correlation by(SourceOrigin origin) {
    Correlation toReturn = CANONICAL_MAP.get(origin);
    if (toReturn == null) {
      toReturn = new Correlation(Axis.ORIGIN, origin.getFileName() + ":"
          + origin.getStartLine(), origin);
      CANONICAL_MAP.put(origin, toReturn);
    }
    return toReturn;
  }

  private static String getMethodIdent(JMethod method) {
    StringBuilder sb = new StringBuilder();
    sb.append(method.getEnclosingType().getName()).append("::");
    sb.append(method.getName()).append("(");
    for (JType type : method.getOriginalParamTypes()) {
      sb.append(type.getJsniSignatureName());
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * This may contain a reference to either a Java or Js AST node.
   */
  protected final Serializable astReference;

  protected final Axis axis;

  /**
   * This should be a uniquely-identifying value within the Correlation's axis
   * that is suitable for human consumption. It may be the case that two
   * Correlations have different AST references, but the same calculated ident,
   * so this should not be relied upon for uniqueness.
   */
  protected final String ident;

  private Correlation(Axis axis, String ident, Serializable astReference) {
    if (axis == null) {
      throw new NullPointerException("axis");
    } else if (ident == null) {
      throw new NullPointerException("ident");
    } else if (astReference == null) {
      throw new NullPointerException("astReference");
    }

    this.axis = axis;
    this.ident = ident;
    this.astReference = astReference;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Correlation)) {
      return false;
    }
    Correlation c = (Correlation) obj;

    boolean astSame = astReference == c.astReference
        || (astReference != null && astReference.equals(c.astReference));
    return axis == c.axis && astSame;
  }

  public Axis getAxis() {
    return axis;
  }

  public JField getField() {
    if (axis == Axis.FIELD) {
      return (JField) astReference;
    } else {
      return null;
    }
  }

  public JsFunction getFunction() {
    if (axis == Axis.FUNCTION) {
      return (JsFunction) astReference;
    } else {
      return null;
    }
  }

  /**
   * Returns a human-readable identifier that can be used to identify the
   * Correlation within its axis.
   */
  public String getIdent() {
    return ident;
  }

  public Literal getLiteral() {
    if (axis == Axis.LITERAL) {
      return (Literal) astReference;
    } else {
      return null;
    }
  }

  public JMethod getMethod() {
    if (axis == Axis.METHOD) {
      return (JMethod) astReference;
    } else {
      return null;
    }
  }

  public JsName getName() {
    if (axis == Axis.JS_NAME || axis == Axis.JS_ALIAS) {
      return (JsName) astReference;
    } else {
      return null;
    }
  }

  public SourceOrigin getOrigin() {
    if (axis == Axis.ORIGIN) {
      return (SourceOrigin) astReference;
    } else {
      return null;
    }
  }

  public JReferenceType getType() {
    if (axis == Axis.CLASS) {
      return (JReferenceType) astReference;
    } else if (axis == Axis.METHOD) {
      return ((JMethod) astReference).getEnclosingType();
    } else if (axis == Axis.FIELD) {
      return ((JField) astReference).getEnclosingType();
    } else {
      return null;
    }
  }

  @Override
  public int hashCode() {
    /*
     * The null checks are because this method gets called during
     * deserialization, but without values having been set.
     */
    return 37 * (axis == null ? 1 : axis.hashCode())
        + (astReference == null ? 0 : astReference.hashCode()) + 13;
  }

  /**
   * Defined for debugging convenience.
   */
  @Override
  public String toString() {
    return axis.toString() + ": " + ident;
  }
}