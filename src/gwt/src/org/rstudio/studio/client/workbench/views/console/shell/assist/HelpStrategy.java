/*
 * HelpStrategy.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import java.util.HashMap;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.codetools.RCompletionType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.ConsoleConstants;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo.ParsedInfo;

import com.google.gwt.core.client.GWT;
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
      cache_ = new HashMap<>();
   }
   
   public void showHelpTopic(final QualifiedName selectedItem)
   {
      if (selectedItem.helpHandler != null)
      {
         server_.showCustomHelpTopic(selectedItem.helpHandler, 
                                     selectedItem.name,
                                     selectedItem.source);
         
      }
      else if (selectedItem.type == RCompletionType.ROXYGEN)
      {
         server_.showVignette(selectedItem.source, "roxygen2");
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
         case RCompletionType.ROXYGEN:
            showRoxygenHelp(item, display);
            break;
         case RCompletionType.ARGUMENT:
         case RCompletionType.SECUNDARY_ARGUMENT:
         case RCompletionType.OPTION:
            showParameterHelp(item, display);
            break;
         case RCompletionType.FILE:
         case RCompletionType.DIRECTORY:
         case RCompletionType.STRING:
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
      ParsedInfo cachedHelp = cache_.get(helpKey(selectedItem));
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
               onHelpError(error, selectedItem, display);
            }

            public void onResponseReceived(HelpInfo.Custom result)
            {
               HelpInfo.ParsedInfo help =
                     result == null ? null : result.toParsedInfo();
               showLoadedHelp(selectedItem, help, display);
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
               onHelpError(error, selectedItem, display);
            }
   
            public void onResponseReceived(HelpInfo result)
            {
               HelpInfo.ParsedInfo help =
                     result == null ? null : result.parse(selectedItem.name);
               showLoadedHelp(selectedItem, help, display);
            }
         });
      }

   }

   private void onHelpError(ServerError error,
                            QualifiedName item,
                            CompletionPopupDisplay display)
   {
      Debug.logError(error);
      if (!isSelected(item, display))
         return;

      RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
            constants_.errorRetrievingHelp(), error.getUserMessage());
      display.clearHelp(false);
   }

   private void showLoadedHelp(QualifiedName item,
                               HelpInfo.ParsedInfo help,
                               CompletionPopupDisplay display)
   {
      boolean hasInfo = help != null && help.hasInfo();
      if (hasInfo)
         cache_.put(helpKey(item), help);

      if (!isSelected(item, display))
         return;

      if (hasInfo)
      {
         display.displayHelp(help);
      }
      else
      {
         display.setHelpVisible(false);
         display.clearHelp(false);
      }
   }
   
   private void showParameterHelp(final QualifiedName selectedItem,
                                  final CompletionPopupDisplay display)
   {
      
      final String name = selectedItem.name.replaceAll("\\s*=\\s*$", "");
      ParsedInfo cachedHelp = cache_.get(helpKey(selectedItem));
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
               if (isSelected(selectedItem, display))
                  display.clearHelp(false);
            }

            public void onResponseReceived(HelpInfo.Custom response)
            {
               ParsedInfo info =
                     response == null ? null : response.toParsedInfo();
               showLoadedParameterHelp(selectedItem, info, name, display);
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
               if (isSelected(selectedItem, display))
                  display.clearHelp(false);
            }

            @Override
            public void onResponseReceived(HelpInfo response)
            {
               ParsedInfo info =
                     response == null ? null : response.parse(selectedItem.source);
               showLoadedParameterHelp(selectedItem, info, name, display);
            }
         });
      }
   }

   private void showLoadedParameterHelp(QualifiedName item,
                                        ParsedInfo info,
                                        String parameter,
                                        CompletionPopupDisplay display)
   {
      if (info != null)
         cache_.put(helpKey(item), info);

      if (!isSelected(item, display))
         return;

      if (info != null)
      {
         doShowParameterHelp(info, parameter, display);
      }
      else
      {
         display.setHelpVisible(false);
         display.clearHelp(false);
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
   
   private void showRoxygenHelp(QualifiedName item, 
                                CompletionPopupDisplay display)
   {
      display.displayRoxygenHelp(item.display, item.meta, item.source != null);
   }
   
   private void showPackageHelp(final QualifiedName selectedItem,
                                final CompletionPopupDisplay display)
   {
      ParsedInfo cachedHelp = cache_.get(helpKey(selectedItem));
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
            if (isSelected(selectedItem, display))
               display.clearHelp(false);
         }

         @Override
         public void onResponseReceived(HelpInfo response)
         {
            ParsedInfo info = null;
            if (response != null)
            {
               info = response.parse(packageName);
               cache_.put(helpKey(selectedItem), info);
            }

            if (!isSelected(selectedItem, display))
               return;

            if (info != null)
            {
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

   // Identifies an item's help topic. QualifiedName.equals ignores the
   // source, but e.g. filter {dplyr} and filter {stats} have different help.
   static String helpKey(QualifiedName item)
   {
      return item.type + "\u0000" + item.name + "\u0000" +
             StringUtil.notNull(item.source) + "\u0000" +
             StringUtil.notNull(item.helpHandler);
   }

   // The selection can move on while help loads; help that arrives for an
   // item no longer selected would be shown beside, and read with, another.
   private static boolean isSelected(QualifiedName item,
                                     CompletionPopupDisplay display)
   {
      QualifiedName selected = display.getSelectedValue();
      return selected != null && helpKey(item).equals(helpKey(selected));
   }

   HashMap<String, ParsedInfo> cache_;
   private static final ConsoleConstants constants_ = GWT.create(ConsoleConstants.class);

}
