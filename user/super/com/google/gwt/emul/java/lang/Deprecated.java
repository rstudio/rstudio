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
package java.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A program element annotated &#64;Deprecated is one that programmers are
 * discouraged from using, typically because it is dangerous, or because a
 * better alternative exists. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Deprecated.html">[Sun
 * docs]</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Deprecated {
}
