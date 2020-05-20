/*
 * RSxpInfo.hpp
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

#ifndef R_SXPINFO_HPP
#define R_SXPINFO_HPP

#define TYPE_BITS 5
#define NAMED_BITS 16

namespace rstudio {
namespace r {

// This structure definition mirrors sxpinfo_struct from Rinternals.h in R 3.5. The bit fields moved
// in R 3.5 in order to support ALTREP objects, so this structure definition is only accurate in R >
// 3.5. 
struct sxpinfo {
    unsigned int type  :  TYPE_BITS;
    unsigned int scalar:  1;
    unsigned int obj   :  1;
    unsigned int alt   :  1;
    unsigned int gp    : 16;
    unsigned int mark  :  1;
    unsigned int debug :  1;
    unsigned int trace :  1;
    unsigned int spare :  1;
    unsigned int gcgen :  1;
    unsigned int gccls :  3;
    unsigned int named : NAMED_BITS;
    unsigned int extra : 32 - NAMED_BITS;
};

} // namespace r
} // namespace rstudio

#endif
