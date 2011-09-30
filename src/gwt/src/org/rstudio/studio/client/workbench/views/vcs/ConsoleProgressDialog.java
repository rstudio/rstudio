/*
 * ConsoleProgressDialog.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.VirtualConsole;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.BottomScrollPanel;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent.Handler;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.server.Void;

public class ConsoleProgressDialog extends ModalDialogBase
      implements Handler, ClickHandler, ProcessExitEvent.Handler
{
   public ConsoleProgressDialog(String title, ConsoleProcess consoleProcess)
   {
      consoleProcess_ = consoleProcess;

      setText(title);

      registrations_ = new HandlerRegistrations();
      registrations_.add(consoleProcess.addConsoleOutputHandler(this));
      registrations_.add(consoleProcess.addProcessExitHandler(this));

      output_ = new PreWidget();
      output_.getElement().getStyle().setMargin(0, Unit.PX);
      output_.getElement().getStyle().setFontSize(11, Unit.PX);

      scrollPanel_ = new BottomScrollPanel(output_);
      scrollPanel_.setSize("640px", "200px");

      Style style = scrollPanel_.getElement().getStyle();
      style.setBackgroundColor("white");
      style.setBorderStyle(BorderStyle.SOLID);
      style.setBorderColor("#BBB");
      style.setBorderWidth(1, Style.Unit.PX);
      style.setMargin(0, Unit.PX);
      style.setMarginBottom(3, Unit.PX);
      style.setPadding(4, Unit.PX);

      status_ = new Label("The process is executing...");

      button_ = new ThemedButton("Stop", this);
      addOkButton(button_);

      consoleProcess.start(new SimpleRequestCallback<Void>()
      {
         @Override
         public void onError(ServerError error)
         {
            // Show error and stop
            super.onError(error);
            closeDialog();
         }
      });
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      registrations_.removeHandler();
   }

   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel vpanel = new VerticalPanel();
      vpanel.add(scrollPanel_);
      vpanel.add(status_);
      return vpanel;
   }

   @Override
   public void onConsoleOutput(ConsoleOutputEvent event)
   {
      boolean scrolledToBottom = scrollPanel_.isScrolledToBottom();
      console_.submit(event.getOutput());
      output_.setText(console_.toString());
      if (scrolledToBottom)
         scrollPanel_.scrollToBottom();
   }

   @Override
   public void onProcessExit(ProcessExitEvent event)
   {
      running_ = false;
      button_.setText("Close");

      if (event.getExitCode() == 0)
         status_.setText("The process executed successfully.");
      else
         status_.setText("The process exited with error code " +
                         event.getExitCode());
   }

   @Override
   public void onClick(ClickEvent event)
   {
      if (running_)
         consoleProcess_.interrupt(new SimpleRequestCallback<Void>());

      // Whether success or failure, we don't want to interrupt again
      running_ = false;

      closeDialog();
   }

   private boolean running_ = false;
   private final ConsoleProcess consoleProcess_;
   private final PreWidget output_;
   private final ThemedButton button_;
   private HandlerRegistrations registrations_;
   private Label status_;
   private VirtualConsole console_ = new VirtualConsole();
   private BottomScrollPanel scrollPanel_;
}
