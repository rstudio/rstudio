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
package com.google.gwt.safecss.shared;

import java.io.Serializable;

/**
 * An object that implements this interface encapsulates zero or more CSS
 * properties that are guaranteed to be safe to use (with respect to potential
 * Cross-Site-Scripting vulnerabilities) in a CSS (Cascading Style Sheet)
 * attribute context. A CSS attribute context can be inside of a CSS rule in a
 * {@code style} element, or inside the {@code style} attribute of a DOM
 * element.
 * 
 * <p>
 * Note on usage: {@link SafeStyles} should be used to ensure user input is not
 * executed in the browser. {@link SafeStyles} should not be used to sanitize
 * input before sending it to the server: The server cannot rely on the type
 * contract of {@link SafeStyles} values received from clients, because a
 * malicious client could provide maliciously crafted serialized forms of
 * implementations of this type that violate the type contract.
 * 
 * <p>
 * All implementing classes must maintain the class invariant (by design and
 * implementation and/or convention of use), that invoking {@link #asString()}
 * on any instance will return a string that is safe to assign to a CSS
 * attribute in a browser, in the sense that doing so must not cause execution
 * of script in the browser. Generally, {@link SafeStyles} should be of the form
 * {@code cssPropertyName:value;}, where neither the name nor the value contain
 * malicious scripts.
 * 
 * <p>
 * {@link SafeStyles} may never contain literal angle brackets. Otherwise, it
 * could be unsafe to place a {@link SafeStyles} into a &lt;style&gt; tag (where
 * it can't be HTML escaped). For example, if the {@link SafeStyles} containing
 * "<code>font: 'foo &lt;style&gt;&lt;script&gt;evil&lt;/script&gt;</code>'" is
 * used in a style sheet in a &lt;style&gt; tag, this could then break out of
 * the style context into HTML.
 * 
 * <p>
 * {@link SafeStyles} may contain literal single or double quotes, and as such
 * the entire style string must be escaped when used in a style attribute (if
 * this were not the case, the string could contain a matching quote that would
 * escape from the style attribute).
 * 
 * <p>
 * Furthermore, values of this type must be composable, i.e. for any two values
 * {@code A} and {@code B} of this type, {@code A.asString() + B.asString()}
 * must itself be a value that satisfies the {@link SafeStyles} type constraint.
 * This requirement implies that for any value {@code A} of this type,
 * {@code A.asString()} must not end in a "CSS value" or "CSS name" context. For
 * example, a value of {@code background:url("} or {@code font-} would not
 * satisfy the {@link SafeStyles} contract. This is because concatenating such
 * strings with a second value that itself does not contain unsafe CSS can
 * result in an overall string that does. For example, if
 * {@code javascript:evil())"} is appended to {@code background:url("}, the
 * resulting string may result in the execution of a malicious script.
 * 
 * <p>
 * The following example values comply with this type's contract:
 * <ul>
 * <li><code>width: 1em;</code></li>
 * <li><code>height:1em;</code></li>
 * <li><code>width: 1em;height: 1em;</code></li>
 * <li><code>background:url('http://url');</code></li>
 * </ul>
 * In addition, the empty string is safe for use in a CSS attribute.
 * 
 * <p>
 * The following example values do <em>not</em> comply with this type's contract:
 * <ul>
 * <li><code>background: red</code> (missing a trailing semi-colon)</li>
 * <li><code>background:</code> (missing a value and a trailing semi-colon)</li>
 * <li><code>1em</code> (missing an attribute name, which provides context for the value)</li>
 * </ul>
 * 
 * <p>
 * All implementations must implement equals() and hashCode() to behave
 * consistently with the result of asString().equals() and asString.hashCode().
 * 
 * <p>
 * Implementations must not return {@code null} from {@link #asString()}.
 */
public interface SafeStyles extends Serializable {
  /*
   * Notes regarding serialization:
   * 
   * - It may be reasonable to allow deserialization on the client of objects
   * serialized on the server (i.e. RPC responses), based on the assumption that
   * server code is trusted and would not provide a malicious serialized form
   * (if a MitM were able to modify server responses, the client would be fully
   * compromised in any case). However, the GWT RPC framework currently does not
   * seem to provide a facility for restricting deserialization on the Server
   * only (though this shouldn't be difficult to implement through a custom
   * SerializationPolicy)
   * 
   * - Some implementations of SafeStyles would in principle be able to enforce
   * their class invariant on deserialization. However, the GWT RPC framework
   * does not provide for an equivalent of readResolve() to enforce the class
   * invariant on deserialization.
   */

  /**
   * Returns this object's contained CSS as a string.
   * 
   * <p>
   * Based on this class' contract, the returned value will be non-null and a
   * string that is safe to use in an CSS attribute context.
   * 
   * @return the contents as a String
   */
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
