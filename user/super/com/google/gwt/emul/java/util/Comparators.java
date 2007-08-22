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
package java.util;

class Comparators {
  /*
   * This is a utility class that provides a default Comparator. Instead of
   * having a directly accessible field, a function is used in anticipation of
   * generics support. This class exists so Arrays and Collections can share the
   * natural comparator without having to know internals of each other.
   * 
   * This class is package protected since it is not in the JRE.
   */

  /**
   * Compares two Objects according to their <i>natural ordering</i>.
   * 
   * @see java.lang.Comparable
   */
  @SuppressWarnings("unchecked")
  private static final Comparator NATURAL = new Comparator() {
    public int compare(Object o1, Object o2) {
      return ((Comparable) o1).compareTo(o2); // suppress unchecked warning
    }
  };

  /**
   * Returns the natural Comparator.
   * 
   * @return the natural Comparator
   */
  @SuppressWarnings("unchecked")
  public static <T> Comparator<T> natural() {
    /*
     * Code for generics support is commented. Example calling code, which
     * should be moved into the Javadoc comment when generics are added: <code>Comparator&lt;String&gt; =
     * Comparators.natural();</code>
     */
    return NATURAL; // suppress unchecked warning
  }
}
