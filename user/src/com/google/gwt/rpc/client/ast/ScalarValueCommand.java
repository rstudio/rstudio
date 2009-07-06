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
package com.google.gwt.rpc.client.ast;

/**
 * Represents a type hierarchy of values that do not maintain object identity on
 * the client.
 */
public abstract class ScalarValueCommand extends ValueCommand {
  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof ScalarValueCommand)) {
      return false;
    }
    ScalarValueCommand other = (ScalarValueCommand) o;
    Object myValue = getValue();
    Object otherValue = other.getValue();
    if (myValue == null && otherValue == null) {
      return true;
    } else if (myValue == null && otherValue != null) {
      return false;
    } else {
      return myValue.equals(otherValue);
    }
  }

  /**
   * Returns the value represented by the ScalarValueCommand.
   */
  public abstract Object getValue();

  @Override
  public final int hashCode() {
    return getValue() == null ? 0 : getValue().hashCode();
  }
}
