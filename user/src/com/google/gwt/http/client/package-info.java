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

/**
 * Provides the client-side classes and interfaces for making HTTP requests and 
 * processing the associated responses. 
 * 
 * <p>
 * Most applications will be interested in the {@link com.google.gwt.http.client.Request}, {@link com.google.gwt.http.client.RequestBuilder}, 
 * {@link com.google.gwt.http.client.RequestCallback} and {@link com.google.gwt.http.client.Response} classes.
 * </p>
 * 
 * <h2>Caveats</h2>
 * <h3>Same-Origin Security Policy</h3>
 * Modern web browsers restrict client-side scripts from accessing items outside
 * of their source origin.  This means that a script loaded from <code>www.foo.com</code> cannot access
 * content from <code>www.bar.com</code>.  For more details please see, <a
 * href="http://en.wikipedia.org/wiki/Same_origin_policy">Same-Origin Security
 * Policy</a>.
 * 
 * <h3>Pending Request Limit</h3>
 * Modern web browsers are limited to having only two HTTP requests outstanding at
 * any one time.  If your server experiences an error that prevents it from sending 
 * a response, it can tie up your outstanding requests.  If you are concerned about 
 * this, you can always set timeouts for the request via {@link com.google.gwt.http.client.RequestBuilder#setTimeoutMillis(int)}.
 * 
 * <h3>Required Module</h3>
 * Modules that use the classes and interfaces in this package should inherit
 * the <code>com.google.gwt.http.HTTP</code> module.
 * 
 * {@gwt.include com/google/gwt/examples/http/InheritsExample.gwt.xml}
 *  
 * <h2>Quick Howto's</h2>
 * <h3>How should I write a RequestCallback handler class?</h3>
 * The following code shows how a {@link com.google.gwt.http.client.RequestCallback} instance should be written.
 * {@example com.google.gwt.examples.http.client.RequestCallbackExample}
 * 
 * <h3>How do I make a GET request?</h3>
 * The following example demonstrates how to perform an HTTP GET request.
 * {@example com.google.gwt.examples.http.client.GetExample}
 * 
 * <h3>How do I make a POST request?</h3>
 * The following example demonstrates how to perform an HTTP POST request.
 * {@example com.google.gwt.examples.http.client.PostExample}
 * 
 * <h3>How do I use request timeouts?</h3>
 * The following example demonstrates how to use the timeout feature.
 * {@example com.google.gwt.examples.http.client.TimeoutExample}
 * 
 * <h3>How do I construct a string for use in a query or POST body?</h3>
 * The following example demonstrates how to build a x-www-form-urlencoded string that can be used as a query string or as the body of a POST request.
 * {@example com.google.gwt.examples.http.client.QueryAndFormDataExample}
 * 
 * <h3>How can I make a {@link com.google.gwt.http.client.RequestBuilder} send a request other than GET or POST?</h3>
 * The following example demonstrates how to allow an HTTP request other than a GET or a POST to be made.  <em>Beware: if you plan on supporting Safari, you cannot use this scheme.</em>
 * {@example com.google.gwt.examples.http.client.RequestBuilderForAnyHTTPMethodTypeExample}
 */
@com.google.gwt.util.PreventSpuriousRebuilds
package com.google.gwt.http.client;
