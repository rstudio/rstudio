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

package com.google.gwt.event.dom.client;

/**
 * Convenience interface used to implement all key handlers at once.
 * 
 * <p>
 * WARNING, PLEASE READ: In the unlikely event that more key handler subtypes
 * are added to GWT, this interface will be expanded, so only implement this
 * interface if you wish to have your widget break if a new key event type is
 * introduced.
 * </p>
 */
public interface HasAllKeyHandlers extends HasKeyUpHandlers,
    HasKeyDownHandlers, HasKeyPressHandlers {

}
