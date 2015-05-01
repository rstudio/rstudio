/*
 * FileTypeRegistry.java
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
package org.rstudio.studio.client.common.filetypes;

import java.util.HashMap;

import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent.NavigationMethod;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileTypeRegistry
{
   private static final FileIconResources ICONS = FileIconResources.INSTANCE;

   public static final TextFileType TEXT =
         new TextFileType("text", "Text File", EditorLanguage.LANG_PLAIN, "",
                          ICONS.iconText(),
                          true,
                          false, false, false, false, false, false, false, false, false, true, false, false);

   public static final TextFileType R =
         new RFileType("r_source", "R Script", EditorLanguage.LANG_R, ".R",
                       ICONS.iconRdoc());

   public static final TextFileType RD =
      new TextFileType("r_doc", "Rd File", EditorLanguage.LANG_RDOC, ".Rd",
                       ICONS.iconRd(),
                       true, // word-wrap
                       true, // source on save aka preview on save
                       false, false, false,
                       true, // preview html
                       false, false, false, false,
                       true, // check spelling
                       false,
                       false);

   public static final TextFileType DCF =
         new TextFileType("dcf", "DCF", EditorLanguage.LANG_DCF, ".dcf",
                          ICONS.iconText(), false, false, false, false, false,
                          false, false, false, false, false, false, false, false);
   
   public static final TextFileType STAN = new StanFileType();
   
   public static final TextFileType MERMAID = new MermaidFileType();
   
   public static final TextFileType GRAPHVIZ = new GraphvizFileType();


   public static final TextFileType NAMESPACE =
     new TextFileType("r_namespace", "NAMESPACE", EditorLanguage.LANG_R, "",
                      ICONS.iconText(), false, false, false, false, false,
                      false, false, false, false, false, false, false, false);

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

   public static final RWebContentFileType RPRESENTATION = new RPresentationFileType();

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
                          false, false, false, false, false, false, false, false, false, false, false, false);

   public static final TextFileType JS =
         new TextFileType("js", "JavaScript", EditorLanguage.LANG_JAVASCRIPT, ".js",
                          ICONS.iconJavascript(),
                          true,
                          false, false, false, false, false, false, false, false, false, false, false, false);
   
   public static final TextFileType JSON =
         new TextFileType("json", "JSON", EditorLanguage.LANG_JAVASCRIPT, ".json",
                          ICONS.iconJavascript(),
                          true,
                          false, false, false, false, false, false, false, false, false, false, false, false);
   

   public static final TextFileType PYTHON = new ScriptFileType(
     "python", "Python", EditorLanguage.LANG_PYTHON, ".py",ICONS.iconPython(),
     "python", true);

   public static final TextFileType SQL =
         new TextFileType("sql", "SQL", EditorLanguage.LANG_SQL, ".sql",
                          ICONS.iconSql(), false, false, false, false, false,
                          false, false, false, false, false, false, false, false);

   public static final TextFileType SH = new ScriptFileType(
         "sh", "Shell", EditorLanguage.LANG_SH, ".sh", ICONS.iconSh(),
         null, false);
   
   public static final TextFileType YAML =
         new TextFileType("yaml", "YAML", EditorLanguage.LANG_YAML, ".yaml",
                          ICONS.iconYaml(), false, false, false, false, false,
                          false, false, false, false, false, false, false, false);

   public static final TextFileType XML =
         new TextFileType("xml", "XML", EditorLanguage.LANG_XML, ".xml",
                          ICONS.iconXml(), false, false, false, false, false,
                          false, false, false, false, false, false, false, false);
   
   public static final TextFileType H = new CppFileType("h", ".h", ICONS.iconH(), true, false);
   public static final TextFileType C = new CppFileType("c", ".c", ICONS.iconC(), false, false);
   public static final TextFileType HPP = new CppFileType("hpp", ".hpp", ICONS.iconHpp(), true, false);
   public static final TextFileType CPP = new CppFileType("cpp", ".cpp", ICONS.iconCpp(), true, true);
   
   public static final TextFileType CLOJURE = 
         new TextFileType("clojure", "Clojure", EditorLanguage.LANG_CLOJURE, ".clj", ICONS.iconClojure(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType COFFEE = 
         new TextFileType("coffee", "Coffee", EditorLanguage.LANG_COFFEE, ".coffee", ICONS.iconCoffee(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType CSHARP = 
         new TextFileType("csharp", "C#", EditorLanguage.LANG_CSHARP, ".cs", ICONS.iconCsharp(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   
   public static final TextFileType GITIGNORE = 
         new TextFileType("gitignore", "Gitignore", EditorLanguage.LANG_GITIGNORE, ".gitignore", ICONS.iconGitignore(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType GO = 
         new TextFileType("go", "Go", EditorLanguage.LANG_GO, ".go", ICONS.iconGo(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType GROOVY = 
         new TextFileType("groovy", "Groovy", EditorLanguage.LANG_GROOVY, ".groovy", ICONS.iconGroovy(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType HASKELL = 
         new TextFileType("haskell", "Haskell", EditorLanguage.LANG_HASKELL, ".haskell", ICONS.iconHaskell(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType HAXE = 
         new TextFileType("haxe", "Haxe", EditorLanguage.LANG_HAXE, ".haxe", ICONS.iconHaxe(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType JAVA = 
         new TextFileType("java", "Java", EditorLanguage.LANG_JAVA, ".java", ICONS.iconJava(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType JULIA = 
         new TextFileType("julia", "Julia", EditorLanguage.LANG_JULIA, ".julia", ICONS.iconJulia(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType LISP = 
         new TextFileType("lisp", "Lisp", EditorLanguage.LANG_LISP, ".lisp", ICONS.iconLisp(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType LUA = 
         new TextFileType("lua", "Lua", EditorLanguage.LANG_LUA, ".lua", ICONS.iconLua(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType MATLAB = 
         new TextFileType("matlab", "Matlab", EditorLanguage.LANG_MATLAB, ".m", ICONS.iconMatlab(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType PERL = 
         new TextFileType("perl", "Perl", EditorLanguage.LANG_PERL, ".pl", ICONS.iconPerl(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType RUBY = 
         new TextFileType("ruby", "Ruby", EditorLanguage.LANG_RUBY, ".rb", ICONS.iconRuby(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType RUST = 
         new TextFileType("rust", "Rust", EditorLanguage.LANG_RUST, ".rs", ICONS.iconRust(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType SCALA = 
         new TextFileType("scala", "Scala", EditorLanguage.LANG_SCALA, ".scala", ICONS.iconScala(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final TextFileType SNIPPETS =
         new TextFileType("snippets", "Snippets", EditorLanguage.LANG_SNIPPETS, ".snippets", ICONS.iconSnippets(),
               false, false, false, false, false,
               false, false, false, false, false, false, false, false);
   
   public static final RDataType RDATA = new RDataType();
   public static final RProjectType RPROJECT = new RProjectType();

   public static final DataFrameType DATAFRAME = new DataFrameType();
   public static final UrlContentType URLCONTENT = new UrlContentType();
   public static final CodeBrowserType CODEBROWSER = new CodeBrowserType();
   public static final ProfilerType PROFILER = new ProfilerType();

   public static final BrowserType BROWSER = new BrowserType();

   @Inject
   public FileTypeRegistry(EventBus eventBus,
                           Satellite satellite,
                           Session session,
                           GlobalDisplay globalDisplay,
                           FilesServerOperations server)
   {
      eventBus_ = eventBus;
      satellite_ = satellite;
      server_ = server;
      session_ = session;
      globalDisplay_ = globalDisplay;

      if (!satellite_.isCurrentWindowSatellite())
         exportEditFileCallback();

      FileIconResources icons = ICONS;

      register("", TEXT, icons.iconText());
      register("*.txt", TEXT, icons.iconText());
      register("*.log", TEXT, icons.iconText());
      register("README", TEXT, icons.iconText());
      register(".gitignore", TEXT, icons.iconText());
      register(".Rbuildignore", TEXT, icons.iconText());
      register("packrat.lock", DCF, icons.iconText());
      register("*.r", R, icons.iconRdoc());
      register("*.q", R, icons.iconRdoc());
      register("*.s", R, icons.iconRdoc());
      register(".rprofile", R, icons.iconRprofile());
      register("Rprofile.site", R, icons.iconRprofile());
      register("DESCRIPTION", DCF, icons.iconText());
      register("INDEX", TEXT, icons.iconText());
      register("LICENCE", TEXT, icons.iconText());
      register("MD5", TEXT, icons.iconText());
      register("NEWS", TEXT, icons.iconText());
      register("PORTING", TEXT, icons.iconText());
      register("COPYING", TEXT, icons.iconText());
      register("COPYING.LIB", TEXT, icons.iconText());
      register("BUGS", TEXT, icons.iconText());
      register("CHANGES", TEXT, icons.iconText());
      register("CHANGELOG", TEXT, icons.iconText());
      register("INSTALL", SH, icons.iconSh());
      register("TODO", TEXT, icons.iconText());
      register("THANKS", TEXT, icons.iconText());
      register("configure", SH, icons.iconSh());
      register("configure.win", SH, icons.iconSh());
      register("cleanup", SH, icons.iconSh());
      register("cleanup.win", SH, icons.iconSh());
      register("Makevars", SH, icons.iconSh());
      register("Makevars.win", SH, icons.iconSh());
      register("TUTORIAL", DCF, icons.iconText());
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
      register("*.dtx", TEX, icons.iconTex());
      register("*.ins", TEX, icons.iconTex());
      register("*.rhtml", RHTML, icons.iconRhtml());
      register("*.htm", HTML, icons.iconHTML());
      register("*.html", HTML, icons.iconHTML());
      register("*.css", CSS, icons.iconCss());
      register("*.js", JS, icons.iconJavascript());
      register("*.json", JSON, icons.iconJavascript());
      register("*.rmd", RMARKDOWN, icons.iconRmarkdown());
      register("*.rmarkdown", RMARKDOWN, icons.iconRmarkdown());
      register("*.rpres", RPRESENTATION, icons.iconRpresentation());
      register("*.md", MARKDOWN, icons.iconMarkdown());
      register("*.mdtxt", MARKDOWN, icons.iconMarkdown());
      register("*.markdown*", MARKDOWN, icons.iconMarkdown());
      register("*.bib", TEXT, icons.iconText());
      register("*.c", C, icons.iconC());
      register("*.cpp", CPP, icons.iconCpp());
      register("*.cc", CPP, icons.iconCpp());
      register("*.h", H, icons.iconH());
      register("*.hpp", HPP, icons.iconHpp());
      register("*.f", TEXT, icons.iconText());
      register("*.Rout.save", TEXT, icons.iconText());
      register("*.rd", RD, icons.iconRd());
      register("*.rdata", RDATA, icons.iconRdata());
      register("*.rda", RDATA, icons.iconRdata());
      register("*.Rproj", RPROJECT, icons.iconRproject());
      register("*.dcf", DCF, icons.iconText());
      register("*.mmd", MERMAID, icons.iconMermaid());
      register("*.gv", GRAPHVIZ, icons.iconGraphviz());
      register("*.dot", GRAPHVIZ, icons.iconGraphviz());
      register("*.py", PYTHON, icons.iconPython());
      register("*.sql", SQL, icons.iconSql());
      register("*.sh", SH, icons.iconSh());
      register("*.yml", YAML, icons.iconYaml());
      register("*.yaml", YAML, icons.iconYaml());
      register("*.xml", XML, icons.iconXml());
      register("*.stan", STAN, icons.iconStan());
      
      register("*.clj", CLOJURE, icons.iconClojure());
      register("*.cloj", CLOJURE, icons.iconClojure());
      register("*.clojure", CLOJURE, icons.iconClojure());
      register("*.coffee", COFFEE, icons.iconCoffee());
      register("*.cs", CSHARP, icons.iconCsharp());
      register(".gitignore", GITIGNORE, icons.iconGitignore());
      register("*.go", GO, icons.iconGo());
      register("*.groovy", GROOVY, icons.iconGroovy());
      register("*.haskell", HASKELL, icons.iconHaskell());
      register("*.haxe", HAXE, icons.iconHaxe());
      register("*.java", JAVA, icons.iconJava());
      register("*.julia", JULIA, icons.iconJulia());
      register("*.lisp", LISP, icons.iconLisp());
      register(".emacs", LISP, icons.iconLisp());
      register("*.el", LISP, icons.iconLisp());
      register("*.lua", LUA, icons.iconLua());
      register("*.m", MATLAB, icons.iconMatlab());
      register("*.pl", PERL, icons.iconPerl());
      register("*.rb", RUBY, icons.iconRuby());
      register("*.rs", RUST, icons.iconRust());
      register("*.scala", SCALA, icons.iconScala());
      register("*.snippets", SNIPPETS, icons.iconSnippets());

      registerIcon(".jpg", icons.iconPng());
      registerIcon(".jpeg", icons.iconPng());
      registerIcon(".gif", icons.iconPng());
      registerIcon(".bmp", icons.iconPng());
      registerIcon(".tiff", icons.iconPng());
      registerIcon(".tif", icons.iconPng());
      registerIcon(".png", icons.iconPng());

      registerIcon(".pdf", icons.iconPdf());
      registerIcon(".csv", icons.iconCsv());
      registerIcon(".docx", icons.iconWord());

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
               {
                  if (session_.getSessionInfo().getAllowFileDownloads())
                  {
                     BROWSER.openFile(file, eventBus_);
                  }
                  else
                  {
                     globalDisplay_.showErrorMessage(
                       "File Download Error",
                       "Unable to show file because file downloads are " +
                       "restricted on this server.\n");
                  }
               }
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
      editFile(file, position, false);
   }

   public void editFile(FileSystemItem file,
                        FilePosition position,
                        boolean highlightLine)
   {
      if (satellite_.isCurrentWindowSatellite())
      {
         satellite_.focusMainWindow();
         callSatelliteEditFile(file.cast(), position.cast(), highlightLine);
      }
      else
      {
         FileType fileType = getTypeForFile(file);
         if (fileType != null && !(fileType instanceof TextFileType))
            fileType = TEXT;

         if (fileType != null)
            fileType.openFile(file,
                  position,
                  highlightLine ?
                        NavigationMethod.HighlightLine :
                        NavigationMethod.Default,
                  eventBus_);
      }
   }

   private void satelliteEditFile(JavaScriptObject file,
                                  JavaScriptObject position,
                                  boolean highlightLine)
   {
      FileSystemItem fsi = file.cast();
      FilePosition pos = position.cast();
      editFile(fsi, pos);
   }

   private final native void exportEditFileCallback()/*-{
      var registry = this;
      $wnd.editFileFromRStudioSatellite = $entry(
         function(file, position, highlightLine) {
            registry.@org.rstudio.studio.client.common.filetypes.FileTypeRegistry::satelliteEditFile(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;Z)(file,position,highlightLine);
         }
      );
   }-*/;

   private final native void callSatelliteEditFile(
                                       JavaScriptObject file,
                                       JavaScriptObject position,
                                       boolean highlightLine)/*-{
      $wnd.opener.editFileFromRStudioSatellite(file, position, highlightLine);
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
            iconsByFilename_.put(filespec.toLowerCase(), icon);
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
   private final Session session_;
   private final GlobalDisplay globalDisplay_;
   private final FilesServerOperations server_;
}
