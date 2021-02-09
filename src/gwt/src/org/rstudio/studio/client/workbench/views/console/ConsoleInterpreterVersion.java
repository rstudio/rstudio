/*
 * ConsoleInterpreterVersion.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.console;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.RVersionsInfo;
import org.rstudio.studio.client.events.ReticulateEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.views.PythonInterpreter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class ConsoleInterpreterVersion
   extends Composite
   implements ReticulateEvent.Handler

{
   @Inject
   private void initialize(Session session,
                           EventBus events)
   {
      session_ = session;
      events_ = events;
   }
   
   public ConsoleInterpreterVersion()
   {
      this(false);
   }
   
   // isTabbedView is used to control styling based on whether
   // this widget is displayed in the "tabbed" version of the Console Pane
   // versus the "untabbed" version (when no other tabs are available)
   public ConsoleInterpreterVersion(boolean isTabbedView)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      events_.addHandler(ReticulateEvent.TYPE, this);
      
      label_ = new Label(rVersionLabel());
      label_.addStyleName(RES.styles().label());
      
      if (isTabbedView)
         label_.addStyleName(RES.styles().labelTabbed());
      else
         label_.addStyleName(RES.styles().labelUntabbed());
         
      initWidget(label_);
      setVisible(true);
   }
   
   @Override
   public void onReticulate(ReticulateEvent event)
   {
      String type = event.getType();
      
      if (StringUtil.equals(type, ReticulateEvent.TYPE_REPL_INITIALIZED))
      {
         PythonInterpreter info = event.getPayload().cast();
         label_.setText("Python (" + info.getVersion() + ")");
         label_.setTitle(info.getDescription());
      }
      else if (StringUtil.equals(type, ReticulateEvent.TYPE_REPL_TEARDOWN))
      {
         label_.setText(rVersionLabel());
      }
   }
   
   
   public String rVersionLabel()
   {
      SessionInfo sessionInfo = session_.getSessionInfo();
      if (sessionInfo == null)
         return "R (unknown)";
      
      RVersionsInfo rVersionInfo = sessionInfo.getRVersionsInfo();
      if (rVersionInfo == null)
         return "R (unknown)";
      
      String version = rVersionInfo.getRVersion();
      if (StringUtil.isNullOrEmpty(version))
         return "R (unknown)";
      
      return "R (" + version + ")";
   }
   
   public int getWidth()
   {
      return 64;
   }
   
   public int getHeight()
   {
      return 18;
   }
   
   private final Label label_;
   
   // Injected ----
   private Session session_;
   private EventBus events_;
   
   // Resources ----
   
   public interface Styles extends CssResource
   {
      String label();
      String labelTabbed();
      String labelUntabbed();
   }

   public interface Resources extends ClientBundle
   {
      @Source("ConsoleInterpreterVersion.css")
      Styles styles();
   }

   private static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }

}
