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
package com.google.gwt.dev.cfg;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Generates all possible permutations of properties in a module.
 */
public class PropertyPermutations {

  private int currPermIndex;

  private final int lastProp;

  private final Property[] properties;

  private final String[][] values;

  public PropertyPermutations(Properties properties) {
    this.properties = properties.toArray();
    lastProp = this.properties.length - 1;
    int permCount = countPermutations();
    values = new String[permCount][];
    if (this.properties.length > 0) {
      permute(null, 0);
      assert (permCount == currPermIndex);
    } else {
      values[0] = new String[0];
    }
  }

  public Property[] getOrderedProperties() {
    return properties;
  }

  /**
   * Enumerates each permutation as an array of strings such that the index of
   * each string in the array corresponds to the property at the same index in
   * the array returned from {@link #getOrderedProperties()}.
   */
  public Iterator<String[]> iterator() {
    return new Iterator<String[]>() {

      private int iterPermIndex;

      public boolean hasNext() {
        return iterPermIndex < values.length;
      }

      public String[] next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return values[iterPermIndex++];
      }

      public void remove() {
        throw new UnsupportedOperationException("remove");
      }
    };
  }

  private int countPermutations() {
    int count = 1;
    for (int i = 0; i < properties.length; i++) {
      Property prop = properties[i];
      String[] options = getPossibilities(prop);
      assert (options.length > 0);
      count *= options.length;
    }
    return count;
  }

  private String[] getPossibilities(Property prop) {
    String activeValue = prop.getActiveValue();
    if (activeValue != null) {
      // This property is fixed.
      //
      return new String[] {activeValue};
    } else {
      // This property is determined on the client.
      //
      return prop.getKnownValues();
    }
  }

  private void permute(String[] soFar, int whichProp) {
    Property prop = properties[whichProp];
    String[] options = getPossibilities(prop);

    for (int i = 0; i < options.length; i++) {
      String knownValue = options[i];

      String[] nextStep = new String[whichProp + 1];
      if (whichProp > 0) {
        System.arraycopy(soFar, 0, nextStep, 0, soFar.length);
      }
      nextStep[whichProp] = knownValue;

      if (whichProp < lastProp) {
        permute(nextStep, whichProp + 1);
      } else {
        // Finished this permutation.
        //
        values[currPermIndex] = nextStep;
        ++currPermIndex;
      }
    }
  }
}
