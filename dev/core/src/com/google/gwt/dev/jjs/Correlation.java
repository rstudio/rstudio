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

import java.io.Serializable;
import java.util.Comparator;

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
     * TODO(bobv): Consider whether or not this should be a proper class
     * hierarchy. The nice thing about an enum is that all possible types are
     * programmatically enumerable.
     * 
     * Also, consider MODULE and PACKAGE values.
     */

    /**
     * Represents a physical source file.
     */
    FILE(true, true),

    /**
     * A Java class or interface type.
     */
    CLASS(true, false),

    /**
     * A Java method.
     */
    METHOD(true, false),

    /**
     * A field defined within a Java type.
     */
    FIELD(true, false),

    /**
     * A JavaScript function derived from a class or method.
     */
    FUNCTION(false, true);

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

  public static Correlation by(JField field) {
    return new Correlation(Axis.FIELD, field.getEnclosingType().getName()
        + "::" + field.getName(), field);
  }

  public static Correlation by(JMethod method) {
    StringBuilder sb = new StringBuilder();
    sb.append(method.getEnclosingType().getName()).append("::");
    sb.append(method.getName()).append("(");
    for (JType type : method.getOriginalParamTypes()) {
      sb.append(type.getJsniSignatureName());
    }
    sb.append(")");

    return new Correlation(Axis.METHOD, sb.toString(), method);
  }

  public static Correlation by(JReferenceType type) {
    return new Correlation(Axis.CLASS, type.getName(), type);
  }

  public static Correlation by(JsFunction function) {
    return new Correlation(Axis.FUNCTION, function.getName().getIdent(),
        function);
  }

  /**
   * Constructs a {@link Axis#FILE} Correlation.
   */
  public static Correlation by(String filename) {
    return new Correlation(Axis.FILE, filename, filename);
  }

  /**
   * This may contain a reference to either a Java or Js AST node.
   */
  protected final Object astReference;

  protected final Axis axis;

  /**
   * This should be a uniquely-identifying value within the Correlation's axis
   * that is suitable for human consumption. It may be the case that two
   * Correlations have different AST references, but the same calculated ident,
   * so this should not be relied upon for uniqueness.
   */
  protected final String ident;

  private Correlation(Axis axis, String ident, Object astReference) {
    if (ident == null) {
      throw new NullPointerException("ident");
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
    return axis.equals(c.axis) && astSame;
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

  public JMethod getMethod() {
    if (axis == Axis.METHOD) {
      return (JMethod) astReference;
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
    return 37 * axis.hashCode() + astReference.hashCode() + 13;
  }

  /**
   * Defined for debugging convenience.
   */
  @Override
  public String toString() {
    return axis.toString() + ": " + ident;
  }
}