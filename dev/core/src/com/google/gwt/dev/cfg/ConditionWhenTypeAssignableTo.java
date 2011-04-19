/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationProblemReporter;

/**
 * A deferred binding condition to determine whether the type being rebound is
 * assignment-compatible with a particular type.
 */
public class ConditionWhenTypeAssignableTo extends Condition {

  private static boolean warnedMissingValidationJar = false;

  private final String assignableToTypeName;

  public ConditionWhenTypeAssignableTo(String assignableToTypeName) {
    this.assignableToTypeName = assignableToTypeName;
  }

  public String getAssignableToTypeName() {
    return assignableToTypeName;
  }

  @Override
  public String toString() {
    return "<when-assignable class='" + assignableToTypeName + "'/>";
  }

  @Override
  protected boolean doEval(TreeLogger logger, DeferredBindingQuery query)
      throws UnableToCompleteException {
    TypeOracle typeOracle = query.getTypeOracle();
    String testType = query.getTestType();
    JClassType fromType = typeOracle.findType(testType);
    if (fromType == null) {
      CompilationProblemReporter.logMissingTypeErrorWithHints(logger, testType,
          query.getCompilationState());
      throw new UnableToCompleteException();
    }

    JClassType toType = typeOracle.findType(assignableToTypeName);
    if (toType == null) {
      // If we don't know the type, it can't be assignable to it.
      // This isn't a strict failure case because stale rules can reference
      // types that have been deleted.
      //
      TreeLogger.Type level = TreeLogger.WARN;
      if (shouldSuppressWarning(logger, assignableToTypeName)) {
        // Suppress validation related errors
        level = TreeLogger.DEBUG;
      }
      logger.log(level, "Unknown type '" + assignableToTypeName
          + "' specified in deferred binding rule", null);
      return false;
    }

    if (fromType.isAssignableTo(toType)) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected String getEvalAfterMessage(String testType, boolean result) {
    if (result) {
      return "Yes, the requested type was assignable";
    } else {
      return "No, the requested type was not assignable";
    }
  }

  @Override
  protected String getEvalBeforeMessage(String testType) {
    return toString();
  }

  /**
   * Suppress multiple validation related messages and replace with a hint.  
   *     
   * @param typeName fully qualified type name to check for filtering
   */
  // TODO(zundel): Can be removed when javax.validation is included in the JRE
  private boolean shouldSuppressWarning(TreeLogger logger, String typeName) {
    if (typeName.startsWith("javax.validation.")
        || typeName.startsWith("com.google.gwt.validation.")
        || typeName.startsWith("com.google.gwt.editor.client")) {
      if (!warnedMissingValidationJar) {
        warnedMissingValidationJar = true;
        logger.log(TreeLogger.WARN, "Detected warnings related to '" + typeName + "'. "
            + "  Are validation-api-<version>.jar and validation-api-<version>-sources.jar on the classpath?");
        logger.log(TreeLogger.INFO, "Specify -logLevel DEBUG to see all errors.");
        // Show the first error that matches
        return false;
      }
      // Suppress subsequent errors that match
      return true;
    }
    return false;
  }
}
