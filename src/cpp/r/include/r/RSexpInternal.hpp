/*
 * RSexpInternal.hpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef R_R_SEXP_INTERNAL_HPP
#define R_R_SEXP_INTERNAL_HPP

#define R_NO_REMAP
#include <Rinternals.h>

// These structure definitions mirror the internal R structures from Rinternals.h.
// The sxpinfo_struct bit fields moved in R 3.5 in order to support ALTREP objects,
// so these definitions are only accurate for R >= 3.5.

extern "C" {

struct sxpinfo_struct
{
   unsigned int type  :  5;
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
   unsigned int named : 16;
   unsigned int extra : 32 - 16;
};

struct primsxp_struct
{
    int offset;
};

struct symsxp_struct
{
    struct SEXPREC *pname;
    struct SEXPREC *value;
    struct SEXPREC *internal;
};

struct listsxp_struct
{
    struct SEXPREC *carval;
    struct SEXPREC *cdrval;
    struct SEXPREC *tagval;
};

struct envsxp_struct
{
    struct SEXPREC *frame;
    struct SEXPREC *enclos;
    struct SEXPREC *hashtab;
};

struct closxp_struct
{
    struct SEXPREC *formals;
    struct SEXPREC *body;
    struct SEXPREC *env;
};

struct promsxp_struct
{
    struct SEXPREC *value;
    struct SEXPREC *expr;
    struct SEXPREC *env;
};

typedef struct SEXPREC
{
   struct sxpinfo_struct sxpinfo;
   struct SEXPREC* attrib;
   struct SEXPREC* gengc_next_node;
   struct SEXPREC* gengc_prev_node;
   union
   {
      struct primsxp_struct primsxp;
      struct symsxp_struct symsxp;
      struct listsxp_struct listsxp;
      struct envsxp_struct envsxp;
      struct closxp_struct closxp;
      struct promsxp_struct promsxp;
   } u;
} SEXPREC;

} // extern "C"

#endif // R_R_SEXP_INTERNAL_HPP
