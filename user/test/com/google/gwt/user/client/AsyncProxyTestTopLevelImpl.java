/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.user.client;

/**
 * Used by AsyncProxyTest to validate that a top-level class can be an AsyncProxy.
 */
public class AsyncProxyTestTopLevelImpl implements AsyncProxyTest.Test {

  @Override
  public boolean defaultBool() {
    return false;
  }

  @Override
  public byte defaultByte() {
    return 3;
  }

  @Override
  public char defaultChar() {
    return 0;
  }

  @Override
  public double defaultDouble() {
    return 0;
  }

  @Override
  public float defaultFloat() {
    return 0;
  }

  @Override
  public int defaultInt() {
    return 0;
  }

  @Override
  public long defaultLong() {
    return 0;
  }

  @Override
  public Object defaultObject() {
    return null;
  }

  @Override
  public short defaultShort() {
    return 0;
  }

  @Override
  public String defaultString() {
    return null;
  }

  @Override
  public void one() {
  }

  @Override
  public void three() {
  }

  @Override
  public void two() {
  }

}
