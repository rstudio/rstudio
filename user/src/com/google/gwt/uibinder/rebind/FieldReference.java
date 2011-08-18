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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

import java.util.Arrays;
import java.util.LinkedHashSet;

/**
 * Represents a <code>{field.reference}</code>. Collects all the types a
 * particular reference has been asked to return, and can validate that it
 * actually does so.
 */
public class FieldReference {
  private static class LeftHand {
    /**
     * The type of values acceptible to this LHS, in order of preference
     */
    private final JType[] types;
    /**
     * The element on the LHS, for error reporting
     */
    private final XMLElement source;

    LeftHand(XMLElement source, JType... types) {
      this.types = Arrays.copyOf(types, types.length);
      this.source = source;
    }
  }

  public static String renderTypesList(JType[] types) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < types.length; i++) {
      if (i > 0 && i == types.length - 1) {
        b.append(" or ");
      } else if (i > 0) {
        b.append(", ");
      }
      b.append(types[i].getQualifiedSourceName());
    }

    return b.toString();
  }

  private final FieldManager fieldManager;
  private final XMLElement source;
  private final String debugString;
  private final String[] elements;

  private final LinkedHashSet<LeftHand> leftHandTypes = new LinkedHashSet<LeftHand>();

  private final TypeOracle typeOracle;

  FieldReference(String reference, XMLElement source, FieldManager fieldManager,
      TypeOracle typeOracle) {
    this.source = source;
    this.debugString = "{" + reference + "}";
    this.fieldManager = fieldManager;
    this.typeOracle = typeOracle;
    elements = reference.split("\\.");
  }

  public void addLeftHandType(XMLElement source, JType... types) {
    leftHandTypes.add(new LeftHand(source, types));
  }

  public String getFieldName() {
    return elements[0];
  }

  public JType getReturnType() {
    return getReturnType(null);
  }

  /**
   * Returns the type returned by this field ref.
   * 
   * @param logger optional logger to report errors on, may be null
   * @return the field ref, or null
   */
  public JType getReturnType(MonitoredLogger logger) {
    FieldWriter field = fieldManager.lookup(elements[0]);
    if (field == null) {
      if (logger != null) {
        /*
         * It's null when called from HtmlTemplateMethodWriter, which fires
         * after validation has already succeeded.
         */
        logger.error(source, "in %s, no field named %s", this, elements[0]);
      }
      return null;
    }

    return field.getReturnType(elements, logger);
  }

  public XMLElement getSource() {
    return source;
  }

  @Override
  public String toString() {
    return debugString;
  }

  public void validate(MonitoredLogger logger) {
    JType myReturnType = getReturnType(logger);
    if (myReturnType == null) {
      return;
    }

    for (LeftHand left : leftHandTypes) {
      ensureAssignable(left, myReturnType, logger);
    }
  }

  /**
   * Returns a failure message if the types don't mesh, or null on success.
   */
  private void ensureAssignable(LeftHand left, JType rightHandType, MonitoredLogger logger) {
    assert left.types.length > 0;

    for (JType leftType : left.types) {

      if (leftType == rightHandType) {
        return;
      }

      if (matchingNumberTypes(leftType, rightHandType)) {
        return;
      }

      boolean[] explicitFailure = {false};
      if (handleMismatchedNonNumericPrimitives(leftType, rightHandType, explicitFailure)) {
        if (explicitFailure[0]) {
          continue;
        }
      }

      JClassType leftClass = leftType.isClassOrInterface();
      if (leftClass != null) {
        JClassType rightClass = rightHandType.isClassOrInterface();
        if ((rightClass == null) || !leftClass.isAssignableFrom(rightClass)) {
          continue;
        }
      }

      /*
       * If we have reached the bottom of the loop, we don't see a problem with
       * assigning to this left hand type. Return without logging any error.
       * This is pretty conservative -- we have a white list of bad conditions,
       * not an exhaustive check of valid assignments. We're not confident that
       * we know every error case, and are more worried about being artificially
       * restrictive.
       */
      return;
    }

    /*
     * Every possible left hand type had some kind of failure. Log this sad
     * fact, which will halt processing.
     */
    logger.error(left.source, "%s required, but %s returns %s", renderTypesList(left.types),
        FieldReference.this, rightHandType.getQualifiedSourceName());
  }

  private boolean handleMismatchedNonNumericPrimitives(JType leftType, JType rightHandType,
      boolean[] explicitFailure) {
    JPrimitiveType leftPrimitive = leftType.isPrimitive();
    JPrimitiveType rightPrimitive = rightHandType.isPrimitive();

    if (leftPrimitive == null && rightPrimitive == null) {
      return false;
    }

    if (leftPrimitive != null) {
      JClassType autobox = typeOracle.findType(leftPrimitive.getQualifiedBoxedSourceName());
      if (rightHandType != autobox) {
        explicitFailure[0] = true;
      }
    } else { // rightPrimitive != null
      JClassType autobox = typeOracle.findType(rightPrimitive.getQualifiedBoxedSourceName());
      if (leftType != autobox) {
        explicitFailure[0] = true;
      }
    }

    return true;
  }

  private boolean isNumber(JType type) {
    JClassType numberType = typeOracle.findType(Number.class.getCanonicalName());

    JClassType asClass = type.isClass();
    if (asClass != null) {
      return numberType.isAssignableFrom(asClass);
    }

    JPrimitiveType asPrimitive = type.isPrimitive();
    if (asPrimitive != null) {
      JClassType autoboxed = typeOracle.findType(asPrimitive.getQualifiedBoxedSourceName());
      return numberType.isAssignableFrom(autoboxed);
    }

    return false;
  }

  private boolean matchingNumberTypes(JType leftHandType, JType rightHandType) {
    /*
     * int i = (int) 1.0 is okay Integer i = (int) 1.0 is okay int i = (int)
     * Double.valueOf(1.0) is not
     */
    if (isNumber(leftHandType) && isNumber(rightHandType) //
        && (rightHandType.isPrimitive() != null)) {
      return true; // They will be cast into submission
    }

    return false;
  }
}
