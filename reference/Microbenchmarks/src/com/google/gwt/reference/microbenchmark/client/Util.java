package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.i18n.client.NumberFormat;

class Util {
  private static final DivElement detachedDiv = Document.get().createDivElement();

  static final boolean hasQSA = hasQSA();
  
  static final String EMPTY_OUTER_HTML = "<div>" + Util.EMPTY_INNER_HTML + "</div>";
  
  static final String EMPTY_INNER_HTML = "<div id='div1'>"
    +     "<div id='div2'></div>" 
    +     "<span id='span1'></span>" 
    +   "</div>"
    +   "<div>" 
    +     "<div id='div3'>" 
    +       "<div id='div4'></div>"
    +       "<span id='span2'></span>" 
    +     "</div>" 
    +   "</div>";

  static final int TABLE_ROW_COUNT = 40;

  static final int TABLE_COLUMN_COUNT = 10;

  static final String TEXTY_OUTER_HTML = "<div>" + Util.TEXTY_INNER_HTML + "</div>";
  
  static final String TEXTY_INNER_HTML = "Div root start" 
  +   "<div id='div1'>Div1 start"
  +     "<div id='div2'>Div2</div>" 
  +     "<span id='span1'>Span1</span>" 
  +   "Div1 end</div>"
  +   "<div>Div anon start" 
  +     "<div id='div3'>Div3" 
  +       "<div id='div4'>Div4</div>"
  +       "<span id='span2'>Span2</span>" 
  +     "Div 3 end</div>" 
  +   "Div anon end</div>"
  + "Div root end";

  private static int uniqueId = 0;
  private static UtilImpl impl = GWT.create(UtilImpl.class);

  static void addText(Element elm, String text) {
    elm.appendChild(Document.get().createTextNode(text));
  }

  static String createTableHtml() {
    StringBuilder sb = new StringBuilder();
    sb.append("<table><tbody>");
    sb.append(createTableRowsHtml());
    sb.append("</tbody></table>");
    return sb.toString();
  }

  static String createTableRowsHtml() {
    // Assign a unique ID to ensure that we actually change the content.
    uniqueId++;
    StringBuilder sb = new StringBuilder();
    for (int row = 0; row < Util.TABLE_ROW_COUNT; row++) {
      if (row % 2 == 0) {
        sb.append("<tr class=\"evenRow\">");
      } else {
        sb.append("<tr class=\"oddRow\">");
      }
      for (int column = 0; column < Util.TABLE_COLUMN_COUNT; column++) {
        sb.append("<td align=\"center\" valign=\"middle\"><div>");
        sb.append(uniqueId + " - Cell " + row + ":" + column);
        sb.append("</div></td>");
      }
      sb.append("</tr>");
    }
    return sb.toString();
  }

  static String format(double median) {
    return NumberFormat.getFormat("0").format(median);
  }

  static Element fromHtml(String html) {
    Util.detachedDiv.setInnerHTML(html);
    Element e = Util.detachedDiv.getFirstChildElement();
    e.getParentElement().removeChild(e);
    return e;
  }

  static String outerHtml(Element e) {
    String string = "<" + e.getNodeName() + ">" 
    + e.getInnerHTML()
    + "</" + e.getNodeName() + ">";
    return string;
  }

  static native JsArray<Element> querySelectorAll(Node root, String selector) /*-{
    return root.querySelectorAll(selector);
  }-*/;

  /**
   * Replace all of the rows in the specified tbody.
   * 
   * @param tbody the tbody element
   * @param rowHtml the HTML that represents the rows
   */
  static void replaceTableBodyRows(TableSectionElement tbody, String rowHtml) {
    impl.replaceTableBodyRows(tbody, rowHtml);
  }

  static long roundToTens(double median) {
    return Math.round(median/10)*10;
  }
  
  private static native boolean hasQSA() /*-{
    var qsa = document.querySelectorAll;
    return !(null == qsa);
  }-*/;
}
