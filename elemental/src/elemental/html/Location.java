/*
 * Copyright 2012 Google Inc.
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
package elemental.html;
import elemental.util.Indexable;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * Returns a <a href="#Location_object"> <code>Location</code> object</a>, which contains information about the URL of the document and provides methods for changing that URL. You can also assign to this property to load another URL.
  */
public interface Location {

  Indexable getAncestorOrigins();


  /**
    * the part of the URL that follows the # symbol, including the # symbol.<br> You can listen for the <a title="en/DOM/window.onhashchange" rel="internal" href="https://developer.mozilla.org/en/DOM/window.onhashchange">hashchange event</a> to get notified of changes to the hash in supporting browsers.
    */
  String getHash();

  void setHash(String arg);


  /**
    * the host name and port number.
    */
  String getHost();

  void setHost(String arg);


  /**
    * the host name (without the port number or square brackets).
    */
  String getHostname();

  void setHostname(String arg);


  /**
    * the entire URL.
    */
  String getHref();

  void setHref(String arg);

  String getOrigin();


  /**
    * the path (relative to the host).
    */
  String getPathname();

  void setPathname(String arg);


  /**
    * the port number of the URL.
    */
  String getPort();

  void setPort(String arg);


  /**
    * the protocol of the URL.
    */
  String getProtocol();

  void setProtocol(String arg);


  /**
    * the part of the URL that follows the&nbsp;? symbol, including the&nbsp;? symbol.
    */
  String getSearch();

  void setSearch(String arg);


  /**
    * Load the document at the provided URL.
    */
  void assign(String url);


  /**
    * Reload the document from the current URL. <code>forceget</code> is a boolean, which, when it is <code>true</code>, causes the page to always be reloaded from the server. If it is <code>false</code> or not specified, the browser may reload the page from its cache.
    */
  void reload();


  /**
    * Replace the current document with the one at the provided URL. The difference from the <code>assign()</code> method is that after using <code>replace()</code> the current page will not be saved in session history, meaning the user won't be able to use the Back button to navigate to it.
    */
  void replace(String url);
}
