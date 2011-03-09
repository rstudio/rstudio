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
package com.google.gwt.uibinder.rebind.messages;

/**
 * Associates an attribute name and a message expression. Can generate
 * code that refers to the message.
 */
public class AttributeMessage {
  private final String attribute;
  private final String message;

  public AttributeMessage(String attribute, String message) {
    super();
    this.attribute = attribute;
    this.message = message;
  }

  public String getAttribute() {
    return attribute;
  }

  public String getMessageUnescaped() {
    return message;
  }
}
