/*
 * SlideRenderer.cpp
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


#include "SlideRenderer.hpp"

#include <boost/foreach.hpp>
#include <boost/format.hpp>
#include <boost/regex.hpp>
#include <boost/algorithm/string.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/StringUtils.hpp>
#include <core/json/Json.hpp>

#include <core/markdown/Markdown.hpp>

#include <session/SessionModuleContext.hpp>

#include "SlideParser.hpp"
#include "SlideMediaRenderer.hpp"
#include "SlideNavigationList.hpp"
#include "SlideQuizRenderer.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {

namespace {

std::string commandsAsJsonArray(const Slide& slide)
{
   json::Array commandsJsonArray;

   std::vector<Command> commands = slide.commands();
   BOOST_FOREACH(const Command& command, commands)
   {
      commandsJsonArray.push_back(command.asJson());
   }

   std::ostringstream ostr;
   json::write(commandsJsonArray, ostr);
   return ostr.str();
}

Error renderMarkdown(const std::string& content, std::string* pHTML)
{
   markdown::Extensions extensions;
   markdown::HTMLOptions htmlOptions;
   return markdown::markdownToHTML(content,
                                   extensions,
                                   htmlOptions,
                                   pHTML);
}


std::string divWrap(const std::string& classNames,
                    const std::string& width,
                    const std::string& contents)
{
   std::string styleAttribute;
   if (!width.empty())
      styleAttribute = "style=\"width: " + width + ";\" ";

   boost::format fmt("\n<div class=\"%1%\" %2%>\n%3%\n</div>\n");
   return boost::str(fmt % classNames % styleAttribute % contents);
}

std::string divWrap(const std::string& classNames,
                    const std::string& contents)
{
   return divWrap(classNames, "", contents);
}

std::string mediaClass(const std::string& html)
{
   boost::regex imageRegex(
     "\\s*<p>\\s*<img src=\"([^\"]+)\"(?: [\\w]+=\"[^\"]*\")*/>\\s*</p>\\s*");

   boost::smatch match;
   if (regex_utils::search(html, match, imageRegex))
   {
      std::string::const_iterator begin = match[0].first;
      std::string::const_iterator end = match[0].second;

      if (begin == html.begin() && end == html.end())
         return "mediaOnly";
      else
         return "mediaInline";
   }
   // look for videos
   else
   {
      // trim for comparison
      std::string trimmedHtml = boost::algorithm::trim_copy(html);
      if (boost::algorithm::contains(trimmedHtml, "<video id="))
      {
         bool starts = boost::algorithm::starts_with(trimmedHtml, "<video id=");
         bool ends = boost::algorithm::ends_with(trimmedHtml, "</video>");
         if (starts && ends)
            return "mediaOnly";
         else
            return "mediaInline";
      }
   }

   return std::string();
}

void addFragmentClass(const std::string& fragmentClass,
                      std::string* pHTML)
{
   // add fragment class to elements eligible for incremental build
   *pHTML = boost::regex_replace(*pHTML,
                                 boost::regex("<(h[1-6]|p|blockquote|pre|li)>"),
                                 "<$1 class=\"" + fragmentClass + "\">");

   // remove class from first paragraph element
   std::string pWithClass = "<p class=\"" + fragmentClass + "\">";
   boost::algorithm::replace_first(*pHTML, pWithClass, "<p>");

   // remove class from paragraph inside blockquote
   std::string bqWithClass = "<blockquote class=\"" + fragmentClass + "\">";
   boost::algorithm::replace_first(*pHTML, bqWithClass + "\n" + pWithClass,
                                   bqWithClass + "\n<p>");
}

void validateTransitionType(const std::string& type)
{
   bool isValid = boost::iequals(type, "none") ||
                  boost::iequals(type, "default") ||
                  boost::iequals(type, "linear") ||
                  boost::iequals(type, "fade") ||
                  boost::iequals(type, "zoom") ||
                  boost::iequals(type, "concave");

   if (!isValid)
   {
      module_context::consoleWriteError("Invalid value for transition field: "
                                        + type + "\n");
   }
}

void validateTransitionSpeedType(const std::string& speed)
{
   bool isValid = speed.empty() ||
                  boost::iequals(speed, "default") ||
                  boost::iequals(speed, "fast") ||
                  boost::iequals(speed, "slow");
   if (!isValid)
   {
      module_context::consoleWriteError("Invalid value for transition-speed "
                                        "field: " + speed + "\n");
   }
}

void validateNavigationType(const std::string& type)
{
   bool isValid = boost::iequals(type, "slide") ||
                  boost::iequals(type, "section") ||
                  boost::iequals(type, "none");

   if (!isValid)
   {
      module_context::consoleWriteError("Invalid value for navigation field: "
                                        + type + "\n");
   }
}

void validateIncrementalType(const std::string& type)
{
   bool isValid = boost::iequals(type, "false") ||
                  boost::iequals(type, "true");

   if (!isValid)
   {
      module_context::consoleWriteError("Invalid value for incremental field: "
                                        + type + "\n");
   }
}

