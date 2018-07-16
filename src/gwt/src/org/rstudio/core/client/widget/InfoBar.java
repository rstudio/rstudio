/*
 * InfoBar.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.TextDecoration;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.WhiteSpace;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.studio.client.RStudioGinjector;
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
      switch(mode)
      {
      case WARNING:
         icon_ = new Image(new ImageResource2x(ThemeResources.INSTANCE.warningSmall2x()));
         break;
      case ERROR:
         icon_ = new Image(new ImageResource2x(ThemeResources.INSTANCE.errorSmall2x()));
         break;
      case INFO:
      default:
         icon_ = new Image(new ImageResource2x(ThemeResources.INSTANCE.infoSmall2x()));
         break;
      }
     
      labelRight_ = new HorizontalPanel();
      initWidget(binder.createAndBindUi(this));
      
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
   }

   public int getHeight()
   {
      return 19;
   }
   
   public void showRequiredPackagesMissingWarning(List<String> packages,
                                                  CommandWithArg<Boolean> onPackageInstallCompleted)
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
         message = "Packages " + packages.get(0) + ", " + packages.get(1) + ", and " + (n - 2) + " others are required but not installed.";
      }
      
      label_.setText(message);
      
      if (labelRight_.getWidgetCount() == 0)
      {
         Label anchor = new Label("Install");
         anchor.getElement().getStyle().setTextDecoration(TextDecoration.UNDERLINE);
         anchor.getElement().getStyle().setPaddingLeft(5, Unit.PX);
         anchor.getElement().getStyle().setCursor(Cursor.POINTER);
         anchor.getElement().getStyle().setWhiteSpace(WhiteSpace.NOWRAP);
         anchor.addClickHandler((ClickEvent event) -> { RStudioGinjector.INSTANCE.getDependencyManager().installPackages(packages, onPackageInstallCompleted); });
         labelRight_.add(anchor);
      }
   }
   
   public void showReadOnlyWarning(List<String> alternatives)
   {
      if (alternatives.size() == 0)
      {
         label_.setText("This document is read only.");
      }
      else
      {
         label_.setText("This document is read only. Generated from:");
         for (String alternative : alternatives)
         {
            Label anchor = new Label(alternative);
            anchor.getElement().getStyle().setTextDecoration(TextDecoration.UNDERLINE);
            anchor.getElement().getStyle().setPaddingLeft(5, Unit.PX);
            anchor.getElement().getStyle().setCursor(Cursor.POINTER);
            anchor.getElement().getStyle().setWhiteSpace(WhiteSpace.NOWRAP);
            anchor.addClickHandler((ClickEvent event) -> {
               Session session = RStudioGinjector.INSTANCE.getSession();
               FileTypeRegistry registry = RStudioGinjector.INSTANCE.getFileTypeRegistry();
               FileSystemItem projDir = session.getSessionInfo().getActiveProjectDir();
               FileSystemItem targetItem = FileSystemItem.createFile(projDir.completePath(alternative));
               registry.editFile(targetItem);
            });
            labelRight_.add(anchor);
         }
      }
   }

   @UiField
   protected DockLayoutPanel container_;
   @UiField(provided = true)
   protected Image icon_;
   @UiField
   protected Label label_;
   @UiField(provided = true)
   protected HorizontalPanel labelRight_;
   @UiField
   Image dismiss_;

   interface MyBinder extends UiBinder<Widget, InfoBar>{}
   private static MyBinder binder = GWT.create(MyBinder.class);
}
