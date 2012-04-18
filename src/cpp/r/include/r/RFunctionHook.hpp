/*
 * RFunctionHook.hpp
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

#ifndef R_FUNCTION_HOOK_HPP
#define R_FUNCTION_HOOK_HPP

#include <string>

typedef struct SEXPREC *SEXP;

// R-INTERNAL-IMPORT: from Defn.h
typedef SEXP (*CCODE)(SEXP, SEXP, SEXP, SEXP);

namespace core {
   class Error;
}

// IMPORTANT NOTE: all code running within a function hook must provide the
// "no jump" guarantee. See comment in RInternal.hpp for more info on this

namespace r {
namespace function_hook {
   
core::Error registerReplaceHook(const std::string& name,
                                CCODE hook,
                                CCODE* pOriginal);
   
// convenience utility method to allow hooks to checkArity
void checkArity(SEXP op, SEXP args, SEXP call);
   
core::Error registerUnsupported(const std::string& name, 
                                const std::string& package);

core::Error registerUnsupportedInternal(const std::string& name);
 
} // namespace function_hook   
} // namespace r


#endif // R_FUNCTION_HOOK_HPP 

