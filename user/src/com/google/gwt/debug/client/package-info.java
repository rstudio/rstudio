/*
 * Copyright 2009 Google Inc.
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
 * Provides low-level functionality to support the creation of testing and
 * diagnostic frameworks.
 * 
 * <p>
 * To use the functionality in this package, your GWT module should inherit the
 * module <code>com.google.gwt.debug.Debug</code>. The <code>Debug</code>
 * module introduces the client property <code>gwt.enableDebugId</code>,
 * which controls whether or not this debug code is enabled (and therefore
 * included in the final compiled result). It is set to <code>true</code> by
 * default, but a module being compiled for production would very likely want to
 * set it to <code>false</code> to avoid unnecessary extra code in the final
 * compiled output.
 * 
 * <h3>Example</h3>
 * A module using this package might look like the following:
 * 
 * <pre>
 * &lt;module>
 *   &lt;inherits name='com.google.gwt.user.User'/>
 *   
 *   &lt;!-- Inheriting 'Debug' on the next line makes the features available. -->
 *   &lt;inherits name='com.google.gwt.debug.Debug'/>
 *   
 *   &lt;!-- Disable for production by uncommenting the next line -->
 *   &lt;!-- &lt;set-property name="gwt.enableDebugId" value="false"/> -->
 *   
 *   &lt;entry-point class='your-entry-point-class'/>
 * &lt;/module>
 * </pre>
 */
@com.google.gwt.util.PreventSpuriousRebuilds
package com.google.gwt.debug.client;
