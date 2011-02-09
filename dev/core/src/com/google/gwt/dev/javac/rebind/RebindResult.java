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
package com.google.gwt.dev.javac.rebind;

/**
 * A class for returning the result of a rebind operation.
 */
public class RebindResult {
  private final RebindStatus resultStatus;
  private final String returnedTypeName;
  private final CachedClientDataMap clientData;
 
  public RebindResult(RebindStatus resultStatus, 
      String returnedType, CachedClientDataMap clientData) {
    this.resultStatus = resultStatus;
    this.returnedTypeName = returnedType;
    this.clientData = clientData;
  }

  public RebindResult(RebindStatus resultStatus, String returnedType) {
    this(resultStatus, returnedType, null);
  }
 
  public Object getClientData(String key) {
    if (clientData == null) {
      return null;
    } else {
      return clientData.get(key);
    }
  }
 
  public CachedClientDataMap getClientDataMap() {
    return clientData;
  }
 
  public RebindStatus getResultStatus() {
    return resultStatus;
  }

  public String getReturnedTypeName() {
    return returnedTypeName;
  }
}
