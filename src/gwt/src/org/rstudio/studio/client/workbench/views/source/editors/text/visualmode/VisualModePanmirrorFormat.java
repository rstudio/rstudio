/*
 * VisualModePanmirrorFormat.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.panmirror.PanmirrorWidget;
import org.rstudio.studio.client.panmirror.format.PanmirrorExtendedDocType;
import org.rstudio.studio.client.panmirror.format.PanmirrorFormat;
import org.rstudio.studio.client.panmirror.format.PanmirrorHugoExtensions;
import org.rstudio.studio.client.panmirror.format.PanmirrorRmdExtensions;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorPandocFormatConfig;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsFormat;
import org.rstudio.studio.client.quarto.QuartoHelper;
import org.rstudio.studio.client.rmarkdown.model.RmdOutputFormat;
import org.rstudio.studio.client.rmarkdown.model.YamlFrontMatter;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.model.BlogdownConfig;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.inject.Inject;

// determine the current panmirror editing format (requires consultation of 
// many thing including the current RStudio project, the type of the source 
// file, yaml editing options, etc.). also provides some utility functions
// for checking the output format / type of the current document.

public class VisualModePanmirrorFormat
{
   public VisualModePanmirrorFormat(DocUpdateSentinel docUpdateSentinel,
                                    DocDisplay docDisplay,
                                    TextEditingTarget target,
                                    TextEditingTarget.Display view)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      docUpdateSentinel_ = docUpdateSentinel;
      docDisplay_ = docDisplay;
      target_ = target;
      view_ = view;
   }
   
   @Inject
   void initialize(WorkbenchContext workbenchContext, Session session)
   {
      workbenchContext_ = workbenchContext;
      sessionInfo_ = session.getSessionInfo();
   }
   
   public PanmirrorWidget.FormatSource formatSource()
   {
      return new PanmirrorWidget.FormatSource()
      {
         @Override
         public PanmirrorFormat getFormat(PanmirrorUIToolsFormat formatTools)
         {
            // create format
            PanmirrorFormat format = new PanmirrorFormat();
            
            // see if we have a format comment
            PanmirrorPandocFormatConfig formatComment = formatTools.parseFormatConfig(getEditorCode(), true);
            
            // doctypes
            if (formatComment.doctypes != null)
            {
               format.docTypes = formatComment.doctypes;
            }
            else if (isQuartoDocument())
            {
               List<String> quartoDocTypes = new ArrayList<>();
               quartoDocTypes.add(PanmirrorExtendedDocType.quarto);
               if (getOutputFormats().contains("hugo"))
                  quartoDocTypes.add(PanmirrorExtendedDocType.hugo);
               if (isQuartoPresentation())
                  quartoDocTypes.add(PanmirrorExtendedDocType.presentation);
               format.docTypes = quartoDocTypes.toArray(new String[] {}); 
            }
            else if (formatComment.doctypes == null || formatComment.doctypes.length == 0)
            {
               List<String> configDocTypes = new ArrayList<>();
               if (isBookdownProjectDocument())
                  configDocTypes.add(PanmirrorExtendedDocType.bookdown);
               if (isHugoProjectDocument() || isHugodownDocument())
                  configDocTypes.add(PanmirrorExtendedDocType.hugo);
               if (isRmdPresentation())
                  configDocTypes.add(PanmirrorExtendedDocType.presentation);
               format.docTypes = configDocTypes.toArray(new String[] {});
            }
            else
            {
               format.docTypes = new String[] {};
            }
                  
            // mode and extensions         
            // non-standard mode and extension either come from a format comment,
            // a detection of an alternate engine (likely due to blogdown/hugo)
            
            Pair<String,String> alternateEngine = alternateMarkdownEngine(format.docTypes);
            if (formatComment.mode != null)
            {
               format.pandocMode = formatComment.mode;
               format.pandocExtensions = StringUtil.notNull(formatComment.extensions);
            }
            else if (alternateEngine != null)
            {
               format.pandocMode = alternateEngine.first;
               format.pandocExtensions = alternateEngine.second;
               if (formatComment.extensions != null)
                  format.pandocExtensions += formatComment.extensions;
            }
            else
            {
               format.pandocMode = "markdown";
               if (!isQuartoDocument())
               { 
                  format.pandocExtensions = "+autolink_bare_uris+tex_math_single_backslash";
               }
               if (formatComment.extensions != null)
                  format.pandocExtensions += formatComment.extensions;
            }
              
            
            // rmdExtensions
            
            // get any rmd extensions declared by the user in the format comment
            PanmirrorRmdExtensions rmdExtensions = rmdExtensionsFromFormatConfig(formatComment);
          
            // create extensions
            format.rmdExtensions = new PanmirrorRmdExtensions();
            
            // chunk execution is always enabled b/c sometimes users will have 
            // code chunks in a .md file and w/o these handler the code chunks
            // get butchered (b/c source capsules aren't run)
            format.rmdExtensions.codeChunks = true;
            
            // support for bookdown part headers is always enabled b/c typing 
            // (PART\*) in the visual editor would result in an escaped \, which
            // wouldn't parse as a port. the odds of (PART\*) occurring naturally
            // in an H1 are also vanishingly small
            format.rmdExtensions.bookdownPart = true;
            
            // support for bookdown cross-references is always enabled b/c they would not 
            // serialize correctly in markdown modes that don't escape @ if not enabled,
            // and the odds that someone wants to literally write @ref(foo) w/o the leading
            // \ are vanishingly small)
            format.rmdExtensions.bookdownXRef = true;
            format.rmdExtensions.bookdownXRefUI = 
               hasBookdownCrossReferences() || rmdExtensions.bookdownXRefUI;
            
            // check for blogdown math in code (e.g. `$math$`)
            format.rmdExtensions.blogdownMathInCode = 
              hasBlogdownMathInCode(formatComment, format.docTypes) || rmdExtensions.blogdownMathInCode;
            
            // hugoExtensions
            format.hugoExtensions = new PanmirrorHugoExtensions();
            
            // always enable hugo shortcodes (w/o this we can end up destroying
            // shortcodes during round-tripping, and we don't want to require that 
            // blogdown files be opened within projects). this idiom is obscure 
            // enough that it's vanishingly unlikely to affect non-blogdown docs
            format.hugoExtensions.shortcodes = true;
            
            // return format
            return format;
         }
      };
   }
   
   public boolean isQuartoDocument()
   {
      return target_.getExtendedFileType() == SourceDocument.XT_QUARTO_DOCUMENT;
   }
   
   public boolean isQuartoBookDocument()
   {
      return isQuartoDocument() && 
             QuartoHelper.isQuartoBookDoc(docUpdateSentinel_.getPath(), sessionInfo_.getQuartoConfig());
   }
   
   public boolean isQuartoPresentation()
   {
      if (isQuartoDocument())
      {
         List<String> formats = getOutputFormats();
         final ArrayList<String> presentationFormats = new ArrayList<String>(Arrays.asList(
               "beamer", 
               "pptx",
               "revealjs",
               "slidy"
         ));
         for (String format : formats)
         {
            if (presentationFormats.stream().anyMatch(presFormat -> format.startsWith(presFormat)))
               return true;
         }
      }
     
      return false;
   }
   
   public boolean isRmdPresentation()
   {
      if (isRMarkdownDocument() && !isQuartoDocument())
      {
         List<String> formats = getOutputFormats();
         final ArrayList<String> presentationFormats = new ArrayList<String>(Arrays.asList(
            RmdOutputFormat.OUTPUT_BEAMER_PRESENTATION, 
            RmdOutputFormat.OUTPUT_PPT_PRESENTATION,
            RmdOutputFormat.OUTPUT_REVEALJS_PRESENTATION,
            RmdOutputFormat.OUTPUT_SLIDY_PRESENTATION
         ));
         for (String format : formats)
         {
            if (presentationFormats.stream().anyMatch(presFormat -> format.endsWith(presFormat)))
               return true;
         }
      }
      return false;
   }
   
   
   public boolean isRMarkdownDocument()
   {
      if (target_.canExecuteChunks()) 
      {
         return true;
      }
      else
      {
         String path = docUpdateSentinel_.getPath();
         return (path != null && path.toLowerCase().endsWith(".rmd"));
      }
   }
   
   public boolean isHugoProjectDocument()
   {
      return (getBlogdownConfig().is_hugo_project && isDocInProject());
   }
   
   
   public boolean isBookdownProjectDocument() 
   {
      return sessionInfo_.getIsBookdownProject() && isDocInProject();
   }
   
   public String validateSourceForVisualMode()
   {
      String invalid = null;
      
      if (isXaringanDocument())
      {
         invalid = constants_.xaringanPresentationsVisualMode();
      }
      else if (hasVcsConflictMarkers())
      {
         invalid = constants_.versionControlConflict();
      }
      
      return invalid;
   }
    
  
   private List<String> getOutputFormats()
   {
      String yaml = YamlFrontMatter.getFrontMatter(docDisplay_);
      if (yaml == null)
      {
         return new ArrayList<>();
      }
      else
      {
         List<String> formats = isQuartoDocument() 
            ? TextEditingTargetRMarkdownHelper.getQuartoOutputFormats(yaml)
            : TextEditingTargetRMarkdownHelper.getOutputFormats(yaml);
         if (formats == null)
            return new ArrayList<>();
         else
            return formats;   
      }
   }
   
   private PanmirrorRmdExtensions rmdExtensionsFromFormatConfig(PanmirrorPandocFormatConfig config)
   {
      PanmirrorRmdExtensions rmdExtensions = new PanmirrorRmdExtensions();
      if (config.rmdExtensions != null)
      {
         rmdExtensions.bookdownXRefUI = config.rmdExtensions.contains("+bookdown_cross_references");
         rmdExtensions.blogdownMathInCode = config.rmdExtensions.contains("+tex_math_dollars_in_code");
      }
      return rmdExtensions;
   }
   

   private boolean isBlogdownProjectDocument() 
   {
      return getBlogdownConfig().is_blogdown_project && isDocInProject() && !isHugodownDocument();
   }
   
  
   private boolean isHugodownDocument()
   {
      return getOutputFormats().contains("hugodown::md_document");
   }
   
   @SuppressWarnings("unused")
   private boolean isGitHubDocument()
   {
      List<String> formats = getOutputFormats();
      return formats.contains("github_document") || 
             formats.contains("gfm") ||
             isGitREADME();
   }
   
   private boolean isGitREADME()
   {
      String path = docUpdateSentinel_.getPath();
      if (path != null && isGitRepo())
         return FileSystemItem.createFile(path).getStem().equalsIgnoreCase("README");
      else
         return false;
   }
   
   private boolean isGitRepo()
   {
      return sessionInfo_.getVcsName().equalsIgnoreCase("git");
   }
   
   private boolean isDistillDocument()
   {
      return (sessionInfo_.getIsDistillProject() && isDocInProject()) ||
             getOutputFormats().contains("distill::distill_article");
   }
   
   private boolean isXaringanDocument()
   {
      List<String> formats = getOutputFormats();
      for (String format : formats)
      {
         if (format.startsWith("xaringan"))
            return true;
      }
      return false;
   }
   
   private boolean hasBookdownCrossReferences()
   {
      return isBookdownProjectDocument() || 
             isBookdownStandaloneDocument() || 
             isBlogdownProjectDocument() || 
             isDistillDocument();
   }
   
   private boolean isBookdownStandaloneDocument()
   {
      List<String> formats = getOutputFormats();
      for (String format : formats) 
      {
         boolean isBookdown = format.startsWith("bookdown::") && format.endsWith("2");
         if (isBookdown)
            return true;
      }
      return false;
   }
   
   private boolean hasBlogdownMathInCode(PanmirrorPandocFormatConfig config, String[] docTypes)
   {
      if (alternateMarkdownEngine(docTypes) != null && getBlogdownConfig().rmd_extensions != null)
         return getBlogdownConfig().rmd_extensions.contains("+tex_math_dollars_in_code") &&
                !this.disableBlogdownMathInCode(config);
      else
         return false;
   }
   
   private boolean disableBlogdownMathInCode(PanmirrorPandocFormatConfig config)
   {
      return config.rmdExtensions != null && 
             config.rmdExtensions.contains("-tex_math_dollars_in_code");
   }
   
   private boolean hasVcsConflictMarkers()
   {
      String code = getEditorCode();
      int markers = 0;
      int nonEqualsMarkers = 0;
      boolean vcsMarkers = false;
      
      Match match = VCS_CONFLICT_PATTERN.match(code, 0);
      while (match != null)
      {
         markers++;
         nonEqualsMarkers += !match.getGroup(1).equals("=") ? 1 : 0;
         vcsMarkers = markers > 1 && nonEqualsMarkers > 0;
         if (vcsMarkers)
           break;
         match = match.nextMatch();
      }
      
      return vcsMarkers;
   }
   
   // see if there's an alternate markdown engine in play
   private Pair<String,String> alternateMarkdownEngine(String[] docTypes)
   {
      // if we have a doc
      String docPath = docUpdateSentinel_.getPath();
      if (docPath != null)
      {   
         // collect any alternate mode we may have
         BlogdownConfig config = getBlogdownConfig();
         Pair<String,String> alternateMode = new Pair<>(
            config.markdown_engine,
            config.markdown_extensions
         );
         
         // if it's a blogdown document
         if (isBlogdownProjectDocument() && !isQuartoDocument())
         {
            // if it has an extension indicating hugo will render markdown
            String extension = FileSystemItem.getExtensionFromPath(docPath);
            if (extension.compareToIgnoreCase(".md") == 0 ||
                extension.compareToIgnoreCase(".Rmarkdown") == 0)
            {
               return alternateMode;
            }
         }
         // if it's a hugo document (that is not a blogdown document)
         else if (isHugoProjectDocument())
         {
            return alternateMode;
         }
         // other valid ways of having a hugo document
         else if (isHugodownDocument() || hasHugoDocType(docTypes))
         {
            return new Pair<>("goldmark", "");
         }
      }
   
      return null;   
   }
   
   private boolean hasHugoDocType(String[] docTypes)
   {
      for (int i=0; i<docTypes.length; i++) {
         if (docTypes[i].compareToIgnoreCase(PanmirrorExtendedDocType.hugo) == 0)
            return true;
      }
      return false;
   }

   private boolean isDocInProject()
   {  
      return VisualModeUtil.isDocInProject(workbenchContext_, docUpdateSentinel_);
   }
   
   
   private String getEditorCode()
   {
      return VisualModeUtil.getEditorCode(view_);
   }   

   
   private BlogdownConfig getBlogdownConfig()
   {
      return sessionInfo_.getBlogdownConfig();
   }

   private SessionInfo sessionInfo_;
   private WorkbenchContext workbenchContext_;
   private final DocUpdateSentinel docUpdateSentinel_;
   private final DocDisplay docDisplay_;
   private final TextEditingTarget target_;
   private final TextEditingTarget.Display view_;

   private final static Pattern VCS_CONFLICT_PATTERN = 
         Pattern.create("^[\\+\\-]?([\\<\\>|=])\\1{6}.*?$", "gm");
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);
}
