/*
 * HelpStrategy.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import java.util.HashMap;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo.ParsedInfo;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HelpStrategy
{
   final CodeToolsServerOperations server_;
   
   @Inject
   public HelpStrategy(CodeToolsServerOperations server)
   {
      server_ = server;
      cache_ = new HashMap<QualifiedName, ParsedInfo>();
   }
   
   public void showHelpTopic(final QualifiedName selectedItem)
   {
      switch (selectedItem.type)
      {
         case RCompletionType.PACKAGE:
            server_.showHelpTopic(selectedItem.pkgName + "-package", null);
            break;
         default:
            server_.showHelpTopic(selectedItem.pkgName, null);
            break;
      }
   }
   
   public void showHelp(final QualifiedName item,
                        final CompletionPopupDisplay display)
   {
      switch (item.type)
      {
         case RCompletionType.PACKAGE:
            showPackageHelp(item, display);
            break;
         case RCompletionType.ARGUMENTS:
            showParameterHelp(item, display);
            break;
         default:
            showFunctionHelp(item, display);
            break;
      }
   }
   
   public void clearCache()
   {
      cache_.clear();
   }
   
   private void showFunctionHelp(final QualifiedName selectedItem,
                                 final CompletionPopupDisplay display)
   {
      ParsedInfo cachedHelp = cache_.get(selectedItem);
      if (cachedHelp != null)
      {
         display.displayFunctionHelp(cachedHelp);
         return;
      }
      
      server_.getHelp(selectedItem.name,
                      selectedItem.pkgName,
                      RCompletionType.FUNCTION,
                      new ServerRequestCallback<HelpInfo>() {
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                  "Error Retrieving Help", error.getUserMessage());
            display.clearHelp(false) ;
         }

         public void onResponseReceived(HelpInfo result)
         {
            if (result != null)
            {
               HelpInfo.ParsedInfo help = result.parse(selectedItem.name) ;
               if (help.hasInfo())
               {
                  cache_.put(selectedItem, help);
                  display.displayFunctionHelp(help) ;
                  return;
               }
            }
            display.setHelpVisible(false);
            display.clearHelp(false) ;
         }
      }) ;

   }
   
   private void showParameterHelp(final QualifiedName selectedItem,
                                  final CompletionPopupDisplay display)
   {
      
      final String name = selectedItem.name.replaceAll("\\s*=\\s*$", "");
      ParsedInfo cachedHelp = cache_.get(selectedItem);
      if (cachedHelp != null)
      {
         doShowParameterHelp(cachedHelp, name, display);
         return;
      }

         server_.getHelp(selectedItem.pkgName,
                         null,
                         RCompletionType.ARGUMENTS,
                         new ServerRequestCallback<HelpInfo>() {
            @Override
            public void onError(ServerError error)
            {
               display.clearHelp(false) ;
            }

            @Override
            public void onResponseReceived(HelpInfo response)
            {
               if (response != null)
               {
                  ParsedInfo info = response.parse(selectedItem.pkgName);
                  cache_.put(selectedItem, info);
                  doShowParameterHelp(info, name, display);
               }
               else
               {
                  display.clearHelp(false);
               }
            }
         }) ;
   }
   
   private void doShowParameterHelp(final ParsedInfo info,
                                    final String parameter,
                                    final CompletionPopupDisplay display)
   {
      String desc = info.getArgs().get(parameter) ;
      if (desc == null)
      {
         display.setHelpVisible(false);
         display.clearHelp(false) ;
      }
      else
      {
         display.displayParameterHelp(info, parameter) ;
      }
   }
   
   private void showPackageHelp(final QualifiedName selectedItem,
                                final CompletionPopupDisplay display)
   {
      ParsedInfo cachedHelp = cache_.get(selectedItem);
      if (cachedHelp != null)
      {
         doShowPackageHelp(cachedHelp, display);
         return;
      }
      
      final String packageName = selectedItem.name;
      server_.getHelp(packageName, null, RCompletionType.PACKAGE,
                      new ServerRequestCallback<HelpInfo>() {
         @Override
         public void onError(ServerError error)
         {
            display.clearHelp(false) ;
         }

         @Override
         public void onResponseReceived(HelpInfo response)
         {
            if (response != null)
            {
               ParsedInfo info = response.parse(packageName);
               cache_.put(selectedItem, info);
               doShowPackageHelp(info, display);
            }
            else
            {
               display.clearHelp(false);
            }
         }
      }) ;
   }
   
   private void doShowPackageHelp(final ParsedInfo info,
                                  final CompletionPopupDisplay display)
   {
      display.displayPackageHelp(info) ;
   }
   
   HashMap<QualifiedName, ParsedInfo> cache_;
   
}
