package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.NumberFormat;

class Util {
  private static final DivElement detachedDiv = Document.get().createDivElement();

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
  
  static void addText(Element elm, String text) {
    elm.appendChild(Document.get().createTextNode(text));
  }

  static Element fromHtml(String html) {
    Util.detachedDiv.setInnerHTML(html);
    Element e = Util.detachedDiv.getFirstChildElement();
    e.getParentElement().removeChild(e);
    return e;
  }

  static long roundToTens(double median) {
    return Math.round(median/10)*10;
  }

  static String format(double median) {
    return NumberFormat.getFormat("0").format(median);
  }

  static String outerHtml(Element e) {
    String string = "<" + e.getNodeName() + ">" 
    + e.getInnerHTML()
    + "</" + e.getNodeName() + ">";
    return string;
  }
}
