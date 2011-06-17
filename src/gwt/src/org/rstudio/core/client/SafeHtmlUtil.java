package org.rstudio.core.client;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class SafeHtmlUtil
{
   public static void appendDiv(SafeHtmlBuilder sb, 
                                String style, 
                                String textContent)
   {
      StringBuilder div = new StringBuilder();
      div.append("<div class=\"");
      div.append(style);
      div.append("\">");
      sb.appendHtmlConstant(div.toString());
      sb.appendEscaped(textContent);
      sb.appendHtmlConstant("</div>");
   }
   
   public static void appendDiv(SafeHtmlBuilder sb, 
                                String style, 
                                SafeHtml htmlContent)
   {
      StringBuilder div = new StringBuilder();
      div.append("<div class=\"");
      div.append(style);
      div.append("\">");
      sb.appendHtmlConstant(div.toString());
      sb.append(htmlContent);
      sb.appendHtmlConstant("</div>");
   }
}

