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

import java.util.LinkedHashSet;

/**
 * Represents a <code>{field.reference}</code>. Collects all the types a
 * particular reference has been asked to return, and can validate that it
 * actually does so.
 */
public class FieldReference {
  private final FieldManager fieldManager;

  private final String debugString;
  private final String[] elements;
  private final LinkedHashSet<JType> leftHandTypes = new LinkedHashSet<JType>();

  private final TypeOracle types;

  FieldReference(String reference, FieldManager fieldManager, TypeOracle types) {
    this.debugString = "{" + reference + "}";
    this.fieldManager = fieldManager;
    this.types = types;
    elements = reference.split("\\.");
  }

  public void addLeftHandType(JType type) {
    leftHandTypes.add(type);
  }

  public String getFieldName() {
    return elements[0];
  }

  @Override
  public String toString() {
    return debugString;
  }

  public void validate(MonitoredLogger logger) {
    JType myReturnType = findReturnType(logger);
    if (myReturnType == null) {
      return;
    }

    for (JType t : leftHandTypes) {
      ensureAssignable(t, myReturnType, logger);
    }
  }

  /**
   * @return A failure message if the types don't mesh, or null on success
   */
  private void ensureAssignable(JType leftHandType, JType rightHandType,
      MonitoredLogger logger) {

    if (leftHandType == rightHandType) {
      return;
    }

    if (handleMismatchedNumbers(leftHandType, rightHandType)) {
      return;
    }

    if (handleMismatchedNonNumericPrimitives(leftHandType, rightHandType,
        logger)) {
      return;
    }

    JClassType leftClass = leftHandType.isClassOrInterface();
    if (leftClass != null) {
      JClassType rightClass = rightHandType.isClassOrInterface();
      if ((rightClass == null) || !leftClass.isAssignableFrom(rightClass)) {
        logTypeMismatch(leftHandType, rightHandType, logger);
      }
    }

    /*
     * We're conservative and fall through here, allowing success. Not yet
     * confident that we know every error case, and are more worried about being
     * artificially restrictive.
     */
  }

  private JType findReturnType(MonitoredLogger logger) {
    FieldWriter field = fieldManager.lookup(elements[0]);
    if (field == null) {
      logger.error("No field named %s", elements[0]);
      return null;
    }

    return field.getReturnType(elements, logger);
  }

  private boolean handleMismatchedNonNumericPrimitives(JType leftHandType,
      JType rightHandType, MonitoredLogger logger) {
    JPrimitiveType leftPrimitive = leftHandType.isPrimitive();
    JPrimitiveType rightPrimitive = rightHandType.isPrimitive();

    if (leftPrimitive == null && rightPrimitive == null) {
      return false;
    }

    if (leftPrimitive != null) {
      JClassType autobox = types.findType(leftPrimitive.getQualifiedBoxedSourceName());
      if (rightHandType != autobox) {
        logger.error("Returns %s, can't be used as %s", rightHandType,
            leftHandType);
      }
    } else { // rightPrimitive != null
      JClassType autobox = types.findType(rightPrimitive.getQualifiedBoxedSourceName());
      if (leftHandType != autobox) {
        logger.error("Returns %s, can't be used as %s", rightHandType,
            leftHandType);
      }
    }

    return true;
  }

  private boolean handleMismatchedNumbers(JType leftHandType,
      JType rightHandType) {
    /*
     * int i = (int) 1.0 is okay
     * Integer i = (int) 1.0 is okay
     * int i = (int) Double.valueOf(1.0) is not
     */
    if (isNumber(leftHandType) && isNumber(rightHandType) 
        && (rightHandType.isPrimitive() != null)) {
      return true; // They will be cast into submission
    }

    return false;
  }

  private boolean isNumber(JType t) {
    JClassType numberType = types.findType(Number.class.getCanonicalName());

    JClassType asClass = t.isClass();
    if (asClass != null) {
      return numberType.isAssignableFrom(asClass);
    }

    JPrimitiveType asPrimitive = t.isPrimitive();
    if (asPrimitive != null) {
      JClassType autoboxed = types.findType(asPrimitive.getQualifiedBoxedSourceName());
      return numberType.isAssignableFrom(autoboxed);
    }

    return false;
  }

  private void logTypeMismatch(JType leftHandType, JType rightHandType,
      MonitoredLogger logger) {
    logger.error("Returns %s, can't be used as %s", rightHandType, leftHandType);
  }
}
