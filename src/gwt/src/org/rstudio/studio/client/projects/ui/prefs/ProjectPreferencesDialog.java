/*
 * ProjectPreferencesDialog.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.prefs.PreferencesDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.packrat.PackratUtil;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.projects.model.RProjectPackratOptions;
import org.rstudio.studio.client.projects.ui.prefs.buildtools.ProjectBuildToolsPreferencesPane;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ProjectPreferencesDialog extends PreferencesDialogBase<RProjectOptions>
{
   public static final int GENERAL = 0;
   public static final int EDITING = 1;
   public static final int SWEAVE = 2;
   public static final int BUILD = 3;
   public static final int VCS = 4;
   public static final int PACKRAT = 5;
   public static final int SHARING = 6;
   
   @Inject
   public ProjectPreferencesDialog(ProjectsServerOperations server,
                                   Provider<UIPrefs> pUIPrefs,
                                   Provider<EventBus> pEventBus,
                                   Provider<PackratUtil> pPackratUtil,
                                   Provider<Session> session,
                                   ProjectGeneralPreferencesPane general,
                                   ProjectEditingPreferencesPane editing,
                                   ProjectCompilePdfPreferencesPane compilePdf,
                                   ProjectSourceControlPreferencesPane source,
                                   ProjectBuildToolsPreferencesPane build,
                                   ProjectPackratPreferencesPane packrat,
                                   ProjectSharingPreferencesPane sharing)
   {      
      super("Project Options",
            RES.styles().panelContainer(),
            false,
            new ProjectPreferencesPane[] {general, editing, compilePdf, build, 
                                          source, packrat, sharing});
           
      session_ = session;
      server_ = server;
      pUIPrefs_ = pUIPrefs;
      pEventBus_ = pEventBus;
      pPackratUtil_ = pPackratUtil;
   }
   
   @Override
   public void initialize(RProjectOptions options)
   {
      super.initialize(options);
      
      initialPackratOptions_ = options.getPackratOptions();
      
      if (!session_.get().getSessionInfo().getAllowVcs())
         hidePane(VCS);
      
      if (!options.getPackratContext().isAvailable())
         hidePane(PACKRAT);
      
      if (!session_.get().getSessionInfo().projectSupportsSharing())
         hidePane(SHARING);
   }
   
   @Override
   protected RProjectOptions createEmptyPrefs()
   {
      return RProjectOptions.createEmpty();
   }
   
   
   @Override
   protected void doSaveChanges(final RProjectOptions options,
                                final Operation onCompleted,
                                final ProgressIndicator indicator,
                                final boolean reload)
   {
      
      server_.writeProjectOptions(
          options, 
          new ServerRequestCallback<Void>() {
             @Override
             public void onResponseReceived(Void response)
             {
                indicator.onCompleted();
                
                // update project ui prefs
                RProjectConfig config = options.getConfig();
                UIPrefs uiPrefs = pUIPrefs_.get();
                uiPrefs.useSpacesForTab().setProjectValue(
                                           config.getUseSpacesForTab());
                uiPrefs.numSpacesForTab().setProjectValue(
                                           config.getNumSpacesForTab());
                uiPrefs.autoAppendNewline().setProjectValue(
                                           config.getAutoAppendNewline());
                uiPrefs.stripTrailingWhitespace().setProjectValue(
                                           config.getStripTrailingWhitespace());
                uiPrefs.defaultEncoding().setProjectValue(
                                           config.getEncoding()); 
                uiPrefs.defaultSweaveEngine().setProjectValue(
                                           config.getDefaultSweaveEngine());
                uiPrefs.defaultLatexProgram().setProjectValue(
                                           config.getDefaultLatexProgram());
                uiPrefs.rootDocument().setProjectValue(
                                           config.getRootDocument());
                uiPrefs.useRoxygen().setProjectValue(
                                           config.hasPackageRoxygenize());
                
                // convert packrat option changes to console actions
                emitPackratConsoleActions(options.getPackratOptions());
                
                if (onCompleted != null)
                   onCompleted.execute();
                if (reload)
                   reload();
             }

             @Override
             public void onError(ServerError error)
             {
                indicator.onError(error.getUserMessage());
             }         
          });
      
   }
   
   private void emitPackratConsoleActions(RProjectPackratOptions options)
   {
      String packratFunction = null;
      String packratArgs = null;
      
      // case: enabling packrat
      if (options.getUsePackrat() && !initialPackratOptions_.getUsePackrat())
      {
         packratFunction = "init";
         String optionArgs = packratArgs(options);
         if (optionArgs.length() > 0)
            packratArgs = "options = list(" + optionArgs + ")";
      }
      // case: disabling packart
      else if (!options.getUsePackrat() && initialPackratOptions_.getUsePackrat())
      {
         packratFunction = "disable";
      }
      // case: changing packrat options
      else
      {
         packratArgs = packratArgs(options);
         if (!StringUtil.isNullOrEmpty(packratArgs))
            packratFunction = "set_opts";
      }
      
      if (packratFunction != null)
      {
         // build the call
         StringBuilder b = new StringBuilder();
         
         b.append("packrat::");
         b.append(packratFunction);
         b.append("(");
         
         String projectArg = pPackratUtil_.get().packratProjectArg();
         if (projectArg.length() > 0)
         {
            b.append(projectArg);
            if (packratArgs != null)
               b.append(", ");
         }
         
         if (packratArgs != null)
            b.append(packratArgs);
         
         b.append(")"); 
         
         pEventBus_.get().fireEvent(new SendToConsoleEvent(b.toString(), 
                                                           true, 
                                                           true));
      }
      
   }
   
   private boolean equals(JsArrayString lhsJson, JsArrayString rhsJson)
   {
      String[] lhs = JsUtil.toStringArray(lhsJson);
      String[] rhs = JsUtil.toStringArray(rhsJson);
      if (lhs.length != rhs.length) return false;
      for (int i = 0; i < lhs.length; ++i)
      {
         if (!lhs[i].equals(rhs[i]))
         {
            return false;
         }
      }
      return true;
   }
   
   private String packratArgs(RProjectPackratOptions options)
   {
      ArrayList<String> opts = new ArrayList<String>();
      
      if (options.getAutoSnapshot() != initialPackratOptions_.getAutoSnapshot())
         opts.add(packratBoolArg("auto.snapshot", options.getAutoSnapshot()));

      if (options.getVcsIgnoreLib() != initialPackratOptions_.getVcsIgnoreLib())
         opts.add(packratBoolArg("vcs.ignore.lib", options.getVcsIgnoreLib()));
      
      if (options.getVcsIgnoreSrc() != initialPackratOptions_.getVcsIgnoreSrc())
         opts.add(packratBoolArg("vcs.ignore.src", options.getVcsIgnoreSrc()));
      
      if (options.getUseCache() != initialPackratOptions_.getUseCache())
         opts.add(packratBoolArg("use.cache", options.getUseCache()));
      
      if (!equals(options.getExternalPackages(),
            initialPackratOptions_.getExternalPackages()))
         opts.add(packratVectorArg("external.packages",
               options.getExternalPackages()));
      
      if (!equals(options.getLocalRepos(),
            initialPackratOptions_.getLocalRepos()))
         opts.add(packratVectorArg("local.repos",
               options.getLocalRepos()));
      
      return StringUtil.joinStrings(opts, ", "); 
   }
   
   private String packratBoolArg(String name, boolean value)
   {
      return name + " = " + (value ? "TRUE" : "FALSE");
   }
 
   private String packratVectorArg(String name, JsArrayString valueJson)
   {
      String[] value = JsUtil.toStringArray(valueJson);
      String result = name + " = ";
      if (value.length < 1) return result + "\"\"";
      result += "c(";
      for (int i = 0; i < value.length - 1; ++i)
      {
         result += StringUtil.ensureSurroundedWith(
               value[i].replaceAll("\"",  "\\\\\""), '"');
         result += ", ";
      }
      result += StringUtil.ensureSurroundedWith(
            value[value.length - 1].replaceAll("\"",  "\\\\\""), '"');
      result += ")";
      return result;
   }
 
   private final Provider<Session> session_;
   private final ProjectsServerOperations server_;
   private final Provider<UIPrefs> pUIPrefs_;
   private final Provider<EventBus> pEventBus_;
   private final Provider<PackratUtil> pPackratUtil_;
   
   private RProjectPackratOptions initialPackratOptions_ = null;
   
   private static final ProjectPreferencesDialogResources RES =
                                 ProjectPreferencesDialogResources.INSTANCE;


  
}
