/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui;

import java.util.EventObject;

/**
 * Event object containing information about {@link DisclosurePanel} changes.
 * 
 * @deprecated Use
 *             {@link com.google.gwt.event.logical.shared.CloseEvent} and
 *             {@link  com.google.gwt.event.logical.shared.OpenEvent} instead
 */
@Deprecated
public class DisclosureEvent extends EventObject {
  /**
   * Creates a new instance of the event object.
   * 
   * @param sender the panel from which the event is originating.
   * 
   * @see DisclosureHandler
   * @deprecated Use
   *             {@link com.google.gwt.event.logical.shared.CloseEvent} and
   *             {@link  com.google.gwt.event.logical.shared.OpenEvent} instead
   */
  @Deprecated
  public DisclosureEvent(DisclosurePanel sender) {
    super(sender);
  }
}
