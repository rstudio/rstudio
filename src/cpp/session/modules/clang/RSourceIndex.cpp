/*
 * RSourceIndex.cpp
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

#include "RSourceIndex.hpp"

#include <boost/bind.hpp>

#include <session/SessionUserSettings.hpp>

#include <core/libclang/LibClang.hpp>

#include "RCompilationDatabase.hpp"

using namespace rscore ;
using namespace rscore::libclang;

namespace session {
namespace modules { 
namespace clang {

namespace {

class RSourceIndex : public SourceIndex
{
public:
   RSourceIndex()
      : SourceIndex(rCompilationDatabase(), userSettings().clangVerbose())
   {
   }
};

} // anonymous namespace

SourceIndex& rSourceIndex()
{
   static RSourceIndex instance;
   return instance;
}

} // namespace clang
} // namespace modules
} // namesapce session

