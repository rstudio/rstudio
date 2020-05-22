/*
 * VcsState.java
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
package org.rstudio.studio.client.workbench.views.vcs.common.model;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.WidgetHandlerRegistration;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.StatusAndPathInfo;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeEvent;
import org.rstudio.studio.client.workbench.views.files.events.FileChangeHandler;
import org.rstudio.studio.client.workbench.views.files.model.FileChange;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent.Reason;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;

import java.util.ArrayList;

public abstract class VcsState
{
   public VcsState(EventBus eventBus,
                   GlobalDisplay globalDisplay,
                   final Session session)
   {
      eventBus_ = eventBus;
      globalDisplay_ = globalDisplay;
      session_ = session;
      final HandlerRegistrations registrations = new HandlerRegistrations();
      registrations.add(eventBus_.addHandler(VcsRefreshEvent.TYPE, new VcsRefreshHandler()
      {
         @Override
         public void onVcsRefresh(VcsRefreshEvent event)
         {
            if (!session.getSessionInfo().isVcsEnabled())
               registrations.removeHandler();

            if (event.getDelayMs() > 0)
            {
               Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
               {
                  @Override
                  public boolean execute()
                  {
                     refresh(false);

                     return false;
                  }
               }, event.getDelayMs());
            }
            else
            {
               Scheduler.get().scheduleDeferred(new ScheduledCommand()
               {
                  @Override
                  public void execute()
                  {
                     refresh(false);
                  }
               });
            }
         }
      }));
      registrations.add(eventBus_.addHandler(FileChangeEvent.TYPE, new FileChangeHandler()
      {
         @Override
         public void onFileChange(FileChangeEvent event)
         {
            if (!session.getSessionInfo().isVcsEnabled())
               registrations.removeHandler();

            FileChange fileChange = event.getFileChange();
            FileSystemItem file = fileChange.getFile();

            StatusAndPath status = StatusAndPath.fromInfo(
                  getStatusFromFile(file));

            if (needsFullRefresh(file))
            {
               refresh(false);
               return;
            }

            if (status_ != null && status != null)
            {
               for (int i = 0; i < status_.size(); i++)
               {
                  if (status.getRawPath() == status_.get(i).getRawPath())
                  {
                     if (StringUtil.notNull(status.getStatus()).trim().length() == 0)
                        status_.remove(i);
                     else
                        status_.set(i, status);
                     handlers_.fireEvent(new VcsRefreshEvent(Reason.FileChange));
                     return;
                  }
               }

               if (status.getStatus().trim().length() != 0)
               {
                  status_.add(status);
                  handlers_.fireEvent(new VcsRefreshEvent(Reason.FileChange));
                  return;
               }
            }
         }
      }));

      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            refresh(false);
         }
      });
   }

   public void bindRefreshHandler(Widget owner,
                                  final VcsRefreshHandler handler)
   {
      new WidgetHandlerRegistration(owner)
      {
         @Override
         protected HandlerRegistration doRegister()
         {
            return addVcsRefreshHandler(handler);
         }
      };
   }

   public HandlerRegistration addVcsRefreshHandler(VcsRefreshHandler handler)
   {
      return addVcsRefreshHandler(handler, true);
   }

   public HandlerRegistration addVcsRefreshHandler(VcsRefreshHandler handler,
                                                   boolean fireOnAdd)
   {
      HandlerRegistration hreg = handlers_.addHandler(
            VcsRefreshEvent.TYPE, handler);

      if (fireOnAdd && isInitialized())
         handler.onVcsRefresh(new VcsRefreshEvent(Reason.VcsOperation));

      return hreg;
   }

   public ArrayList<StatusAndPath> getStatus()
   {
      return status_;
   }

   public void refresh()
   {
      if (session_.getSessionInfo().isVcsEnabled())
         refresh(true);
   }

   protected abstract StatusAndPathInfo getStatusFromFile(FileSystemItem file);

   protected abstract boolean needsFullRefresh(FileSystemItem file);

   public abstract void refresh(final boolean showError);

   protected abstract boolean isInitialized();

   protected final HandlerManager handlers_ = new HandlerManager(this);
   protected ArrayList<StatusAndPath> status_;
   protected final EventBus eventBus_;
   protected final GlobalDisplay globalDisplay_;
   protected final Session session_;
}
