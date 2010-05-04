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
package com.google.gwt.dev.jjs.impl.gflow.constants;

import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.JDoubleLiteral;
import com.google.gwt.dev.jjs.ast.JFloatLiteral;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.impl.gflow.Assumption;
import com.google.gwt.dev.util.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assumptions for ConstantsAnalysis.
 * 
 * Is a map from variable into it's constant value. If variable is not present
 * in the map, then it's not a constant. 
 * 
 * Empty ConstantsAssumption is a top of the lattice, and is not equals to 
 * null assumption (which is the bottom of every lattice).
 */
public class ConstantsAssumption implements Assumption<ConstantsAssumption> {
  /**
   * Updates the assumption by copying it on first write.
   */
  public static class Updater {
    private ConstantsAssumption assumption;
    private boolean copied = false;
    
    public Updater(ConstantsAssumption assumption) {
      this.assumption = assumption;
    }

    public Updater copy() {
      return new Updater(assumption);
    }

    public boolean hasAssumption(JVariable target) {
      if (assumption == null) {
        return false;
      }
      return assumption.hasAssumption(target);
    }
    
    public void set(JVariable target, JValueLiteral literal) {
      copyIfNeeded();
      assumption.set(target, literal);
    }

    public ConstantsAssumption unwrap() {
      if (assumption != null && assumption.isEmpty()) {
        return TOP;
      }
      return assumption;
    }

    private void copyIfNeeded() {
      if (!copied) {
        assumption = new ConstantsAssumption(assumption);
        copied = true;
      }
    }
  }
  
  /**
   * A wrapper around JValueLiteral to give it equals() method.
   */
  private static class LiteralWrapper {
    private final JValueLiteral literal;
    
    LiteralWrapper(JValueLiteral literal) {
      Preconditions.checkNotNull(literal);
      this.literal = literal;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      
      if (obj == this) {
        return true;
      }
      
      LiteralWrapper other = (LiteralWrapper) obj;
      return equal(this.literal, other.literal);
    }

    @Override
    public int hashCode() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return literal.toString();
    }
  }

  /**
   * A TOP for the lattice. Means that all variables are not constant.
   */
  public static ConstantsAssumption TOP = new ConstantsAssumption();

  private static boolean equal(JValueLiteral literal1, JValueLiteral literal2) {
    if (literal1 == null || literal2 == null) {
      return literal1 == literal2;
    } 
    
    if (literal1 == literal2) {
      return true;
    }

    if (literal1.getClass() != literal2.getClass()) {
      // these are different literal types. 
      return false;
    }
    
    if (literal1 instanceof JFloatLiteral) {
      int bits1 = Float.floatToRawIntBits(
          ((JFloatLiteral) literal1).getValue());
      int bits2 = Float.floatToRawIntBits(
          ((JFloatLiteral) literal2).getValue());
      return bits1 == bits2;
    }
    
    if (literal1 instanceof JDoubleLiteral) {
      long bits1 = Double.doubleToRawLongBits(
          ((JDoubleLiteral) literal1).getValue());
      long bits2 = Double.doubleToRawLongBits(
          ((JDoubleLiteral) literal2).getValue());
      return bits1 == bits2;
    }

    Object valueObj1 = literal1.getValueObj();
    Object valueObj2 = literal2.getValueObj();
    if (valueObj1 == null || valueObj2 == null) {
      return valueObj1 == valueObj2;
    }
    
    return valueObj1.equals(valueObj2);
  }
  
  private static JValueLiteral join(JValueLiteral value1, JValueLiteral value2) {
    if (!equal(value1, value2)) {
      return null;
    }
    
    return value1;
  }
  
  private static JValueLiteral join(LiteralWrapper wrapper1, 
      LiteralWrapper wrapper2) {
    if (wrapper1 == null || wrapper2 == null) {
      return null;
    }
    
    return join(wrapper1.literal, wrapper2.literal);
  }

  /**
   * Contains individual assumptions about variables. If variable isn't in the
   * map, then variable assumption is _|_ (bottom), if variable's value is
   * null, then variable assumption is T - variable has non-constant value.
   */
  private final Map<JVariable, LiteralWrapper> values;

  public ConstantsAssumption() {
    values = new HashMap<JVariable, LiteralWrapper>();
  }

  public ConstantsAssumption(ConstantsAssumption a) {
    if (a != null) {
      values = new HashMap<JVariable, LiteralWrapper>(a.values);
    } else {
      values = new HashMap<JVariable, LiteralWrapper>();
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    ConstantsAssumption other = (ConstantsAssumption) obj;
    return values.equals(other.values);
  }
  
  /**
   * Get variable constant assumption. <code>null</code> if there's no constant
   * assumption for this variable. 
   */
  public JValueLiteral get(JVariable variable) {
    LiteralWrapper wrapper = values.get(variable);
    return wrapper != null ? wrapper.literal : null;
  }

  /**
   * Check if we have constant (i.e. not top and not bottom) assumption about 
   * the variable.
   */
  public boolean hasAssumption(JVariable variable) {
    return get(variable) != null;
  }

  @Override
  public int hashCode() {
    return values.hashCode();
  }
  
  public boolean isEmpty() {
    return values.isEmpty();
  }

  public ConstantsAssumption join(ConstantsAssumption other) {
    if (other == null) {
      return this;
    }
    
    if (other == TOP || this == TOP || isEmpty() || other.isEmpty()) {
      return TOP;
    }
    
    ConstantsAssumption result = new ConstantsAssumption();
    
    for (JVariable var : other.values.keySet()) {
      if (values.containsKey(var)) {
        // Var is present in both assumptions. Join their values.
        JValueLiteral value = join(values.get(var), other.values.get(var));
        if (value != null) {
          result.values.put(var, new LiteralWrapper(value));
        }
      } 
    }
    
    if (result.isEmpty()) {
      return TOP;
    }
    
    return result;
  }

  public String toDebugString() {
    if (this == TOP || isEmpty()) {
      return "T";
    }
    StringBuffer result = new StringBuffer();
    
    result.append("{");
    List<JVariable> variables = new ArrayList<JVariable>(values.keySet());
    HasName.Util.sortByName(variables);
    for (JVariable variable : variables) {
      if (result.length() > 1) {
        result.append(", ");
      }
      result.append(variable.getName());
      result.append(" = ");
      if (values.get(variable) == null) {
        result.append("T");
      } else {
        result.append(values.get(variable));
      }
    }
    result.append("}");
    
    return result.toString();
  }
  
  @Override
  public String toString() {
    return toDebugString();
  }
  
  void set(JVariable variable, JValueLiteral literal) {
    if (literal != null) {
      values.put(variable, new LiteralWrapper(literal));
    } else {
      values.remove(variable);
    }
  }
}
