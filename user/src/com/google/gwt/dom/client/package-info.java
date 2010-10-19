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
 * Classes for low-level DOM programming.
 * 
 * This package contains classes that expose the W3C standard HTML document object model for programmatic access 
 * and manipulation of HTML pages directly in client-side Java source, accounting for most browser variations.
 * These classes provide an efficient, type-safe, and IDE-friendly alternative to writing 
 * JavaScript Native Interface (JSNI) methods for many common tasks.
 * 
 * <p>
 * These classes extend {@link com.google.gwt.core.client.JavaScriptObject}, which enables them to be
 * used directly as Java types without introducing any object-oriented size or speed overhead beyond the 
 * underlying JavaScript objects they represent. 
 * Consequently, these DOM classes are efficient enough to be used directly when maximum performance is required 
 * and are lightweight enough to serve as the basic building blocks upon which widget libraries may be constructed.
 */
@com.google.gwt.util.PreventSpuriousRebuilds
package com.google.gwt.dom.client;
