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
package com.google.gwt.user.client.rpc.impl;

/**
 * Base class for the client and server serialization streams. This class
 * handles the basic serialization and desirialization formatting for primitive
 * types since these are common between the client and the server.
 */
public abstract class AbstractSerializationStream {

  public static final int SERIALIZATION_STREAM_FLAGS_NO_TYPE_VERSIONING = 1;
  public static final int SERIALIZATION_STREAM_VERSION = 2;

  protected int flags = 0;

  protected int version;

  public final void addFlags(int flags) {
    this.flags |= flags;
  }

  public final int getFlags() {
    return flags;
  }

  public final int getVersion() {
    return version;
  }

  public final void setFlags(int flags) {
    this.flags = flags;
  }

  public final boolean shouldEnforceTypeVersioning() {
    return (flags & SERIALIZATION_STREAM_FLAGS_NO_TYPE_VERSIONING) == 0;
  }

  protected final void setVersion(int version) {
    this.version = version;
  }
}
