package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableSectionElement;

/**
 * IE implementation of Util.
 */
class UtilImplTrident extends UtilImpl {

  private final com.google.gwt.user.client.Element tmpElem = Document.get().createDivElement()
      .cast();

  /**
   * IE doesn't support innerHTML on tbody, nor does it support removing or
   * replacing a tbody. The only solution is to remove and replace the rows
   * themselves.
   */
  @Override
  void replaceTableBodyRows(TableSectionElement tbody, String rowHtml) {
    // Remove all children.
    Element child = tbody.getFirstChildElement();
    while (child != null) {
      Element next = child.getNextSiblingElement();
      tbody.removeChild(child);
      child = next;
    }

    // Convert the row html to child elements.
    tmpElem.setInnerHTML("<table><tbody>" + rowHtml + "</tbody></table>");
    TableElement tableElem = tmpElem.getFirstChildElement().cast();
    TableSectionElement newRows = tableElem.getTBodies().getItem(0);

    // Add new child elements.
    child = newRows.getFirstChildElement();
    while (child != null) {
      Element next = child.getNextSiblingElement();
      tbody.appendChild(child);
      child = next;
    }
  }
}
