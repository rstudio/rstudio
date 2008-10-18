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
package com.google.gwt.user.client.impl;

/**
 * DOM implementation differences for older version of Mozilla (mostly the
 * hosted mode browser on linux). The main difference is due to changes in
 * getBoxObjectFor in later versions of mozilla. The relevant bugzilla issues:
 * https://bugzilla.mozilla.org/show_bug.cgi?id=328881
 * https://bugzilla.mozilla.org/show_bug.cgi?id=330619
 */
public class DOMImplMozillaOld extends DOMImplMozilla {
}
