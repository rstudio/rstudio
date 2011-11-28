/*
 * SVNPresenter.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.common.vcs.SVNServerOperations.ProcessResult;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;
import org.rstudio.studio.client.workbench.views.vcs.svn.model.SVNState;

import java.util.ArrayList;

public class SVNPresenter extends BasePresenter
{
   interface Binder extends CommandBinder<Commands, SVNPresenter>
   {
   }

   public interface Display extends WorkbenchView, IsWidget
   {
      HasClickHandlers getAddFilesButton();
      HasClickHandlers getDeleteFilesButton();
      HasClickHandlers getRevertFilesButton();
      HasClickHandlers getUpdateButton();
      HasClickHandlers getCommitButton();
      ArrayList<StatusAndPath> getSelectedItems();
   }

   private class ProcessCallback extends SimpleRequestCallback<ProcessResult>
   {
      public ProcessCallback(String title)
      {
         super(title);
         title_ = title;
      }

      @Override
      public void onResponseReceived(ProcessResult response)
      {
         if (!StringUtil.isNullOrEmpty(response.getOutput()))
         {
            new ConsoleProgressDialog(title_,
                                      response.getOutput(),
                                      response.getExitCode()).showModal();
         }
      }

      private final String title_;
   }

   @Inject
   public SVNPresenter(Display view,
                       Commands commands,
                       SVNServerOperations server,
                       SVNState svnState)
   {
      super(view);
      view_ = view;
      server_ = server;
      svnState_ = svnState;

      GWT.<Binder>create(Binder.class).bind(commands, this);

      view_.getAddFilesButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            JsArrayString paths = getPathArray();

            if (paths.length() > 0)
               server_.svnAdd(paths, new ProcessCallback("SVN Add"));
         }
      });

      view_.getDeleteFilesButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            JsArrayString paths = getPathArray();

            if (paths.length() > 0)
               server_.svnDelete(paths, new ProcessCallback("SVN Delete"));
         }
      });

      view_.getRevertFilesButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            JsArrayString paths = getPathArray();

            if (paths.length() > 0)
               server_.svnRevert(paths, new ProcessCallback("SVN Revert"));
         }
      });

      view_.getUpdateButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            server_.svnUpdate(new SimpleRequestCallback<ConsoleProcess>()
            {
               @Override
               public void onResponseReceived(ConsoleProcess response)
               {
                  new ConsoleProgressDialog("SVN Update", response).showModal();
               }
            });
         }
      });

      view_.getCommitButton().addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            // TODO: implement
         }
      });
   }

   private JsArrayString getPathArray()
   {
      ArrayList<StatusAndPath> items = view_.getSelectedItems();
      JsArrayString paths = JavaScriptObject.createArray().cast();
      for (StatusAndPath item : items)
         paths.push(item.getPath());
      return paths;
   }

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }

   @Handler
   void onVcsRefresh()
   {
      svnState_.refresh(true);
   }

   private final Display view_;
   private final SVNServerOperations server_;
   private final SVNState svnState_;
}
