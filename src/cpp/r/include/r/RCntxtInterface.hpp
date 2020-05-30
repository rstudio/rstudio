/*
 * RCntxtInterface.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef R_CONTEXT_INTERFACE_HPP
#define R_CONTEXT_INTERFACE_HPP

#include "RSexp.hpp"

namespace rstudio {
namespace r {
namespace context {

// forward declare
class RCntxt;

// RCntxtInterface represents the subset of members of the RCNTXT struct which
// are accessed elsewhere; it may be safely extended with other members.
class RCntxtInterface
{
public:
   // accessors for RCNTXT entries
   virtual SEXP callfun() const       = 0;
   virtual int callflag() const       = 0;
   virtual SEXP call() const          = 0;
   virtual SEXP srcref() const        = 0;
   virtual SEXP cloenv() const        = 0;

   // computed properties
   virtual bool isNull() const        = 0;
   virtual RCntxt nextcontext() const = 0;
   
   virtual ~RCntxtInterface() {}
};

} // namespace context
} // namespace r
} // namespace rstudio

#endif