void validateSlideDeckFields(const SlideDeck& slideDeck)
{
   validateTransitionType(slideDeck.transition());
   validateTransitionSpeedType(slideDeck.transitionSpeed());
   validateNavigationType(slideDeck.navigation());
   validateIncrementalType(slideDeck.incremental());
}

void computeColumnWidths(const std::string& width,
                         std::string* pSpecifiedWidth,
                         std::string* pOtherWidth)
{
   int w = core::safe_convert::stringTo<int>(width, 50);
   *pSpecifiedWidth = safe_convert::numberToString(w - 2) + "%";
   *pOtherWidth = safe_convert::numberToString(100 - w - 2) + "%";
}

void computeColumnWidths(const Slide& slide,
                         std::string* pLeftWidth,
                         std::string* pRightWidth)
{
   boost::regex re("([0-9]+)\\s*%?");
   boost::smatch match;

   std::string slideLeft = slide.left();
   std::string slideRight = slide.right();
   if (regex_utils::match(slideLeft, match, re))
      computeColumnWidths(match[1], pLeftWidth, pRightWidth);
   else if (regex_utils::match(slideRight, match, re))
      computeColumnWidths(match[1], pRightWidth, pLeftWidth);
}

Error slideToHtml(const Slide& slide,
                  int slideNumber,
                  const std::string& extraContent,
                  const std::string& incremental,
                  std::string* pHead,
                  std::string* pHTML)
{
   // invalid fields
   if (slide.invalidFields().size() > 0)
   {
      std::ostringstream ostr;
      ostr << "<div class=\"fieldError\">";
      BOOST_FOREACH(const std::string& field, slide.invalidFields())
      {
         ostr << "<span>Unrecognized slide field:</span> "
              << "<code>" << field << "</code><br/>";
      }
      ostr << "</div>";
      pHTML->append(ostr.str());
   }

   // render the markdown
   std::string markdownHTML;
   Error error = renderMarkdown(slide.content(), &markdownHTML);
   if (error)
      return error;

   // see if we need to render a quiz
   if (slide.type() == "quiz-multichoice")
   {
      std::string head;
      renderQuiz(slideNumber, &head, &markdownHTML);
      pHead->append(head);
   }

   // append the html
   pHTML->append(markdownHTML);

   // add the extra content
   pHTML->append(extraContent);

   // slide content classes
   std::string slideClasses = "slideContent";
   if (!slide.showTitle())
      slideClasses += " noTitle";
   if (!slide.cssClass().empty())
      slideClasses += " " + slide.cssClass();

   // look for an <hr/> splitting the html into columns
   const std::string kHRTag = "<hr/>";
   std::size_t hrLoc = pHTML->find(kHRTag);
   if (hrLoc != std::string::npos)
   {
      // get the columns
      std::string column1 = pHTML->substr(0, hrLoc);
      std::string column2;
      if (pHTML->length() > (column1.length() + kHRTag.length()))
         column2 = pHTML->substr(hrLoc + kHRTag.length());

      // compute the column widths
      std::string leftWidth;
      std::string rightWidth;
      computeColumnWidths(slide, &leftWidth, &rightWidth);

      // now render two divs with the columns
      pHTML->clear();
      std::ostringstream ostr;
      ostr << divWrap("column column1 " + slideClasses, leftWidth, column1);
      ostr << divWrap("column column2 " + slideClasses, rightWidth, column2);
      *pHTML = ostr.str();
   }

   // apply standard (and optional media) classes
   else
   {
      std::string extraMediaClass = mediaClass(*pHTML);
      if (!extraMediaClass.empty())
         slideClasses = extraMediaClass + " " + slideClasses;

      *pHTML = divWrap(slideClasses, *pHTML);
   }

   // check whether we need to apply the fragment style (create
   // fragmentClass string so we can support other fragment
   // reveal visual styles in the future if we want)
   std::string fragmentClass;
   if (incremental == "true")
      fragmentClass = "fragment";

   // apply fragmentClass if necessary
   if (!fragmentClass.empty())
      addFragmentClass(fragmentClass, pHTML);

   return Success();
}

} // anonymous namespace


