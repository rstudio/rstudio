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
package com.google.web.bindery.requestfactory.server;

/**
 * A service API that doesn't have static methods and that can't be
 * default-instantiated.
 */
public class InstanceServiceImpl implements InstanceService {
  private final int base;

  public InstanceServiceImpl(int base) {
    this.base = base;
  }

  public Integer add(int value) {
    return base + value;
  }
}
