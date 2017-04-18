/*
 * SlideRequestHandler.cpp
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


#include "SlideRequestHandler.hpp"

#include <iostream>

#include <boost/utility.hpp>
#include <boost/foreach.hpp>
#include <boost/format.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/trim.hpp>

#include <boost/regex.hpp>
#include <boost/iostreams/filter/regex.hpp>

#include <core/FileSerializer.hpp>
#include <core/HtmlUtils.hpp>
#include <core/markdown/Markdown.hpp>
#include <core/text/TemplateFilter.hpp>
#include <core/system/Process.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "PresentationState.hpp"
#include "PresentationLog.hpp"
#include "SlideParser.hpp"
#include "SlideRenderer.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {

void onSlideDeckChangedOverlay(const SlideDeck& slideDeck);

namespace {

const char * const kDefaultRevealFont = "\"Lato\"";
const char * const kDefaultRevealHeadingFont = "\"News Cycle\"";
const char * const kMediaPrint = "media=\"print\"";

class ResourceFiles : boost::noncopyable
{
private:
   ResourceFiles() {}

public:
   std::string get(const std::string& path)
   {
      if (cache_.find(path) == cache_.end())
         cache_[path] = module_context::resourceFileAsString(path);
      return cache_[path];
   }

private:
   friend ResourceFiles& resourceFiles();
   std::map<std::string,std::string> cache_;
};

ResourceFiles& resourceFiles()
{
   static ResourceFiles instance;
   return instance;
}


std::string revealResource(const std::string& path,
                           bool embed,
                           const std::string& extraAttribs)
{
   // determine type
   bool isCss = boost::algorithm::ends_with(path, "css");

   // generate code for link vs. embed
   std::string code;
   if (embed)
   {
      if (isCss)
      {
         code = "<style type=\"text/css\" " + extraAttribs + " >\n" +
               resourceFiles().get("presentation/" + path) + "\n"
               + "</style>";
      }
      else
      {
         code = "<script type=\"text/javascript\" " + extraAttribs + " >\n" +
               resourceFiles().get("presentation/" + path) + "\n"
               + "</script>";
      }
   }
   else
   {
      if (isCss)
      {
         code = "<link rel=\"stylesheet\" type=\"text/css\" href=\""
                + path + "\" " + extraAttribs + " >";
      }
      else
      {
         code = "<script src=\"" + path + "\" " + extraAttribs + " ></script>";
      }
   }

   return code;
}

std::string revealEmbed(const std::string& path,
                        const std::string& extraAttribs = std::string())
{
   return revealResource(path, true, extraAttribs);
}

std::string revealLink(const std::string& path,
                       const std::string& extraAttribs = std::string())
{
   return revealResource(path, false, extraAttribs);
}


std::string remoteMathjax()
{
   return resourceFiles().get("presentation/mathjax.html");
}

std::string alternateMathjax(const std::string& prefix)
{
   return boost::algorithm::replace_first_copy(
        remoteMathjax(),
        "https://mathjax.rstudio.com/latest",
        prefix);
}


std::string localMathjax()
{
   return alternateMathjax("mathjax-26");
}

std::string copiedMathjax(const FilePath& targetFile)
{
   // determine target files dir and create it if necessary
   std::string presFilesDir = targetFile.stem() + "_files";
   FilePath filesTargetDir = targetFile.parent().complete(presFilesDir);
   Error error = filesTargetDir.ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
      return remoteMathjax();
   }

   // copy the mathjax directory
   r::exec::RFunction fileCopy("file.copy");
   fileCopy.addParam("from", string_utils::utf8ToSystem(
                         session::options().mathjaxPath().absolutePath()));
   fileCopy.addParam("to", string_utils::utf8ToSystem(
                         filesTargetDir.absolutePath()));
   fileCopy.addParam("recursive", true);
   error = fileCopy.call();
   if (error)
   {
      LOG_ERROR(error);
      return remoteMathjax();
   }

   // return fixed up html
   return alternateMathjax(presFilesDir + "/mathjax-26");
}

std::string localWebFonts()
{
   return "@import url('revealjs/fonts/NewsCycle.css');\n"
          "@import url('revealjs/fonts/Lato.css');";
}

std::string remoteWebFonts()
{
   return "@import url('https://fonts.googleapis.com/css?family=News+Cycle:400,700');\n"
          "@import url('https://fonts.googleapis.com/css?family=Lato:400,700,400italic,700italic');";
}

std::string embeddedWebFonts()
{
   std::string fonts = "presentation/revealjs/fonts";
   std::string css = resourceFiles().get(fonts + "/Lato.css") +
                     resourceFiles().get(fonts + "/NewsCycle.css");

   try
   {
      // input stream
      std::istringstream cssStream(css);

      // filtered output stream
      boost::iostreams::filtering_ostream filteredStream;

      // base64 encoder
      FilePath fontPath = session::options().rResourcesPath().complete(fonts);
      filteredStream.push(html_utils::CssUrlFilter(fontPath));

      // target stream
      std::ostringstream os;
      os.exceptions(std::istream::failbit | std::istream::badbit);
      filteredStream.push(os);

      // copy and return
      boost::iostreams::copy(cssStream, filteredStream, 128);
      return os.str();
   }
   catch(const std::exception& e)
   {
      LOG_ERROR_MESSAGE(e.what());
      return remoteWebFonts();
   }

}

bool hasKnitrVersion_1_2()
{
   return module_context::isPackageVersionInstalled("knitr", "1.2");
}

std::string extractKnitrError(const std::string& stdError)
{
   std::string knitrError = stdError;

   // strip everything before "Error in"
   size_t errorInPos = knitrError.find("Error in");
   if (errorInPos != std::string::npos)
      knitrError = knitrError.substr(errorInPos);

   // strip everything (inclusive) after "Calls: "
   size_t callsPos = knitrError.find("Calls: ");
   if (callsPos != std::string::npos)
      knitrError = knitrError.substr(0, callsPos);

   return boost::algorithm::trim_copy(knitrError);

}

bool performKnit(const FilePath& rmdPath,
                 bool clearCache,
                 ErrorResponse* pErrorResponse)
{
   // calculate the target md path
   FilePath mdPath = rmdPath.parent().childPath(rmdPath.stem() + ".md");

   // remove the md if we are clearing the cache
   if (clearCache)
   {
      Error error = mdPath.removeIfExists();
      if (error)
         LOG_ERROR(error);
   }

   // Now detect whether we even need to knit -- if there is an .md
   // file with timestamp the same as or later than the .Rmd then skip it
   if (mdPath.exists() && (mdPath.lastWriteTime() > rmdPath.lastWriteTime()))
      return true;

   // R binary
   FilePath rProgramPath;
   Error error = module_context::rScriptPath(&rProgramPath);
   if (error)
   {
      *pErrorResponse = ErrorResponse(error.summary());
      return false;
   }

   // confirm correct version of knitr
   if (!hasKnitrVersion_1_2())
   {
      *pErrorResponse = ErrorResponse("knitr version 1.2 or greater is "
                                      "required for presentations");
      return false;
   }

   // removet the target file
   error = mdPath.removeIfExists();
   if (error)
      LOG_ERROR(error);

   // remove the cache if requested
   if (clearCache)
   {
      FilePath cachePath = rmdPath.parent().childPath(rmdPath.stem()+"-cache");
      if (cachePath.exists())
      {
         Error error = cachePath.remove();
         if (error)
            LOG_ERROR(error);
      }
   }

   // args
   std::vector<std::string> args;
   args.push_back("--slave");
   args.push_back("--no-save");
   args.push_back("--no-restore");
   args.push_back("-e");
   boost::format fmt("library(knitr); "
                     "opts_chunk$set(cache.path='%1%-cache/', "
                                    "fig.path='%1%-figure/', "
                                    "tidy=FALSE, "
                                    "warning=FALSE, "
                                    "error=FALSE, "
                                    "message=FALSE, "
                                    "comment=NA); "
                     "render_markdown(); "
                     "knit('%2%', output = '%3%', encoding='%4%');");
   std::string encoding = projects::projectContext().defaultEncoding();
   if(encoding.empty()) encoding = "UTF-8";
   std::string cmd = boost::str(
      fmt % string_utils::utf8ToSystem(rmdPath.stem())
          % string_utils::utf8ToSystem(rmdPath.filename())
          % string_utils::utf8ToSystem(mdPath.filename())
          % encoding);
   args.push_back(cmd);

   // options
   core::system::ProcessOptions options;
   core::system::ProcessResult result;
   options.workingDir = rmdPath.parent();

   // run knit
   error = core::system::runProgram(
            core::string_utils::utf8ToSystem(rProgramPath.absolutePath()),
            args,
            "",
            options,
            &result);
   if (error)
   {
      *pErrorResponse = ErrorResponse(error.summary());
      return false;
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      // if the markdown file doesn't exist then create one to
      // play the error text back into
      if (!mdPath.exists())
      {
         Error error = core::writeStringToFile(mdPath,
                                               mdPath.stem() +
                                               "\n=======================\n");
         if (error)
            LOG_ERROR(error);
      }

      // append the knitr error message to the file
      std::ostringstream ostr;
      ostr << std::endl
           << "```" << std::endl
           << extractKnitrError(result.stdErr) << std::endl
           << "```" << std::endl;

      Error error = core::appendToFile(mdPath, ostr.str());
      if (error)
         LOG_ERROR(error);

      return true;
   }
   else
   {
      return true;
   }
}

std::string presentationCommandClickHandler(const std::string& name,
                                            const std::string& params)
{
   using namespace boost::algorithm;
   std::ostringstream ostr;
   ostr << "onclick='";
   ostr << "window.parent.dispatchPresentationCommand(";
   json::Object cmdObj;
   using namespace boost::algorithm;
   cmdObj["name"] = name;
   cmdObj["params"] = params;
   json::write(cmdObj, ostr);
   ostr << "); return false;'";
   return ostr.str();
}

std::string fixupLink(const boost::cmatch& match)
{
   std::string href = http::util::urlDecode(match[1]);

   if (boost::algorithm::starts_with(href, "#"))
   {
      // leave internal links alone
      return match[0];
   }
   else if (href.find("://") != std::string::npos)
   {
      // open external links in a new window
      return match[0] + " target=\"_blank\"";
   }
   else if (boost::algorithm::starts_with(href, "help-topic:") ||
            boost::algorithm::starts_with(href, "help-doc:"))
   {
      // convert help commands to javascript calls
      std::string onClick;
      std::size_t colonLoc = href.find_first_of(':');
      if (href.size() > colonLoc+2)
      {
         using namespace boost::algorithm;
         std::string name =  trim_copy(href.substr(0, colonLoc));
         std::string params = trim_copy(href.substr(colonLoc+1));
         onClick = presentationCommandClickHandler(name, params);
      }

      return match[0] + " " + onClick;
   }
   else if (boost::algorithm::starts_with(href, "tutorial:"))
   {
      // paths are relative to the parent dir of the presenentation dir
      using namespace boost::algorithm;
      std::size_t colonLoc = href.find_first_of(':');
      std::string path = trim_copy(href.substr(colonLoc+1));
      path = core::http::util::urlDecode(path);
      if (boost::algorithm::starts_with(path, "~/"))
         path = module_context::resolveAliasedPath(path).absolutePath();
      FilePath filePath = presentation::state::directory()
                                                   .parent().complete(path);

      Error error = core::system::realPath(filePath, &filePath);
      if (error)
      {
         if (!core::isPathNotFoundError(error))
            LOG_ERROR(error);
         return match[0];
      }

      // bulid the call
      std::string onClick;
      if (href.size() > colonLoc+2)
      {
         std::string name = "tutorial";
         std::string params = module_context::createAliasedPath(filePath);
         onClick = presentationCommandClickHandler(name, params);
      }

      return match[0] + " " + onClick;
   }
   else
   {
      return match[0];
   }
}

boost::iostreams::regex_filter linkFilter()
{
   return boost::iostreams::regex_filter(
            boost::regex("<a href=\"([^\"]+)\""),
            fixupLink);
}

std::string userSlidesCss(const SlideDeck& slideDeck)
{
   // first determine the path to the css file -- check for a css field
   // first and if that doesn't exist form one from the basename of
   // the presentation
   std::string cssFile = slideDeck.css();
   if (cssFile.empty())
      cssFile = presentation::state::filePath().stem() + ".css";
   FilePath cssPath = presentation::state::directory().complete(cssFile);

   // read user css if it exists
   std::string userSlidesCss;
   if (cssPath.exists())
   {
      Error error = core::readStringFromFile(cssPath, &userSlidesCss);
      if (error)
         LOG_ERROR(error);
   }

   // return it
   return userSlidesCss;
}


bool readPresentation(SlideDeck* pSlideDeck,
                      std::string* pSlides,
                      std::string* pInitActions,
                      std::string* pSlideActions,
                      std::map<std::string,std::string>* pVars,
                      ErrorResponse* pErrorResponse)
{
   // look for slides and knit if we need to
   FilePath rmdFile = presentation::state::filePath();
   std::string ext = rmdFile.extensionLowerCase();
   if (rmdFile.exists() && (ext != ".md"))
   {
      if (!performKnit(rmdFile, false, pErrorResponse))
         return false;
   }

   // look for slides markdown
   FilePath slidesFile = rmdFile.parent().childPath(rmdFile.stem() + ".md");
   if (!slidesFile.exists())
   {
      *pErrorResponse = ErrorResponse(slidesFile.absolutePath() +
                                      " not found");
      return false;
   }

   // parse the slides
   Error error = pSlideDeck->readSlides(slidesFile);
   if (error)
   {
      LOG_ERROR(error);
      *pErrorResponse = ErrorResponse(error.summary());
      return false;
   }

   // render the slides
   std::string slidesHead;
   std::string revealConfig;
   error = presentation::renderSlides(*pSlideDeck,
                                      &slidesHead,
                                      pSlides,
                                      &revealConfig,
                                      pInitActions,
                                      pSlideActions);
   if (error)
   {
      LOG_ERROR(error);
      *pErrorResponse = ErrorResponse(error.summary());
      return false;
   }

   // build template variables
   std::map<std::string,std::string>& vars = *pVars;
   vars["title"] = pSlideDeck->title();
   vars["slides_head"] = slidesHead;
   vars["slides"] = *pSlides;
   vars["slides_css"] =  resourceFiles().get("presentation/slides.css");
   vars["user_slides_css"] = userSlidesCss(*pSlideDeck);
   vars["r_highlight"] = resourceFiles().get("r_highlight.html");
   vars["reveal_config"] = revealConfig;
   vars["preamble"] = pSlideDeck->preamble();

   return true;
}

bool renderPresentation(
                   const std::map<std::string,std::string>& vars,
                   const std::vector<boost::iostreams::regex_filter>& filters,
                   std::ostream& os,
                   ErrorResponse* pErrorResponse)
{
   std::string presentationTemplate =
                           resourceFiles().get("presentation/slides.html");
   std::istringstream templateStream(presentationTemplate);

   try
   {
      os.exceptions(std::istream::failbit | std::istream::badbit);
      boost::iostreams::filtering_ostream filteredStream ;

      // template filter
      text::TemplateFilter templateFilter(vars);
      filteredStream.push(templateFilter);

      // custom filters
      for (std::size_t i=0; i<filters.size(); i++)
         filteredStream.push(filters[i]);

      // target stream
      filteredStream.push(os);

      boost::iostreams::copy(templateStream, filteredStream, 128);
   }
   catch(const std::exception& e)
   {
      *pErrorResponse = ErrorResponse(e.what());
      return false;
   }

   return true;
}

typedef boost::function<void(const FilePath&,
                             const std::string&,
                             std::map<std::string,std::string>*)> VarSource;

void publishToRPubsVars(const FilePath&,
                        const std::string& slides,
                        std::map<std::string,std::string>* pVars)
{
   std::map<std::string,std::string>& vars = *pVars;

   // webfonts w/ remote url
   vars["google_webfonts"] = remoteWebFonts();

   // mathjax w/ remote url
   if (markdown::isMathJaxRequired(slides))
      vars["mathjax"] = remoteMathjax();
   else
      vars["mathjax"] = "";
}

void saveAsStandaloneVars(const FilePath& targetFile,
                          const std::string& slides,
                          std::map<std::string,std::string>* pVars)
{
   std::map<std::string,std::string>& vars = *pVars;

   // embedded web fonts
   vars["google_webfonts"] = embeddedWebFonts();

   // mathjax w/ remote url
   if (markdown::isMathJaxRequired(slides))
      vars["mathjax"] = copiedMathjax(targetFile);
   else
      vars["mathjax"] = "";
}


void viewInBrowserVars(const std::string& slides,
                       std::map<std::string,std::string>* pVars)
{
   std::map<std::string,std::string>& vars = *pVars;

   vars["google_webfonts"] = localWebFonts();

   // mathjax w/ local
   if (markdown::isMathJaxRequired(slides))
      vars["mathjax"] = localMathjax();
   else
      vars["mathjax"] = "";
}

void fontVars(const SlideDeck& slideDeck,
              std::map<std::string,std::string>* pVars)
{
   std::map<std::string,std::string>& vars = *pVars;

   // font imports
   vars["user_font_imports"] = std::string();
   if (slideDeck.slides().size() > 0)
   {
      std::ostringstream ostr;
      std::vector<std::string> fontImports =
            slideDeck.slides().at(0).fieldValues("font-import");

      BOOST_FOREACH(const std::string& fontImport, fontImports)
      {
         ostr << "@import url('" << fontImport << "');" << std::endl;
      }

      vars["user_font_imports"] = ostr.str();
   }

   // fonts
   if (!slideDeck.fontFamily().empty())
   {
      vars["reveal_font"] = slideDeck.fontFamily();
      vars["reveal_heading_font"] = slideDeck.fontFamily();
   }
   else
   {
      vars["reveal_font"] = kDefaultRevealFont;
      vars["reveal_heading_font"] = kDefaultRevealHeadingFont;
   }
}


void localRevealVars(std::map<std::string,std::string>* pVars)
{
   std::map<std::string,std::string>& vars = *pVars;

   vars["reveal_css"] = revealLink("revealjs/css/reveal.css");
   vars["reveal_theme_css"] = revealLink("revealjs/css/theme/simple.css");
   vars["reveal_head_js"] = revealLink("revealjs/lib/js/head.min.js");
   vars["reveal_js"] = revealLink("revealjs/js/reveal.js");
}


void revealSizeVars(const SlideDeck& slideDeck,
                    bool zoom,
                    bool allowAutosize,
                    std::map<std::string,std::string>* pVars)
{
   std::map<std::string,std::string>& vars = *pVars;

   bool autosize = allowAutosize && slideDeck.autosize();
   vars["reveal_autosize"] = autosize ? "true" : "false";
   if (autosize)
   {
      std::string zoomStr = zoom ? "true" : "false";
      vars["reveal_width"] = "revealDetectWidth(" + zoomStr + ")";
      vars["reveal_height"] = "revealDetectHeight(" + zoomStr + ")";
   }
   else
   {
      vars["reveal_width"] = safe_convert::numberToString(slideDeck.width());
      vars["reveal_height"] = safe_convert::numberToString(slideDeck.height());
   }
}

void externalBrowserVars(const SlideDeck& slideDeck,
                         std::map<std::string,std::string>* pVars)
{
   std::map<std::string,std::string>& vars = *pVars;

   vars["slide_commands"] = "";
   vars["slides_js"] = "";
   vars["init_commands"] = "";

   // width and height
   revealSizeVars(slideDeck, false, false, pVars);

   // use transitions for standalone
   vars["reveal_transition"] = slideDeck.transition();
   vars["reveal_transition_speed"] = slideDeck.transitionSpeed();

   // rtl
   vars["reveal_rtl"] = slideDeck.rtl();
}

bool createStandalonePresentation(const FilePath& targetFile,
                                  const VarSource& varSource,
                                  ErrorResponse* pErrorResponse)
{
   // read presentation
   presentation::SlideDeck slideDeck;
   std::string slides, initCommands, slideCommands;
   std::map<std::string,std::string> vars;
   if (!readPresentation(&slideDeck,
                         &slides,
                         &initCommands,
                         &slideCommands,
                         &vars,
                         pErrorResponse))
   {
      return false;
   }

   // embedded versions of reveal assets
   vars["reveal_print_css"] = revealEmbed("revealjs/css/print/pdf.css",
                                          kMediaPrint);
   vars["reveal_css"] = revealEmbed("revealjs/css/reveal.min.css");
   vars["reveal_theme_css"] = revealEmbed("revealjs/css/theme/simple.css");
   vars["reveal_head_js"] = revealEmbed("revealjs/lib/js/head.min.js");
   vars["reveal_js"] = revealEmbed("revealjs/js/reveal.min.js");

   // font vars
   fontVars(slideDeck, &vars);

   // call var source hook function
   varSource(targetFile, slides, &vars);

   // no IDE interaction
   externalBrowserVars(slideDeck, &vars);

   // target file stream
   boost::shared_ptr<std::ostream> pOfs;
   Error error = targetFile.open_w(&pOfs);
   if (error)
   {
      LOG_ERROR(error);
      *pErrorResponse = ErrorResponse(error.summary());
      return false;
   }

   // create image filter
   FilePath dirPath = presentation::state::directory();
   std::vector<boost::iostreams::regex_filter> filters;
   filters.push_back(html_utils::Base64ImageFilter(dirPath));

   // render presentation
   return renderPresentation(vars, filters, *pOfs, pErrorResponse);
}


void loadSlideDeckDependencies(const SlideDeck& slideDeck)
{
   // see if there is a depends field
   std::string dependsField = slideDeck.depends();
   std::vector<std::string> depends;
   boost::algorithm::split(depends,
                           dependsField,
                           boost::algorithm::is_any_of(","));

   // load any dependencies
   BOOST_FOREACH(std::string pkg, depends)
   {
      boost::algorithm::trim(pkg);

      if (module_context::isPackageInstalled(pkg))
      {
         Error error = r::exec::RFunction(".rs.loadPackage", pkg, "").call();
         if (error)
            LOG_ERROR(error);
      }
   }
}

void setWebCacheableFileResponse(const FilePath& path,
                                 const http::Request& request,
                                 http::Response* pResponse)
{
   if (options().programMode() == kSessionProgramModeServer)
   {
      pResponse->setCacheWithRevalidationHeaders();
      pResponse->setCacheableBody(path, request);
   }
   else
   {
      // Qt doesn't deal well with etag-based caching, so just send the body
      // without cache headers
      pResponse->setBody(path);
   }
}

void handlePresentationRootRequest(const std::string& path,
                                   http::Response* pResponse)
{   
   // read presentation
   presentation::SlideDeck slideDeck;
   std::string slides, initCommands, slideCommands;
   std::map<std::string,std::string> vars;
   ErrorResponse errorResponse;
   if (!readPresentation(&slideDeck,
                         &slides,
                         &initCommands,
                         &slideCommands,
                         &vars,
                         &errorResponse))
   {
      pResponse->setError(errorResponse.statusCode, errorResponse.message);
      return;
   }

   // load any dependencies
   loadSlideDeckDependencies(slideDeck);

   // notify slide deck changed
   log().onSlideDeckChanged(slideDeck);
   onSlideDeckChangedOverlay(slideDeck);

   // set preload to none for media
   vars["slides"] = boost::algorithm::replace_all_copy(
                                          vars["slides"],
                                          "controls preload=\"auto\"",
                                          "controls preload=\"none\"");

   // linked versions of reveal assets
   localRevealVars(&vars);

   // font vars
   fontVars(slideDeck, &vars);

   // no print css for qtwebkit
   vars["reveal_print_css"]  = "";

   // webfonts local
   vars["google_webfonts"] = localWebFonts();

   // mathjax local
   if (markdown::isMathJaxRequired(slides))
      vars["mathjax"] = localMathjax();
   else
      vars["mathjax"] = "";

   // javascript supporting IDE interaction
   vars["slide_commands"] = slideCommands;
   vars["slides_js"] = resourceFiles().get("presentation/slides.js");
   vars["init_commands"] = initCommands;

   // width and height
   bool zoom = path == "zoom";
   revealSizeVars(slideDeck, zoom, true, &vars);

   // no transition in desktop mode (qtwebkit can't keep up)
   bool isDesktop = options().programMode() == kSessionProgramModeDesktop;
   vars["reveal_transition"] =  isDesktop? "none" : slideDeck.transition();
   vars["reveal_transition_speed"] = isDesktop ? "default" :
                                                 slideDeck.transitionSpeed();

   // rtl
   vars["reveal_rtl"] = slideDeck.rtl();

   // render to output stream
   std::stringstream previewOutputStream;
   std::vector<boost::iostreams::regex_filter> filters;
   filters.push_back(linkFilter());
   if (renderPresentation(vars, filters, previewOutputStream, &errorResponse))
   {
      // set response
      pResponse->setNoCacheHeaders();
      pResponse->setContentType("text/html");
      pResponse->setBody(previewOutputStream);

      // also save a view in browser version if that path already exists
      // (allows the user to do a simple browser refresh to see changes)
      FilePath viewInBrowserPath = presentation::state::viewInBrowserPath();
      if (viewInBrowserPath.exists())
      {
         ErrorResponse errorResponse;
         if (!savePresentationAsStandalone(viewInBrowserPath, &errorResponse))
            LOG_ERROR_MESSAGE(errorResponse.message);
      }
   }
   else
   {
      pResponse->setError(errorResponse.statusCode, errorResponse.message);
   }

   if (!zoom)
   {
      ClientEvent event(client_events::kPresentationPaneRequestCompleted);
      module_context::enqueClientEvent(event);
   }
}




void handlePresentationHelpMarkdownRequest(const FilePath& filePath,
                                           const std::string& jsCallbacks,
                                           core::http::Response* pResponse)
{
   // indirection on the actual md file (related to processing R markdown)
   FilePath mdFilePath;

   // knit if required
   if (filePath.mimeContentType() == "text/x-r-markdown")
   {
      // actual file path will be the md file
      mdFilePath = filePath.parent().complete(filePath.stem() + ".md");

      // do the knit if we need to
      ErrorResponse errorResponse;
      if (!performKnit(filePath, false, &errorResponse))
      {
         pResponse->setError(errorResponse.statusCode, errorResponse.message);
         return;
      }
   }
   else
   {
      mdFilePath = filePath;
   }

   // read in the file (process markdown)
   std::string helpDoc;
   Error error = markdown::markdownToHTML(mdFilePath,
                                          markdown::Extensions(),
                                          markdown::HTMLOptions(),
                                          &helpDoc);
   if (error)
   {
      pResponse->setError(error);
      return;
   }

   // process the template
   std::map<std::string,std::string> vars;
   vars["title"] = html_utils::defaultTitle(helpDoc);
   vars["styles"] = resourceFiles().get("presentation/helpdoc.css");
   vars["r_highlight"] = resourceFiles().get("r_highlight.html");
   if (markdown::isMathJaxRequired(helpDoc))
      vars["mathjax"] = localMathjax();
   else
      vars["mathjax"] = "";
   vars["content"] = helpDoc;
   vars["js_callbacks"] = jsCallbacks;
   pResponse->setNoCacheHeaders();
   pResponse->setContentType("text/html");
   pResponse->setBody(resourceFiles().get("presentation/helpdoc.html"),
                      text::TemplateFilter(vars));
}

void handleRangeRequest(const FilePath& targetFile,
                        const http::Request& request,
                        http::Response* pResponse)
{
   // cache the last file
   struct RangeFileCache
   {
      FileInfo file;
      std::string contentType;
      std::string contents;

      void clear()
      {
         file = FileInfo();
         contentType.clear();
         contents.clear();
      }
   };
   static RangeFileCache s_cache;

   // see if we need to do a fresh read
   if (targetFile.absolutePath() != s_cache.file.absolutePath() ||
       targetFile.lastWriteTime() != s_cache.file.lastWriteTime())
   {
      // clear the cache
      s_cache.clear();

      // read the file in from disk
      Error error = core::readStringFromFile(targetFile, &(s_cache.contents));
      if (error)
      {
         pResponse->setError(error);
         return;
      }

      // update the cache
      s_cache.file = FileInfo(targetFile);
      s_cache.contentType = targetFile.mimeContentType();
   }

   // always serve from the cache
   pResponse->setRangeableFile(s_cache.contents,
                               s_cache.contentType,
                               request);


}

void handlePresentationViewInBrowserRequest(const http::Request& request,
                                            http::Response* pResponse)
{
   // read presentation
   presentation::SlideDeck slideDeck;
   std::string slides, initCommands, slideCommands;
   std::map<std::string,std::string> vars;
   ErrorResponse errorResponse;
   if (!readPresentation(&slideDeck,
                         &slides,
                         &initCommands,
                         &slideCommands,
                         &vars,
                         &errorResponse))
   {
      pResponse->setError(errorResponse.statusCode, errorResponse.message);
      return;
   }

   // linked versions of reveal assets
   localRevealVars(&vars);

   // font vars
   fontVars(slideDeck, &vars);

   // link to reveal print css
   vars["reveal_print_css"] = revealLink("revealjs/css/print/pdf.css",
                                         kMediaPrint);

   // webfonts local
   viewInBrowserVars(slides, &vars);

   // external browser vars
   externalBrowserVars(slideDeck, &vars);

   // render to output stream
   std::stringstream previewOutputStream;
   std::vector<boost::iostreams::regex_filter> filters;
   filters.push_back(linkFilter());
   if (renderPresentation(vars, filters, previewOutputStream, &errorResponse))
   {
      pResponse->setNoCacheHeaders();
      pResponse->setContentType("text/html");
      pResponse->setBody(previewOutputStream);
   }
   else
   {
      pResponse->setError(errorResponse.statusCode, errorResponse.message);
   }
}

void handlePresentationFileRequest(const http::Request& request,
                                  const std::string& dir,
                                  http::Response* pResponse)
{
   std::string path = http::util::pathAfterPrefix(request,
                                                  "/presentation/" + dir + "/");
   FilePath resPath = options().rResourcesPath().complete("presentation");
   FilePath filePath = resPath.complete(dir + "/" + path);
   pResponse->setContentType(filePath.mimeContentType());
   setWebCacheableFileResponse(filePath, request, pResponse);
}

} // anonymous namespace

bool clearKnitrCache(ErrorResponse* pErrorResponse)
{
   FilePath rmdFile = presentation::state::filePath();
   std::string ext = rmdFile.extensionLowerCase();
   if (rmdFile.exists() && (ext != ".md"))
      return performKnit(rmdFile, true, pErrorResponse);
   else
      return true;
}


void handlePresentationPaneRequest(const http::Request& request,
                                   http::Response* pResponse)
{
   // return not found if presentation isn't active
   if (!presentation::state::isActive())
   {
      pResponse->setNotFoundError(request.uri());
      return;
   }

   // get the requested path
   std::string path = http::util::pathAfterPrefix(request, "/presentation/");

   // special handling for the root
   if (path.empty() || (path == "zoom"))
   {
      handlePresentationRootRequest(path, pResponse);
   }

   // special handling for view in browser
   else if (boost::algorithm::starts_with(path, "view"))
   {
      handlePresentationViewInBrowserRequest(request, pResponse);
   }

   // special handling for reveal.js assets
   else if (boost::algorithm::starts_with(path, "revealjs/"))
   {
      handlePresentationFileRequest(request, "revealjs", pResponse);
   }

   // special handling for images
   else if (boost::algorithm::starts_with(path, "slides-images/"))
   {
      handlePresentationFileRequest(request, "slides-images", pResponse);
   }

   // special handling for mathjax assets
   else if (boost::algorithm::starts_with(path, "mathjax-26/"))
   {
      FilePath filePath =
            session::options().mathjaxPath().parent().childPath(path);
      setWebCacheableFileResponse(filePath, request, pResponse);
   }


   // serve the file back
   else
   {
      FilePath targetFile = presentation::state::directory().childPath(path);
      if (!request.headerValue("Range").empty())
      {
         handleRangeRequest(targetFile, request, pResponse);
      }
      else
      {
         // indicate that we accept byte range requests
         pResponse->addHeader("Accept-Ranges", "bytes");

         // return the file
         setWebCacheableFileResponse(targetFile, request, pResponse);
      }
   }
}

void handlePresentationHelpRequest(const core::http::Request& request,
                                   const std::string& jsCallbacks,
                                   core::http::Response* pResponse)
{
   // we save the most recent /help/presentation/&file=parameter so we
   // can resolve relative file references against it. we do this
   // separately from presentation::state::directory so that the help
   // urls can be available within the help pane (and history)
   // independent of the duration of the presentation tab
   static FilePath s_presentationHelpDir;

   // check if this is a root request
   std::string file = request.queryParamValue("file");
   if (!file.empty())
   {
      // ensure file exists
      FilePath filePath = module_context::resolveAliasedPath(file);
      if (!filePath.exists())
      {
         pResponse->setNotFoundError(request.uri());
         return;
      }

      // save the help dir
      s_presentationHelpDir = filePath.parent();

      // check for markdown
      if (filePath.mimeContentType() == "text/x-markdown" ||
          filePath.mimeContentType() == "text/x-r-markdown")
      {
         handlePresentationHelpMarkdownRequest(filePath,
                                               jsCallbacks,
                                               pResponse);
      }

      // just a stock file
      else
      {
         setWebCacheableFileResponse(filePath, request, pResponse);
      }
   }

   // it's a relative file reference
   else
   {
      // make sure the directory exists
      if (!s_presentationHelpDir.exists())
      {
         pResponse->setNotFoundError(s_presentationHelpDir.absolutePath());
         return;
      }

      // resolve the file reference
      std::string path = http::util::pathAfterPrefix(request,
                                                     "/help/presentation/");

      // serve the file back
      setWebCacheableFileResponse(s_presentationHelpDir.complete(path),
                                  request, pResponse);
   }
}

bool savePresentationAsStandalone(const core::FilePath& filePath,
                                  ErrorResponse* pErrorResponse)
{
   return createStandalonePresentation(filePath,
                                       saveAsStandaloneVars,
                                       pErrorResponse);
}

bool savePresentationAsRpubsSource(const core::FilePath& filePath,
                                   ErrorResponse* pErrorResponse)
{
   return createStandalonePresentation(filePath,
                                       publishToRPubsVars,
                                       pErrorResponse);
}




} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

