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
package com.google.gwt.resources.css;

import com.google.gwt.resources.css.ast.Context;
import com.google.gwt.resources.css.ast.CssCompilerException;
import com.google.gwt.resources.css.ast.CssModVisitor;
import com.google.gwt.resources.css.ast.CssNoFlip;
import com.google.gwt.resources.css.ast.CssProperty;
import com.google.gwt.resources.css.ast.CssRule;
import com.google.gwt.resources.css.ast.CssProperty.IdentValue;
import com.google.gwt.resources.css.ast.CssProperty.NumberValue;
import com.google.gwt.resources.css.ast.CssProperty.Value;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * Applies RTL transforms to a stylesheet.
 */
public class RtlVisitor extends CssModVisitor {
  /**
   * Records if we're currently visiting a CssRule whose only selector is
   * "body".
   */
  private boolean inBodyRule;

  @Override
  public void endVisit(CssProperty x, Context ctx) {
    String name = x.getName();

    if (name.equalsIgnoreCase("left")) {
      x.setName("right");
    } else if (name.equalsIgnoreCase("right")) {
      x.setName("left");
    } else if (name.endsWith("-left")) {
      int len = name.length();
      x.setName(name.substring(0, len - 4) + "right");
    } else if (name.endsWith("-right")) {
      int len = name.length();
      x.setName(name.substring(0, len - 5) + "left");
    } else if (name.contains("-right-")) {
      x.setName(name.replace("-right-", "-left-"));
    } else if (name.contains("-left-")) {
      x.setName(name.replace("-left-", "-right-"));
    } else {
      List<Value> values = new ArrayList<Value>(x.getValues().getValues());
      invokePropertyHandler(x.getName(), values);
      x.setValue(new CssProperty.ListValue(values));
    }
  }

  @Override
  public boolean visit(CssNoFlip x, Context ctx) {
    return false;
  }

  @Override
  public boolean visit(CssRule x, Context ctx) {
    inBodyRule = x.getSelectors().size() == 1
        && x.getSelectors().get(0).getSelector().equals("body");
    return true;
  }

  void propertyHandlerBackground(List<Value> values) {
    /*
     * The first numeric value will be treated as the left position only if we
     * havn't seen any value that could potentially be the left value.
     */
    boolean seenLeft = false;

    for (ListIterator<Value> it = values.listIterator(); it.hasNext();) {
      Value v = it.next();
      Value maybeFlipped = flipLeftRightIdentValue(v);
      NumberValue nv = v.isNumberValue();
      if (v != maybeFlipped) {
        it.set(maybeFlipped);
        seenLeft = true;

      } else if (isIdent(v, "center")) {
        seenLeft = true;

      } else if (!seenLeft && (nv != null)) {
        seenLeft = true;
        if ("%".equals(nv.getUnits())) {
          float position = 100f - nv.getValue();
          it.set(new NumberValue(position, "%"));
          break;
        }
      }
    }
  }

  void propertyHandlerBackgroundPosition(List<Value> values) {
    propertyHandlerBackground(values);
  }

  Value propertyHandlerBackgroundPositionX(Value v) {
    ArrayList<Value> list = new ArrayList<Value>(1);
    list.add(v);
    propertyHandlerBackground(list);
    return list.get(0);
  }

  /**
   * Note there should be no propertyHandlerBorder(). The CSS spec states that
   * the border property must set all values at once.
   */
  void propertyHandlerBorderColor(List<Value> values) {
    swapFour(values);
  }

  void propertyHandlerBorderStyle(List<Value> values) {
    swapFour(values);
  }

  void propertyHandlerBorderWidth(List<Value> values) {
    swapFour(values);
  }

  Value propertyHandlerClear(Value v) {
    return propertyHandlerFloat(v);
  }

