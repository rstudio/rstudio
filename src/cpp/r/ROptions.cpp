/*
 * ROptions.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#define R_INTERNAL_FUNCTIONS
#include <r/ROptions.hpp>

#include <boost/format.hpp>

#include <core/Log.hpp>
#include <core/FilePath.hpp>

#include <r/RExec.hpp>

using namespace core ;

namespace r {
namespace options {
      
Error saveOptions(const FilePath& filePath)
{
   return exec::RFunction(".rs.saveOptions", filePath.absolutePath()).call();
}
   
Error restoreOptions(const FilePath& filePath)
{
   return exec::RFunction(".rs.restoreOptions", filePath.absolutePath()).call();
}
   
const int kDefaultWidth = 80;   
   
void setOptionWidth(int width)
{
   boost::format fmt("options(width=%1%)");
   Error error = r::exec::executeString(boost::str(fmt % width));
   if (error)
      LOG_ERROR(error);
}
   
int getOptionWidth()
{
   return getOption<int>("width", kDefaultWidth);
}

SEXP getOption(const std::string& name)
{
   return Rf_GetOption(Rf_install(name.c_str()), R_BaseEnv);
}


namespace {

static SEXP FindTaggedItem(SEXP lst, SEXP tag)
{
    for ( ; lst!=R_NilValue ; lst=CDR(lst)) {
   if (TAG(lst) == tag)
       return lst;
    }
    return R_NilValue;
}

}

// copy of static SetOption method from options.c. we need the
// lower level version of this so we can set an option without
// invoking r::exec (which explicilty sets the error option to
// prevent error handlers from running)
/* Change the value of an option or add a new option or, */
/* if called with value R_NilValue, remove that option. */

SEXP setOption(SEXP tag, SEXP value)
{
    SEXP opt, old, t;
    t = opt = SYMVALUE(Rf_install(".Options"));
    if (!Rf_isList(opt))
      Rf_error("corrupted options list");
    opt = FindTaggedItem(opt, tag);

    /* The option is being removed. */
    if (value == R_NilValue) {
   for ( ; t != R_NilValue ; t = CDR(t))
       if (TAG(CDR(t)) == tag) {
      old = CAR(t);
      SETCDR(t, CDDR(t));
      return old;
       }
   return R_NilValue;
    }
    /* If the option is new, a new slot */
    /* is added to the end of .Options */
    if (opt == R_NilValue) {
   while (CDR(t) != R_NilValue)
       t = CDR(t);
   PROTECT(value);
   SETCDR(t, Rf_allocList(1));
   UNPROTECT(1);
   opt = CDR(t);
   SET_TAG(opt, tag);
    }
    old = CAR(opt);
    SETCAR(opt, value);
    return old;
}
    
} // namespace options   
} // namespace r



