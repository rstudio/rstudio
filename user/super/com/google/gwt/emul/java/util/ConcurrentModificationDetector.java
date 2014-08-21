/*
 * Copyright 2014 Google Inc.
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
 * A helper to detect concurrent modifications to collections. This is implemented as a helper
 * utility so that we could remove the checks easily by a flag.
 */
class ConcurrentModificationDetector {

  public static void structureChanged(Object map) {
    // Ensure that modCount is initialized if it is not already.
    int modCount = getModCount(map) | 0;
    setModCount(map, modCount + 1);
  }

  public static void recordLastKnownStructure(Object host, Iterator<?> iterator) {
    setModCount(iterator, getModCount(host));
  }
  
  public static void checkStructuralChange(Object host, Iterator<?> iterator) {
    if (getModCount(iterator) != getModCount(host)) {
      throw new ConcurrentModificationException();
    }
  }

  private static native void setModCount(Object o, int modCount) /*-{
    o._gwt_modCount = modCount;
  }-*/;

  private static native int getModCount(Object o) /*-{
    return o._gwt_modCount;
  }-*/;
}
