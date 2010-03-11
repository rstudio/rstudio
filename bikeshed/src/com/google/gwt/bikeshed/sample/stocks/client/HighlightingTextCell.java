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
package com.google.gwt.bikeshed.sample.stocks.client;

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

/**
 * A {@link Cell} used to render text, with portions matching a given
 * regular expression highlighted.
 */
public class HighlightingTextCell extends Cell<String> {
  
  private String highlightRegex;

  @Override
  public void render(String value, StringBuilder sb) {
    sb.append("<div style='overflow:hidden; white-space:nowrap; text-overflow:ellipsis;'>");
    if (highlightRegex == null || highlightRegex.length() == 0) {
      sb.append(value);
      sb.append("</div>");
      return;
    }

    RegExp regExp = RegExp.compile(highlightRegex, "gi");
    int fromIndex = 0;
    int length = value.length();
    MatchResult result;
    while (fromIndex < length) {
      // Find the next match of the highlight regex
      result = regExp.exec(value);
      if (result == null) {
        // No more matches
        break;
      }
      int index = result.getIndex();
      String match = result.getGroup(0);
      
      // Append the characters leading up to the match
      sb.append(value.substring(fromIndex, index));
      // Append the match in boldface
      sb.append("<b>");
      sb.append(match);
      sb.append("</b>");
      // Skip past the matched string
      fromIndex = index + match.length();
      regExp.setLastIndex(fromIndex);
    }
    // Append the tail of the string
    if (fromIndex < length) {
      sb.append(value.substring(fromIndex));
    }
    sb.append("</div>");
  }

  public void setHighlightRegex(String highlightRegex) {
    this.highlightRegex = highlightRegex;
  }
}
