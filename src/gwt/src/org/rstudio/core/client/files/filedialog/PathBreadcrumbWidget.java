/*
 * PathBreadcrumbWidget.java
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
package org.rstudio.core.client.files.filedialog;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.ImageButton;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
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

      panel_ = new HorizontalPanel();
      panel_.setSize("100%", "100%");
      panel_.add(pathPanel_);

      outer_ = new SimplePanel();
      outer_.setWidget(panel_);
      outer_.setStylePrimaryName(RES.styles().breadcrumb());
      Roles.getGroupRole().setAriaLabelProperty(outer_.getElement(), "Selected path breadcrumb");

      Image fade = new Image(RES.fade());
      fade.setStylePrimaryName(STYLES.fade());
      fade_ = fade.getElement();

      fadeWrapper_ = new HTMLPanel("");
      fadeWrapper_.setStylePrimaryName(STYLES.fadeWrapper());
      fadeWrapper_.getElement().appendChild(fade_);

      outer_.getElement().appendChild(fadeWrapper_.getElement());

      frame_ = new DockLayoutPanel(Unit.PX);
      eastFrame_ = new FlowPanel();

      Button browse = new Button("...");
      browse.setStyleName(STYLES.browse());
      browse.addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());
      browse.addClickHandler(event -> browse());
      String buttonTitle = "Go to directory";
      browse.setTitle(buttonTitle);
      Roles.getButtonRole().setAriaLabelProperty(browse.getElement(), buttonTitle);
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
         ImageButton projIcon = new ImageButton("Go to project directory", new ImageResource2x(RES.projectImage2x()));

         projIcon.addClickHandler(event -> SelectionCommitEvent.fire(PathBreadcrumbWidget.this, projDir));
         projIcon.getImage().addStyleName(RES.styles().project());

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

      Widget lastAnchor = null;
      for (FileSystemItem item : pathElements)
      {
         boolean browseable = true;
         if (lastBrowseable != null)
            browseable = item.getPath().startsWith(lastBrowseable);

         lastAnchor = addAnchor(item, browseable);
      }

      if (lastAnchor != null)
      {
         lastAnchor.addStyleName(RES.styles().last());
         A11y.setARIACurrent(lastAnchor, "location");
      }

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
         fadeWrapper_.getElement().getStyle().setDisplay(Style.Display.BLOCK);
         fadeWrapper_.getElement().getStyle().setLeft(scrollPos, Style.Unit.PX);
      }
      else
      {
         fadeWrapper_.getElement().getStyle().setDisplay(Style.Display.NONE);
      }
   }

   private Widget addAnchor(final FileSystemItem item, boolean browseable)
   {
      boolean isHome = context_.isRoot(item);
      boolean isCloudRoot = context_.isCloudRoot(item);
      String text;
      if (isHome)
         text = "Home";
      else if (isCloudRoot)
         text = "Cloud";
      else
         text = item.getName();
      Widget link;

      if (browseable || isHome)
      {
         HyperlinkLabel anchor = new HyperlinkLabel(
               text, () -> SelectionCommitEvent.fire(PathBreadcrumbWidget.this, item));
         anchor.addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());
         link = anchor;
      }
      else
      {
         link = new InlineLabel(text);
      }
      link.setTitle(item.getPath());
      if (isHome)
         link.addStyleName(RES.styles().home());
      else if (isCloudRoot)
         link.addStyleName(RES.styles().cloudHome());

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
               pSession_.get().getSessionInfo().getActiveProjectDir(),
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
         SelectionCommitEvent.Handler<FileSystemItem> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }

   public void onResize()
   {
      onWidthsChanged();
   }

   private final HorizontalPanel panel_;
   private final HorizontalPanel pathPanel_;
   private final Element fade_;
   private final FileSystemContext context_;
   private FileDialogResources RES = FileDialogResources.INSTANCE;
   private FileDialogStyles STYLES = RES.styles();
   private SimplePanel outer_;
   private FlowPanel eastFrame_;
   private boolean projectIconsAdded_ = false;
   private final DockLayoutPanel frame_;
   private HTMLPanel fadeWrapper_;

   private Provider<Session> pSession_;
}
