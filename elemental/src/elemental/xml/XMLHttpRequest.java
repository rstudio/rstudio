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
package elemental.xml;
import elemental.events.EventTarget;
import elemental.html.ArrayBuffer;
import elemental.html.Blob;
import elemental.html.FormData;
import elemental.events.EventListener;
import elemental.dom.Document;
import elemental.events.Event;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p><code>XMLHttpRequest</code> is a <a class="internal" title="En/JavaScript" rel="internal" href="https://developer.mozilla.org/en/JavaScript">JavaScript</a> object that was designed by Microsoft and adopted by Mozilla, Apple, and Google. It's now being <a class="external" title="http://www.w3.org/TR/XMLHttpRequest/" rel="external" href="http://www.w3.org/TR/XMLHttpRequest/" target="_blank">standardized in the W3C</a>. It provides an easy way to retrieve data at a URL. Despite its name, <code>XMLHttpRequest</code> can be used to retrieve any type of data, not just XML, and it supports protocols other than <a title="en/HTTP" rel="internal" href="https://developer.mozilla.org/en/HTTP">HTTP</a> (including <code>file</code> and <code>ftp</code>).</p>
<p>To create an instance of <code>XMLHttpRequest</code>, simply do this:</p>
<pre>var req = new XMLHttpRequest();
</pre>
<p>For details about how to use <code>XMLHttpRequest</code>, see <a class="internal" title="En/Using XMLHttpRequest" rel="internal" href="https://developer.mozilla.org/en/DOM/XMLHttpRequest/Using_XMLHttpRequest">Using XMLHttpRequest</a>.</p>
  */
public interface XMLHttpRequest extends EventTarget {

  /**
    * The operation is complete.
    */

    static final int DONE = 4;

  /**
    * <code>send()</code> has been called, and headers and status are available.
    */

    static final int HEADERS_RECEIVED = 2;

  /**
    * Downloading; <code>responseText</code> holds partial data.
    */

    static final int LOADING = 3;

  /**
    * <code>send()</code>has not been called yet.
    */

    static final int OPENED = 1;

  /**
    * <code>open()</code>has not been called yet.
    */

    static final int UNSENT = 0;

  boolean isAsBlob();

  void setAsBlob(boolean arg);

  EventListener getOnabort();

  void setOnabort(EventListener arg);

  EventListener getOnerror();

  void setOnerror(EventListener arg);

  EventListener getOnload();

  void setOnload(EventListener arg);

  EventListener getOnloadend();

  void setOnloadend(EventListener arg);

  EventListener getOnloadstart();

  void setOnloadstart(EventListener arg);

  EventListener getOnprogress();

  void setOnprogress(EventListener arg);

  EventListener getOnreadystatechange();

  void setOnreadystatechange(EventListener arg);


  /**
    * <p>The state of the request:</p> <table class="standard-table"> <tbody> <tr> <td class="header">Value</td> <td class="header">State</td> <td class="header">Description</td> </tr> <tr> <td><code>0</code></td> <td><code>UNSENT</code></td> <td><code>open()</code>has not been called yet.</td> </tr> <tr> <td><code>1</code></td> <td><code>OPENED</code></td> <td><code>send()</code>has not been called yet.</td> </tr> <tr> <td><code>2</code></td> <td><code>HEADERS_RECEIVED</code></td> <td><code>send()</code> has been called, and headers and status are available.</td> </tr> <tr> <td><code>3</code></td> <td><code>LOADING</code></td> <td>Downloading; <code>responseText</code> holds partial data.</td> </tr> <tr> <td><code>4</code></td> <td><code>DONE</code></td> <td>The operation is complete.</td> </tr> </tbody> </table>
    */
  int getReadyState();


  /**
    * The response entity body according to <code><a href="#responseType">responseType</a></code>, as an <a title="en/JavaScript typed arrays/ArrayBuffer" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBuffer"><code>ArrayBuffer</code></a>, <a title="en/DOM/Blob" rel="internal" href="https://developer.mozilla.org/en/DOM/Blob"><code>Blob</code></a>, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Document">Document</a></code>
, JavaScript object (for "moz-json"), or string. This is <code>NULL</code>&nbsp;if the request is not complete or was not successful.
    */
  Object getResponse();

