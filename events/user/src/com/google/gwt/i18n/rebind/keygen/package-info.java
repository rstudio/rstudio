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

/**
 * Generators which compute the value of a key to use for looking up translated
 * resources.  These exist to allow easy extension to proprietary or internal
 * message catalog systems which use different algorithms to compute keys for
 * message aggregation. 
 */
@com.google.gwt.util.PreventSpuriousRebuilds
package com.google.gwt.i18n.rebind.keygen;
