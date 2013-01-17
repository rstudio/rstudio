/*
 * SlideParser.cpp
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


#include "SlideParser.hpp"

#include <boost/foreach.hpp>
#include <boost/algorithm/string.hpp>

#include <core/Error.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace learning {

namespace {

struct CompareName
{
  CompareName(const std::string& name) : name_(name) {}
  bool operator()(const Slide::Field& field) const {
    return boost::iequals(name_, field.first);
  }
  private:
    std::string name_;
};

} // anonymous namespace


std::string Slide::fieldValue(const std::string& name) const
{
   std::vector<Field>::const_iterator it =
        std::find_if(fields_.begin(), fields_.end(), CompareName(name));
   if (it != fields_.end())
      return it->second;
   else
      return std::string();
}


std::vector<std::string> Slide::fieldValues(const std::string& name) const
{
   std::vector<std::string> values;
   BOOST_FOREACH(const Field& field, fields_)
   {
      if (boost::iequals(name, field.first))
         values.push_back(field.second);
   }
   return values;
}



} // namespace learning
} // namespace modules
} // namesapce session