  Blob getResponseBlob();


  /**
    * The response to the request as text, or <code>null</code> if the request was unsuccessful or has not yet been sent. <strong>Read-only.</strong>
    */
  String getResponseText();


  /**
    * <p>Can be set to change the response type. This tells the server what format you want the response to be in.</p> <table class="standard-table"> <tbody> <tr> <td class="header">Value</td> <td class="header">Data type of <code>response</code> property</td> </tr> <tr> <td><em>empty string</em></td> <td>String (this is the default)</td> </tr> <tr> <td>"arraybuffer"</td> <td><a title="en/JavaScript typed arrays/ArrayBuffer" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBuffer"><code>ArrayBuffer</code></a></td> </tr> <tr> <td>"blob"</td> <td><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Blob">Blob</a></code>
</td> </tr> <tr> <td>"document"</td> <td><code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Document">Document</a></code>
</td> </tr> <tr> <td>"text"</td> <td>String</td> </tr> <tr> <td>"moz-json"</td> <td>JavaScript object, parsed from a JSON string returned by the server 
<span title="(Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
">Requires Gecko 9.0</span>
</td> </tr> </tbody> </table>
    */
  String getResponseType();

  void setResponseType(String arg);


  /**
    * <p>The response to the request as a DOM <code><a class="internal" title="En/DOM/Document" rel="internal" href="https://developer.mozilla.org/en/DOM/document">Document</a></code> object, or <code>null</code> if the request was unsuccessful, has not yet been sent, or cannot be parsed as XML. The response is parsed as if it were a <code>text/xml</code> stream. <strong>Read-only.</strong></p> <div class="note"><strong>Note:</strong> If the server doesn't apply the <code>text/xml</code> Content-Type header, you can use <code>overrideMimeType()</code>to force <code>XMLHttpRequest</code> to parse it as XML anyway.</div>
    */
  Document getResponseXML();


  /**
    * The status of the response to the request. This is the HTTP result code (for example, <code>status</code> is 200 for a successful request). <strong>Read-only.</strong>
    */
  int getStatus();


  /**
    * The response string returned by the HTTP server. Unlike <code>status</code>, this includes the entire text of the response message ("<code>200 OK</code>", for example). <strong>Read-only.</strong>
    */
  String getStatusText();


  /**
    * The upload process can be tracked by adding an event listener to <code>upload</code>. 
<span>New in <a rel="custom" href="https://developer.mozilla.org/en/Firefox_3.5_for_developers">Firefox 3.5</a></span>
    */
  XMLHttpRequestUpload getUpload();


  /**
    * <p>Indicates whether or not cross-site Access-Control requests should be made using credentials such as cookies or authorization headers. 
<span>New in <a rel="custom" href="https://developer.mozilla.org/en/Firefox_3.5_for_developers">Firefox 3.5</a></span>
</p> <div class="note"><strong>Note:</strong> This never affects same-site requests.</div> <p>The default is <code>false</code>.</p>
    */
  boolean isWithCredentials();

  void setWithCredentials(boolean arg);


  /**
    * Aborts the request if it has already been sent.
    */
  void abort();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  boolean dispatchEvent(Event evt);


  /**
    * <pre>string getAllResponseHeaders();
</pre>
<p>Returns all the response headers as a string, or <code>null</code> if no response has been received.<strong> Note:</strong> For multipart requests, this returns the headers from the <em>current</em> part of the request, not from the original channel.</p>
    */
  String getAllResponseHeaders();


  /**
    * Returns the string containing the text of the specified header, or <code>null</code> if either the response has not yet been received or the header doesn't exist in the response.
    */
  String getResponseHeader(String header);


