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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.notebookv2.CompileNotebookv2Options;
import org.rstudio.studio.client.notebookv2.CompileNotebookv2OptionsDialog;
import org.rstudio.studio.client.notebookv2.CompileNotebookv2Prefs;
import org.rstudio.studio.client.rmarkdown.events.RenderRmdEvent;
import org.rstudio.studio.client.rmarkdown.events.RenderRmdSourceEvent;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownContext;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdChosenTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdCreatedTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatterOutputOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdOutputFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateContent;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateData;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;
import org.rstudio.studio.client.rmarkdown.model.RmdYamlData;
import org.rstudio.studio.client.rmarkdown.model.RmdYamlResult;
import org.rstudio.studio.client.rmarkdown.model.YamlTree;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.events.FileEditEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

public class TextEditingTargetRMarkdownHelper
{
   public class RmdSelectedTemplate
   {
      public RmdSelectedTemplate (RmdTemplate template, String format, 
                                  boolean isShiny)
      {
         this.template = template;
         this.format = format;
         this.isShiny = isShiny;
      }

      RmdTemplate template;
      String format;
      boolean isShiny;
   }

   public TextEditingTargetRMarkdownHelper()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   public void initialize(Session session,
                          GlobalDisplay globalDisplay,
                          EventBus eventBus,
                          UIPrefs prefs,
                          FileTypeCommands fileTypeCommands,
                          DependencyManager dependencyManager,
                          RMarkdownServerOperations server,
                          FilesServerOperations fileServer)
   {
      session_ = session;
      fileTypeCommands_ = fileTypeCommands;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      prefs_ = prefs;
      dependencyManager_ = dependencyManager;
      server_ = server;
      fileServer_ = fileServer;
   }
   
   public String detectExtendedType(String contents,
                                    String extendedType,
                                    TextFileType fileType)
   {
      if (extendedType.length() == 0 && 
          fileType.isMarkdown() &&
          useRMarkdownV2(contents))
      {
         return "rmarkdown";
      }
      else
      {
         return extendedType;
      }
   }
   
   public void withRMarkdownPackage(
          final String userAction, 
          final boolean isShinyDoc,
          final CommandWithArg<RMarkdownContext> onReady)
   {
      dependencyManager_.withRMarkdown(
         
         userAction,  
         
         new Command() {
            
            @Override
            public void execute()
            { 
               // command to execute when we are ready
               Command callReadyCommand = new Command() {
                  @Override
                  public void execute()
                  {
                     server_.getRMarkdownContext(
                        new SimpleRequestCallback<RMarkdownContext>() {

                           @Override
                           public void onResponseReceived(RMarkdownContext ctx)
                           {
                              if (onReady != null)
                                 onReady.execute(ctx);
                           }      
                        });
                  }  
               };
               
               // check if this is a Shiny Doc
               if (isShinyDoc)
               {
                  dependencyManager_.withShiny("Running shiny documents",
                                               callReadyCommand);
               }
               else
               {
                  callReadyCommand.execute();
               }
            } 
         });
   }
   
   public void renderNotebookv2(final DocUpdateSentinel sourceDoc)
   { 
      withRMarkdownPackage("Compiling notebooks from R scripts",
                           false,
         new CommandWithArg<RMarkdownContext>() {
            @Override
            public void execute(RMarkdownContext arg)
            {
               // see if we already have a format defined
               server_.rmdOutputFormat(sourceDoc.getPath(),
                                       sourceDoc.getEncoding(),
                                       new SimpleRequestCallback<String>() {
                     @Override
                     public void onResponseReceived(String format)
                     {
                        if (format == null)
                           renderNotebookv2WithDialog(sourceDoc);
                        else
                           renderNotebookv2(sourceDoc, format);
                     }
               });
            }
          });
   }
  
   final String NOTEBOOK_FORMAT = "notebook_format";
   
