/*
 * HelpStrategy.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;

public abstract class HelpStrategy
{
   public abstract void showHelp(QualifiedName selectedItem, 
                                 CompletionPopupDisplay display) ;
   
   public abstract void showHelpTopic(QualifiedName selectedItem) ;

   public abstract boolean isNull();

   public static HelpStrategy createFunctionStrategy(
         HelpServerOperations server)
   {
      return new FunctionStrategy(server) ;
   }
   
   public static HelpStrategy createParameterStrategy(
         HelpServerOperations server,
         String functionName)
   {
      return new ParameterStrategy(server, functionName) ;
   }

   public static HelpStrategy createNullStrategy()
   {
      return new NullStrategy();
   }

   static class NullStrategy extends HelpStrategy
   {
      public NullStrategy() {}

      @Override
      public void showHelp(QualifiedName selectedItem, CompletionPopupDisplay display)
      {
      }
      
      @Override
      public void showHelpTopic(QualifiedName selectedItem)
      {
      }

      @Override
      public boolean isNull()
      {
         return true;
      }
   }
   
   static class FunctionStrategy extends HelpStrategy
   {
      public FunctionStrategy(HelpServerOperations server)
      {
         super() ;
         server_ = server ;
      }

      @Override
      public void showHelp(final QualifiedName selectedItem,
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

               display.clearHelp(false) ;
            }
         }) ;
         
      }
      
      @Override
      public void showHelpTopic(QualifiedName selectedItem)
      {
         server_.showHelpTopic(selectedItem.name, selectedItem.pkgName) ;
      }

      @Override
      public boolean isNull()
      {
         return false;
      }

      protected final HelpServerOperations server_ ;
   }
   
   static class ParameterStrategy extends FunctionStrategy
   {
      public ParameterStrategy(HelpServerOperations server, String functionName)
      {
         super(server) ;
         functionName_ = functionName ;
      }
      
      @Override
      public void showHelp(QualifiedName qname,
                           final CompletionPopupDisplay display)
      {
         String selectedItem = qname.name ;
         
         if (selectedItem.endsWith("="))
         {
            assert StringUtil.isNullOrEmpty(qname.pkgName)
                              : "Completion parameter had a package name!? " +
                                qname.pkgName + "::" + qname.name;
            selectedItem = selectedItem.substring(0, selectedItem.length() - 1) ;
            
            parameter_ = selectedItem ;
            if (helpInfo_ != null)
            {
               doShow(display) ;
            }
            else
            {
               server_.getHelp(functionName_, null, 0,
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
                              helpInfo_ = response.parse(functionName_) ;
                           else
                              helpInfo_ = null;

                           if (helpInfo_ != null)
                              doShow(display) ;
                           else
                              display.clearHelp(false);
                        }
                     }) ;
            }
         }
         else
         {
            super.showHelp(qname, display) ;
         }
      }
      
      @Override
      public void showHelpTopic(QualifiedName selectedItem)
      {
         server_.showHelpTopic(functionName_, null) ;
      }
      
      private void doShow(CompletionPopupDisplay display)
      {
         assert helpInfo_ != null && parameter_ != null && display != null ;
         String desc = helpInfo_.getArgs().get(parameter_) ;
         if (desc == null)
         {
            display.clearHelp(false) ;
         }
         else
         {
            display.displayParameterHelp(helpInfo_, parameter_) ;
         }
      }
      
      private final String functionName_ ;
      
      private HelpInfo.ParsedInfo helpInfo_ ;
      private String parameter_ ;
   }
}
