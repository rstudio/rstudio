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
package com.google.gwt.list.shared;

import com.google.gwt.event.shared.HandlerRegistration;

/**
 * Registration returned from a call to ListModel#addListHandler.
 */
public interface ListRegistration extends HandlerRegistration {

  /**
   * Set the range of data that is interesting to the {@link ListHandler}.
   * 
   * @param start the start index
   * @param length the length
   */
  void setRangeOfInterest(int start, int length);
}
