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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The default {@link com.google.gwt.user.client.ui.SuggestOracle}. The default
 * oracle returns potential suggestions based on breaking the query into
 * separate words and looking for matches. It also modifies the returned text to
 * show which prefix matched the query term. The matching is case insensitive.
 * All suggestions are sorted before being passed into a response.
 * <p>
 * Example Table
 * </p>
 * <p>
 * <table width = "100%" border = "1">
 * <tr>
 * <td><b> All Suggestions </b> </td>
 * <td><b>Query string</b> </td>
 * <td><b>Matching Suggestions</b></td>
 * </tr>
 * <tr>
 * <td> John Smith, Joe Brown, Jane Doe, Jane Smith, Bob Jones</td>
 * <td> Jo</td>
 * <td> John Smith, Joe Brown, Bob Jones</td>
 * </tr>
 * <tr>
 * <td> John Smith, Joe Brown, Jane Doe, Jane Smith, Bob Jones</td>
 * <td> Smith</td>
 * <td> John Smith, Jane Smith</td>
 * </tr>
 * <tr>
 * <td> Georgia, New York, California</td>
 * <td> g</td>
 * <td> Georgia</td>
 * </tr>
 * </table>
 * </p>
 */
public final class MultiWordSuggestOracle extends SuggestOracle {

  /**
   * Suggestion class for {@link MultiWordSuggestOracle}.
   */
  public static class MultiWordSuggestion implements Suggestion, IsSerializable {
    private String displayString;
    private String replacementString;

    /**
     * Constructor used by RPC.
     */
    public MultiWordSuggestion() {
    }

    /**
     * Constructor for <code>MultiWordSuggestion</code>.
     * 
     * @param replacementString the string to enter into the SuggestBox's text
     *          box if the suggestion is chosen
     * @param displayString the display string
     */
    public MultiWordSuggestion(String replacementString, String displayString) {
      this.replacementString = replacementString;
      this.displayString = displayString;
    }

    public String getDisplayString() {
      return displayString;
    }

    public String getReplacementString() {
      return replacementString;
    }
  }

  private static final char WHITESPACE_CHAR = ' ';
  private static final String WHITESPACE_STRING = " ";

  /**
   * Regular expression used to collapse all whitespace in a query string.
   */
  private static final String NORMALIZE_TO_SINGLE_WHITE_SPACE = "\\s+";

  private static HTML convertMe = new HTML();

  /**
   * Associates substrings with words.
   */
  private final PrefixTree tree = new PrefixTree();

  /**
   * Associates individual words with candidates.
   */
  private HashMap<String, Set<String>> toCandidates = new HashMap<String, Set<String>>();

  /**
   * Associates candidates with their formatted suggestions.
   */
  private HashMap<String, String> toRealSuggestions = new HashMap<String, String>();

  /**
   * The whitespace masks used to prevent matching and replacing of the given
   * substrings.
   */
  private char[] whitespaceChars;

  /**
   * Constructor for <code>MultiWordSuggestOracle</code>. This uses a space
   * as the whitespace character.
   * 
   * @see #MultiWordSuggestOracle(String)
   */
  public MultiWordSuggestOracle() {
    this(" ");
  }

  /**
   * Constructor for <code>MultiWordSuggestOracle</code> which takes in a set
   * of whitespace chars that filter its input.
   * <p>
   * Example: If <code>".,"</code> is passed in as whitespace, then the string
   * "foo.bar" would match the queries "foo", "bar", "foo.bar", "foo...bar", and
   * "foo, bar". If the empty string is used, then all characters are used in
   * matching. For example, the query "bar" would match "bar", but not "foo
   * bar".
   * </p>
   * 
   * @param whitespaceChars the characters to treat as word separators
   */
  public MultiWordSuggestOracle(String whitespaceChars) {
    this.whitespaceChars = new char[whitespaceChars.length()];
    for (int i = 0; i < whitespaceChars.length(); i++) {
      this.whitespaceChars[i] = whitespaceChars.charAt(i);
    }
  }

  /**
   * Adds a suggestion to the oracle. Each suggestion must be plain text.
   * 
   * @param suggestion the suggestion
   */
  public void add(String suggestion) {
    String candidate = normalizeSuggestion(suggestion);
    // candidates --> real suggestions.
    toRealSuggestions.put(candidate, suggestion);

    // word fragments --> candidates.
    String[] words = candidate.split(WHITESPACE_STRING);
    for (int i = 0; i < words.length; i++) {
      String word = words[i];
      tree.add(word);
      Set<String> l = toCandidates.get(word);
      if (l == null) {
        l = new HashSet<String>();
        toCandidates.put(word, l);
      }
      l.add(candidate);
    }
  }

  /**
   * Adds all suggestions specified. Each suggestion must be plain text.
   * 
   * @param collection the collection
   */
  public void addAll(Collection<String> collection) {
    for (String suggestion : collection) {
      add(suggestion);
    }
  }

  /**
   * Removes all of the suggestions from the oracle.
   */
  public void clear() {
    tree.clear();
    toCandidates.clear();
    toRealSuggestions.clear();
  }

  @Override
  public boolean isDisplayStringHTML() {
    return true;
  }

  @Override
  public void requestSuggestions(Request request, Callback callback) {
    final List<MultiWordSuggestion> suggestions = computeItemsFor(
        request.getQuery(), request.getLimit());
    Response response = new Response(suggestions);
    callback.onSuggestionsReady(request, response);
  }

