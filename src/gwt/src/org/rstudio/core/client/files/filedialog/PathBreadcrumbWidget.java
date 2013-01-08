/*
 * PathBreadcrumbWidget.java
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
package org.rstudio.core.client.files.filedialog;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.filetypes.FileIconResources;

public class PathBreadcrumbWidget
      extends Composite
   implements HasSelectionCommitHandlers<FileSystemItem>,
              RequiresResize
{
   public PathBreadcrumbWidget(FileSystemContext context)
   {
      context_ = context;
  
      pathPanel_ = new HorizontalPanel();
      pathPanel_.setStylePrimaryName(RES.styles().path());

      if (INCLUDE_UP_LINK)
      {
         linkUp_ = new Anchor();
         linkUp_.setTitle("Go to parent directory");
         linkUp_.setVisible(false);
         linkUp_.setStylePrimaryName(RES.styles().goUp());
         Image image = new Image(FileIconResources.INSTANCE.iconUpFolder());
         linkUp_.getElement().appendChild(image.getElement());
      }
      else
         linkUp_ = null;

      panel_ = new HorizontalPanel();
      panel_.setSize("100%", "100%");
      panel_.add(pathPanel_);
      if (linkUp_ != null)
      {
         panel_.add(linkUp_);
         panel_.setCellHorizontalAlignment(linkUp_, HorizontalPanel.ALIGN_RIGHT);
         panel_.setCellVerticalAlignment(linkUp_, HorizontalPanel.ALIGN_MIDDLE);
      }

      outer_ = new SimplePanel();
      outer_.setWidget(panel_);
      outer_.setStylePrimaryName(RES.styles().breadcrumb());

      Image fade = new Image(RES.fade());
      fade.setStylePrimaryName(STYLES.fade());
      fade_ = fade.getElement();
      outer_.getElement().appendChild(fade_);

      if (linkUp_ != null)
      {
         linkUp_.addClickHandler(new ClickHandler()
         {
            public void onClick(ClickEvent event)
            {
               FileSystemItem[] dirs = context_.parseDir(
                                                   context_.pwdItem().getPath());
               if (dirs.length > 1)
                  context_.cd(dirs[dirs.length - 2].getPath());
            }
         });
      }

      DockLayoutPanel frame = new DockLayoutPanel(Unit.PX);

      Image browse = new Image(RES.browse());
      browse.setStyleName(STYLES.browse());
      frame.addEast(browse, RES.browse().getWidth());
      browse.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            browse();
         }
      });
      
      frame.add(outer_);
      frame.setStyleName(STYLES.breadcrumbFrame());
      initWidget(frame);
   }

   public void setDirectory(FileSystemItem[] pathElements)
   {
      pathPanel_.clear();

      if (linkUp_ != null)
         linkUp_.setVisible(pathElements.length > 1);

      Anchor lastAnchor = null;
      for (FileSystemItem item : pathElements)
         lastAnchor = addAnchor(item);

      if (lastAnchor != null)
         lastAnchor.addStyleName(RES.styles().last());

      onWidthsChanged();
   }

   private void onWidthsChanged()
   {
      DOM.setElementPropertyInt(
            outer_.getElement(),
            "scrollLeft",
            DOM.getElementPropertyInt(outer_.getElement(), "scrollWidth"));

      int scrollPos = DOM.getElementPropertyInt(outer_.getElement(),
                                                "scrollLeft");
      if (scrollPos > 0)
      {
         fade_.getStyle().setDisplay(Style.Display.BLOCK);
         fade_.getStyle().setLeft(scrollPos, Style.Unit.PX);
      }
      else
      {
         fade_.getStyle().setDisplay(Style.Display.NONE);
      }
   }

   private Anchor addAnchor(final FileSystemItem item)
   {
      boolean isHome = context_.isRoot(item);
      Anchor link = new Anchor(isHome ? "Home"
                                      : item.getName(),
                               false);
      link.setTitle(item.getPath());
      if (isHome)
         link.addStyleName(RES.styles().home());

      link.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            SelectionCommitEvent.fire(PathBreadcrumbWidget.this, item);
         }
      });

      pathPanel_.add(link);
      return link;
   }

   private void browse()
   {
      if (Desktop.isDesktop())
      {
         FileSystemContext tempContext =
               RStudioGinjector.INSTANCE.getRemoteFileSystemContext();
         RStudioGinjector.INSTANCE.getFileDialogs().chooseFolder(
               "Go To Folder",
               tempContext,
               null,
               new ProgressOperationWithInput<FileSystemItem>()
               {
                  public void execute(FileSystemItem input,
                                      ProgressIndicator indicator)
                  {
                     if (input == null)
                        return;
                     context_.cd(input.getPath());
                     indicator.onCompleted();
                  }
               });
      }
      else
      {
         context_.messageDisplay().promptForText(
               "Go To Folder", 
               "Path to folder (use ~ for home directory):",
               "", 
               new OperationWithInput<String>() {

                  @Override
                  public void execute(String input)
                  {
                     if (input == null)
                        return;
                     
                     context_.cd(input);
                  }
                  
               });
      }
      
   }

   public HandlerRegistration addSelectionCommitHandler(
         SelectionCommitHandler<FileSystemItem> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }

   public void onResize()
   {
      onWidthsChanged();
   }

   private final HorizontalPanel panel_;
   private final HorizontalPanel pathPanel_;
   private final Anchor linkUp_;
   private final Element fade_;
   private final FileSystemContext context_;
   private FileDialogResources RES = FileDialogResources.INSTANCE;
   private FileDialogStyles STYLES = RES.styles();
   private static final boolean INCLUDE_UP_LINK = false;
   private SimplePanel outer_;
}
