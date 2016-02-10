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
package com.google.gwt.safehtml.shared;

import com.google.gwt.safehtml.shared.annotations.IsSafeUri;

/**
 * An object that implements this interface encapsulates a URI that is
 * guaranteed to be safe to use (with respect to potential Cross-Site-Scripting
 * vulnerabilities) in a URL context, for example in a URL-typed attribute in an
 * HTML document.
 *
 * <p>
 * Note on usage: SafeUri should be used to ensure user input is not executed in
 * the browser. SafeUri should not be used to sanitize input before sending it
 * to the server: The server cannot rely on the type contract of SafeUri values
 * received from clients, because a malicious client could provide maliciously
 * crafted serialized forms of implementations of this type that violate the
 * type contract.
 *
 * <p>
 * All implementing classes must maintain the class invariant (by design and
 * implementation and/or convention of use), that invoking {@link #asString()}
 * on any instance will return a string that is safe to assign to a URL-typed
 * DOM or CSS property in a browser (or to use similarly in a "URL context"), in
 * the sense that doing so must not cause unintended execution of script in the
 * browser.
 *
 * <p>
 * In determining safety of a URL both the value itself as well as its
 * provenance matter. An arbitrary URI, including e.g. a
 * <code>javascript:</code> URI, can be deemed safe in the sense of this type's
 * contract if it is entirely under the program's control (e.g., a string
 * literal, {@see UriUtils#fromSafeConstant}).
 *
 * <p>
 * All implementations must implement equals() and hashCode() to behave
 * consistently with the result of asString().equals() and asString.hashCode().
 *
 * <p>
 * Implementations must not return {@code null} from {@link #asString()}.
 * 
 * @see UriUtils
 */
public interface SafeUri {

  /**
   * Returns this object's contained URI as a string.
   *
   * <p>
   * Based on this class' contract, the returned value will be non-null and a
   * string that is safe to use in a URL context.
   *
   * @return the contents as a String
   */
  @IsSafeUri
  String asString();

  /**
   * Compares this string to the specified object. Must be equal to
   * asString().equals().
   *
   * @param anObject the object to compare to
   */
  boolean equals(Object anObject);

  /**
   * Returns a hash code for this string. Must be equal to
   * asString().hashCode().
   */
  int hashCode();
}
