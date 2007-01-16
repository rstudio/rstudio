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
package java.util;

/**
 * Utility methods related to native arrays.
 */
public class Arrays {

  private static Comparator natural = new Comparator() {
    public int compare(Object o1, Object o2) {
      return ((Comparable) o1).compareTo(o2);
    }
  };

  public static List asList(Object[] array) {
    List accum = new ArrayList();
    for (int i = 0; i < array.length; i++) {
      accum.add(array[i]);
    }
    return accum;
  }

  public static void sort(Object[] x) {
    nativeSort(x, x.length, natural);
  }

  public static void sort(Object[] x, Comparator s) {
    nativeSort(x, x.length, s);
  }

  // FUTURE: 5.0 support
  // public static String toString(Object[] x) {
  // if (x == null) {
  // return "null";
  // }
  //
  // StringBuffer b = new StringBuffer("[");
  // for (int i = 0; i < x.length; i++) {
  // if (i != 0) {
  // b.append(", ");
  // }
  // if (x[i] == null) {
  // b.append("null");
  // } else {
  // b.append(x[i].toString());
  // }
  // }
  // b.append("]");
  // return b.toString();
  // }

  private static native void nativeSort(Object[] array, int size,
      Comparator compare) /*-{ 
    if (size == 0) {
      return;
    }
   
    var v = new Array();
    for(var i = 0; i < size; ++i){
      v[i] = array[i];
    }
   
    if(compare != null) {
      var f = function(a,b) {
        var c = compare.@java.util.Comparator::compare(Ljava/lang/Object;Ljava/lang/Object;)(a,b);
        return c;
      }
      v.sort(f);
    } else {
      v.sort();
    }

    for(i = 0; i < size; ++i){
      array[i] = v[i];
    }

  }-*/;

}