  /**
    * <p>Initializes a request. This method is to be used from JavaScript code; to initialize a request from native code, use <a class="internal" title="/en/XMLHttpRequest#openRequest()" rel="internal" href="https://developer.mozilla.org/en/nsIXMLHttpRequest#openRequest()"><code>openRequest()</code></a>instead.</p>
<div class="note"><strong>Note:</strong> Calling this method an already active request (one for which <code>open()</code>or <code>openRequest()</code>has already been called) is the equivalent of calling <code>abort()</code>.</div>

<div id="section_9"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>method</code></dt> <dd>The HTTP method to use; either "POST" or "GET". Ignored for non-HTTP(S) URLs.</dd> <dt><code>url</code></dt> <dd>The URL to which to send the request.</dd> <dt><code>async</code></dt> <dd>An optional boolean parameter, defaulting to <code>true</code>, indicating whether or not to perform the operation asynchronously. If this value is <code>false</code>, the <code>send()</code>method does not return until the response is received. If <code>true</code>, notification of a completed transaction is provided using event listeners. This <em>must</em> be true if the <code>multipart</code> attribute is <code>true</code>, or an exception will be thrown.</dd> <dt><code>user</code></dt> <dd>The optional user name to use for authentication purposes; by default, this is an empty string.</dd> <dt><code>password</code></dt> <dd>The optional password to use for authentication purposes; by default, this is an empty string.</dd>
</dl>
<p></p></div>
    */
  void open(String method, String url);


  /**
    * <p>Initializes a request. This method is to be used from JavaScript code; to initialize a request from native code, use <a class="internal" title="/en/XMLHttpRequest#openRequest()" rel="internal" href="https://developer.mozilla.org/en/nsIXMLHttpRequest#openRequest()"><code>openRequest()</code></a>instead.</p>
<div class="note"><strong>Note:</strong> Calling this method an already active request (one for which <code>open()</code>or <code>openRequest()</code>has already been called) is the equivalent of calling <code>abort()</code>.</div>

<div id="section_9"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>method</code></dt> <dd>The HTTP method to use; either "POST" or "GET". Ignored for non-HTTP(S) URLs.</dd> <dt><code>url</code></dt> <dd>The URL to which to send the request.</dd> <dt><code>async</code></dt> <dd>An optional boolean parameter, defaulting to <code>true</code>, indicating whether or not to perform the operation asynchronously. If this value is <code>false</code>, the <code>send()</code>method does not return until the response is received. If <code>true</code>, notification of a completed transaction is provided using event listeners. This <em>must</em> be true if the <code>multipart</code> attribute is <code>true</code>, or an exception will be thrown.</dd> <dt><code>user</code></dt> <dd>The optional user name to use for authentication purposes; by default, this is an empty string.</dd> <dt><code>password</code></dt> <dd>The optional password to use for authentication purposes; by default, this is an empty string.</dd>
</dl>
<p></p></div>
    */
  void open(String method, String url, boolean async);


  /**
    * <p>Initializes a request. This method is to be used from JavaScript code; to initialize a request from native code, use <a class="internal" title="/en/XMLHttpRequest#openRequest()" rel="internal" href="https://developer.mozilla.org/en/nsIXMLHttpRequest#openRequest()"><code>openRequest()</code></a>instead.</p>
<div class="note"><strong>Note:</strong> Calling this method an already active request (one for which <code>open()</code>or <code>openRequest()</code>has already been called) is the equivalent of calling <code>abort()</code>.</div>

<div id="section_9"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>method</code></dt> <dd>The HTTP method to use; either "POST" or "GET". Ignored for non-HTTP(S) URLs.</dd> <dt><code>url</code></dt> <dd>The URL to which to send the request.</dd> <dt><code>async</code></dt> <dd>An optional boolean parameter, defaulting to <code>true</code>, indicating whether or not to perform the operation asynchronously. If this value is <code>false</code>, the <code>send()</code>method does not return until the response is received. If <code>true</code>, notification of a completed transaction is provided using event listeners. This <em>must</em> be true if the <code>multipart</code> attribute is <code>true</code>, or an exception will be thrown.</dd> <dt><code>user</code></dt> <dd>The optional user name to use for authentication purposes; by default, this is an empty string.</dd> <dt><code>password</code></dt> <dd>The optional password to use for authentication purposes; by default, this is an empty string.</dd>
</dl>
<p></p></div>
    */
  void open(String method, String url, boolean async, String user);


