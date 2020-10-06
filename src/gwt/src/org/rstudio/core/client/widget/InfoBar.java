/*
 * InfoBar.java
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
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.TextDecoration;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.WhiteSpace;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.common.Timers;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.model.Session;

public class InfoBar extends Composite 
{
   public static final int INFO = 0;
   public static final int WARNING = 1;
   public static final int ERROR = 2;
   
   public InfoBar(int mode)
   {
      this(mode, null);
   }
   
   public InfoBar(int mode, ClickHandler dismissHandler)
   {
      switch (mode)
      {
      case WARNING:
         icon_ = new DecorativeImage(new ImageResource2x(ThemeResources.INSTANCE.warningSmall2x()));
         break;
      case ERROR:
         icon_ = new DecorativeImage(new ImageResource2x(ThemeResources.INSTANCE.errorSmall2x()));
         break;
      case INFO:
      default:
         icon_ = new DecorativeImage(new ImageResource2x(ThemeResources.INSTANCE.infoSmall2x()));
         break;
      }
     
      labelRight_ = new HorizontalPanel();
      initWidget(binder.createAndBindUi(this));

      A11y.setARIAHidden(label_);
      if (!RStudioGinjector.INSTANCE.getAriaLiveService().isDisabled(AriaLiveService.INFO_BAR))
      {
         if (mode == ERROR)
            Roles.getAlertRole().set(live_.getElement());
         else
            Roles.getStatusRole().set(live_.getElement());
      }
      dismiss_.addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());
      
      if (dismissHandler != null)
         dismiss_.addClickHandler(dismissHandler);
      else
         dismiss_.setVisible(false);
   }
   
  
   public String getText()
   {
      return label_.getText();
   }

   public void setText(String text)
   {
      label_.setText(text);
      Timers.singleShot(AriaLiveService.UI_ANNOUNCEMENT_DELAY, () -> live_.setText(text));
      labelRight_.clear();
   }

   public int getHeight()
   {
      return 19;
   }
   
   public void setTextWithAction(String text, String actionLabel, Command command)
   {
      setText(text);
      labelRight_.add(label(actionLabel, command));
   }
   
   public void showRequiredPackagesMissingWarning(List<String> packages,
                                                  Command onInstall,
                                                  Command onDismiss)
   {
      String message;
      
      int n = packages.size();
      if (n == 1)
      {
         message = "Package " + packages.get(0) + " required but is not installed.";
      }
      else if (n == 2)
      {
         message = "Packages " + packages.get(0) + " and " + packages.get(1) + " required but are not installed.";
      }
      else if (n == 3)
      {
         message = "Packages " + packages.get(0) + ", " + packages.get(1) + ", and " + packages.get(2) + " required but are not installed.";
      }
      else
      {
         message = "Packages " + packages.get(0) + ", " + packages.get(1) + ", and " + (n - 2) + " others required but are not installed.";
      }
      
      setText(message);

      labelRight_.add(label("Install", () -> {
         onInstall.execute();
      }));

      labelRight_.add(label("Don't Show Again", () -> {
         onDismiss.execute();
      }));
   }
   
   public void showPanmirrorFormatChanged(Command onReload)
   {
      setText("Markdown format changes require a reload of the visual editor.");
      labelRight_.clear();
      labelRight_.add(label("Reload Now", () -> {
         onReload.execute();
      }));
   }
   
   public void showTexInstallationMissingWarning(String message,
                                                 Command onInstall)
   {
      setText(message);
      labelRight_.add(label("Install TinyTeX", () -> { onInstall.execute(); }));
   }
   
   public void showReadOnlyWarning(List<String> alternatives)
   {
      if (alternatives.size() == 0)
      {
         setText("This document is read only.");
      }
      else
      {
         setText("This document is read only. Generated from:");
         for (String alternative : alternatives)
         {
            labelRight_.add(label(alternative, () -> {
               Session session = RStudioGinjector.INSTANCE.getSession();
               FileTypeRegistry registry = RStudioGinjector.INSTANCE.getFileTypeRegistry();
               FileSystemItem projDir = session.getSessionInfo().getActiveProjectDir();
               FileSystemItem targetItem = FileSystemItem.createFile(projDir.completePath(alternative));
               registry.editFile(targetItem);
            }));
         }
      }
   }
   
   private Label label(String text, Command onClick)
   {
      Label label = new Label(text);
      label.getElement().getStyle().setTextDecoration(TextDecoration.UNDERLINE);
      label.getElement().getStyle().setPaddingLeft(5, Unit.PX);
      label.getElement().getStyle().setCursor(Cursor.POINTER);
      label.getElement().getStyle().setWhiteSpace(WhiteSpace.NOWRAP);
      label.addClickHandler((ClickEvent event) -> { onClick.execute(); });
      return label;
      
   }

   @UiField
   protected DockLayoutPanel container_;
   @UiField(provided = true)
   protected DecorativeImage icon_;
   @UiField
   protected Label label_;
   @UiField
   protected Label live_;
   @UiField(provided = true)
   protected HorizontalPanel labelRight_;
   @UiField
   ImageButton dismiss_;

   interface MyBinder extends UiBinder<Widget, InfoBar>{}
   private static MyBinder binder = GWT.create(MyBinder.class);
}
