/*
 * Copyright 2007 Google Inc.
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
package java.lang;

import java.io.Serializable;

/**
 * The first-class representation of an enumeration.
 */
public abstract class Enum<E extends Enum<E>>
    implements Comparable<E>, Serializable {

  public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  private String name;

  private int ordinal;

  protected Enum(String name, int ordinal) {
    this.name = name;
    this.ordinal = ordinal;
  }

  public int compareTo(E o) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  public boolean equals(Object other) {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  public Class<E> getDeclaringClass() {
    throw new UnsupportedOperationException("not yet implemented.");
  }

  public int hashCode() {
    // TODO(tobyr) - consider rewriting this in terms of System.identityHashCode
    int result;
    result = (name != null ? name.hashCode() : 0);
    result = 31 * result + ordinal;
    return result;
  }

  public String name() {
    return name;
  }

  public int ordinal() {
    return ordinal;
  }

  public String toString() {
    return name;
  }

  protected Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException("Enums can not be cloned.");
  }

  protected void finalize() {
  }
}
