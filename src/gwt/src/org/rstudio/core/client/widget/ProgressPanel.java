/*
 * ProgressPanel.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.images.ProgressImages;

public class ProgressPanel extends Composite implements IsHideableWidget
{
   public ProgressPanel()
   {
      this(ProgressImages.createLarge());
   }
   
   public ProgressPanel(Widget progressImage)
   {
      this(progressImage, 100);
   }
   
   public ProgressPanel(Widget progressImage, int verticalOffset)
   {
      this(progressImage, verticalOffset, true);
   }
   
   public ProgressPanel(Widget progressImage, int verticalOffset, boolean allowSpinner)
   { 
      timer_ = new Timer()
      {
         public void run()
         {
            progressImage_.setVisible(true);
            progressSpinner_.setVisible(true);
            if (message_ != null)
            {
               progressLabel_.setText(message_);
               progressLabel_.setVisible(true);
            }
         }
      };
      
      progressImage_ = progressImage;

      progressSpinner_ = new ProgressSpinner(getSpinnerColor(isDark()));
      progressSpinner_.getElement().getStyle().setWidth(32, Unit.PX);
      progressSpinner_.getElement().getStyle().setHeight(32, Unit.PX);
      
      progressLabel_ = new Label();
      progressLabel_.getElement().getStyle().setOpacity(0.5);

      VerticalPanel panel = new VerticalPanel();
      Widget spinner = allowSpinner && progressSpinner_.isSupported() ? progressSpinner_ : progressImage_;
      panel.add(spinner);
      panel.setCellHorizontalAlignment(spinner, DockPanel.ALIGN_CENTER);
      panel.add(progressLabel_);

      progressImage_.setVisible(false);
      progressSpinner_.setVisible(false);
      progressLabel_.setVisible(false);

      progressPanel_ = new HorizontalCenterPanel(panel, verticalOffset);
      progressPanel_.setSize("100%", "100%");
      progressPanel_.addStyleName(ThemeStyles.INSTANCE.progressPanel());
      
      if (isDark())
         progressPanel_.addStyleName("ace_editor_theme");
      
      initWidget(progressPanel_);
   }

   public void beginProgressOperation(int delayMs)
   {
      beginProgressOperation(delayMs, null);
   }
   
   public void beginProgressOperation(int delayMs, final String message)
   {
      timer_.cancel();

      progressSpinner_.setColorType(getSpinnerColor(isDark()));
      progressSpinner_.setVisible(false);
      progressImage_.setVisible(false);
      progressLabel_.setVisible(false);
      message_ = message;

      timer_.schedule(delayMs);
   }

   public void endProgressOperation()
   {
      timer_.cancel();
      
      progressImage_.setVisible(false);
      progressSpinner_.setVisible(false);
      progressLabel_.setVisible(false);
   }
   
   @Override
   public void focus()
   {
      // implement to satisfy IsHideableWidget, don't actually take focus when called
   }
   
   public boolean isDark()
   {
      return Document.get().getBody().hasClassName("editor_dark");
   }

   private int getSpinnerColor(boolean isDark)
   {
      return isDark ? ProgressSpinner.COLOR_WHITE : ProgressSpinner.COLOR_BLACK;
   }

   private final HorizontalCenterPanel progressPanel_;
   private final Widget progressImage_;
   private final ProgressSpinner progressSpinner_;
   private final Label progressLabel_;
   private Timer timer_;
   private String message_;
  
}
