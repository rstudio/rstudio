package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.dom.client.TableSectionElement;

/**
 * Implementation of {@link Util}.
 */
class UtilImpl {

  void replaceTableBodyRows(TableSectionElement tbody, String rowHtml) {
    tbody.setInnerHTML(rowHtml);
  }
}
