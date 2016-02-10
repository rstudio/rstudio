/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.safehtml.shared.annotations;

/**
 * Annotates methods that rely on potentially-unsafe type-annotation casts.
 * <p>
 * This annotation marks methods in which an expression without a
 * {@link com.google.gwt.safehtml.shared.annotations.IsTrustedResourceUri} annotation is used in a
 * context where such an annotation is required (e.g., the return statement of a method that
 * returns {@code @IsTrustedResourceUri String}).
 * <p>
 * As such, use of this annotation marks code that is potentially prone to XSS
 * vulnerabilities, and which hence needs to be carefully security reviewed.
 */
public @interface SuppressIsTrustedResourceUriCastCheck {

}