  String escapeText(String escapeMe) {
    convertMe.setText(escapeMe);
    String escaped = convertMe.getHTML();
    return escaped;
  }

  /**
   * Compute the suggestions that are matches for a given query.
   * 
   * @param query search string
   * @param limit limit
   * @return matching suggestions
   */
  private List<MultiWordSuggestion> computeItemsFor(String query, int limit) {
    query = normalizeSearch(query);

    // Get candidates from search words.
    List<String> candidates = createCandidatesFromSearch(query, limit);

    // Convert candidates to suggestions.
    return convertToFormattedSuggestions(query, candidates);
  }

  /**
   * Returns real suggestions with the given query in <code>strong</code> html
   * font.
   * 
   * @param query query string
   * @param candidates candidates
   * @return real suggestions
   */
  private List<MultiWordSuggestion> convertToFormattedSuggestions(String query,
      List<String> candidates) {
    List<MultiWordSuggestion> suggestions = new ArrayList<MultiWordSuggestion>();

    for (int i = 0; i < candidates.size(); i++) {
      String candidate = candidates.get(i);
      int index = 0;
      int cursor = 0;
      // Use real suggestion for assembly.
      String formattedSuggestion = toRealSuggestions.get(candidate);

      // Create strong search string.
      StringBuffer accum = new StringBuffer();

      while (true) {
        index = candidate.indexOf(query, index);
        if (index == -1) {
          break;
        }
        int endIndex = index + query.length();
        if (index == 0 || (WHITESPACE_CHAR == candidate.charAt(index - 1))) {
          String part1 = escapeText(formattedSuggestion.substring(cursor, index));
          String part2 = escapeText(formattedSuggestion.substring(index,
              endIndex));
          cursor = endIndex;
          accum.append(part1).append("<strong>").append(part2).append(
              "</strong>");
        }
        index = endIndex;
      }

      // Check to make sure the search was found in the string.
      if (cursor == 0) {
        continue;
      }

      // Finish creating the formatted string.
      String end = escapeText(formattedSuggestion.substring(cursor));
      accum.append(end);
      MultiWordSuggestion suggestion = new MultiWordSuggestion(
          formattedSuggestion, accum.toString());
      suggestions.add(suggestion);
    }
    return suggestions;
  }

  /**
   * Find the sorted list of candidates that are matches for the given query.
   */
  private List<String> createCandidatesFromSearch(String query, int limit) {
    ArrayList<String> candidates = new ArrayList<String>();

    if (query.length() == 0) {
      return candidates;
    }

    // Find all words to search for.
    String[] searchWords = query.split(WHITESPACE_STRING);
    HashSet<String> candidateSet = null;
    for (int i = 0; i < searchWords.length; i++) {
      String word = searchWords[i];

      // Eliminate bogus word choices.
      if (word.length() == 0 || word.matches(WHITESPACE_STRING)) {
        continue;
      }

      // Find the set of candidates that are associated with all the
      // searchWords.
      HashSet<String> thisWordChoices = createCandidatesFromWord(word);
      if (candidateSet == null) {
        candidateSet = thisWordChoices;
      } else {
        candidateSet.retainAll(thisWordChoices);

        if (candidateSet.size() < 2) {
          // If there is only one candidate, on average it is cheaper to
          // check if that candidate contains our search string than to
          // continue intersecting suggestion sets.
          break;
        }
      }
    }
    if (candidateSet != null) {
      candidates.addAll(candidateSet);
      Collections.sort(candidates);
      // Respect limit for number of choices.
      for (int i = candidates.size() - 1; i > limit; i--) {
        candidates.remove(i);
      }
    }
    return candidates;
  }

  /**
   * Creates a set of potential candidates that match the given query.
   * 
   * @param query query string
   * @return possible candidates
   */
  private HashSet<String> createCandidatesFromWord(String query) {
    HashSet<String> candidateSet = new HashSet<String>();
    List<String> words = tree.getSuggestions(query, Integer.MAX_VALUE);
    if (words != null) {
      // Find all candidates that contain the given word the search is a
      // subset of.
      for (int i = 0; i < words.size(); i++) {
        Collection<String> belongsTo = toCandidates.get(words.get(i));
        if (belongsTo != null) {
          candidateSet.addAll(belongsTo);
        }
      }
    }
    return candidateSet;
  }

  /**
   * Normalize the search key by making it lower case, removing multiple spaces,
   * apply whitespace masks, and make it lower case.
   */
  private String normalizeSearch(String search) {
    // Use the same whitespace masks and case normalization for the search
    // string as was used with the candidate values.
    search = normalizeSuggestion(search);

    // Remove all excess whitespace from the search string.
    search = search.replaceAll(NORMALIZE_TO_SINGLE_WHITE_SPACE,
        WHITESPACE_STRING);

    return search.trim();
  }

  /**
   * Takes the formatted suggestion, makes it lower case and blanks out any
   * existing whitespace for searching.
   */
  private String normalizeSuggestion(String formattedSuggestion) {
    // Formatted suggestions should already have normalized whitespace. So we
    // can skip that step.

    // Lower case suggestion.
    formattedSuggestion = formattedSuggestion.toLowerCase();

    // Apply whitespace.
    if (whitespaceChars != null) {
      for (int i = 0; i < whitespaceChars.length; i++) {
        char ignore = whitespaceChars[i];
        formattedSuggestion = formattedSuggestion.replace(ignore,
            WHITESPACE_CHAR);
      }
    }
    return formattedSuggestion;
  }
}
