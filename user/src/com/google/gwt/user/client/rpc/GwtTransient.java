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
package com.google.gwt.user.client.rpc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation means the same thing as the <code>transient</code> keyword,
 * but it is ignored by all serialization systems other than GWT's. Usually the
 * <code>transient</code> keyword should be used in preference to this
 * annotation. However, for types used with multiple serialization systems, it
 * can be useful.
 * <p>
 * Note that GWT will actually accept any annotation named GwtTransient for this
 * purpose. This is done to allow libraries to support GWT serialization without
 * creating a direct dependency on the GWT distribution.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GwtTransient {
}
