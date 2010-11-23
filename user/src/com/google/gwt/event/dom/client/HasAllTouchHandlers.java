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

package com.google.gwt.event.dom.client;

/**
 * This is a convenience interface that includes all touch handlers defined by
 * the core GWT system.
 * <p>
 * WARNING, PLEASE READ: As this interface is intended for developers who wish
 * to handle all touch events in GWT, new touch event handlers will be added to
 * it. Therefore, updates can cause breaking API changes.
 * </p>
 */
public interface HasAllTouchHandlers extends HasTouchStartHandlers,
    HasTouchMoveHandlers, HasTouchEndHandlers, HasTouchCancelHandlers {
}
