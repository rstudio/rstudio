/*
 * VcsState.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
import com.google.gwt.user.client.Timer;
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
import org.rstudio.studio.client.workbench.views.files.model.FileChange;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent.Reason;

import java.util.ArrayList;
import java.util.LinkedHashMap;

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
      registrations.add(eventBus_.addHandler(VcsRefreshEvent.TYPE, new VcsRefreshEvent.Handler()
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
      
      registrations.add(eventBus_.addHandler(FileChangeEvent.TYPE, new FileChangeEvent.Handler()
      {
         @Override
         public void onFileChange(FileChangeEvent event)
         {
            if (!session.getSessionInfo().isVcsEnabled())
               registrations.removeHandler();

            FileChange fileChange = event.getFileChange();
            FileSystemItem file = fileChange.getFile();

            if (needsFullRefresh(file))
            {
               fullRefreshPending_ = true;
               pendingChanges_.clear();
               scheduleApplyFileChanges();
               return;
            }

            StatusAndPath status = StatusAndPath.fromInfo(
                  getStatusFromFile(file));

            if (status_ == null || status == null)
               return;

            // coalesce changes (last change per path wins) and apply them in
            // batches -- a bulk operation can produce thousands of file change
            // events, and updating the status list and firing a refresh per
            // event makes the UI unresponsive (rebuilding the changelist table
            // once per changed file)
            pendingChanges_.put(status.getRawPath(), status);
            scheduleApplyFileChanges();
         }
      }));
   }

   private void scheduleApplyFileChanges()
   {
      // throttle rather than debounce so a steady stream of file change
      // events can't postpone applying the changes indefinitely
      if (!applyFileChangesTimer_.isRunning())
         applyFileChangesTimer_.schedule(APPLY_FILE_CHANGES_DELAY_MS);
   }

   private void applyFileChanges()
   {
      if (fullRefreshPending_)
      {
         fullRefreshPending_ = false;
         pendingChanges_.clear();
         refresh(false);
         return;
      }

      if (status_ == null || pendingChanges_.isEmpty())
      {
         pendingChanges_.clear();
         return;
      }

      // apply all pending changes in a single pass over the status list
      LinkedHashMap<String, StatusAndPath> statusByPath = new LinkedHashMap<>();
      for (StatusAndPath status : status_)
         statusByPath.put(status.getRawPath(), status);

      boolean changed = false;
      for (StatusAndPath status : pendingChanges_.values())
      {
         if (StringUtil.notNull(status.getStatus()).trim().length() == 0)
         {
            if (statusByPath.remove(status.getRawPath()) != null)
               changed = true;
         }
         else
         {
            statusByPath.put(status.getRawPath(), status);
            changed = true;
         }
      }
      pendingChanges_.clear();

      if (!changed)
         return;

      status_ = new ArrayList<>(statusByPath.values());
      handlers_.fireEvent(new VcsRefreshEvent(Reason.FileChange));
   }

   public void bindRefreshHandler(Widget owner,
                                  final VcsRefreshEvent.Handler handler)
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

   public HandlerRegistration addVcsRefreshHandler(VcsRefreshEvent.Handler handler)
   {
      return addVcsRefreshHandler(handler, true);
   }

   public HandlerRegistration addVcsRefreshHandler(VcsRefreshEvent.Handler handler,
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

   private final LinkedHashMap<String, StatusAndPath> pendingChanges_ = new LinkedHashMap<>();
   private boolean fullRefreshPending_ = false;

   private final Timer applyFileChangesTimer_ = new Timer()
   {
      @Override
      public void run()
      {
         applyFileChanges();
      }
   };

   private static final int APPLY_FILE_CHANGES_DELAY_MS = 100;
}
