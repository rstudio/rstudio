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
package com.google.gwt.tools.apichecker;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;

import java.util.HashSet;
import java.util.Set;

/**
 * Immutable class that encapsulates an API Field. Useful for set-operations. An
 * ApiField is attached to an ApiClass.
 */
final class ApiField implements Comparable<ApiField>, ApiElement {

  static String computeApiSignature(JField tempField) {
    return tempField.getEnclosingType().getQualifiedSourceName() + "::" + tempField.getName();
  }

  private final ApiClass apiClass;
  private volatile String apiSignature = null; // cached, lazily initialized
  private final JField field;
  private volatile String relativeSignature = null; // cached, lazily

  // initialized

  ApiField(JField field, ApiClass apiClass) {
    this.field = field;
    this.apiClass = apiClass;
  }

  public int compareTo(ApiField other) {
    return getRelativeSignature().compareTo(other.getRelativeSignature());
  }

  /**
   * Used during set operations.
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof ApiField) {
      ApiField other = (ApiField) o;
      return getApiSignature().equals(other.getApiSignature());
    }
    return false;
  }

  public ApiClass getApiClass() {
    return apiClass;
  }

  public String getRelativeSignature() {
    if (relativeSignature == null) {
      relativeSignature = computeRelativeSignature();
    }
    return relativeSignature;
  }

  @Override
  public int hashCode() {
    return getApiSignature().hashCode();
  }

  @Override
  public String toString() {
    return field.toString();
  }

  String getApiSignature() {
    if (apiSignature == null) {
      apiSignature = computeApiSignature();
    }
    return apiSignature;
  }

  JField getField() {
    return field;
  }

  Set<ApiChange> getModifierChanges(ApiField newField) {
    Set<ApiChange> statuses = new HashSet<ApiChange>();
    if (!field.isFinal() && newField.getField().isFinal()) {
      statuses.add(new ApiChange(this, ApiChange.Status.FINAL_ADDED));
    }
    if ((field.isStatic() && !newField.getField().isStatic())) {
      statuses.add(new ApiChange(this, ApiChange.Status.STATIC_REMOVED));
    }
    return statuses;
  }

  private String computeApiSignature() {
    return computeApiSignature(field);
  }

  private String computeRelativeSignature() {
    String signature = field.getName();
    if (ApiCompatibilityChecker.DEBUG) {
      JClassType enclosingType = field.getEnclosingType();
      return apiClass.getClassObject().getQualifiedSourceName()
          + "::"
          + signature
          + " defined in "
          + (enclosingType == null ? "null enclosing type " : enclosingType
              .getQualifiedSourceName());
    }
    return apiClass.getClassObject().getQualifiedSourceName() + "::" + signature;
  }

}