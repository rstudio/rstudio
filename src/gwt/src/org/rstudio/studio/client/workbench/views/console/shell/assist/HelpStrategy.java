/*
 * HelpStrategy.java
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
      if (selectedItem.helpHandler != null)
      {
         server_.showCustomHelpTopic(selectedItem.helpHandler, 
                                     selectedItem.name,
                                     selectedItem.source);
         
      }
      else
      {
         server_.showHelpTopic(
            selectedItem.name,
            selectedItem.source,
            selectedItem.type);
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
         case RCompletionType.ARGUMENT:
         case RCompletionType.OPTION:
            showParameterHelp(item, display);
            break;
         default:
            showDefaultHelp(item, display);
            break;
      }
   }
   
   public void clearCache()
   {
      cache_.clear();
   }
    
   private void showDefaultHelp(final QualifiedName selectedItem,
                                final CompletionPopupDisplay display)
   {
      ParsedInfo cachedHelp = cache_.get(selectedItem);
      if (cachedHelp != null)
      {
         display.displayHelp(cachedHelp);
         return;
      }
      
      if (selectedItem.helpHandler != null)
      {
         // call server
         server_.getCustomHelp(selectedItem.helpHandler,
                               selectedItem.name,
                               selectedItem.source,
                               selectedItem.language,
                               new ServerRequestCallback<HelpInfo.Custom>() {
            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
               RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                     "Error Retrieving Help", error.getUserMessage());
               display.clearHelp(false);
            }

            public void onResponseReceived(HelpInfo.Custom result)
            {
               if (result != null)
               {
                  HelpInfo.ParsedInfo help = result.toParsedInfo();
                  if (help.hasInfo())
                  {
                     cache_.put(selectedItem, help);
                     display.displayHelp(help);
                     return;
                  }
               }
               display.setHelpVisible(false);
               display.clearHelp(false);
            }
         });
      }
      else
      {
         server_.getHelp(selectedItem.name,
                         selectedItem.source,
                         selectedItem.type,
                         new ServerRequestCallback<HelpInfo>() {
            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
               RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                     "Error Retrieving Help", error.getUserMessage());
               display.clearHelp(false);
            }
   
            public void onResponseReceived(HelpInfo result)
            {
               if (result != null)
               {
                  HelpInfo.ParsedInfo help = result.parse(selectedItem.name);
                  if (help.hasInfo())
                  {
                     cache_.put(selectedItem, help);
                     display.displayHelp(help);
                     return;
                  }
               }
               display.setHelpVisible(false);
               display.clearHelp(false);
            }
         });
      }

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

      if (selectedItem.helpHandler != null)
      {
         server_.getCustomParameterHelp(
                               selectedItem.helpHandler,
                               selectedItem.source,
                               selectedItem.language,
               new ServerRequestCallback<HelpInfo.Custom>() {
            @Override
            public void onError(ServerError error)
            {
               display.clearHelp(false);
            }

            public void onResponseReceived(HelpInfo.Custom response)
            {
               if (response != null)
               {
                  HelpInfo.ParsedInfo info = response.toParsedInfo();
                  cache_.put(selectedItem, info);
                  doShowParameterHelp(info, name, display);
               }
               else
               {
                  display.setHelpVisible(false);
                  display.clearHelp(false);
               }
            }
         });
      }
      else
      {
         server_.getHelp(selectedItem.source,
                         null,
                         selectedItem.type,
                         new ServerRequestCallback<HelpInfo>() {
            @Override
            public void onError(ServerError error)
            {
               display.clearHelp(false);
            }

            @Override
            public void onResponseReceived(HelpInfo response)
            {
               if (response != null)
               {
                  ParsedInfo info = response.parse(selectedItem.source);
                  cache_.put(selectedItem, info);
                  doShowParameterHelp(info, name, display);
               }
               else
               {
                  display.setHelpVisible(false);
                  display.clearHelp(false);
               }
            }
         });
      }
   }
   
   private void doShowParameterHelp(final ParsedInfo info,
                                    final String parameter,
                                    final CompletionPopupDisplay display)
   {
      String desc = null;
      
      HashMap<String, String> mapToUse = info.getArgs();
      if (mapToUse != null)
      {
         desc = mapToUse.get(parameter);
      }
      
      if (desc == null)
      {
         mapToUse = info.getSlots();
         if (mapToUse != null)
         {
            desc = mapToUse.get(parameter);
         }
      }
      
      if (desc == null)
      {
         display.setHelpVisible(false);
         display.clearHelp(false);
      }
      else
      {
         display.displayParameterHelp(mapToUse, parameter);
      }
   }
   
   @SuppressWarnings("unused")
   private void showDataHelp(final QualifiedName selectedItem,
                             final CompletionPopupDisplay display)
   {
      ParsedInfo cachedHelp = cache_.get(selectedItem);
      if (cachedHelp != null)
      {
         doShowDataHelp(cachedHelp, display);
         return;
      }
      
      server_.getHelp(
            selectedItem.name,
            selectedItem.source,
            selectedItem.type,
            new ServerRequestCallback<HelpInfo>() {
         
         @Override
         public void onError(ServerError error)
         {
            display.clearHelp(false);
         }

         @Override
         public void onResponseReceived(HelpInfo response)
         {
            if (response != null)
            {
               ParsedInfo info = response.parse(selectedItem.name);
               cache_.put(selectedItem, info);
               doShowDataHelp(info, display);
            }
            else
            {
               display.setHelpVisible(false);
               display.clearHelp(false);
            }
         }
      });
   }
   
   private void doShowDataHelp(final ParsedInfo info,
                               final CompletionPopupDisplay display)
   {
      if (info.hasInfo())
      {
         display.displayDataHelp(info);
      }
      else
      {
         display.setHelpVisible(false);
         display.clearHelp(false);
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
      server_.getHelp(
            packageName,
            null,
            selectedItem.type,
            new ServerRequestCallback<HelpInfo>() {
         
         @Override
         public void onError(ServerError error)
         {
            display.clearHelp(false);
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
               display.setHelpVisible(false);
               display.clearHelp(false);
            }
         }
      });
   }
   
   private void doShowPackageHelp(final ParsedInfo info,
                                  final CompletionPopupDisplay display)
   {
      if (info.hasInfo())
      {
         display.displayPackageHelp(info);
      }
      else
      {
         display.setHelpVisible(false);
         display.clearHelp(false);
      }
   }
   
   HashMap<QualifiedName, ParsedInfo> cache_;
   
}
