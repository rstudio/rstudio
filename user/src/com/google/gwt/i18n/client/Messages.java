/*
 * Copyright 2006 Google Inc.
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

package com.google.gwt.i18n.client;

/**
 * A tag interface that facilitates locale-sensitive, compile-time binding of
 * messages supplied from properties files. Using
 * <code>GWT.create(<i>class</i>)</code> to "instantiate" an interface that
 * extends <code>Messages</code> returns an instance of an automatically
 * generated subclass that is implemented using message templates from a
 * property file selected based on locale. Message templates are based on a
 * subset of the format used by <a
 * href="http://java.sun.com/j2se/1.4.2/docs/api/java/text/MessageFormat.html"><code>MessageFormat</code></a>.
 * 
 * <p>
 * Locale is specified at run time using a meta tag or query string as described
 * for {@link com.google.gwt.i18n.client.Localizable}.
 * </p>
 * 
 * <h3>Extending <code>Messages</code></h3>
 * To use <code>Messages</code>, begin by defining an interface that extends
 * it. Each interface method is referred to as a <i>message accessor</i>, and
 * the name of each message accessor is assumed to match the name of a property
 * defined in a properties file. For example,
 * 
 * {@example com.google.gwt.examples.i18n.GameStatusMessages}
 * 
 * expects to find properties named <code>turnsLeft</code> and
 * <code>currentScore</code> in an associated properties file, formatted as
 * message templates taking two arguments and one argument, respectively. For
 * example, the following properties would correctly bind to the
 * <code>GameStatusMessages</code> interface:
 * 
 * {@gwt.include com/google/gwt/examples/i18n/GameStatusMessages.properties}
 * 
 * <p>
 * The following example demonstrates how to use constant accessors defined in
 * the interface above:
 * 
 * {@example com.google.gwt.examples.i18n.GameStatusMessagesExample#beginNewGameRound(String)}
 * </p>
 * 
 * <p>
 * It is possible to change the property name bound to a message accessor using
 * the <code>gwt.key</code> doc comment, exactly as is done for constant
 * accessors. See {@link Constants} for an example.
 * </p>
 * 
 * <h3>Defining Message Accessors</h3>
 * Message accessors must be of the form
 * 
 * <pre>String methodName(<i>optional-params</i>)</pre>
 * 
 * and parameters may be of any type. Arguments are converted into strings at
 * runtime using Java string concatenation syntax (the '+' operator), which
 * uniformly handles primitives, <code>null</code>, and invoking
 * <code>toString()</code> to format objects.
 * 
 * <p>
 * Compile-time checks are performed to ensure that the number of placeholders
 * in a message template (e.g. <code>{0}</code>) matches the number of
 * parameters supplied.
 * </p>
 * 
 * <h3>Binding to Properties Files</h3>
 * Interfaces extending <code>Messages</code> are bound to properties file
 * using the same algorithm as interfaces extending <code>Constants</code>.
 * See the documentation for {@link Constants} for a description of the
 * algorithm.
 * 
 * <h3>Required Module</h3>
 * Modules that use this interface should inherit
 * <code>com.google.gwt.i18n.I18N</code>.
 * 
 * {@gwt.include com/google/gwt/examples/i18n/InheritsExample.gwt.xml}
 * 
 * <h3>Note</h3>
 * You should not directly implement this interface or interfaces derived from
 * it since an implementation is generated automatically when message interfaces
 * are created using {@link com.google.gwt.core.client.GWT#create(Class)}.
 */
public interface Messages extends Localizable {
}
