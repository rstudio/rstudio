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

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.sample.expenses.shared.ReportProxy;

/**
 * A place in the app that shows a list of reports.
 */
public class ReportPlace extends Place {

  /**
   * Tokenizer.
   */
  @Prefix("r")
  public static class Tokenizer implements PlaceTokenizer<ReportPlace> {
    private final RequestFactory requests;
    private final ReportListPlace.Tokenizer listTokenizer;

    public Tokenizer(ReportListPlace.Tokenizer listTokenizer,
        RequestFactory requests) {
      this.requests = requests;
      this.listTokenizer = listTokenizer;
    }

    private static final String SEPARATOR = ReportListPlace.Tokenizer.SEPARATOR
        + ReportListPlace.Tokenizer.SEPARATOR;

    public ReportPlace getPlace(String token) {
      String[] bits = token.split(SEPARATOR);
      if (bits.length != 2) {
        return null;
      }

      String listPlaceToken = bits[0];
      String reporterToken = bits[1];
      
      return new ReportPlace(listTokenizer.getPlace(listPlaceToken),
          requests.<ReportProxy> getProxyId(reporterToken));
    }

    public String getToken(ReportPlace place) {
      return listTokenizer.getToken(place.getListPlace()) + SEPARATOR
          + requests.getHistoryToken(place.getReportId());
    }
  }

  private final ReportListPlace listPlace;

  private final EntityProxyId<ReportProxy> reportId;

  public ReportPlace(ReportListPlace listPlace,
      EntityProxyId<ReportProxy> reportId) {
    this.listPlace = listPlace;
    this.reportId = reportId;
  }

  public ReportListPlace getListPlace() {
    return listPlace;
  }

  public EntityProxyId<ReportProxy> getReportId() {
    return reportId;
  }
}
