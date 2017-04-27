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
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.workbench.model.Session;

public class PathBreadcrumbWidget
      extends Composite
   implements HasSelectionCommitHandlers<FileSystemItem>,
              RequiresResize
{
   public PathBreadcrumbWidget(FileSystemContext context)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      context_ = context;
  
      pathPanel_ = new HorizontalPanel();
      pathPanel_.setStylePrimaryName(RES.styles().path());

      if (INCLUDE_UP_LINK)
      {
         linkUp_ = new Anchor();
         linkUp_.setTitle("Go to parent directory");
         linkUp_.setVisible(false);
         linkUp_.setStylePrimaryName(RES.styles().goUp());
         linkUp_.addStyleName(ThemeStyles.INSTANCE.handCursor());
         Image image = new Image(new ImageResource2x(FileIconResources.INSTANCE.iconUpFolder2x()));
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

      frame_ = new DockLayoutPanel(Unit.PX);
      eastFrame_ = new FlowPanel();

      Button browse = new Button("...");
      browse.setStyleName(STYLES.browse());
      browse.addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());
      browse.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            browse();
         }
      });
      browse.setTitle("Go to directory");
      eastFrame_.add(browse);
      frame_.addEast(eastFrame_, 22);
      
      frame_.add(outer_);
      frame_.setStyleName(STYLES.breadcrumbFrame());
      initWidget(frame_);
   }
   
   @Inject
   public void initialize(Provider<Session> pSession)
   {
      pSession_ = pSession;
   }
   
   private void maybeAddProjectIcon()
   {
      if (projectIconsAdded_)
         return;
      
      if (pSession_ == null ||
          pSession_.get() == null)
         return;
      
      final FileSystemItem projDir =
            pSession_.get().getSessionInfo().getActiveProjectDir();
      
      if (projDir != null)
      {
         Image projIcon = new Image(new ImageResource2x(RES.projectImage2x()));
         projIcon.addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());

         projIcon.addClickHandler(new ClickHandler()
         {
            public void onClick(ClickEvent event)
            {
               SelectionCommitEvent.fire(PathBreadcrumbWidget.this, projDir);
            }
         });
         projIcon.addStyleName(RES.styles().project());
         projIcon.setTitle(
               "Go to project directory");
         
         eastFrame_.insert(projIcon, 0);
         
         // TODO: infer from contents
         double width = 42;
         
         frame_.setWidgetSize(eastFrame_, width);
         projectIconsAdded_ = true;
         
      }
   }

   public void setDirectory(FileSystemItem[] pathElements, 
         String lastBrowseable)
   {
      pathPanel_.clear();
      maybeAddProjectIcon();
      
      if (linkUp_ != null)
         linkUp_.setVisible(pathElements.length > 1);

      Widget lastAnchor = null;
      for (FileSystemItem item : pathElements)
      {
         boolean browseable = true;
         if (lastBrowseable != null) 
            browseable = item.getPath().startsWith(lastBrowseable);
         
         lastAnchor = addAnchor(item, browseable);
      }

      if (lastAnchor != null)
         lastAnchor.addStyleName(RES.styles().last());

      onWidthsChanged();
   }

   private void onWidthsChanged()
   {
      outer_.getElement().setPropertyInt(
            "scrollLeft",
            outer_.getElement().getPropertyInt("scrollWidth"));

      int scrollPos = outer_.getElement().getPropertyInt("scrollLeft");
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

   private Widget addAnchor(final FileSystemItem item, boolean browseable)
   {
      boolean isHome = context_.isRoot(item);
      String text = isHome ? "Home" : item.getName();
      Widget link = null;
       
      if (browseable || isHome)
      {
         Anchor anchor = new Anchor(text, false);
         anchor.addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());
         
         anchor.addClickHandler(new ClickHandler()
         {
            public void onClick(ClickEvent event)
            {
               SelectionCommitEvent.fire(PathBreadcrumbWidget.this, item);
            }
         });
         link = anchor;
      }
      else
      {
         link = new InlineLabel(text);
      }
      link.setTitle(item.getPath());
      if (isHome)
         link.addStyleName(RES.styles().home());

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
   private FlowPanel eastFrame_;
   private boolean projectIconsAdded_ = false;
   private final DockLayoutPanel frame_;
   
   private Provider<Session> pSession_;
}
