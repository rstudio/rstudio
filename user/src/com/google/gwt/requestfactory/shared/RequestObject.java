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
  // reset the DeltaValueStore.
  void reset();

  void fire(Receiver<T> receiver);

  // can get access to a DeltaValueStore only in the context of a RequestObject.
  DeltaValueStore getDeltaValueStore();

  RequestData getRequestData();

  void handleResponseText(String responseText);
  
}