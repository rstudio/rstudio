/*
 * TextEditingTargetPresentationHelper.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.presentation.model.SlideNavigation;
import org.rstudio.studio.client.common.presentation.model.SlideNavigationItem;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarPopupMenu;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarPopupRequest;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;

public class TextEditingTargetPresentationHelper
{
   public static interface SlideNavigator
   {
      void navigateToSlide(int index);
   }
   
   public TextEditingTargetPresentationHelper(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(PresentationServerOperations server)
   {
      server_ = server;
   }
   
   public String getCurrentSlide()
   {
      // search starting two lines ahead
      Position cursorPos = docDisplay_.getCursorPosition();
      Position searchPos = Position.create(cursorPos.getRow()+2, 0);
      InputEditorSelection sel = docDisplay_.search(SLIDE_REGEX, 
                                                true, 
                                                false, 
                                                false, 
                                                false, 
                                                searchPos, 
                                                null, 
                                                true);
                                                
      
      if (sel != null)
      {
         InputEditorPosition titlePos = sel.getStart().moveToPreviousLine();
         String title = docDisplay_.getLine(
                           docDisplay_.selectionToPosition(titlePos).getRow());
         title = title.trim();
         if (title.length() > 0 && SLIDE_PATTERN.match(title, 0) == null)
            return title;
         else
            return "(Untitled Slide)";
      }
      else
         return "(No Slides)";
   }
   
   public void buildSlideMenu(
                        final String path,
                        boolean isDirty,
                        final EditingTarget editor,
                        final CommandWithArg<StatusBarPopupRequest> onCompleted)
   {
      // rpc response handler
      SimpleRequestCallback<SlideNavigation> requestCallback = 
                        new SimpleRequestCallback<SlideNavigation>() {
         
         @Override
         public void onResponseReceived(SlideNavigation slideNavigation)
         {
            // create the menu and make sure we have some slides to return
            StatusBarPopupMenu menu =  new StatusBarPopupMenu();
            if (slideNavigation.getTotalSlides() == 0)
            {
               onCompleted.execute(new StatusBarPopupRequest(menu, null));
               return;
            }
            
            MenuItem defaultMenuItem = null;
            int length = slideNavigation.getItems().length();
            for (int i=0; i<length; i++)
            {
               SlideNavigationItem item = slideNavigation.getItems().get(i);
               String title = item.getTitle();
               if (StringUtil.isNullOrEmpty(title))
                  title = "(Untitled Slide)";
               
               StringBuilder indentBuilder = new StringBuilder();
               for (int level=0; level<item.getIndent(); level++)
                  indentBuilder.append("&nbsp;&nbsp;");
               
               SafeHtmlBuilder labelBuilder = new SafeHtmlBuilder();
               labelBuilder.appendHtmlConstant(indentBuilder.toString());
               labelBuilder.appendEscaped(title);

               final int targetSlide = i;
               final MenuItem menuItem = new MenuItem(
                  labelBuilder.toSafeHtml(),
                  new Command()
                  {
                     public void execute()
                     {
                        navigateToSlide(editor, targetSlide);
                     }           
                  });
               menu.addItem(menuItem);
               
               // see if this is the default menu item
               if (defaultMenuItem == null &&
                   item.getLine() >= (docDisplay_.getSelectionStart().getRow()))
               {
                  defaultMenuItem = menuItem;
               }
            }
             
            StatusBarPopupRequest request = new StatusBarPopupRequest(
                                                              menu, 
                                                              defaultMenuItem);
            onCompleted.execute(request);
         }   
      };
      
      // send code over the wire if we are dirty
      if (isDirty)
      {
         server_.getSlideNavigationForCode(
                     docDisplay_.getCode(), 
                     FileSystemItem.createFile(path).getParentPathString(), 
                     requestCallback);
      }
      else
      {
         server_.getSlideNavigationForFile(path, requestCallback);
      }
   }
   
   public static void navigateToSlide(final EditingTarget editor, 
                                      int slideIndex)
   {
      // scan for the specified slide
      int currentSlide = 0;
      Position navPos = null;
      Position pos = Position.create(0, 0);
      while ((pos = editor.search(pos, "^\\={3,}\\s*$")) != null)
      { 
         if (currentSlide++ == slideIndex)
         {
            navPos = Position.create(pos.getRow() - 1, 0);
            break;
         }
         
         pos = Position.create(pos.getRow() + 1, 0);
      }
      
      // navigate to the slide
      if (navPos != null)
      {
         final Position navPosAlias = navPos;
         Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute()
            {
               editor.navigateToPosition(
                 SourcePosition.create(navPosAlias.getRow(), 0), 
                 false);
               
            }
         });
      }
   }
   
   
   private final DocDisplay docDisplay_;
   private PresentationServerOperations server_;
   
   private static final String SLIDE_REGEX = "^\\={3,}\\s*$";
   private static final Pattern SLIDE_PATTERN = Pattern.create(SLIDE_REGEX);
}
