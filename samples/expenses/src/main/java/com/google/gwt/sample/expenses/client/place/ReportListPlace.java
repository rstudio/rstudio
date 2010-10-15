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
package com.google.gwt.sample.expenses.client.place;

import com.google.gwt.http.client.URL;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.sample.expenses.shared.EmployeeProxy;

/**
 * A place in the app that shows a list of reports.
 */
public class ReportListPlace extends Place {

  /**
   * Tokenizer.
   */
  @Prefix("l")
  public static class Tokenizer implements PlaceTokenizer<ReportListPlace> {
    static final String SEPARATOR = "!";
    private final RequestFactory requests;

    public Tokenizer(RequestFactory requests) {
      this.requests = requests;
    }

    public ReportListPlace getPlace(String token) {
      String bits[] = token.split(SEPARATOR);

      if (bits.length != 3) {
        return null;
      }

      String reporterIdToken = bits[0];
      String search = bits[1];
      int page = Integer.valueOf(bits[2]);

      return new ReportListPlace(requests.<EmployeeProxy> getProxyId(reporterIdToken),
          URL.decodePathSegment(search), page);
    }

    public String getToken(ReportListPlace place) {
      return requests.getHistoryToken(place.getReporterId()) + SEPARATOR
          + URL.encodePathSegment(place.getSearch());
    }
  }

  private final int page;
  private final EntityProxyId<EmployeeProxy> reporterId;
  private final String search;

  public ReportListPlace(EntityProxyId<EmployeeProxy> reporter, String search, int page) {
    this.reporterId = reporter;
    this.search = search;
    this.page = page;
  }

  public int getPage() {
    return page;
  }
  
  public EntityProxyId<EmployeeProxy> getReporterId() {
    return reporterId;
  }

  public String getSearch() {
    return search;
  }
}
