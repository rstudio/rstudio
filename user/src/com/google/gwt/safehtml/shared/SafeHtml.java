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
package com.google.gwt.safehtml.shared;

import java.io.Serializable;

/**
 * An object that implements this interface encapsulates HTML that is guaranteed
 * to be safe to use (with respect to potential Cross-Site-Scripting
 * vulnerabilities) in an HTML context.
 * 
 * Note on usage: SafeHtml should be used to ensure user input is not executed 
 * in the browser. SafeHtml should not be used to sanitize input before sending 
 * it to the server.
 *
 * <p>
 * All implementing classes must maintain the class invariant (by design and
 * implementation and/or convention of use), that invoking {@link #asString()}
 * on any instance will return a string that is safe to assign to the {@code
 * .innerHTML} DOM property in a browser (or to use similarly in an "inner HTML"
 * context), in the sense that doing so must not cause execution of script in
 * the browser.
 *
 * All implementations must implement equals() and hashCode() to behave
 * consistently with the result of asString().equals() and asString.hashCode().
 * 
 * The internal string must not be null.
 *
 * <p>
 * Implementations of this interface must not implement
 * {@link com.google.gwt.user.client.rpc.IsSerializable}, since deserialization
 * can result in violation of the class invariant.
 */
public interface SafeHtml extends Serializable {
  /*
   * Notes regarding serialization: - It may be reasonable to allow
   * deserialization on the client of objects serialized on the server (i.e. RPC
   * responses), based on the assumption that server code is trusted and would
   * not provide a malicious serialized form (if a MitM were able to modify
   * server responses, the client would be fully compromised in any case).
   * However, the GWT RPC framework currently does not seem to provide a
   * facility for restricting deserialization on the Server only (thought this
   * shouldn't be difficult to implement through a custom SerializationPolicy)
   *
   * - Some implementations of SafeHtml would in principle be able to enforce
   * their class invariant on deserialization (e.g., SimpleHtmlSanitizer could
   * apply HTML sanitization on deserialization). However, the GWT RPC framework
   * does not provide for an equivalent of readResolve() to enforce the class
   * invariant on deserialization.
   */

  /**
   * Returns this object's contained HTML as a string. Based on this class'
   * contract, the returned string will be safe to use in an HTML context.
   */
  String asString();

  /**
   * Compares this string to the specified object.
   * Must be equal to asString().equals()
   */
  boolean equals(Object anObject);

  /**
   * Returns a hash code for this string.
   * Must be equal to asString().hashCode()
   */
  int hashCode();
}
