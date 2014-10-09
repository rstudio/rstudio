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
package com.google.web.bindery.autobean.gwt.client.impl;

import com.google.web.bindery.autobean.shared.Splittable;

import java.util.List;

/**
 * Implements {@link Splittable#NULL} in prod mode.
 */
public class NullSplittable implements Splittable {
  public static final NullSplittable INSTANCE = new NullSplittable();

  private NullSplittable() { }

  @Override
  public boolean asBoolean() {
    return false;
  }

  @Override
  public double asNumber() {
    return 0;
  }

  @Override
  public native void assign(Splittable parent, int index) /*-{
    parent[index] = null;
  }-*/;

  @Override
  public native void assign(Splittable parent, String propertyName) /*-{
    delete parent[propertyName];
  }-*/;

  @Override
  public String asString() {
    return null;
  }

  @Override
  public Splittable deepCopy() {
    return this;
  }

  @Override
  public Splittable get(int index) {
    throw new NullPointerException();
  }

  @Override
  public Splittable get(String key) {
    throw new NullPointerException();
  }

  @Override
  public String getPayload() {
    return "null";
  }

  @Override
  public List<String> getPropertyKeys() {
    throw new NullPointerException();
  }

  @Override
  public Object getReified(String key) {
    throw new NullPointerException();
  }

  @Override
  public boolean isBoolean() {
    return false;
  }

  @Override
  public boolean isIndexed() {
    return false;
  }

  @Override
  public boolean isKeyed() {
    return false;
  }

  @Override
  public boolean isNull(int index) {
    throw new NullPointerException();
  }

  @Override
  public boolean isNull(String key) {
    throw new NullPointerException();
  }

  @Override
  public boolean isNumber() {
    return false;
  }

  @Override
  public boolean isReified(String key) {
    throw new NullPointerException();
  }

  @Override
  public boolean isString() {
    return false;
  }

  @Override
  public boolean isUndefined(String key) {
    throw new NullPointerException();
  }

  @Override
  public void removeReified(String key) {
    throw new NullPointerException();
  }

  @Override
  public void setReified(String key, Object object) {
    throw new NullPointerException();
  }

  @Override
  public void setSize(int i) {
    throw new NullPointerException();
  }

  @Override
  public int size() {
    throw new NullPointerException();
  }
}
