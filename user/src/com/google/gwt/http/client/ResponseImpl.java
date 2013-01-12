/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.http.client;

import com.google.gwt.xhr.client.XMLHttpRequest;

/**
 * A {@link Response} implementation based on a {@link XMLHttpRequest}.
 */
class ResponseImpl extends Response {

  private final XMLHttpRequest xmlHttpRequest;

  public ResponseImpl(XMLHttpRequest xmlHttpRequest) {
    this.xmlHttpRequest = xmlHttpRequest;

    assert isResponseReady();
  }

  @Override
  public String getHeader(String header) {
    StringValidator.throwIfEmptyOrNull("header", header);

    return xmlHttpRequest.getResponseHeader(header);
  }

  @Override
  public Header[] getHeaders() {
    String allHeaders = xmlHttpRequest.getAllResponseHeaders();
    String[] unparsedHeaders = allHeaders.split("\n");
    Header[] parsedHeaders = new Header[unparsedHeaders.length];

    for (int i = 0, n = unparsedHeaders.length; i < n; ++i) {
      String unparsedHeader = unparsedHeaders[i];

      if (unparsedHeader.length() == 0) {
        continue;
      }

      int endOfNameIdx = unparsedHeader.indexOf(':');
      if (endOfNameIdx < 0) {
        continue;
      }

      final String name = unparsedHeader.substring(0, endOfNameIdx).trim();
      final String value = unparsedHeader.substring(endOfNameIdx + 1).trim();
      Header header = new Header() {
        @Override
        public String getName() {
          return name;
        }

        @Override
        public String getValue() {
          return value;
        }

        @Override
        public String toString() {
          return name + " : " + value;
        }
      };

      parsedHeaders[i] = header;
    }

    return parsedHeaders;
  }

  @Override
  public String getHeadersAsString() {
    return xmlHttpRequest.getAllResponseHeaders();
  }

  @Override
  public int getStatusCode() {
    return xmlHttpRequest.getStatus();
  }

  @Override
  public String getStatusText() {
    return xmlHttpRequest.getStatusText();
  }

  @Override
  public String getText() {
    return xmlHttpRequest.getResponseText();
  }

  private boolean isResponseReady() {
    return xmlHttpRequest.getReadyState() == XMLHttpRequest.DONE;
  }
}
