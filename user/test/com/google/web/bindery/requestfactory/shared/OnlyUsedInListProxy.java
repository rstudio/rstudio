/*
 * Copyright 2011 Google Inc.
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
package com.google.web.bindery.requestfactory.shared;

import com.google.web.bindery.requestfactory.server.SimpleFoo;

/**
 * This proxy type should only be used as the parameterization of a list type to
 * ensure that proxy types reachable only through a parameterization can be
 * created by a RequestContext.
 */
@ProxyFor(SimpleFoo.class)
public interface OnlyUsedInListProxy extends ValueProxy {
  String getUserName();
}
