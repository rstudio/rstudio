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

package com.google.gwt.user.client.impl;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.UIObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Creates a mapping from elements to their associated ui objects.
 * 
 * @param <T> the type that the element is mapped to
 */
public class ElementMapperImpl<T extends UIObject> {

  private static class FreeNode {
    int index;
    ElementMapperImpl.FreeNode next;

    public FreeNode(int index, ElementMapperImpl.FreeNode next) {
      this.index = index;
      this.next = next;
    }
  }

  private static native void clearIndex(Element elem) /*-{
    elem["__uiObjectID"] = null;
  }-*/;

  private static native int getIndex(Element elem) /*-{
    var index = elem["__uiObjectID"];
    return (index == null) ? -1 : index;
  }-*/;

  private static native void setIndex(Element elem, int index) /*-{
    elem["__uiObjectID"] = index;
  }-*/;

  private ElementMapperImpl.FreeNode freeList = null;

  private final ArrayList<T> uiObjectList = new ArrayList<T>();

  /**
   * Returns the uiObject associated with the given element.
   * 
   * @param elem uiObject's element
   * @return the uiObject
   */
  public T get(Element elem) {
    int index = getIndex(elem);
    if (index < 0) {
      return null;
    }
    return uiObjectList.get(index);
  }

  /**
   * Gets the list of ui objects contained in this element mapper.
   * 
   * @return the list of ui objects
   */
  public ArrayList<T> getObjectList() {
    return uiObjectList;
  }

  /**
   * Creates an iterator from the ui objects stored within.
   * 
   * @return an iterator of the ui objects indexed by this element mapper.
   */
  public Iterator<T> iterator() {
    return uiObjectList.iterator();
  }

  /**
   * Adds the MappedType.
   * 
   * @param uiObject uiObject to add
   */
  public void put(T uiObject) {
    int index;
    if (freeList == null) {
      index = uiObjectList.size();
      uiObjectList.add(uiObject);
    } else {
      index = freeList.index;
      uiObjectList.set(index, uiObject);
      freeList = freeList.next;
    }
    setIndex(uiObject.getElement(), index);
  }

  /**
   * Remove the uiObject associated with the given element.
   * 
   * @param elem the uiObject's element
   */
  public void removeByElement(Element elem) {
    int index = getIndex(elem);
    removeImpl(elem, index);
  }

  private void removeImpl(Element elem, int index) {
    clearIndex(elem);
    uiObjectList.set(index, null);
    freeList = new FreeNode(index, freeList);
  }
}
