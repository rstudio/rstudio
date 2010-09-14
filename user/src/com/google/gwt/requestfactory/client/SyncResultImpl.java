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
package com.google.gwt.requestfactory.client;

import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.SyncResult;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Concrete implementation of SyncResult.
 */
public class SyncResultImpl implements SyncResult {

  private final EntityProxy record;
  private final Object futureId;
  
  public SyncResultImpl(EntityProxy record, Object futureId) {
    this.record = record;
    this.futureId = futureId;
  }

  public Object getFutureId() {
    return futureId;
  }

  public EntityProxy getProxy() {
    return record;
  }  
}
