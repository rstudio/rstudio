/*
 * SlideNavigationList.hpp
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

#ifndef SESSION_PRESENTATION_SLIDE_NAVIGATION_LIST_HPP
#define SESSION_PRESENTATION_SLIDE_NAVIGATION_LIST_HPP

#include <string>

#include <boost/utility.hpp>

#include <core/json/Json.hpp>

#include "SlideParser.hpp"

namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {

class SlideNavigationList : public boost::noncopyable
{
public:
   SlideNavigationList(const std::string& type);
   void add(const Slide& slide);
   void complete();

   std::string asCall() const;

   core::json::Object asJson() const;

private:
   void addSlide(const std::string& title, int indent, int index, int line);
   core::json::Array slides_;
   bool allowNavigation_;
   bool allowSlideNavigation_;
   int index_;
   bool inSubSection_;
   bool hasSections_;
};


} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_PRESENTATION_SLIDE_NAVIGATION_LIST_HPP