   private void renderNotebookv2WithDialog(final DocUpdateSentinel sourceDoc)
   {
      // default format
      String format = sourceDoc.getProperty(NOTEBOOK_FORMAT);
      if (StringUtil.isNullOrEmpty(format))
      {
         format = prefs_.compileNotebookv2Options()
                                             .getValue().getFormat();
         if (StringUtil.isNullOrEmpty(format))
            format = CompileNotebookv2Options.FORMAT_DEFAULT;
      }
      
      CompileNotebookv2OptionsDialog dialog = 
            new CompileNotebookv2OptionsDialog(
                  format,
                  new OperationWithInput<CompileNotebookv2Options>()
      {
         @Override
         public void execute(CompileNotebookv2Options input)
         { 
            renderNotebookv2(sourceDoc, input.getFormat());
            
            // save options for this document
            HashMap<String, String> changedProperties 
                                          = new HashMap<String, String>();
            changedProperties.put(NOTEBOOK_FORMAT, input.getFormat());
            sourceDoc.modifyProperties(changedProperties, null);

            // save global prefs
            CompileNotebookv2Prefs prefs = 
                  CompileNotebookv2Prefs.create(input.getFormat());
            if (!CompileNotebookv2Prefs.areEqual(
                  prefs, 
                  prefs_.compileNotebookv2Options().getValue()))
            {
               prefs_.compileNotebookv2Options().setGlobalValue(prefs);
               prefs_.writeUIPrefs();
            }
         }
      }
      );
      dialog.showModal();
   }
   
   private void renderNotebookv2(final DocUpdateSentinel sourceDoc,
                                 String format)
   {
      eventBus_.fireEvent(new RenderRmdEvent(sourceDoc.getPath(), 
                                             1, 
                                             format, 
                                             sourceDoc.getEncoding(),
                                             false,
                                             false));
   }
   
   public void renderRMarkdown(final String sourceFile, 
                               final int sourceLine,
                               final String format,
                               final String encoding, 
                               final boolean asTempfile,
                               final boolean isShinyDoc,
                               final boolean asShiny)
   {
      withRMarkdownPackage("Rendering R Markdown documents", 
                           isShinyDoc,
                           new CommandWithArg<RMarkdownContext>() {
         @Override
         public void execute(RMarkdownContext arg)
         {
            eventBus_.fireEvent(new RenderRmdEvent(sourceFile,
                                                   sourceLine,
                                                   format,
                                                   encoding, 
                                                   asTempfile,
                                                   asShiny));
         }
      });
   }
   
