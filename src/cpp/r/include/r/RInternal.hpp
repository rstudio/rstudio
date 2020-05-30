/*
 * RInternal.hpp
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

#ifndef R_R_INTERNAL_HPP
#define R_R_INTERNAL_HPP

// IMPORTANT NOTE: If R internal functions (functions prefixed with R_ or Rf_,
// as distinct from simple SEXP accessor functions) are called from within a 
// c++ execution context they must either:
//
//  (a) be inspected to make sure they won't ever call error (longjump); or
//
//  (b) be called from within a safe block (see r::exec::executeSafely) 
//
// This is critical to prevent error (longjump) from being called. If it is
// called then execution will pop back to the top of the R stack (repl loop)
// and c++ stack unwinding will not occur!
//
// To prevent inadvertent use of R_ and Rf_ internal functions by clients 
// they are only included if the R_INTERNAL_FUNCTIONS macro is defined.
//
// Note also that public functions and classes exposed from within the r
// namespace have an implicit guarantee not to allow an error/longjump to
// escape from them (they adhere to guideline a or b above). Therefore,
// if client code from outside the r namespace feels the need to call R
// internal functions a sounder approach might be to add a suitable function 
// to the r namespace which performs the intended operation safely. 
//

#define R_NO_REMAP
#include <Rinternals.h>

// Hide macros that are always unsafe for us to use, because their
// interface has changed between versions of R
#undef BODY_EXPR
#define BODY_EXPR UNSAFE_R_FUNCTION

#undef PREXPR
#define PREXPR    UNSAFE_R_FUNCTION

#ifndef R_INTERNAL_FUNCTIONS

// force compiler error if the client tries to call an R internal function
#define Rf_asChar INTERNAL_R_FUNCTION
#define Rf_coerceVector INTERNAL_R_FUNCTION
#define Rf_PairToVectorList INTERNAL_R_FUNCTION
#define Rf_VectorToPairList INTERNAL_R_FUNCTION
#define Rf_asLogical INTERNAL_R_FUNCTION
#define Rf_asInteger INTERNAL_R_FUNCTION
#define Rf_asReal INTERNAL_R_FUNCTION
#define Rf_asComplex INTERNAL_R_FUNCTION
#define Rf_acopy_string INTERNAL_R_FUNCTION
#define Rf_alloc3DArray INTERNAL_R_FUNCTION
#define Rf_allocArray INTERNAL_R_FUNCTION
#define Rf_allocMatrix INTERNAL_R_FUNCTION
#define Rf_allocList INTERNAL_R_FUNCTION
#define Rf_allocS4Object INTERNAL_R_FUNCTION
#define Rf_allocSExp INTERNAL_R_FUNCTION
#define Rf_applyClosure INTERNAL_R_FUNCTION
#define Rf_arraySubscript INTERNAL_R_FUNCTION
#define Rf_asComplex INTERNAL_R_FUNCTION
#define Rf_asInteger INTERNAL_R_FUNCTION
#define Rf_asLogical INTERNAL_R_FUNCTION
#define Rf_asReal INTERNAL_R_FUNCTION
#define Rf_classgets INTERNAL_R_FUNCTION
#define Rf_cons INTERNAL_R_FUNCTION
#define Rf_copyMatrix INTERNAL_R_FUNCTION
#define Rf_copyMostAttrib INTERNAL_R_FUNCTION
#define Rf_copyVector INTERNAL_R_FUNCTION
#define Rf_CreateTag INTERNAL_R_FUNCTION
#define Rf_defineVar INTERNAL_R_FUNCTION
#define Rf_dimgets INTERNAL_R_FUNCTION
#define Rf_dimnamesgets INTERNAL_R_FUNCTION
#define Rf_DropDims INTERNAL_R_FUNCTION
#define Rf_duplicate INTERNAL_R_FUNCTION
#define Rf_duplicated INTERNAL_R_FUNCTION
#define Rf_eval INTERNAL_R_FUNCTION
#define Rf_findFun INTERNAL_R_FUNCTION
#define Rf_findVar INTERNAL_R_FUNCTION
#define Rf_findVarInFrame INTERNAL_R_FUNCTION
#define Rf_findVarInFrame3 INTERNAL_R_FUNCTION
#define Rf_getAttrib INTERNAL_R_FUNCTION
#define Rf_GetArrayDimnames INTERNAL_R_FUNCTION
#define Rf_GetColNames INTERNAL_R_FUNCTION
#define Rf_GetMatrixDimnames INTERNAL_R_FUNCTION
#define Rf_GetOption INTERNAL_R_FUNCTION
#define Rf_GetOptionDigits INTERNAL_R_FUNCTION
#define Rf_GetOptionWidth INTERNAL_R_FUNCTION
#define Rf_GetRowNames INTERNAL_R_FUNCTION
#define Rf_gsetVar INTERNAL_R_FUNCTION
#define Rf_install INTERNAL_R_FUNCTION
#define Rf_isFree INTERNAL_R_FUNCTION
#define Rf_isOrdered INTERNAL_R_FUNCTION
#define Rf_isUnordered INTERNAL_R_FUNCTION
#define Rf_isUnsorted INTERNAL_R_FUNCTION
#define Rf_lengthgets INTERNAL_R_FUNCTION
#define R_lsInternal INTERNAL_R_FUNCTION
#define Rf_match INTERNAL_R_FUNCTION
#define Rf_namesgets INTERNAL_R_FUNCTION
#define Rf_mkCharLen INTERNAL_R_FUNCTION
#define Rf_NonNullStringMatch INTERNAL_R_FUNCTION
#define Rf_ncols INTERNAL_R_FUNCTION
#define Rf_nrows INTERNAL_R_FUNCTION
#define Rf_nthcdr INTERNAL_R_FUNCTION
#define Rf_pmatch INTERNAL_R_FUNCTION
#define Rf_psmatch INTERNAL_R_FUNCTION
#define Rf_PrintValue INTERNAL_R_FUNCTION
#define Rf_protect INTERNAL_R_FUNCTION
#define Rf_setSVector INTERNAL_R_FUNCTION
#define Rf_setVar INTERNAL_R_FUNCTION
#define Rf_str2type INTERNAL_R_FUNCTION
#define Rf_StringBlank INTERNAL_R_FUNCTION
#define Rf_substitute INTERNAL_R_FUNCTION
#define Rf_translateChar INTERNAL_R_FUNCTION
#define Rf_translateCharUTF8 INTERNAL_R_FUNCTION
#define Rf_type2char INTERNAL_R_FUNCTION
#define Rf_type2str INTERNAL_R_FUNCTION
#define Rf_unprotect INTERNAL_R_FUNCTION
#define Rf_unprotect_ptr INTERNAL_R_FUNCTION
#define R_ProtectWithIndex INTERNAL_R_FUNCTION
#define R_Reprotect INTERNAL_R_FUNCTION
#define R_tryEval INTERNAL_R_FUNCTION
#define Rf_asS4 INTERNAL_R_FUNCTION
#define Rf_getCharCE INTERNAL_R_FUNCTION
#define Rf_mkCharCE INTERNAL_R_FUNCTION
#define Rf_mkCharLenCE INTERNAL_R_FUNCTION
#define Rf_reEnc INTERNAL_R_FUNCTION
#define R_MakeExternalPtr INTERNAL_R_FUNCTION
#define R_ExternalPtrAddr INTERNAL_R_FUNCTION
#define R_ExternalPtrTag INTERNAL_R_FUNCTION
#define R_ExternalPtrProtected INTERNAL_R_FUNCTION
#define R_ClearExternalPtr INTERNAL_R_FUNCTION
#define R_SetExternalPtrAddr INTERNAL_R_FUNCTION
#define R_SetExternalPtrTag INTERNAL_R_FUNCTION
#define R_SetExternalPtrProtected INTERNAL_R_FUNCTION
#define R_RegisterFinalizer INTERNAL_R_FUNCTION
#define R_RegisterCFinalizer INTERNAL_R_FUNCTION
#define R_RegisterFinalizerEx INTERNAL_R_FUNCTION
#define R_RegisterCFinalizerEx INTERNAL_R_FUNCTION
#define R_MakeWeakRef INTERNAL_R_FUNCTION
#define R_MakeWeakRefC INTERNAL_R_FUNCTION
#define R_WeakRefKey INTERNAL_R_FUNCTION
#define R_WeakRefValue INTERNAL_R_FUNCTION
#define R_RunWeakRefFinalizer INTERNAL_R_FUNCTION
#define R_PromiseExpr INTERNAL_R_FUNCTION
#define R_ClosureExpr INTERNAL_R_FUNCTION
#define R_initialize_bcode INTERNAL_R_FUNCTION
#define R_bcEncode INTERNAL_R_FUNCTION
#define R_bcDecode INTERNAL_R_FUNCTION
#define R_ToplevelExec INTERNAL_R_FUNCTION
#define R_RestoreHashCount INTERNAL_R_FUNCTION
#define R_IsPackageEnv INTERNAL_R_FUNCTION
#define R_PackageEnvName INTERNAL_R_FUNCTION
#define R_FindPackageEnv INTERNAL_R_FUNCTION
#define R_IsNamespaceEnv INTERNAL_R_FUNCTION
#define R_NamespaceEnvSpec INTERNAL_R_FUNCTION
#define R_FindNamespace INTERNAL_R_FUNCTION
#define R_LockEnvironment INTERNAL_R_FUNCTION
#define R_EnvironmentIsLocked INTERNAL_R_FUNCTION
#define R_LockBinding INTERNAL_R_FUNCTION
#define R_unLockBinding INTERNAL_R_FUNCTION
#define R_MakeActiveBinding INTERNAL_R_FUNCTION
#define R_BindingIsLocked INTERNAL_R_FUNCTION
#define R_BindingIsActive INTERNAL_R_FUNCTION
#define R_HasFancyBindings INTERNAL_R_FUNCTION
#define Rf_errorcall INTERNAL_R_FUNCTION
#define Rf_warningcall INTERNAL_R_FUNCTION
#define Rf_warningcall_immediate INTERNAL_R_FUNCTION
#define R_XDREncodeDouble INTERNAL_R_FUNCTION
#define R_XDRDecodeDouble INTERNAL_R_FUNCTION
#define R_XDREncodeInteger INTERNAL_R_FUNCTION
#define R_XDRDecodeInteger INTERNAL_R_FUNCTION
#define R_InitInPStream INTERNAL_R_FUNCTION
#define R_InitOutPStream INTERNAL_R_FUNCTION
#define R_InitFileInPStream INTERNAL_R_FUNCTION
#define R_InitFileOutPStream INTERNAL_R_FUNCTION
#define R_InitConnOutPStream INTERNAL_R_FUNCTION
#define R_InitConnInPStream INTERNAL_R_FUNCTION
#define R_Serialize INTERNAL_R_FUNCTION
#define R_Unserialize INTERNAL_R_FUNCTION
#define R_do_slot INTERNAL_R_FUNCTION
#define R_do_slot_assign INTERNAL_R_FUNCTION
#define R_has_slot INTERNAL_R_FUNCTION
#define R_do_MAKE_CLASS INTERNAL_R_FUNCTION
#define R_getClassDef INTERNAL_R_FUNCTION
#define R_do_new_object INTERNAL_R_FUNCTION
#define R_PreserveObject INTERNAL_R_FUNCTION
#define R_ReleaseObject INTERNAL_R_FUNCTION
#define R_dot_Last INTERNAL_R_FUNCTION
#define R_RunExitFinalizers INTERNAL_R_FUNCTION
#define R_popen INTERNAL_R_FUNCTION
#define R_system INTERNAL_R_FUNCTION
#define Rf_conformable INTERNAL_R_FUNCTION
#define Rf_elt INTERNAL_R_FUNCTION
#define Rf_inherits INTERNAL_R_FUNCTION
#define Rf_isArray INTERNAL_R_FUNCTION
#define Rf_isFactor INTERNAL_R_FUNCTION
#define Rf_isFrame INTERNAL_R_FUNCTION
#define Rf_isFunction INTERNAL_R_FUNCTION
#define Rf_isInteger INTERNAL_R_FUNCTION
#define Rf_isLanguage INTERNAL_R_FUNCTION
#define Rf_isList INTERNAL_R_FUNCTION
#define Rf_isMatrix INTERNAL_R_FUNCTION
#define Rf_isNewList INTERNAL_R_FUNCTION
#define Rf_isNumeric INTERNAL_R_FUNCTION
#define Rf_isPairList INTERNAL_R_FUNCTION
#define Rf_isPrimitive INTERNAL_R_FUNCTION
#define Rf_isTs INTERNAL_R_FUNCTION
#define Rf_isUserBinop INTERNAL_R_FUNCTION
#define Rf_isValidString INTERNAL_R_FUNCTION
#define Rf_isValidStringF INTERNAL_R_FUNCTION
#define Rf_isVector INTERNAL_R_FUNCTION
#define Rf_isVectorAtomic INTERNAL_R_FUNCTION
#define Rf_isVectorList INTERNAL_R_FUNCTION
#define Rf_isVectorizable INTERNAL_R_FUNCTION
#define Rf_lang1 INTERNAL_R_FUNCTION
#define Rf_lang2 INTERNAL_R_FUNCTION
#define Rf_lang3 INTERNAL_R_FUNCTION
#define Rf_lang4 INTERNAL_R_FUNCTION
#define Rf_lastElt INTERNAL_R_FUNCTION
#define Rf_lcons INTERNAL_R_FUNCTION
#define Rf_length INTERNAL_R_FUNCTION
#define Rf_list1 INTERNAL_R_FUNCTION
#define Rf_list2 INTERNAL_R_FUNCTION
#define Rf_list3 INTERNAL_R_FUNCTION
#define Rf_list4 INTERNAL_R_FUNCTION
#define Rf_listAppend INTERNAL_R_FUNCTION
#define Rf_mkString INTERNAL_R_FUNCTION
#define Rf_nlevels INTERNAL_R_FUNCTION
#define Rf_ScalarComplex INTERNAL_R_FUNCTION
#define Rf_ScalarInteger INTERNAL_R_FUNCTION
#define Rf_ScalarLogical INTERNAL_R_FUNCTION
#define Rf_ScalarRaw INTERNAL_R_FUNCTION
#define Rf_ScalarReal INTERNAL_R_FUNCTION
#define Rf_ScalarString INTERNAL_R_FUNCTION

#endif // R_INTERNAL_FUNCTIONS

#endif // R_R_INTERNAL_HPP 

