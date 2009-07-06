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
 * Represents a hierarchy of value types that must maintain distinct object
 * identity on the client. This type finalizes <code>equals</code> and
 * <code>hashCode</code> to give subtypes identity equality sematics.
 */
public abstract class IdentityValueCommand extends ValueCommand {
  @Override
  public final boolean equals(Object o) {
    return this == o;
  }

  @Override
  public final int hashCode() {
    return System.identityHashCode(this);
  }
}
