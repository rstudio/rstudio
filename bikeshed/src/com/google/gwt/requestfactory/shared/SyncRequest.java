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

import com.google.gwt.valuestore.shared.SyncResult;

import java.util.Set;

/**
 * Request to commit CRUD operations accumulated in a DeltaValueStore.
 */
// TODO this should merge with the main RequestObject
public interface SyncRequest {
  // TODO merge fire() and to(), s.t. compiler enforces providing a callback
  void fire();

  SyncRequest to(Receiver<Set<SyncResult>> receiver);
}
