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
   }
   
   public void showHelpTopic(final QualifiedName selectedItem)
   {
      server_.showHelpTopic(selectedItem.pkgName, null) ;
   }
   
   
   public void showHelp(final QualifiedName item,
                        final CompletionPopupDisplay display)
   {
      switch (item.type)
      {
         case RCompletionType.PACKAGE:
            showPackageHelp(item.name, display);
            break;
         case RCompletionType.ARGUMENTS:
            showParameterHelp(item, display);
            break;
         default:
            showFunctionHelp(item, display);
            break;
      }
   }
   
   private void showFunctionHelp(final QualifiedName selectedItem,
                                 final CompletionPopupDisplay display)
   {
      server_.getHelp(selectedItem.name, selectedItem.pkgName, 0, 
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
                  display.displayFunctionHelp(help) ;
                  return;
               }
            }
            display.setHelpVisible(false);
            display.clearHelp(false) ;
         }
      }) ;

   }
   
   private void showParameterHelp(final QualifiedName qname,
                                  final CompletionPopupDisplay display)
   {
      final String selectedItem = qname.name.replaceAll("\\s*=\\s*$", "");

         server_.getHelp(qname.pkgName, null, 0,
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
                  ParsedInfo info = response.parse(qname.pkgName);
                  doShowParameterHelp(info, selectedItem, display);
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
   
   private void showPackageHelp(final String packageName,
                                final CompletionPopupDisplay display)
   {
      server_.getHelp(packageName, null, 0,
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
}