  Value propertyHandlerCursor(Value v) {
    IdentValue identValue = v.isIdentValue();
    if (identValue == null) {
      return v;
    }

    String ident = identValue.getIdent().toLowerCase();
    if (!ident.endsWith("-resize")) {
      return v;
    }

    StringBuffer newIdent = new StringBuffer();

    if (ident.length() == 9) {
      if (ident.charAt(0) == 'n') {
        newIdent.append('n');
        ident = ident.substring(1);
      } else if (ident.charAt(0) == 's') {
        newIdent.append('s');
        ident = ident.substring(1);
      } else {
        return v;
      }
    }

    if (ident.length() == 8) {
      if (ident.charAt(0) == 'e') {
        newIdent.append("w-resize");
      } else if (ident.charAt(0) == 'w') {
        newIdent.append("e-resize");
      } else {
        return v;
      }
      return new IdentValue(newIdent.toString());
    } else {
      return v;
    }
  }

  Value propertyHandlerDirection(Value v) {
    if (inBodyRule) {
      if (isIdent(v, "ltr")) {
        return new IdentValue("rtl");
      } else if (isIdent(v, "rtl")) {
        return new IdentValue("ltr");
      }
    }
    return v;
  }

  Value propertyHandlerFloat(Value v) {
    return flipLeftRightIdentValue(v);
  }

  void propertyHandlerMargin(List<Value> values) {
    swapFour(values);
  }

  void propertyHandlerPadding(List<Value> values) {
    swapFour(values);
  }

  Value propertyHandlerPageBreakAfter(Value v) {
    return flipLeftRightIdentValue(v);
  }

  Value propertyHandlerPageBreakBefore(Value v) {
    return flipLeftRightIdentValue(v);
  }

  Value propertyHandlerTextAlign(Value v) {
    return flipLeftRightIdentValue(v);
  }

  private Value flipLeftRightIdentValue(Value v) {
    if (isIdent(v, "right")) {
      return new IdentValue("left");

    } else if (isIdent(v, "left")) {
      return new IdentValue("right");
    }
    return v;
  }

  /**
   * Reflectively invokes a propertyHandler method for the named property.
   * Dashed names are transformed into camel-case names; only letters following
   * a dash will be capitalized when looking for a method to prevent
   * <code>fooBar<code> and <code>foo-bar</code> from colliding.
   */
  private void invokePropertyHandler(String name, List<Value> values) {
    // See if we have a property-handler function
    try {
      String[] parts = name.toLowerCase().split("-");
      StringBuffer methodName = new StringBuffer("propertyHandler");
      for (String part : parts) {
        if (part.length() > 0) {
          // A leading hyphen, or something like foo--bar, which is weird
          methodName.append(Character.toUpperCase(part.charAt(0)));
          methodName.append(part, 1, part.length());
        }
      }

      try {
        // Single-arg for simplicity
        Method m = getClass().getDeclaredMethod(methodName.toString(),
            Value.class);
        assert Value.class.isAssignableFrom(m.getReturnType());
        Value newValue = (Value) m.invoke(this, values.get(0));
        values.set(0, newValue);
      } catch (NoSuchMethodException e) {
        // OK
      }

      try {
        // Or the whole List for completeness
        Method m = getClass().getDeclaredMethod(methodName.toString(),
            List.class);
        m.invoke(this, values);
      } catch (NoSuchMethodException e) {
        // OK
      }

    } catch (SecurityException e) {
      throw new CssCompilerException(
          "Unable to invoke property handler function for " + name, e);
    } catch (IllegalArgumentException e) {
      throw new CssCompilerException(
          "Unable to invoke property handler function for " + name, e);
    } catch (IllegalAccessException e) {
      throw new CssCompilerException(
          "Unable to invoke property handler function for " + name, e);
    } catch (InvocationTargetException e) {
      throw new CssCompilerException(
          "Unable to invoke property handler function for " + name, e);
    }
  }

  private boolean isIdent(Value value, String query) {
    IdentValue v = value.isIdentValue();
    return v != null && v.getIdent().equalsIgnoreCase(query);
  }

  /**
   * Swaps the second and fourth values in a list of four values.
   */
  private void swapFour(List<Value> values) {
    if (values.size() == 4) {
      Collections.swap(values, 1, 3);
    }
  }
}