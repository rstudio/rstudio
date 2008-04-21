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

/**
 * 
 * An ApiChange message.Each message is a Status followed by an optional String
 * description.
 * 
 */

public class ApiChange {

  // add specific changes. For example, add FINAL_ADDED and
  // ABSTRACT_ADDED instead of API_ATTRIBUTES_CHANGED
  static enum Status {
    ABSTRACT_ADDED("ABSTRACT_ADDED"),

    ATTRIBUTES_WARNING("ATTRIBUTES_CHANGED_WARNING"),

    COMPATIBLE("COMPATIBLE"),

    COMPATIBLE_WITH("COMPATIBLE_WITH"),

    EXCEPTIONS_ERROR("EXCEPTION_TYPE_ERROR"),

    FINAL_ADDED("FINAL_ADDED"),

    MISSING("MISSING"),

    NONABSTRACT_CLASS_MADE_INTERFACE("NONABSTRACT_CLASS_MADE_INTERFACE"),

    OVERLOADED("OVERLOADED_METHOD_CALL"),

    RETURN_TYPE_ERROR("RETURN_TYPE_ERROR"),

    STATIC_ADDED("STATIC_ADDED"),

    STATIC_REMOVED("STATIC_REMOVED"),

    SUBCLASSABLE_API_CLASS_MADE_INTERFACE("SUBCLASSABLE_CLASS_MADE_INTERFACE"),

    SUBCLASSABLE_API_INTERFACE_MADE_CLASS("SUBCLASSABLE_INTERFACE_MADE_CLASS");

    private final String str;

    Status(String str) {
      this.str = str;
    }

    @Override
    public final String toString() {
      return str;
    }
  }

  private String message = null;
  private Status status = null;

  public ApiChange(Status status) {
    this.status = status;
  }

  public ApiChange(Status status, String message) {
    this.status = status;
    this.message = message;
  }

  public Status getStatus() {
    return status;
  }
 
  @Override
  public String toString() {
    if (message != null) {
      return status + " " + message;
    }
    return status.toString();
  }

}
