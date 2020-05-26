/*
 * CppCompletionToolTip.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.model.CppCompletionText;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class CppCompletionToolTip extends PopupPanel
{
   public CppCompletionToolTip()
   {
      this("", "");
   }
   
   public CppCompletionToolTip(String text)
   {
      this(text, null);
   }
   
   public CppCompletionToolTip(CppCompletionText text)
   {
      this(text.getText(), text.getComment());
   }
   
   public CppCompletionToolTip(String text, String comment)
   {
      super(true);
      CppCompletionResources.Styles styles = 
                              CppCompletionResources.INSTANCE.styles();
      
      panel_ = new HorizontalPanel();
      panel_.addStyleName(styles.toolTip());
      
      VerticalPanel textPanel = new VerticalPanel();
      textPanel.add(label_ = new Label()); 
      label_.addStyleName(styles.toolTipText());
      commentLabel_ = new Label();
      commentLabel_.addStyleName(styles.commentText());
      commentLabel_.setVisible(false);
      textPanel.add(commentLabel_);
      
      panel_.add(textPanel);
      setWidget(panel_);
      setText(text, comment);
   }

   public void setText(String text)
   {
      setText(text, null);
   }
   
   public void setText(CppCompletionText text)
   {
      setText(text.getText(), text.getComment());
   }
   
   public void setText(String text, String comment)
   {
      if (text != label_.getText())
         label_.setText(text);
      
      if (!StringUtil.isNullOrEmpty(comment))
      {
         commentLabel_.setText(comment);
         commentLabel_.setVisible(true);
      }
      else
      {
         commentLabel_.setText("");
         commentLabel_.setVisible(false);
      }
   }
   
   public void addLeftWidget(Widget widget)
   {
      panel_.insert(widget, 0);
   }
   
   public void setMaxWidth(int maxWidth)
   {
      getElement().getStyle().setPropertyPx("maxWidth", maxWidth);
   }
   
   // show the tooltip immediately above (however, if there is code
   // on the line above that would be obstructed by the tooltip
   // then bump it up some more)
   public static int tooltipTopPadding(DocDisplay docDisplay)
   {
      int topPad = 9;
      int lineNumberAbove = docDisplay.getCurrentLineNum() - 1;
      if (lineNumberAbove > 0)
      {
         if (docDisplay.getLine(lineNumberAbove).length() > 
             docDisplay.getLength(docDisplay.getCurrentLineNum()))
         {
            Double fontPad = RStudioGinjector.INSTANCE.getUserPrefs()
                  .fontSizePoints().getValue();
            if (fontPad >= 13)
               fontPad *= 1.3;
            topPad = topPad + fontPad.intValue();
         }
      }
      return topPad;
   }
   
   public String getLabel()
   {
      return label_.getText();
   }
   
   private Label label_;
   private Label commentLabel_;
   private HorizontalPanel panel_;
}
