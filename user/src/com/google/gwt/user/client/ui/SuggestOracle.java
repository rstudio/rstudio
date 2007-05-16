/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;

/**
 * A {@link com.google.gwt.user.client.ui.SuggestOracle} can be used to create
 * suggestions associated with a specific query string. It is currently used by
 * {@link SuggestBox}.
 * 
 * @see SuggestBox
 */
public abstract class SuggestOracle {

  /**
   * Constructor for {@link com.google.gwt.user.client.ui.SuggestOracle}.
   */
  public SuggestOracle() {
  }

  /**
   * Should {@link Suggestion} display strings be treated as HTML? If true, this
   * all suggestions' display strings will be interpreted as HTML, otherwise as
   * text.
   * 
   * @return by default, returns false
   */
  public boolean isDisplayStringHTML() {
    return false;
  }

  /**
   * Generate a {@link Response} based on a specific {@link Request}. After the
   * {@link Response} is created, it is passed into
   * {@link Callback#onSuggestionsReady(com.google.gwt.user.client.ui.SuggestOracle.Request, com.google.gwt.user.client.ui.SuggestOracle.Response)}.
   * 
   * @param request the request
   * @param callback the callback to use for the response
   */
  public abstract void requestSuggestions(Request request, Callback callback);

  /**
   * Callback for {@link com.google.gwt.user.client.ui.SuggestOracle}. Every
   * {@link Request} should be associated with a callback that should be called
   * after a {@link  Response} is generated.
   */
  public interface Callback {
    /**
     * Consume the suggestions created by a
     * {@link com.google.gwt.user.client.ui.SuggestOracle} in response to a
     * {@link Request}.
     * 
     * @param request the request
     * @param response the response
     */
    public void onSuggestionsReady(Request request, Response response);
  }

  /**
   * A {@link com.google.gwt.user.client.ui.SuggestOracle} request.
   */
  public static class Request implements IsSerializable {
    private int limit = 20;
    private String query;

    /**
     * Constructor for {@link Request}.
     */
    public Request() {
    }

    /**
     * Constructor for {@link Request}.
     * 
     * @param query the query string
     */
    public Request(String query) {
      setQuery(query);
    }

    /**
     * Constructor for {@link Request}.
     * 
     * @param query the query string
     * @param limit limit on the number of suggestions that should be created
     *          for this query
     */
    public Request(String query, int limit) {
      setQuery(query);
      setLimit(limit);
    }

    /**
     * Gets the limit on the number of suggestions that should be created.
     * 
     * @return the limit
     */
    public int getLimit() {
      return limit;
    }

    /**
     * Gets the query string.
     * 
     * @return the query string
     */
    public String getQuery() {
      return query;
    }

    /**
     * Sets the limit on the number of suggestions that should be created.
     * 
     * @param limit the limit
     */
    public void setLimit(int limit) {
      this.limit = limit;
    }

    /**
     * Sets the query string used for this request.
     * 
     * @param query the query string
     */
    public void setQuery(String query) {
      this.query = query;
    }
  }

  /**
   * {@link com.google.gwt.user.client.ui.SuggestOracle} response.
   */
  public static class Response implements IsSerializable {

    /**
     * @gwt.typeArgs <com.google.gwt.user.client.ui.SuggestOracle.Suggestion>
     */
    private Collection suggestions;

    /**
     * Constructor for {@link Response}.
     */
    public Response() {
    }

    /**
     * Constructor for {@link Response}.
     * 
     * @param suggestions each element of suggestions must implement the
     *          {@link Suggestion} interface
     */
    public Response(Collection suggestions) {
      setSuggestions(suggestions);
    }

    /**
     * Gets the collection of suggestions. Each suggestion must implement the
     * {@link Suggestion} interface.
     * 
     * @return the collection of suggestions
     */
    public Collection getSuggestions() {
      return this.suggestions;
    }

    /**
     * Sets the suggestions for this response. Each suggestion must implement
     * the {@link Suggestion} interface.
     * 
     * @param suggestions the suggestions
     */
    public void setSuggestions(Collection suggestions) {
      this.suggestions = suggestions;
    }
  }

  /**
   * Suggestion supplied by the
   * {@link com.google.gwt.user.client.ui.SuggestOracle}. Each suggestion has a
   * value and a display string. The interpretation of the display string
   * depends upon the value of its oracle's {@link SuggestOracle#isDisplayStringHTML()}.
   * 
   */
  public interface Suggestion {
    /**
     * Gets the display string associated with this suggestion. The
     * interpretation of the display string depends upon the value of
     * its oracle's {@link SuggestOracle#isDisplayStringHTML()}.
     * 
     * @return the display string
     */
    String getDisplayString();

    /**
     * Get the value associated with this suggestion.
     * 
     * @return the value
     */
    Object getValue();
  }
}
