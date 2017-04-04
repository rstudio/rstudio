/*
 * SessionObjectExplorer.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#define R_INTERNAL_FUNCTIONS

#include "SessionObjectExplorer.hpp"

#include <boost/bind.hpp>

#include <core/Algorithm.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {      
namespace explorer {

namespace {

SEXP rs_objectClass(SEXP objectSEXP)
{
   SEXP attribSEXP = ATTRIB(objectSEXP);
   if (attribSEXP == R_NilValue)
      return R_NilValue;
   
   while (attribSEXP != R_NilValue)
   {
      SEXP tagSEXP = TAG(attribSEXP);
      if (TYPEOF(tagSEXP) == SYMSXP)
      {
         const char* tag = CHAR(PRINTNAME(tagSEXP));
         if (::strcmp(tag, "class") == 0)
            return CAR(attribSEXP);
      }
      
      attribSEXP = CDR(attribSEXP);
   }
   
   return R_NilValue;
}

SEXP rs_objectAddress(SEXP objectSEXP)
{
   std::stringstream ss;
   ss << std::hex << (void*) objectSEXP;
   
   r::sexp::Protect protect;
   return r::sexp::create(ss.str(), &protect);
}

SEXP rs_objectAttributes(SEXP objectSEXP)
{
   return ATTRIB(objectSEXP);
}

} // end anonymous namespace

core::Error initialize()
{
   using namespace module_context;
   using boost::bind;
   
   RS_REGISTER_CALL_METHOD(rs_objectAddress, 1);
   RS_REGISTER_CALL_METHOD(rs_objectClass, 1);
   RS_REGISTER_CALL_METHOD(rs_objectAttributes, 1);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionObjectExplorer.R"));
   
   return initBlock.execute();
}
   
} // namespace explorer
} // namespace modules
} // namespace session
} // namespace rstudio
