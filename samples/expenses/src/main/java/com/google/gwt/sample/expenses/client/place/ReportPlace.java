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

import static com.google.gwt.sample.expenses.client.place.ReportListPlace.Tokenizer.SEPARATOR;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import com.google.gwt.sample.expenses.shared.ReportProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.RequestFactory;

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

    public ReportPlace getPlace(String token) {
      int i = token.indexOf(SEPARATOR);
      if (i < 0) {
        return null;
      }

      String reporterToken = token.substring(0, i);
      String listPlaceToken = token.substring(i + SEPARATOR.length());

      return new ReportPlace(listTokenizer.getPlace(listPlaceToken),
          requests.<ReportProxy> getProxyId(reporterToken));
    }

    public String getToken(ReportPlace place) {
      return requests.getHistoryToken(place.getReportId()) + SEPARATOR
          + listTokenizer.getToken(place.getListPlace());
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((listPlace == null) ? 0 : listPlace.hashCode());
    result = prime * result + ((reportId == null) ? 0 : reportId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ReportPlace other = (ReportPlace) obj;
    if (listPlace == null) {
      if (other.listPlace != null)
        return false;
    } else if (!listPlace.equals(other.listPlace))
      return false;
    if (reportId == null) {
      if (other.reportId != null)
        return false;
    } else if (!reportId.equals(other.reportId))
      return false;
    return true;
  }
}
