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
package com.google.gwt.http.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to build a URL from components.
 * 
 * TODO(jlabanca): Add a constructor that parses an existing URL
 */
public class UrlBuilder {

  /**
   * The port to use when no port should be specified.
   */
  public static final int PORT_UNSPECIFIED = Integer.MIN_VALUE;

  /**
   * A mapping of query parameters to their values.
   */
  private Map<String, String[]> listParamMap = new HashMap<String, String[]>();

  private String protocol = "http";
  private String host = null;
  private int port = PORT_UNSPECIFIED;
  private String path = null;
  private String hash = null;

  /**
   * Build the URL and return it as an encoded string.
   * 
   * @return the encoded URL string
   */
  public String buildString() {
    StringBuilder url = new StringBuilder();

    // http://
    url.append(URL.encode(protocol)).append("://");

    // http://www.google.com
    if (host != null) {
      url.append(URL.encode(host));
    }

    // http://www.google.com:80
    if (port != PORT_UNSPECIFIED) {
      url.append(":").append(port);
    }

    // http://www.google.com:80/path/to/file.html
    if (path != null && !"".equals(path)) {
      url.append("/").append(URL.encode(path));
    }

    // Generate the query string.
    // http://www.google.com:80/path/to/file.html?k0=v0&k1=v1
    char prefix = '?';
    for (Map.Entry<String, String[]> entry : listParamMap.entrySet()) {
      for (String val : entry.getValue()) {
        url.append(prefix)
            .append(URL.encodeQueryString(entry.getKey()))
            .append('=');
        if (val != null) {
          // Also encodes +,& etc.
          url.append(URL.encodeQueryString(val));
        }
        prefix = '&';
      }
    }

    // http://www.google.com:80/path/to/file.html?k0=v0&k1=v1#token
    if (hash != null) {
      url.append("#").append(URL.encode(hash));
    }

    return url.toString();
  }

  /**
   * Remove a query parameter from the map.
   * 
   * @param name the parameter name
   */
  public UrlBuilder removeParameter(String name) {
    listParamMap.remove(name);
    return this;
  }

  /**
   * Set the hash portion of the location (ex. myAnchor or #myAnchor).
   * 
   * @param hash the hash
   */
  public UrlBuilder setHash(String hash) {
    if (hash != null && hash.startsWith("#")) {
      hash = hash.substring(1);
    }
    this.hash = hash;
    return this;
  }

  /**
   * Set the host portion of the location (ex. google.com). You can also specify
   * the port in this method (ex. localhost:8888).
   * 
   * @param host the host
   */
  public UrlBuilder setHost(String host) {
    // Extract the port from the host.
    if (host != null && host.contains(":")) {
      String[] parts = host.split(":");
      if (parts.length > 2) {
        throw new IllegalArgumentException(
            "Host contains more than one colon: " + host);
      }
      try {
        setPort(Integer.parseInt(parts[1]));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Could not parse port out of host: "
            + host);
      }
      host = parts[0];
    }
    this.host = host;
    return this;
  }

  /**
   * <p>
   * Set a query parameter to a list of values. Each value in the list will be
   * added as its own key/value pair.
   * 
   * <p>
   * <h3>Example Output</h3>
   * <code>?mykey=value0&mykey=value1&mykey=value2</code>
   * </p>
   * 
   * @param key the key
   * @param values the list of values
   */
  public UrlBuilder setParameter(String key, String... values) {
    assertNotNullOrEmpty(key, "Key cannot be null or empty", false);
    assertNotNull(values,
        "Values cannot null. Try using removeParameter instead.");
    if (values.length == 0) {
      throw new IllegalArgumentException(
          "Values cannot be empty.  Try using removeParameter instead.");
    }
    listParamMap.put(key, values);
    return this;
  }

  /**
   * Set the path portion of the location (ex. path/to/file.html).
   * 
   * @param path the path
   */
  public UrlBuilder setPath(String path) {
    if (path != null && path.startsWith("/")) {
      path = path.substring(1);
    }
    this.path = path;
    return this;
  }

  /**
   * Set the port to connect to.
   * 
   * @param port the port, or {@link #PORT_UNSPECIFIED}
   */
  public UrlBuilder setPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * Set the protocol portion of the location (ex. http).
   * 
   * @param protocol the protocol
   */
  public UrlBuilder setProtocol(String protocol) {
    assertNotNull(protocol, "Protocol cannot be null");
    if (protocol.endsWith("://")) {
      protocol = protocol.substring(0, protocol.length() - 3);
    } else if (protocol.endsWith(":/")) {
      protocol = protocol.substring(0, protocol.length() - 2);
    } else if (protocol.endsWith(":")) {
      protocol = protocol.substring(0, protocol.length() - 1);
    }
    if (protocol.contains(":")) {
      throw new IllegalArgumentException("Invalid protocol: " + protocol);
    }
    assertNotNullOrEmpty(protocol, "Protocol cannot be empty", false);
    this.protocol = protocol;
    return this;
  }

  /**
   * Assert that the value is not null.
   * 
   * @param value the value
   * @param message the message to include with any exceptions
   * @throws IllegalArgumentException if value is null
   */
  private void assertNotNull(Object value, String message)
      throws IllegalArgumentException {
    if (value == null) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Assert that the value is not null or empty.
   * 
   * @param value the value
   * @param message the message to include with any exceptions
   * @param isState if true, throw a state exception instead
   * @throws IllegalArgumentException if value is null
   * @throws IllegalStateException if value is null and isState is true
   */
  private void assertNotNullOrEmpty(String value, String message,
      boolean isState) throws IllegalArgumentException {
    if (value == null || value.length() == 0) {
      if (isState) {
        throw new IllegalStateException(message);
      } else {
        throw new IllegalArgumentException(message);
      }
    }
  }
}
