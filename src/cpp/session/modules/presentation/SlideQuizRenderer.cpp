/*
 * SlideQuizRenderer.cpp
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

#include "SlideQuizRenderer.hpp"

#include <iostream>

#include <boost/bind.hpp>
#include <boost/format.hpp>
#include <boost/algorithm/string/replace.hpp>

#include <core/SafeConvert.hpp>
#include <core/RegexUtils.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace presentation {

namespace {

std::string handleClickFunction(const std::string& formId)
{
   return "handle" + formId + "Click";
}

std::string asFormInput(const boost::cmatch& match,
                        const std::string& formId,
                        int* pItemIndex)
{
   *pItemIndex = *pItemIndex + 1;

   boost::format fmt("<li>"
                     "<input type=\"radio\" name=\"%1%\" value=\"%2%\""
                     " onclick=\"%4%(this);\"/>%3%"
                     "</li>");

   std::string input =  boost::str(fmt % (formId + "_input")
                                       % *pItemIndex
                                       % match[1]
                                       % handleClickFunction(formId));

   return input;
}


} // anonymous namespace

void renderQuiz(int slideIndex,
                int correctItemIndex,
                std::string* pHead,
                std::string* pHTML)
{      
   // tweak the radio button size
   pHead->append("<style>.quiz .reveal input { zoom: 2.5; }</style>\n");

   // build form id
   std::string suffix = safe_convert::numberToString<int>(slideIndex);
   std::string formId = "quizForm" + suffix;

   // build validation script
   boost::format fmtScript(
      "<script>\n"
      "function %1%(clickedBtn) {\n"
      "  var answer = parseInt(clickedBtn.value);\n"
      "  var correct = answer == %2%;\n"
      "  document.getElementById('%3%_correct').style.display ="
            " correct ? \"block\" : \"none\";\n"
      "  document.getElementById('%3%_incorrect').style.display ="
            " correct ? \"none\" : \"block\";\n"
      "  if (window.parent.recordPresentationQuizAnswer)\n"
      "    window.parent.recordPresentationQuizAnswer("
            "%4%, answer, correct);\n "
      "}\n"
      "</script>\n\n");
   pHead->append(boost::str(fmtScript
                              % handleClickFunction(formId)
                              % correctItemIndex
                              % formId
                              % slideIndex));

   // correct and incorrect divs
   std::string cssAttribs = "class=\"quizFeedback\" style=\"display:none\"";
   boost::format fmtFeedback(
      "<div id=\"%1%_correct\" %2%>\n"
      "<img src=\"images/correct.png\"/><span>Correct!</span>\n"
      "</div>\n"
      "<div id=\"%1%_incorrect\" %2%>\n"
      "<img src=\"images/incorrect.png\"/><span>Incorrect!</span>\n"
      "</div>\n");
   std::string feedbackHTML = boost::str(fmtFeedback % formId % cssAttribs);

   // enclose in form
   boost::format fmt("<form id=\"%1%\">\n\n<ul>");
   boost::algorithm::replace_first(*pHTML, "<ul>", boost::str(fmt % formId));
   boost::format suffixFmt("</ul>\n\n%1%\n</form>\n");
   boost::algorithm::replace_last(*pHTML, "</ul>", boost::str(suffixFmt %
                                                               feedbackHTML));

   // create input elements
   int itemIndex = 0;
   boost::iostreams::regex_filter filter(boost::regex("<li>(.+?)<\\/li>"),
                                         boost::bind(asFormInput,
                                                      _1, formId, &itemIndex));

   // inputs html
   Error error = regex_utils::filterString(*pHTML, filter, pHTML);
   if (error)
      LOG_ERROR(error);
}

} // namespace presentation
} // namespace modules
} // namesapce session

