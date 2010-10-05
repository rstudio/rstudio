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
package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A {@link com.google.gwt.user.client.ui.SuggestOracle} can be used to create
 * suggestions associated with a specific query string. It is currently used by
 * {@link SuggestBox}.
 * 
 * @see SuggestBox
 */
public abstract class SuggestOracle { 
  private Response emptyResponse = new Response(new ArrayList<Suggestion>());
  /**
   * Callback for {@link com.google.gwt.user.client.ui.SuggestOracle}. Every
   * {@link Request} should be associated with a callback that should be called
   * after a {@link Response} is generated.
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
    void onSuggestionsReady(Request request, Response response);
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
   * 
   * <p>Can optionally have truncation information provided. To indicate that
   * there are more results but the number is not known, use:
   * 
   * <p><code>response.setMoreSuggestions(true);</code>
   * 
   * <p>Or to indicate more results with an exact number, use:
   * 
   * <p><code>response.setMoreSuggestionsCount(102);</code>
   */
  public static class Response implements IsSerializable {
    private Collection<? extends Suggestion> suggestions;

    /**
     * The response is considered to have "more suggestions" when the number of 
     * matching suggestions exceeds {@link Request#getLimit}, so the response
     * suggestion list is truncated.
     */
    private boolean moreSuggestions = false;

    /**
     * Number of truncated suggestions.
     */
    private int numMoreSuggestions = 0;

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
    public Response(Collection<? extends Suggestion> suggestions) {
      setSuggestions(suggestions);
    }

    /**
     * Gets how many more suggestions there are.
     * 
     * @return the count. if there no more suggestions or the number of more
     *         suggestions is unknown, returns 0.
     */
    public int getMoreSuggestionsCount() {
      return this.numMoreSuggestions;
    }
    
    /**
     * Gets the collection of suggestions. Each suggestion must implement the
     * {@link Suggestion} interface.
     * 
     * @return the collection of suggestions
     */
    public Collection<? extends Suggestion> getSuggestions() {
      return this.suggestions;
    }

    /**
     * Gets whether or not the suggestion list was truncated due to the
     * {@link Request#getLimit}.
     */
    public boolean hasMoreSuggestions() {
      return this.moreSuggestions;
    }

    /**
     * Sets whether or not the suggestion list was truncated due to the
     * {@link Request#getLimit}.
     */
    public void setMoreSuggestions(boolean moreSuggestions) {
      this.moreSuggestions = moreSuggestions;
    }

    /**
     * Sets whether or not the suggestion list was truncated due to the
     * {@link Request#getLimit}, by providing an exact count of remaining 
     * suggestions.
     * 
     * @param count number of truncated suggestions. Pass 0 to indicate there
     *        are no other suggestions, which is equivalent to 
     *        {@link #setMoreSuggestions(boolean) setMoreSuggestions(false)}.
     */
    public void setMoreSuggestionsCount(int count) {
      this.numMoreSuggestions = count;
      this.moreSuggestions = (count > 0);
    }

    /**
     * Sets the suggestions for this response. Each suggestion must implement
     * the {@link Suggestion} interface.
     * 
     * @param suggestions the suggestions
     */
    public void setSuggestions(Collection<? extends Suggestion> suggestions) {
      this.suggestions = suggestions;
    }
  }

  /**
   * Suggestion supplied by the
   * {@link com.google.gwt.user.client.ui.SuggestOracle}. Each suggestion has a
   * display string and a replacement string. The display string is what is
   * shown in the SuggestBox's list of suggestions. The interpretation of the
   * display string depends upon the value of its oracle's
   * {@link SuggestOracle#isDisplayStringHTML()}. The replacement string is the
   * string that is entered into the SuggestBox's text box when the suggestion
   * is selected from the list.
   * 
   * <p>
   * Replacement strings are useful when the display form of a suggestion
   * differs from the input format for the data. For example, suppose that a
   * company has a webpage with a form which requires the user to enter the
   * e-mail address of an employee. Since users are likely to know the name of
   * the employee, a SuggestBox is used to provide name suggestions as the user
   * types. When the user types the letter <i>f</i>, a suggestion with the
   * display string <i>foo bar</i> appears. When the user chooses this
   * suggestion, the replacement string, <i>foobar@company.com</i>, is entered
   * into the SuggestBox's text box.
   * </p>
   * 
   * <p>
   * This is an example where the input data format for the suggestion is not as
   * user-friendly as the display format. In the event that the display of a
   * suggestion exactly matches the input data format, the
   * <code>Suggestion</code> interface would be implemented in such a way that
   * the display string and replacement string would be identical.
   * </p>
   * 
   * <h3>Associating Data Transfer Objects (DTOs) with Suggestion Objects</h3>
   * Some applications retrieve suggesstions from a server, and may want to send
   * back a DTO with each suggestion. In the previous example, a DTO returned
   * with the suggestion may provide additional contact information about the
   * selected employee, and this information could be used to fill out other
   * fields on the form. To send back a DTO with each suggestion, extend the
   * <code>Suggestion</code> interface and define a getter method that has a
   * return value of the DTO's type. Define a class that implements this
   * subinterface and use it to encapsulate each suggestion.
   * 
   * <p>
   * To access a suggestion's DTO when the suggestion is selected, add a
   * {@link com.google.gwt.event.dom.client.ChangeHandler} to the SuggestBox
   * (see SuggestBox's documentation for more information). In the
   * <code>SuggestionHandler.onSuggestionSelected(SuggestionEvent event)</code>
   * method, obtain the selected <code>Suggestion</code> object from the
   * {@link com.google.gwt.event.dom.client.ChangeHandler} object, and downcast
   * the <code>Suggestion</code> object to the subinterface. Then, acces the DTO
   * using the DTO getter method that was defined on the subinterface.
   * </p>
   */
  public interface Suggestion {
    /**
     * Gets the display string associated with this suggestion. The
     * interpretation of the display string depends upon the value of its
     * oracle's {@link SuggestOracle#isDisplayStringHTML()}.
     * 
     * @return the display string for this suggestion
     */
    String getDisplayString();

    /**
     * Gets the replacement string associated with this suggestion. When this
     * suggestion is selected, the replacement string will be entered into the
     * SuggestBox's text box.
     * 
     * @return the string to be entered into the SuggestBox's text box when this
     *         suggestion is selected
     */
    String getReplacementString();
  }

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
   * Generate a {@link Response} based on a default request. The request query
   * must be null as it represents the results the oracle should return based on
   * no query string.
   * <p>
   * After the {@link Response} is created, it is passed into
   * {@link Callback#onSuggestionsReady(com.google.gwt.user.client.ui.SuggestOracle.Request, com.google.gwt.user.client.ui.SuggestOracle.Response)}
   * .
   * </p>
   * 
   * @param request the request
   * @param callback the callback to use for the response
   */
  public void requestDefaultSuggestions(Request request, Callback callback) {
    assert (request.query == null);
    callback.onSuggestionsReady(request, emptyResponse);
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
}
