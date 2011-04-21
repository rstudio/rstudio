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
package com.google.gwt.dev.javac.testing.impl;

import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An in-memory {@link Resource}.
 */
public abstract class MockResource extends Resource {
  private static final AtomicLong lastTimeStamp = new AtomicLong();

  private static long getNextCreationTime() {
    long currentTime = System.currentTimeMillis();
    long lastTime;
    // Go into the future until we succeed.
    do {
      lastTime = lastTimeStamp.get();
      if (currentTime <= lastTime) {
        currentTime = lastTime + 1;
      }
    } while (!lastTimeStamp.compareAndSet(lastTime, currentTime));
    return currentTime;
  }

  private long creationTime = getNextCreationTime();
  private final String path;

  public MockResource(String path) {
    this.path = path;
  }

  public abstract CharSequence getContent();

  @Override
  public long getLastModified() {
    return creationTime;
  }

  @Override
  public String getLocation() {
    return "/mock/" + path;
  }

  @Override
  public String getPath() {
    return path;
  }

  public String getString() {
    return getContent().toString();
  }

  @Override
  public InputStream openContents() {
    return new ByteArrayInputStream(Util.getBytes(getContent().toString()));
  }

  @Override
  public boolean wasRerooted() {
    return false;
  }

  protected void touch() {
    creationTime = getNextCreationTime();
  }
}