   public void renderRMarkdownSource(final String source,
                                     final boolean isShinyDoc)
   {
      withRMarkdownPackage("Rendering R Markdown documents", 
                           isShinyDoc,
            new CommandWithArg<RMarkdownContext>() {
         @Override
         public void execute(RMarkdownContext arg)
         {
            eventBus_.fireEvent(new RenderRmdSourceEvent(source));
         }
      });
   }
   
   
   public void prepareForRmdChunkExecution(String id, 
                                        String contents,
                                        final Command onExecuteChunk)
   {
      // if this is R Markdown v2 then look for params
      if (useRMarkdownV2(contents))
      {
         server_.prepareForRmdChunkExecution(id, 
                                             new VoidServerRequestCallback() {
            
            @Override
            protected void onCompleted()
            {
               onExecuteChunk.execute();
            }
         });
      }
      else
      {
         onExecuteChunk.execute();
      }
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
            
            // quote fields
            quoteField(yamlTree, "title");
            quoteField(yamlTree, "author");
            quoteField(yamlTree, "date");
            
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
         private void quoteField(YamlTree yamlTree, String field)
         {
            String value = yamlTree.getKeyValue(field);

            // The string should be quoted if it's a single line.
            if (value.length() > 0 && value.indexOf("\n") == -1) 
            {
               if (!((value.startsWith("\"") && value.endsWith("\"")) ||
                     (value.startsWith("'") && value.endsWith("'"))))
                  yamlTree.setKeyValue(field, "\"" + value + "\"");
            }
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
   
   // Return the template appropriate to the given output format
   public RmdTemplate getTemplateForFormat(String outFormat)
   {
      JsArray<RmdTemplate> templates = RmdTemplateData.getTemplates();
      for (int i = 0; i < templates.length(); i++)
      {
         RmdTemplateFormat format = templates.get(i).getFormat(outFormat);
         if (format != null)
            return templates.get(i);
      }
      // No template found
      return null;
   }

   // Return the selected template and format given the YAML front matter
   public RmdSelectedTemplate getTemplateFormat(String yaml)
   {
      // This is in the editor load path, so guard against exceptions and log
      // any we find without bringing down the editor. Failing to find a 
      // template here just turns off the template-specific UI format editor.
      try
      {
         YamlTree tree = new YamlTree(yaml);
         boolean isShiny = false;
         
         if (tree.getKeyValue(RmdFrontMatter.KNIT_KEY).length() > 0)
            return null;
         
         // Find the template appropriate to the first output format listed.
         // If no output format is present, assume HTML document (as the 
         // renderer does).
         List<String> outFormats = getOutputFormats(tree);
         String outFormat = outFormats == null ? 
               RmdOutputFormat.OUTPUT_HTML_DOCUMENT :
               outFormats.get(0);
         
         RmdTemplate template = getTemplateForFormat(outFormat);
         if (template == null)
            return null;
         
         // If this format produces HTML and is marked as Shiny, treat it as
         // a Shiny format
         if (template.getFormat(outFormat).getExtension().equals("html") &&
             tree.getKeyValue(RmdFrontMatter.RUNTIME_KEY).equals(
                       RmdFrontMatter.SHINY_RUNTIME))
         {
            isShiny = true;
         }
         
         return new RmdSelectedTemplate(template, outFormat, isShiny);
      }
      catch (Exception e)
      {
         Debug.log("Warning: Exception thrown while parsing YAML:\n" + yaml);
      }
      return null;
   }
   
   // Parses YAML, adds the given format option with any transferable
   // defaults, and returns the resulting YAML
   public void setOutputFormat(String yaml, final String format, 
                               final CommandWithArg<String> onCompleted)
   {
      convertFromYaml(yaml, new CommandWithArg<RmdYamlData>()
      {
         @Override
         public void execute(RmdYamlData arg)
         {
            if (!arg.parseSucceeded())
               onCompleted.execute(null);
            else
               setOutputFormat(arg.getFrontMatter(), format, onCompleted);
         }
      });
   }
   
   public void createDraftFromTemplate(final RmdChosenTemplate template)
   {
      final String target = template.getDirectory() + "/" + 
                            template.getFileName();
      final String targetFile = target + (template.createDir() ? "" : ".Rmd");
      fileServer_.stat(targetFile, new ServerRequestCallback<FileSystemItem>()
      {
         @Override
         public void onResponseReceived(final FileSystemItem fsi)
         {
            // the file doesn't exist--proceed
            if (!fsi.exists())
            {
               createDraftFromTemplate(template, target);
               return;
            }

            // the file exists--offer to clean it up and continue.
            globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION, 
                  "Overwrite " + (template.createDir() ? "Directory" : "File"), 
                  targetFile + " exists. Overwrite it?", false, 
                  new Operation()
                  {
                     @Override
                     public void execute()
                     {
                        cleanAndCreateTemplate(template, target, fsi);
                     }
                  }, null, null, "Overwrite", "Cancel", false);
         }

         @Override
         public void onError(ServerError error)
         {
            // presumably the file doesn't exist, which is what we want.
            createDraftFromTemplate(template, target);
         }
      });
   }
   
   public String convertYamlToShinyDoc(String yaml)
   {
      YamlTree yamlTree = new YamlTree(yaml);
      yamlTree.addYamlValue(null, "runtime", "shiny");
      
      return yamlTree.toString();
   }
   
   public void replaceOutputFormatOptions(final String yaml, 
         final String format, final RmdFrontMatterOutputOptions options, 
         final OperationWithInput<String> onCompleted)
   {
      server_.convertToYAML(options, new ServerRequestCallback<RmdYamlResult>()
      {
         @Override
         public void onResponseReceived(RmdYamlResult result)
         {
            boolean isDefault = options.getOptionList().length() == 0;
            YamlTree yamlTree = new YamlTree(yaml);
            YamlTree optionTree = new YamlTree(result.getYaml());
            // add the output key if needed
            if (!yamlTree.containsKey(RmdFrontMatter.OUTPUT_KEY))
            {
               yamlTree.addYamlValue(null, RmdFrontMatter.OUTPUT_KEY, 
                     RmdOutputFormat.OUTPUT_HTML_DOCUMENT);
            }
            String treeFormat = yamlTree.getKeyValue(RmdFrontMatter.OUTPUT_KEY);

            if (treeFormat.equals(format))
            {
               // case 1: the output format is a simple format and we're not
               // changing to a different format

               if (isDefault)
               {
                  // 1-a: if all options are still at their defaults, leave
                  // untouched
               }
               else
               {
                  // 1-b: not all options are at defaults; replace the simple
                  // format with an option list
                  yamlTree.setKeyValue(RmdFrontMatter.OUTPUT_KEY, "");
                  yamlTree.addYamlValue(RmdFrontMatter.OUTPUT_KEY, format, "");
                  yamlTree.setKeyValue(format, optionTree);
               }
            }
            else if (treeFormat.length() > 0)
            {
               // case 2: the output format is a simple format and we are 
               // changing it
               if (isDefault)
               {
                  // case 2-a: change one simple format to another 
                  yamlTree.setKeyValue(RmdFrontMatter.OUTPUT_KEY, format);
               }
               
               else
               {
                  // case 2-b: change a simple format to a complex one
                  yamlTree.setKeyValue(RmdFrontMatter.OUTPUT_KEY, "");
                  yamlTree.addYamlValue(RmdFrontMatter.OUTPUT_KEY, format, "");
                  yamlTree.setKeyValue(format, optionTree);
               }
            }
            else
            {
               // case 3: the output format is already not simple
               treeFormat = yamlTree.getKeyValue(format);
               
               if (treeFormat.equals(RmdFrontMatter.DEFAULT_FORMAT))
               {
                  if (isDefault)
                  {
                     // case 3-a: still at default settings
                  }
                  else
                  {
                     // case 3-b: default to complex
                     yamlTree.setKeyValue(format, optionTree);
                  }
               }
               else
               {
                  if (isDefault)
                  {
                     // case 3-c: complex to default
                     if (yamlTree.getChildKeys(
                           RmdFrontMatter.OUTPUT_KEY).size() == 1)
                     {
                        // case 3-c-i: only one format, and has default settings
                        yamlTree.clearChildren(RmdFrontMatter.OUTPUT_KEY);
                        yamlTree.setKeyValue(RmdFrontMatter.OUTPUT_KEY, format);
                     }
                     else
                     {
                        // case 3-c-i: multiple formats, this one's becoming
                        // the default
                        yamlTree.clearChildren(format);
                        yamlTree.setKeyValue(format, RmdFrontMatter.DEFAULT_FORMAT);
                     }
                  }
                  else
                  {
                     // case 3-d: complex to complex
                     if (!yamlTree.containsKey(format))
                     {
                        yamlTree.addYamlValue(RmdFrontMatter.OUTPUT_KEY, 
                              format, "");
                     }
                     yamlTree.setKeyValue(format, optionTree);
                  }
               }
            }

            yamlTree.reorder(Arrays.asList(format));
            onCompleted.execute(yamlTree.toString());
         }
         @Override
         public void onError(ServerError error)
         {
            // if we fail, return the unmodified YAML
            onCompleted.execute(yaml);
         }
      });
   }
   
   public void getTemplateContent(
         final RmdChosenTemplate template, 
         final OperationWithInput<String> onContentReceived)
   {
      server_.getRmdTemplate(template.getTemplatePath(), 
         new ServerRequestCallback<RmdTemplateContent>()
         {
            @Override 
            public void onResponseReceived (RmdTemplateContent content)
            {
               onContentReceived.execute(content.getContent());
            }
            @Override
            public void onError(ServerError error)
            {
               globalDisplay_.showErrorMessage("Template Creation Failed", 
                     "Failed to load content from the template at " + 
                     template.getTemplatePath() + ": " + error.getMessage());
            }
         });
   }
   
   public void addAdditionalResourceFiles(String yaml, 
         final ArrayList<String> files, 
         final CommandWithArg<String> onCompleted)
   {
      convertFromYaml(yaml, new CommandWithArg<RmdYamlData>() 
      {
         @Override
         public void execute(RmdYamlData arg)
         {
            if (!arg.parseSucceeded())
               onCompleted.execute(null);
            else
               addAdditionalResourceFiles(arg.getFrontMatter(), files, 
                     onCompleted);
         }
      });
   }

   // Private methods ---------------------------------------------------------
   
   private void cleanAndCreateTemplate(final RmdChosenTemplate template, 
                                       final String target,
                                       final FileSystemItem oldFile)
   {
      ArrayList<FileSystemItem> oldFiles = new ArrayList<FileSystemItem>();
      oldFiles.add(oldFile);
      fileServer_.deleteFiles(oldFiles, new ServerRequestCallback<Void>() 
         {
            @Override
            public void onResponseReceived(Void v)
            {
               createDraftFromTemplate(template, target);
            }

            @Override
            public void onError(ServerError error)
            {
               globalDisplay_.showErrorMessage("File Remove Failed", 
                     "Couldn't remove " + oldFile.getPath());
            }
         });
   }
   
   private void createDraftFromTemplate(final RmdChosenTemplate template, 
                                        final String target)
   {
      final ProgressIndicator progress = new GlobalProgressDelayer(
            globalDisplay_,
            250,
            "Creating R Markdown Document...").getIndicator();

      server_.createRmdFromTemplate(target, 
            template.getTemplatePath(), template.createDir(), 
            new ServerRequestCallback<RmdCreatedTemplate>() {
               @Override
               public void onResponseReceived(RmdCreatedTemplate created)
               {
                  // write a pref indicating this is the preferred template--
                  // we'll default to it the next time we load the template list
                  prefs_.rmdPreferredTemplatePath().setGlobalValue(
                        template.getTemplatePath());
                  prefs_.writeUIPrefs();
                  FileSystemItem file =
                        FileSystemItem.createFile(created.getPath());
                  eventBus_.fireEvent(new FileEditEvent(file));
                  progress.onCompleted();
               }

               @Override
               public void onError(ServerError error)
               {
                  progress.onError(
                        "Couldn't create a template from " + 
                        template.getTemplatePath() + " at " + target + ".\n\n" +
                        error.getMessage());
               }
            });
   }
   
   private void setOutputFormat(RmdFrontMatter frontMatter, String format, 
                                final CommandWithArg<String> onCompleted)
   {
      // If the format list doesn't already contain the given format, add it
      // to the list and transfer any applicable options
      if (!JsArrayUtil.jsArrayStringContains(frontMatter.getFormatList(), 
                                             format))
      {
         RmdTemplate template = getTemplateForFormat(format);
         RmdFrontMatterOutputOptions opts = RmdFrontMatterOutputOptions.create();
         if (template != null)
         {
            opts = transferOptions(frontMatter, template, format);
         }
         frontMatter.setOutputOption(format, opts);
      }
      frontMatterToYAML(frontMatter, format, onCompleted);
   }
   
   private RmdFrontMatterOutputOptions transferOptions(
         RmdFrontMatter frontMatter, 
         RmdTemplate template,
         String format)
   {
      RmdFrontMatterOutputOptions result = RmdFrontMatterOutputOptions.create();

      // loop over each option applicable to the new format; if it's
      // transferable, try to find it in one of the other formats 
      JsArrayString options = template.getFormat(format).getOptions();
      for (int i = 0; i < options.length(); i++)
      {
         String optionName = options.get(i);
         RmdTemplateFormatOption option = template.getOption(optionName);
         if (!option.isTransferable())
            continue;
         
         // option is transferable, is it present in another front matter entry?
         JsArrayString formats = frontMatter.getFormatList();
         for (int j = 0; j < formats.length(); j++)
         {
            RmdFrontMatterOutputOptions outOptions = 
                  frontMatter.getOutputOption(formats.get(j));
            if (outOptions == null)
               continue;
            String val = outOptions.getOptionValue(optionName);
            if (val != null)
               result.setOptionValue(option, val);
         }
      }
      
      return result;
   }
      
   private List<String> getOutputFormats(YamlTree tree)
   {
      List<String> outputs = tree.getChildKeys(RmdFrontMatter.OUTPUT_KEY);
      if (outputs == null)
         return null;
      if (outputs.isEmpty())
         outputs.add(tree.getKeyValue(RmdFrontMatter.OUTPUT_KEY));
      return outputs;
   }
              
   private void showKnitrPreviewWarning(WarningBarDisplay display,
                                        String feature, 
                                        String requiredVersion)
   {
      display.showWarningBar(feature + " requires the " +
                             "knitr package (version " + requiredVersion + 
                             " or higher)");
   }
   
   private void addAdditionalResourceFiles(RmdFrontMatter frontMatter,
         ArrayList<String> additionalFiles, 
         CommandWithArg<String> onCompleted)
   {
      for (String file: additionalFiles)
      {
         frontMatter.addResourceFile(file);
      }

      frontMatterToYAML(frontMatter, null, onCompleted);
   }
   
   private boolean useRMarkdownV2(String contents)
   {
      return !contents.contains("<!-- rmarkdown v1 -->") && 
              session_.getSessionInfo().getRMarkdownPackageAvailable();
   }
   
   private Session session_;
   private GlobalDisplay globalDisplay_;
   private EventBus eventBus_;
   private UIPrefs prefs_;
   private FileTypeCommands fileTypeCommands_;
   private DependencyManager dependencyManager_;
   private RMarkdownServerOperations server_;
   private FilesServerOperations fileServer_;
}
