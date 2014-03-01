/*
 * TextEditingTargetRMarkdownHelper.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.rmarkdown.events.RenderRmdEvent;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownContext;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateData;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdYamlData;
import org.rstudio.studio.client.rmarkdown.model.RmdYamlResult;
import org.rstudio.studio.client.rmarkdown.model.YamlTree;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

public class TextEditingTargetRMarkdownHelper
{
   public class RmdSelectedTemplate
   {
      public RmdSelectedTemplate (RmdTemplate template, String format)
      {
         this.template = template;
         this.format = format;
      }

      RmdTemplate template;
      String format;
   }

   public TextEditingTargetRMarkdownHelper()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   public void initialize(Session session,
                          GlobalDisplay globalDisplay,
                          EventBus eventBus,
                          FileTypeCommands fileTypeCommands,
                          RMarkdownServerOperations server)
   {
      session_ = session;
      fileTypeCommands_ = fileTypeCommands;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      server_ = server;
   }
   
   public String detectExtendedType(String contents,
                                    String extendedType,
                                    TextFileType fileType)
   {
      if (extendedType.length() == 0 && 
          fileType.isMarkdown() &&
          !contents.contains("<!-- rmarkdown v1 -->") && 
          session_.getSessionInfo().getRMarkdownPackageAvailable())
      {
         return "rmarkdown";
      }
      else
      {
         return extendedType;
      }
   }
   
   public void withRMarkdownPackage(
          final String action, 
          final CommandWithArg<RMarkdownContext> onReady)
   {
      final ProgressIndicator progress = new GlobalProgressDelayer(
            globalDisplay_,
            250,
            "R Markdown...").getIndicator();
      
      server_.getRMarkdownContext(new ServerRequestCallback<RMarkdownContext>()
      {
         @Override
         public void onResponseReceived(final RMarkdownContext context)
         { 
            progress.onCompleted();
            
            if (context.getRMarkdownInstalled())
            {
               if (onReady != null)
                  onReady.execute(context);
            }
            else
            {
               installRMarkdownPackage(action, new Command() {
                  @Override
                  public void execute()
                  {
                     if (onReady != null)
                        onReady.execute(context);
                  }
                  
               });
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            progress.onError(error.getUserMessage());
            
         } 
      });
   }
   
  
   
   public void renderRMarkdown(final String sourceFile, 
                               final int sourceLine,
                               final String encoding)
   {
      withRMarkdownPackage("Rendering R Markdown documents", 
                           new CommandWithArg<RMarkdownContext>() {
         @Override
         public void execute(RMarkdownContext arg)
         {
            eventBus_.fireEvent(new RenderRmdEvent(sourceFile,
                                                   sourceLine,
                                                   encoding));
         }
      });
   }
   
   
   public boolean verifyPrerequisites(WarningBarDisplay display,
                                      TextFileType fileType)
   {
      return verifyPrerequisites(null, display, fileType);
   }
   
   public boolean verifyPrerequisites(String feature,
                                      WarningBarDisplay display,
                                      TextFileType fileType)
   {
      if (feature == null)
         feature = fileType.getLabel();
      
      // if this file requires knitr then validate pre-reqs
      boolean haveRMarkdown = 
         fileTypeCommands_.getHTMLCapabiliites().isRMarkdownSupported();
      if (!haveRMarkdown)
      {
         if (fileType.isRpres())
         {
            showKnitrPreviewWarning(display, "R Presentations", "1.2");
            return false;
         }
         else if (fileType.requiresKnit() && 
                  !session_.getSessionInfo().getRMarkdownPackageAvailable())
         {
   
            showKnitrPreviewWarning(display, feature, "1.2");
            return false;
         }
      }
      
      return true;
   }
   
   public void frontMatterToYAML(RmdFrontMatter input, 
                                 final String format,
                                 final CommandWithArg<String> onFinished)
   {
      server_.convertToYAML(input, new ServerRequestCallback<RmdYamlResult>()
      {
         @Override
         public void onResponseReceived(RmdYamlResult yamlResult)
         {
            YamlTree yamlTree = new YamlTree(yamlResult.getYaml());
            
            String title = yamlTree.getKeyValue("title");

            // The title string should be quoted--if it isn't, apply quotes
            // manually (consider: do we need to deal with multi-line titles 
            // here?)
            if (!((title.startsWith("\"") && title.endsWith("\"")) ||
                  (title.startsWith("'") && title.endsWith("'"))))
               yamlTree.setKeyValue("title", "\"" + title + "\"");

            // Order the fields more semantically
            yamlTree.reorder(
                  Arrays.asList("title", "author", "date", "output"));
            
            // Bring the chosen format to the top
            if (format != null)
               yamlTree.reorder(Arrays.asList(format));
            onFinished.execute(yamlTree.toString());
         }
         @Override
         public void onError(ServerError error)
         {
            onFinished.execute("");
         }
      });
   }
   
   public void convertFromYaml(String yaml, 
                               final CommandWithArg<RmdYamlData> onFinished)
   {
      server_.convertFromYAML(yaml, new ServerRequestCallback<RmdYamlData>()
      {
         @Override
         public void onResponseReceived(RmdYamlData yamlData)
         {
            onFinished.execute(yamlData);
         }
         @Override
         public void onError(ServerError error)
         {
            onFinished.execute(null);
         }
      });
   }

   public RmdSelectedTemplate getTemplateFormat(String yaml)
   {
      YamlTree tree = new YamlTree(yaml);
      
      // Find the template appropriate to the first output format listed
      String outFormat = null;
      List<String> outputs = tree.getChildKeys(RmdFrontMatter.OUTPUT_KEY);
      if (outputs == null)
         return null;
      if (outputs.isEmpty())
         outFormat = tree.getKeyValue(RmdFrontMatter.OUTPUT_KEY);
      else
         outFormat = outputs.get(0);
      JsArray<RmdTemplate> templates = RmdTemplateData.getTemplates();
      for (int i = 0; i < templates.length(); i++)
      {
         JsArray<RmdTemplateFormat> formats = 
               templates.get(i).getFormats();
         for (int j = 0; j < formats.length(); j++)
         {
            if (formats.get(j).getName().equals(outFormat))
            {
               return new RmdSelectedTemplate(templates.get(i), outFormat);
            }
         }
      }
      // No template found
      return null;
   }
  
   private void installRMarkdownPackage(String action,
                                        final Command onInstalled)
   {
      globalDisplay_.showYesNoMessage(
         MessageDialog.QUESTION,
         "Install Required Package", 
         action + " requires the rmarkdown package. " +
               "Do you want to install it now?",
         new Operation() {
            @Override
            public void execute()
            {
               server_.installRMarkdown(
                  new SimpleRequestCallback<ConsoleProcess>() {

                     @Override
                     public void onResponseReceived(ConsoleProcess proc)
                     {
                        final ConsoleProgressDialog dialog = 
                              new ConsoleProgressDialog(proc, server_);
                        dialog.showModal();

                        proc.addProcessExitHandler(
                           new ProcessExitEvent.Handler()
                           {
                              @Override
                              public void onProcessExit(ProcessExitEvent event)
                              {
                                 ifRMarkdownInstalled(new Command() {

                                    @Override
                                    public void execute()
                                    {
                                       dialog.hide();
                                       onInstalled.execute();
                                    }
                                 });     
                              }
                           }); 
                     }
                  });
            }
         },
         true);
   }


   private void ifRMarkdownInstalled(final Command onInstalled)
   {
      server_.getRMarkdownContext(new SimpleRequestCallback<RMarkdownContext>(){
         @Override
         public void onResponseReceived(RMarkdownContext context)
         {
            if (context.getRMarkdownInstalled())
               onInstalled.execute();
         }
      });
   }
            
   private void showKnitrPreviewWarning(WarningBarDisplay display,
                                        String feature, 
                                        String requiredVersion)
   {
      display.showWarningBar(feature + " requires the " +
                             "knitr package (version " + requiredVersion + 
                             " or higher)");
   }
   
   private Session session_;
   private GlobalDisplay globalDisplay_;
   private EventBus eventBus_;
   private FileTypeCommands fileTypeCommands_;
   private RMarkdownServerOperations server_;
}
