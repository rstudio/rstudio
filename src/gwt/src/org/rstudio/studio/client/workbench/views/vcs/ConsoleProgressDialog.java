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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.VirtualConsole;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent.Handler;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;

public class ConsoleProgressDialog extends ModalDialogBase
      implements Handler, ClickHandler, ProcessExitEvent.Handler
{
   interface Resources extends ClientBundle
   {
      ImageResource progress();

      @Source("ConsoleProgressDialog.css")
      Styles styles();
   }

   interface Styles extends CssResource
   {
      String consoleProgressDialog();
      String labelCell();
      String progressCell();
      String buttonCell();
      String scrollPanel();
   }

   interface Binder extends UiBinder<Widget, ConsoleProgressDialog>
   {}

   public static void ensureStylesInjected()
   {
      resources_.styles().ensureInjected();
   }

   public ConsoleProgressDialog(String title, ConsoleProcess consoleProcess)
   {
      this(title, consoleProcess, "", null);
   }

   public ConsoleProgressDialog(String title,
                                ConsoleProcess consoleProcess,
                                String initialOutput,
                                Integer exitCode)
   {
      addStyleName(resources_.styles().consoleProgressDialog());

      consoleProcess_ = consoleProcess;

      setText(title);

      registrations_ = new HandlerRegistrations();
      registrations_.add(consoleProcess.addConsoleOutputHandler(this));
      registrations_.add(consoleProcess.addProcessExitHandler(this));

      output_ = new PreWidget();
      output_.getElement().getStyle().setMargin(4, Unit.PX);
      scrollPanel_ = new BottomScrollPanel(output_);

      progressAnim_ = new Image(resources_.progress().getSafeUri());

      centralWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);

      label_.setText(title);

      if (!StringUtil.isNullOrEmpty(initialOutput))
      {
         console_.submit(initialOutput);
         output_.setText(console_.toString());
      }

      Style style = scrollPanel_.getElement().getStyle();
      double skewFactor = (12 + BrowseCap.getFontSkew()) / 12.0;
      style.setWidth((int)(skewFactor * 660), Unit.PX);
      style.setFontSize(12 + BrowseCap.getFontSkew(), Unit.PX);

      stopButton_.addClickHandler(this);
      stopButton_.fillWidth();

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

      addCloseHandler(new CloseHandler<PopupPanel>()
      {
         @Override
         public void onClose(CloseEvent<PopupPanel> popupPanelCloseEvent)
         {
            consoleProcess_.reap(new VoidServerRequestCallback());
         }
      });

      if (exitCode != null)
         setExitCode(exitCode);
   }

   @Override
   protected Widget createMainWidget()
   {
      return centralWidget_;
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      registrations_.removeHandler();
   }

   @Override
   public void onPreviewNativeEvent(NativePreviewEvent event)
   {
      if (event.getTypeInt() == Event.ONKEYDOWN)
      {
         if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE &&
             KeyboardShortcut.getModifierValue(event.getNativeEvent()) == KeyboardShortcut.NONE)
         {
            stopButton_.click();
            event.cancel();
            return;
         }
      }

      super.onPreviewNativeEvent(event);
   }

   @Override
   public void onConsoleOutput(ConsoleOutputEvent event)
   {
      boolean scrolledToBottom = scrollPanel_.isScrolledToBottom();
      appendOutput(event);
      if (scrolledToBottom)
         scrollPanel_.scrollToBottom();
   }

   private void appendOutput(ConsoleOutputEvent event)
   {
      console_.submit(event.getOutput());
      output_.setText(console_.toString());
   }

   @Override
   public void onProcessExit(ProcessExitEvent event)
   {
      setExitCode(event.getExitCode());
   }

   private void setExitCode(int exitCode)
   {
      running_ = false;
      stopButton_.setText("Close");
      progressAnim_.getElement().getStyle().setVisibility(Visibility.HIDDEN);

      // TODO: Show warning if exitCode != 0
   }

   @Override
   public void onClick(ClickEvent event)
   {
      if (running_)
      {
         consoleProcess_.interrupt(new SimpleRequestCallback<Void>() {
            @Override
            public void onResponseReceived(Void response)
            {
               closeDialog();
            }

            @Override
            public void onError(ServerError error)
            {
               stopButton_.setEnabled(true);
               super.onError(error);
            }
         });
         stopButton_.setEnabled(false);
      }
      else
      {
         closeDialog();
      }

      // Whether success or failure, we don't want to interrupt again
      running_ = false;
   }

   private boolean running_ = true;
   private final ConsoleProcess consoleProcess_;
   private final PreWidget output_;
   private HandlerRegistrations registrations_;
   private VirtualConsole console_ = new VirtualConsole();

   @UiField(provided = true)
   BottomScrollPanel scrollPanel_;
   @UiField(provided = true)
   Image progressAnim_;
   @UiField
   Label label_;
   @UiField
   SmallButton stopButton_;
   private Widget centralWidget_;

   private static final Resources resources_ = GWT.<Resources>create(Resources.class);
}
