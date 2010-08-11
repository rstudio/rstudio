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
package com.google.gwt.requestfactory.shared;

import com.google.gwt.valuestore.shared.Record;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Implemented by the request objects created by this factory.
 * @param <T> The return type of objects in the corresponding response.
 */
public interface RequestObject<T> {

  /**
   * Enable a RequestObject to be reused again. For example, when the edit
   * fails on the server.
   */
  void clearUsed();

  // TODO: temporary hack so that I could get rid of DeltaValueStore. This will
  // be removed once the hack for SYNC requests goes away.
  void delete(Record record);

  <P extends Record> P edit(P record);

  void fire(Receiver<T> receiver);

  RequestData getRequestData();

  void handleResponseText(String responseText);

  /**
   * Return true if there are outstanding changes that have not been
   * communicated to the server yet. Note that it is illegal to call this method
   * after a request using it has been fired.
   */
  boolean isChanged();

  // reset the DeltaValueStore.
  void reset();
  
}