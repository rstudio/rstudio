/*
 * SessionPanmirror.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include "SessionPanmirror.hpp"

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <core/Log.hpp>
#include <core/system/Process.hpp>
#include <core/json/JsonRpc.hpp>

#include "SessionPanmirrorPandoc.hpp"
#include "SessionPanmirrorBibliography.hpp"
#include "SessionPanmirrorCrossref.hpp"
#include "SessionPanmirrorDataCite.hpp"
#include "SessionPanmirrorPubMed.hpp"
#include "SessionPanmirrorDOI.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {

Error initialize()
{
   core::ExecBlock initBlock;
   initBlock.addFunctions()
      (pandoc::initialize)
      (bibliography::initialize)
      (crossref::initialize)
      (datacite::initialize)
      (pubmed::initialize)
      (doi::initialize)
    ;
   return initBlock.execute();
}

} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