Error renderSlides(const SlideDeck& slideDeck,
                   std::string* pSlidesHead,
                   std::string* pSlides,
                   std::string* pRevealConfig,
                   std::string* pInitActions,
                   std::string* pSlideActions)
{
   // validate global slide deck fields (will just print warnings)
   validateSlideDeckFields(slideDeck);

   // render the slides to HTML and slide commands to case statements
   std::ostringstream ostr, ostrRevealConfig, ostrInitActions, ostrSlideActions;

   // track json version of slide list
   SlideNavigationList navigationList(slideDeck.navigation());

   // now the slides
   std::string cmdPad(8, ' ');
   int slideNumber = 0;
   for (size_t i=0; i<slideDeck.slides().size(); i++)
   {
      // slide
      const Slide& slide = slideDeck.slides().at(i);

      // is this the first slide?
      bool isFirstSlide = (i == 0);

      // track slide in list
      navigationList.add(slide);

      ostr << "<section";
      if (!slide.id().empty())
         ostr << " id=\"" << slide.id() << "\"";

      // get the slide type
      std::string type = isFirstSlide ? "section" : slide.type();

      // add the state if there is a type
      if (!type.empty())
         ostr << " data-state=\"" << type <<  "\"";

      // add the transition
      std::string transition;
      if (!slide.transition().empty())
      {
         validateTransitionType(slide.transition());
         transition = slide.transition();
      }
      else
      {
         transition = slideDeck.transition();
      }
      ostr << " data-transition=\"" << transition << "\"";

      // add the transition speed
      std::string transitionSpeed;
      if (!slide.transitionSpeed().empty())
      {
         validateTransitionSpeedType(slide.transitionSpeed());
         transitionSpeed = slide.transitionSpeed();
      }
      else
      {
         transitionSpeed = slideDeck.transitionSpeed();
      }
      ostr << " data-transition-speed=\"" << transitionSpeed << "\"";

      // end section tag
      ostr << ">\n";

      // show the title with the appropriate header. also track whether
      // this slide is eligible for incremental display (first slide
      // and section slides are not)
      bool canShowIncremental = true;
      if (isFirstSlide || slide.showTitle())
      {
         std::string hTag;
         if (isFirstSlide)
         {
            hTag = "h1";
            canShowIncremental = false;
         }
         else if (type == "section" || type == "sub-section")
         {
            hTag = "h2";
            canShowIncremental = false;
         }
         else
         {
            hTag = "h3";
         }

         ostr << "<" << hTag << ">"
              << string_utils::htmlEscape(slide.title())
              << "</" << hTag << ">";
      }

      // if this is slide one then render author and date if they are included
      if (isFirstSlide)
      {
         ostr << "<p>";
         if (!slide.author().empty())
         {
            ostr << string_utils::htmlEscape(slide.author());
            if (!slide.date().empty())
               ostr << "<br/>";
         }
         if (!slide.date().empty())
            ostr << string_utils::htmlEscape(slide.date());

         ostr << "</p>" << "\n";
      }

      // determine incremental property
      std::string incremental = "false";
      if (canShowIncremental)
      {
         if (!slide.incremental().empty())
         {
            validateIncrementalType(slide.incremental());
            incremental = slide.incremental();
         }
         else
         {
            incremental = slideDeck.incremental();
         }
      }

      // setup vectors for reveal config and init actions
      std::vector<std::string> revealConfig;
      std::vector<std::string> initActions;

      // setup a vector of js actions to take when the slide loads
      // (we always take the action of adding any embedded commands)
      std::vector<std::string> slideActions;
      slideActions.push_back("cmds = " + commandsAsJsonArray(slide));

      // get at commands
      std::vector<AtCommand> atCommands = slide.atCommands();

      // render video if specified
      std::ostringstream ostrMedia;
      std::string video = slide.video();
      if (!video.empty())
      {
         renderMedia("video",
                     slideNumber,
                     slideDeck.baseDir(),
                     video,
                     atCommands,
                     ostrMedia,
                     &initActions,
                     &slideActions);
      }

      // render audio if specified
      std::string audio = slide.audio();
      if (!audio.empty())
      {
         renderMedia("audio",
                     slideNumber,
                     slideDeck.baseDir(),
                     audio,
                     atCommands,
                     ostrMedia,
                     &initActions,
                     &slideActions);
      }


      // render markdown
      std::string headContent, htmlContent;
      Error error = slideToHtml(slide,
                                slideNumber,
                                ostrMedia.str(),
                                incremental,
                                &headContent,
                                &htmlContent);
      if (error)
         return error;

      // record head
      pSlidesHead->append(headContent);

      // record html
      ostr << htmlContent << "\n";

      // render end section
      ostr << "</section>" << "\n";

      // reveal config actions
      BOOST_FOREACH(const std::string& config, revealConfig)
      {
         ostrRevealConfig << config << "," << "\n";
      }

      // javascript actions to take on slide deck init
      BOOST_FOREACH(const std::string& jsAction, initActions)
      {
         ostrInitActions <<  jsAction << ";" << "\n";
      }

      // javascript actions to take on slide load
      ostrSlideActions << cmdPad << "case " << slideNumber << ":" << "\n";
      BOOST_FOREACH(const std::string& jsAction, slideActions)
      {
         ostrSlideActions << cmdPad << "  " << jsAction << ";" << "\n";
      }
      ostrSlideActions << "\n" << cmdPad << "  break;" << "\n";

      // increment slide number
      slideNumber++;
   }

   // init slide list as part of actions
   navigationList.complete();
   ostrInitActions << navigationList.asCall() << "\n";

   *pSlides = ostr.str();
   *pRevealConfig = ostrRevealConfig.str();
   *pInitActions = ostrInitActions.str();
   *pSlideActions = ostrSlideActions.str();
   return Success();
}


} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

