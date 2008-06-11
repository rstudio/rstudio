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

package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.museum.client.viewer.Museum;

/**
 * Default bug museum. Contains a list of all GWT issues reported in the system
 * to date.
 */
public class DefaultMuseum extends Museum implements EntryPoint {
  public DefaultMuseum() {
    addIssue(new Issue1245());
    addIssue(new Issue1897());
    addIssue(new Issue2261());
    addIssue(new Issue2290());
    addIssue(new Issue2307());
    addIssue(new Issue2321());
    addIssue(new Issue2331());
    addIssue(new Issue2338());
    addIssue(new Issue2339());
    addIssue(new Issue2392());
    addIssue(new Issue2443());
    addIssue(new TestFireEvents());
  }
}
