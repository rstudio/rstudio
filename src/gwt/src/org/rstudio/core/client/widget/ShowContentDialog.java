/*
 * ShowContentDialog.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.theme.res.ThemeResources;

public class ShowContentDialog extends ModalDialogBase
{
   public ShowContentDialog(String title, String content)
   {
      this(title, content, new Size(0,0));
   }

   public ShowContentDialog(String title, String content, Size preferredSize)
   {
      super(Roles.getDialogRole());
      setText(title);
      preferredSize_ = preferredSize;

      if (content.startsWith("<html>") || content.startsWith("<!DOCTYPE "))
      {
         content_ = content;
         styleName_ = ThemeResources.INSTANCE.themeStyles().showFile();
         isFixedFont_ = false;
      }
      else
      {
         content_ = "<pre class=\"" + ThemeResources.INSTANCE.themeStyles().showFilePreFixed() +
            "\">" + content + "</pre>";
         styleName_ = ThemeResources.INSTANCE.themeStyles().showFileFixed();
         isFixedFont_ = true;
      }

      setButtonAlignment(HasHorizontalAlignment.ALIGN_CENTER);

      addButtons();
   }

   protected void addButtons()
   {
      ThemedButton closeButton = new ThemedButton("Close", clickEvent -> closeDialog());
      addOkButton(closeButton);
   }

   @Override
   protected Widget createMainWidget()
   {
     // main widget is scroll panel with embedded html
     ScrollPanel scrollPanel = new ScrollPanel();
     scrollPanel.setStylePrimaryName(styleName_);
     HTML htmlContent = new HTML(content_);
     if (isFixedFont_)
        FontSizer.applyNormalFontSize(htmlContent);
     scrollPanel.setWidget(htmlContent);

     // if we don't have a preferred size then size based on content
     Size size = preferredSize_;
     if (size.isEmpty())
        size = DomMetrics.measureHTML(content_, styleName_);

     // compute the widget size and set it
     Size minimumSize = new Size(300, 300);
     size = DomMetrics.adjustedElementSize(size,
                                           minimumSize,
                                           70,   // pad
                                           100); // client margin
     scrollPanel.setSize(size.width + "px", size.height + "px");

     // return it
     return scrollPanel;
   }

   private final String content_;
   private final String styleName_;
   private final boolean isFixedFont_;
   private final Size preferredSize_;
}
