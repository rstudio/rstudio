/*
 * FileTypeRegistry.java
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
package org.rstudio.studio.client.common.filetypes;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;

import java.util.HashMap;

@Singleton
public class FileTypeRegistry
{
   private static final FileIconResources ICONS = FileIconResources.INSTANCE;

   public static final TextFileType TEXT =
         new TextFileType("text", "Text File", EditorLanguage.LANG_PLAIN, "",
                          ICONS.iconText(),
                          true,
                          false, false, false, false, false, false, false, false, false, true, false);

   public static final TextFileType R =
         new RFileType("r_source", "R Script", EditorLanguage.LANG_R, ".R",
                       ICONS.iconRdoc());

   public static final TextFileType RD =
      new TextFileType("r_doc", "R Doc", EditorLanguage.LANG_RDOC, ".Rd",
                       ICONS.iconRd(),
                       true, // word-wrap
                       true, // source on save aka preview on save
                       false, false, false, 
                       true, // preview html
                       false, false, false, false, 
                       true, // check spelling
                       false);

   public static final TextFileType DCF =
         new TextFileType("dcf", "DCF", EditorLanguage.LANG_DCF, ".dcf",
                          ICONS.iconText(), false, false, false, false, false,
                          false, false, false, false, false, false, false);
   
   public static final TextFileType NAMESPACE =
     new TextFileType("r_namespace", "NAMESPACE", EditorLanguage.LANG_R, "",
                      ICONS.iconText(), false, false, false, false, false,
                      false, false, false, false, false, false, false);
   
   public static final TextFileType SWEAVE =
      new SweaveFileType("sweave", "R Sweave", 
          EditorLanguage.LANG_SWEAVE, ".Rnw",ICONS.iconRsweave());
        
   public static final TexFileType TEX =
         new TexFileType("tex", "TeX", EditorLanguage.LANG_TEX, ".tex",
                          ICONS.iconTex());
   
   public static final PlainTextFileType RHISTORY =
      new PlainTextFileType("r_history", "R History", ".Rhistory",
                            ICONS.iconRhistory(),
                            true);

   public static final RWebContentFileType RMARKDOWN =
         new RWebContentFileType("r_markdown", "R Markdown", EditorLanguage.LANG_RMARKDOWN,
                              ".Rmd", ICONS.iconRmarkdown(), true);
   
   public static final WebContentFileType MARKDOWN =
      new WebContentFileType("markdown", "Markdown", EditorLanguage.LANG_MARKDOWN,
                           ".md", ICONS.iconMarkdown(), true);
   
   
   public static final RWebContentFileType RHTML =
         new RWebContentFileType("r_html", "R HTML", EditorLanguage.LANG_RHTML,
                              ".Rhtml", ICONS.iconRhtml(), false);
  
   public static final WebContentFileType HTML =
         new WebContentFileType("html", "HTML", EditorLanguage.LANG_HTML,
                              ".html", ICONS.iconHTML(), false);
   
   public static final TextFileType CSS =
         new TextFileType("css", "CSS", EditorLanguage.LANG_CSS, ".css",
                          ICONS.iconCss(),
                          true,
                          false, false, false, false, false, false, false, false, false, false, false);

   public static final TextFileType JS =
         new TextFileType("js", "JavaScript", EditorLanguage.LANG_JAVASCRIPT, ".js",
                          ICONS.iconJavascript(),
                          true,
                          false, false, false, false, false, false, false, false, false, false, false);

   
   public static final TextFileType H = new CppFileType("h", ".h", ICONS.iconH(), false);
   public static final TextFileType C = new CppFileType("c", ".c", ICONS.iconC(), false);
   public static final TextFileType HPP = new CppFileType("hpp", ".hpp", ICONS.iconHpp(), true);
   public static final TextFileType CPP = new CppFileType("cpp", ".cpp", ICONS.iconCpp(), true);
   
   
   
   public static final RDataType RDATA = new RDataType();
   public static final RProjectType RPROJECT = new RProjectType();
   
   public static final DataFrameType DATAFRAME = new DataFrameType();
   public static final UrlContentType URLCONTENT = new UrlContentType();
   public static final CodeBrowserType CODEBROWSER = new CodeBrowserType();

   public static final BrowserType BROWSER = new BrowserType();
   
   @Inject
   public FileTypeRegistry(EventBus eventBus,
                           Satellite satellite,
                           FilesServerOperations server)
   {
      eventBus_ = eventBus;
      satellite_ = satellite;
      server_ = server;
      
      if (!satellite_.isCurrentWindowSatellite())
         exportEditFileCallback();

      FileIconResources icons = ICONS;

      register("", TEXT, icons.iconText());
      register("*.txt", TEXT, icons.iconText());
      register("*.log", TEXT, icons.iconText());
      register("README", TEXT, icons.iconText());
      register(".gitignore", TEXT, icons.iconText());
      register(".Rbuildignore", TEXT, icons.iconText());
      register("*.r", R, icons.iconRdoc());
      register("*.q", R, icons.iconRdoc());
      register("*.s", R, icons.iconRdoc());
      register(".rprofile", R, icons.iconRprofile());
      register("Rprofile.site", R, icons.iconRprofile());
      register("DESCRIPTION", DCF, icons.iconText());
      register("NAMESPACE", NAMESPACE, icons.iconText());
      register("*.rhistory", RHISTORY, icons.iconRhistory());
      register("*.rproj", RPROJECT, icons.iconRproject());
      register("*.rnw", SWEAVE, icons.iconRsweave());
      register("*.snw", SWEAVE, icons.iconRsweave());
      register("*.nw", SWEAVE, icons.iconRsweave());
      register("*.tex", TEX, icons.iconTex());
      register("*.latex", TEX, icons.iconTex());
      register("*.sty", TEX, icons.iconTex());
      register("*.cls", TEX, icons.iconTex());
      register("*.bbl", TEX, icons.iconTex());
      register("*.rhtml", RHTML, icons.iconRhtml());
      register("*.htm", HTML, icons.iconHTML());
      register("*.html", HTML, icons.iconHTML());
      register("*.css", CSS, icons.iconCss());
      register("*.js", JS, icons.iconJavascript());
      register("*.rmd", RMARKDOWN, icons.iconRmarkdown());
      register("*.rmarkdown", RMARKDOWN, icons.iconRmarkdown());
      register("*.md", MARKDOWN, icons.iconMarkdown());
      register("*.mdtxt", MARKDOWN, icons.iconMarkdown());
      register("*.markdown", MARKDOWN, icons.iconMarkdown());
      register("*.bib", TEXT, icons.iconText());
      register("*.c", C, icons.iconC());
      register("*.cpp", CPP, icons.iconCpp());
      register("*.h", H, icons.iconH());
      register("*.hpp", HPP, icons.iconHpp());
      register("*.f", TEXT, icons.iconText());
      register("*.Rout.save", TEXT, icons.iconText());
      register("*.rd", RD, icons.iconRd());
      register("*.rdata", RDATA, icons.iconRdata());
      register("*.rda", RDATA, icons.iconRdata());
      register("*.Rproj", RPROJECT, icons.iconRproject());
      register("*.dcf", DCF, icons.iconText());

      registerIcon(".jpg", icons.iconPng());
      registerIcon(".jpeg", icons.iconPng());
      registerIcon(".gif", icons.iconPng());
      registerIcon(".bmp", icons.iconPng());
      registerIcon(".tiff", icons.iconPng());
      registerIcon(".tif", icons.iconPng());
      registerIcon(".png", icons.iconPng());

      registerIcon(".pdf", icons.iconPdf());
      registerIcon(".csv", icons.iconCsv());

      for (FileType fileType : FileType.ALL_FILE_TYPES)
      {
         assert !fileTypesByTypeName_.containsKey(fileType.getTypeId());
         fileTypesByTypeName_.put(fileType.getTypeId(), fileType);
      }
   }

   public void openFile(FileSystemItem file)
   {
      openFile(file, true);
   }
   
   public void openFile(final FileSystemItem file, final boolean canUseBrowser)
   {
      FileType fileType = getTypeForFile(file);
      if (fileType != null)
      {
         fileType.openFile(file, eventBus_);
      }
      else
      {
         // build default command to use if we have an error or the 
         // file is not a text file
         final Command defaultCommand = new Command() {
            @Override
            public void execute()
            {   
               if (canUseBrowser)
                  BROWSER.openFile(file, eventBus_);
            }
         };
         
         // check with the server to see if this is likely to be a text file
         server_.isTextFile(file.getPath(), 
                            new ServerRequestCallback<Boolean>() {
            @Override
            public void onResponseReceived(Boolean isText)
            {
               if (isText)
                  TEXT.openFile(file, eventBus_);
               else
                  defaultCommand.execute();
            }
            
            @Override
            public void onError(ServerError error)
            {
               defaultCommand.execute();
            }
         }); 
      }
   }

   public void editFile(FileSystemItem file)
   {
      editFile(file, null);
   }
   
   public void editFile(FileSystemItem file, FilePosition position)
   {
      if (satellite_.isCurrentWindowSatellite())
      {
         satellite_.focusMainWindow();   
         callSatelliteEditFile(file.cast(), position.cast());
      }
      else
      {
         FileType fileType = getTypeForFile(file);
         if (fileType != null && !(fileType instanceof TextFileType))
            fileType = TEXT;
   
         if (fileType != null)
            fileType.openFile(file, position, eventBus_);
      }
   }
   
   private void satelliteEditFile(JavaScriptObject file, 
                                  JavaScriptObject position)
   {
      FileSystemItem fsi = file.cast();
      FilePosition pos = position.cast();
      editFile(fsi, pos);
   }
   
   private final native void exportEditFileCallback()/*-{
      var registry = this;     
      $wnd.editFileFromRStudioSatellite = $entry(
         function(file, position) {
            registry.@org.rstudio.studio.client.common.filetypes.FileTypeRegistry::satelliteEditFile(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)(file,position);
         }
      ); 
   }-*/;

   private final native void callSatelliteEditFile(
                                       JavaScriptObject file,
                                       JavaScriptObject position)/*-{
      $wnd.opener.editFileFromRStudioSatellite(file, position);
   }-*/;


   public FileType getTypeByTypeName(String name)
   {
      return fileTypesByTypeName_.get(name);
   }
   
   


   public FileType getTypeForFile(FileSystemItem file)
   {
      // last ditch default type -- see if this either a known text file type
      // or (for server mode) NOT a known binary type. the result of this is
      // that unknown files types are treated as text and opened in the editor
      // (we don't do this on desktop because  in that case users have the
      // recourse of using a local editor)
      String defaultType = Desktop.isDesktop() ? "application/octet-stream" :
                                                 "text/plain";
      return getTypeForFile(file, defaultType);
   }

   public FileType getTypeForFile(FileSystemItem file, String defaultType)
   {
      if (file != null)
      {
         String filename = file.getName().toLowerCase();
         FileType result = fileTypesByFilename_.get(filename);
         if (result != null)
            return result;

         String extension = FileSystemItem.getExtensionFromPath(filename);
         result = fileTypesByFileExtension_.get(extension);
         if (result != null)
            return result;
         
         if (defaultType != null)
         {
            String mimeType = file.mimeType(defaultType);
            if (mimeType.startsWith("text/"))
               return TEXT;
         }
      }
      
      return null;
   }

   public TextFileType getTextTypeForFile(FileSystemItem file)
   {
      FileType type = getTypeForFile(file);
      if (type != null && type instanceof TextFileType)
         return (TextFileType) type;
      else
         return TEXT;
   }
   
   public ImageResource getIconForFile(FileSystemItem file)
   {
      if (file.isDirectory())
      {
         if (file.isPublicFolder())
            return ICONS.iconPublicFolder();
         else
            return ICONS.iconFolder();
      }

      return getIconForFilename(file.getName());
   }
   
   public ImageResource getIconForFilename(String filename)
   {
      ImageResource icon = iconsByFilename_.get(filename.toLowerCase());
      if (icon != null)
         return icon;
      String ext = FileSystemItem.getExtensionFromPath(filename);
      icon = iconsByFileExtension_.get(ext.toLowerCase());
      if (icon != null)
         return icon;

      return ICONS.iconText();
   }

   private void register(String filespec, FileType fileType, ImageResource icon)
   {
      if (filespec.startsWith("*."))
      {
         String ext = filespec.substring(1).toLowerCase();
         if (ext.equals("."))
            ext = "";
         fileTypesByFileExtension_.put(ext,
                                       fileType);
         if (icon != null)
            iconsByFileExtension_.put(ext, icon);
      }
      else if (filespec.length() == 0)
      {
         fileTypesByFileExtension_.put("", fileType);
         if (icon != null)
            iconsByFileExtension_.put("", icon);
      }
      else
      {
         assert filespec.indexOf("*") < 0 : "Unexpected filespec format";
         fileTypesByFilename_.put(filespec.toLowerCase(), fileType);
         if (icon != null)
            iconsByFileExtension_.put(filespec.toLowerCase(), icon);
      }
   }

   private void registerIcon(String extension, ImageResource icon)
   {
      iconsByFileExtension_.put(extension, icon);
   }

   private final HashMap<String, FileType> fileTypesByFileExtension_ =
         new HashMap<String, FileType>();
   private final HashMap<String, FileType> fileTypesByFilename_ =
         new HashMap<String, FileType>();
   private final HashMap<String, FileType> fileTypesByTypeName_ =
         new HashMap<String, FileType>();
   private final HashMap<String, ImageResource> iconsByFileExtension_ =
         new HashMap<String, ImageResource>();
   private final HashMap<String, ImageResource> iconsByFilename_ =
         new HashMap<String, ImageResource>();
   private final EventBus eventBus_;
   private final Satellite satellite_;
   private final FilesServerOperations server_;
}
