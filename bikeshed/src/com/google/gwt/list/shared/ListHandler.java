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

import com.google.gwt.event.shared.EventHandler;

/**
 * Handler interface for {@link ListEvent} events.
 * 
 * @param <T> the value about to be changed
 */
public interface ListHandler<T> extends EventHandler {

  /**
   * Called when {@link ListEvent} is fired.
   * 
   * @param event the {@link ListEvent} that was fired
   */
  void onDataChanged(ListEvent<T> event);

  /**
   * Called when a {@link SizeChangeEvent} is fired.
   * 
   * @param event the {@link SizeChangeEvent} that was fired
   */
  void onSizeChanged(SizeChangeEvent event);
}
