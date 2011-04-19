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

import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Each SourceInfo may define one or more axes by which it can be correlated
 * with other SourceInfo objects. Correlation has set and map-key semantics.
 */
public final class Correlation implements Serializable {
  /*
   * NB: The Correlation type uses AST nodes in its factory methods to make it
   * easier to extract whatever information we want to include in the Compile
   * Reports without having to update call sites with additional parameters.
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
    CLASS,

    /**
     * A field defined within a Java type.
     */
    FIELD,

    /**
     * Indicates a literal value in the original source.
     */
    LITERAL,

    /**
     * A Java method.
     */
    METHOD;
  }

  /**
   * Specifies the type of literal value.
   */
  public enum Literal {
    CLASS("class"), STRING("string");

    private final String description;

    private Literal(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * Compares Correlations based on axis and idents. Note that due to inherent
   * limitations of mapping AST nodes into Strings, this Comparator may not
   * always agree with {@link Correlation#equals(Object)}.
   */
  public static final Comparator<Correlation> AXIS_IDENT_COMPARATOR =
      new Comparator<Correlation>() {
        public int compare(Correlation a, Correlation b) {
          int r = a.axis.compareTo(b.axis);
          if (r != 0) {
            return r;
          }

          return a.ident.compareTo(b.ident);
        }
      };

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

  Correlation(Axis axis, String ident, Serializable astReference) {
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

    boolean astSame =
        astReference == c.astReference
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

  public JDeclaredType getType() {
    if (axis == Axis.CLASS) {
      return (JDeclaredType) astReference;
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