  /**
    * <p>Initializes a request. This method is to be used from JavaScript code; to initialize a request from native code, use <a class="internal" title="/en/XMLHttpRequest#openRequest()" rel="internal" href="https://developer.mozilla.org/en/nsIXMLHttpRequest#openRequest()"><code>openRequest()</code></a>instead.</p>
<div class="note"><strong>Note:</strong> Calling this method an already active request (one for which <code>open()</code>or <code>openRequest()</code>has already been called) is the equivalent of calling <code>abort()</code>.</div>

<div id="section_9"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>method</code></dt> <dd>The HTTP method to use; either "POST" or "GET". Ignored for non-HTTP(S) URLs.</dd> <dt><code>url</code></dt> <dd>The URL to which to send the request.</dd> <dt><code>async</code></dt> <dd>An optional boolean parameter, defaulting to <code>true</code>, indicating whether or not to perform the operation asynchronously. If this value is <code>false</code>, the <code>send()</code>method does not return until the response is received. If <code>true</code>, notification of a completed transaction is provided using event listeners. This <em>must</em> be true if the <code>multipart</code> attribute is <code>true</code>, or an exception will be thrown.</dd> <dt><code>user</code></dt> <dd>The optional user name to use for authentication purposes; by default, this is an empty string.</dd> <dt><code>password</code></dt> <dd>The optional password to use for authentication purposes; by default, this is an empty string.</dd>
</dl>
<p></p></div>
    */
  void open(String method, String url, boolean async, String user, String password);


