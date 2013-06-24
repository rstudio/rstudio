/*
 * GwtSymbolMaps.cpp
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

#include <core/gwt/GwtSymbolMaps.hpp>

#include <algorithm>

#include <boost/foreach.hpp>

namespace core {
namespace gwt {

namespace {


} // anonymous namespace

std::vector<StackElement> SymbolMaps::resymbolize(
                                  const std::vector<StackElement>& stack,
                                  const std::string& strongName)
{




   std::vector<StackElement> resymbolizedStack;
   BOOST_FOREACH(const StackElement& se, stack)
   {
      resymbolizedStack.push_back(resymbolize(se, strongName));
   }
   return resymbolizedStack;
}

StackElement SymbolMaps::resymbolize(const StackElement& se,
                                     const std::string& strongName)
{
   return se;
}


} // namespace gwt
} // namespace core


