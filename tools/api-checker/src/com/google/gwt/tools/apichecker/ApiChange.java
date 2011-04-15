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

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * An ApiChange message.Each message is a Status followed by an optional String
 * description.
 * 
 */

final class ApiChange implements Comparable<ApiChange> {

  // add specific changes. For example, add FINAL_ADDED and
  // ABSTRACT_ADDED instead of API_ATTRIBUTES_CHANGED
  enum Status {
    ABSTRACT_ADDED,

    ATTRIBUTES_CHANGED_WARNING,

    COMPATIBLE,

    COMPATIBLE_WITH,

    EXCEPTION_TYPE_ERROR,

    FINAL_ADDED,

    MISSING,

    NONABSTRACT_CLASS_MADE_INTERFACE,

    OVERLOADED_METHOD_CALL,

    /*
     * The api (argument types, return types, exceptions in throws clause) for
     * an api method that can be overridden CANNOT change without breaking api
     * compatibility
     */
    OVERRIDABLE_METHOD_ARGUMENT_TYPE_CHANGE, OVERRIDABLE_METHOD_EXCEPTION_TYPE_CHANGE, OVERRIDABLE_METHOD_RETURN_TYPE_CHANGE,

    RETURN_TYPE_ERROR,

    STATIC_ADDED,

    STATIC_REMOVED,

    SUBCLASSABLE_API_CLASS_MADE_INTERFACE,

    SUBCLASSABLE_API_INTERFACE_MADE_CLASS,
  }

  private static Map<String, Status> cache = new HashMap<String, Status>();
  static {
    for (Status tempStatus : Status.values()) {
      cache.put(tempStatus.name(), tempStatus);
    }
  }

  public static boolean contains(String str) {
    return cache.get(str) != null;
  }

  private ApiElement element = null;
  private String message = null;

  private Status status = null;

  private String stringRepresentation = null;

  private String stringRepresentationWithoutMessage = null;

  public ApiChange(ApiElement element, Status status) {
    this(element, status, null);
  }

  public ApiChange(ApiElement element, Status status, String message) {
    this.element = element;
    this.status = status;
    this.message = message;
  }

  public int compareTo(ApiChange arg0) {
    return this.toString().compareTo(arg0.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ApiChange)) {
      return false;
    }
    return this.toString().equals(o.toString());
  }

  public ApiElement getApiElement() {
    return element;
  }

  public String getMessage() {
    return message;
  }

  public Status getStatus() {
    return status;
  }

  public String getStringRepresentationWithoutMessage() {
    if (stringRepresentationWithoutMessage == null) {
      stringRepresentationWithoutMessage =
          element.getRelativeSignature() + ApiDiffGenerator.DELIMITER + status.name();
    }
    return stringRepresentationWithoutMessage;
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  public int hashCodeForDuplication() {
    return element.hashCode() * 31 + status.hashCode() * 23
        + (message == null ? 0 : message.hashCode());
  }

  @Override
  public String toString() {
    if (stringRepresentation == null) {
      stringRepresentation =
          getStringRepresentationWithoutMessage()
              + (message == null ? "" : (ApiDiffGenerator.DELIMITER + message));
    }
    return stringRepresentation;
  }
}