  /**
    * Overrides the MIME type returned by the server. This may be used, for example, to force a stream to be treated and parsed as text/xml, even if the server does not report it as such.This method must be called before <code>send()</code>.
    */
  void overrideMimeType(String override);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);


  /**
    * <p>Sends the request. If the request is asynchronous (which is the default), this method returns as soon as the request is sent. If the request is synchronous, this method doesn't return until the response has arrived.</p>
<div class="note"><strong>Note:</strong> Any event listeners you wish to set must be set before calling <code>send()</code>.</div>

<div id="section_11"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>body</code></dt> <dd>This may be an <code>nsIDocument</code>, <code>nsIInputStream</code>, or a string (an <code>nsISupportsString</code> if called from native code) that is used to populate the body of a POST request. Starting with Gecko 1.9.2, you may also specify an DOM<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
 , and starting with Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
 you may also specify a <a title="en/XMLHttpRequest/FormData" rel="internal" href="https://developer.mozilla.org/en/DOM/XMLHttpRequest/FormData"><code>FormData</code></a> object.</dd>
</dl>
</div>
    */
  void send();


  /**
    * <p>Sends the request. If the request is asynchronous (which is the default), this method returns as soon as the request is sent. If the request is synchronous, this method doesn't return until the response has arrived.</p>
<div class="note"><strong>Note:</strong> Any event listeners you wish to set must be set before calling <code>send()</code>.</div>

<div id="section_11"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>body</code></dt> <dd>This may be an <code>nsIDocument</code>, <code>nsIInputStream</code>, or a string (an <code>nsISupportsString</code> if called from native code) that is used to populate the body of a POST request. Starting with Gecko 1.9.2, you may also specify an DOM<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
 , and starting with Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
 you may also specify a <a title="en/XMLHttpRequest/FormData" rel="internal" href="https://developer.mozilla.org/en/DOM/XMLHttpRequest/FormData"><code>FormData</code></a> object.</dd>
</dl>
</div>
    */
  void send(ArrayBuffer data);


  /**
    * <p>Sends the request. If the request is asynchronous (which is the default), this method returns as soon as the request is sent. If the request is synchronous, this method doesn't return until the response has arrived.</p>
<div class="note"><strong>Note:</strong> Any event listeners you wish to set must be set before calling <code>send()</code>.</div>

<div id="section_11"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>body</code></dt> <dd>This may be an <code>nsIDocument</code>, <code>nsIInputStream</code>, or a string (an <code>nsISupportsString</code> if called from native code) that is used to populate the body of a POST request. Starting with Gecko 1.9.2, you may also specify an DOM<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
 , and starting with Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
 you may also specify a <a title="en/XMLHttpRequest/FormData" rel="internal" href="https://developer.mozilla.org/en/DOM/XMLHttpRequest/FormData"><code>FormData</code></a> object.</dd>
</dl>
</div>
    */
  void send(Blob data);


  /**
    * <p>Sends the request. If the request is asynchronous (which is the default), this method returns as soon as the request is sent. If the request is synchronous, this method doesn't return until the response has arrived.</p>
<div class="note"><strong>Note:</strong> Any event listeners you wish to set must be set before calling <code>send()</code>.</div>

<div id="section_11"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>body</code></dt> <dd>This may be an <code>nsIDocument</code>, <code>nsIInputStream</code>, or a string (an <code>nsISupportsString</code> if called from native code) that is used to populate the body of a POST request. Starting with Gecko 1.9.2, you may also specify an DOM<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
 , and starting with Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
 you may also specify a <a title="en/XMLHttpRequest/FormData" rel="internal" href="https://developer.mozilla.org/en/DOM/XMLHttpRequest/FormData"><code>FormData</code></a> object.</dd>
</dl>
</div>
    */
  void send(Document data);


  /**
    * <p>Sends the request. If the request is asynchronous (which is the default), this method returns as soon as the request is sent. If the request is synchronous, this method doesn't return until the response has arrived.</p>
<div class="note"><strong>Note:</strong> Any event listeners you wish to set must be set before calling <code>send()</code>.</div>

<div id="section_11"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>body</code></dt> <dd>This may be an <code>nsIDocument</code>, <code>nsIInputStream</code>, or a string (an <code>nsISupportsString</code> if called from native code) that is used to populate the body of a POST request. Starting with Gecko 1.9.2, you may also specify an DOM<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
 , and starting with Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
 you may also specify a <a title="en/XMLHttpRequest/FormData" rel="internal" href="https://developer.mozilla.org/en/DOM/XMLHttpRequest/FormData"><code>FormData</code></a> object.</dd>
</dl>
</div>
    */
  void send(String data);


  /**
    * <p>Sends the request. If the request is asynchronous (which is the default), this method returns as soon as the request is sent. If the request is synchronous, this method doesn't return until the response has arrived.</p>
<div class="note"><strong>Note:</strong> Any event listeners you wish to set must be set before calling <code>send()</code>.</div>

<div id="section_11"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>body</code></dt> <dd>This may be an <code>nsIDocument</code>, <code>nsIInputStream</code>, or a string (an <code>nsISupportsString</code> if called from native code) that is used to populate the body of a POST request. Starting with Gecko 1.9.2, you may also specify an DOM<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
 , and starting with Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
 you may also specify a <a title="en/XMLHttpRequest/FormData" rel="internal" href="https://developer.mozilla.org/en/DOM/XMLHttpRequest/FormData"><code>FormData</code></a> object.</dd>
</dl>
</div>
    */
  void send(FormData data);


  /**
    * <p>Sets the value of an HTTP request header.You must call <a class="internal" title="/en/XMLHttpRequest#open()" rel="internal" href="https://developer.mozilla.org/en/nsIXMLHttpRequest#open()"><code>open()</code></a>before using this method.</p>

<div id="section_15"><span id="Parameters_5"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>header</code></dt> <dd>The name of the header whose value is to be set.</dd> <dt><code>value</code></dt> <dd>The value to set as the body of the header.</dd>
</dl>
</div>
    */
  void setRequestHeader(String header, String value);
}
