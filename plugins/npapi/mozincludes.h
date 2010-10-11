#ifndef _H_mozincludes
#define _H_mozincludes

// Defines private prototypes for copy constructor and assigment operator. Do
// not implement these methods.
#define DISALLOW_EVIL_CONSTRUCTORS(CLASS) \
 private:                                 \
  CLASS(const CLASS&);                    \
  CLASS& operator=(const CLASS&)

#include "npapi/npapi.h"
#include "npapi/nphostapi.h"
#include "npapi/npruntime.h"

void SetNPNFuncs(NPNetscapeFuncs* npnFuncs);
const NPNetscapeFuncs& GetNPNFuncs();

#include "NPObjectWrapper.h"

inline const NPUTF8 *GetNPStringUTF8Characters(const NPString &npstr) {
  return npstr.UTF8Characters;
}

inline uint32 GetNPStringUTF8Length(const NPString &npstr) {
  return npstr.UTF8Length;
}

// Convenience wrappers to make an NPVariant from various string types.
#define STDSTRING_TO_NPVARIANT(str, var) \
  STRINGN_TO_NPVARIANT(str.data(), static_cast<uint32_t>(str.length()), var)

#define NPSTRING_TO_NPVARIANT(npstr, var) \
  STRINGN_TO_NPVARIANT(GetNPStringUTF8Characters(npstr), \
                       GetNPStringUTF8Length(npstr), var)

#ifdef linux
#define OSCALL /**/
#define WINAPI /**/
#define DLLEXP /**/
#define NPINIT_ARG(argname) , NPPluginFuncs* argname
#define NPINIT_GETS_ENTRYPOINTS
#define NP_SHUTDOWN_RETURN_TYPE NPError
#define NP_SHUTDOWN_RETURN(val) (val)
#endif

#ifdef _WINDOWS
#define DLLEXP __declspec(dllexport)
#define NPINIT_ARG(argname) /**/
#define NP_SHUTDOWN_RETURN_TYPE NPError
#define NP_SHUTDOWN_RETURN(val) (val)
#endif

#ifdef __mac
#define OSCALL /**/
#define WINAPI /**/
#define DLLEXP /**/
#define NPINIT_ARG(argname) /**/
#define NP_SHUTDOWN_RETURN_TYPE void
#define NP_SHUTDOWN_RETURN(val) /**/
typedef void (* NP_LOADDS NPP_ShutdownUPP)(void); // from npupp.h
#endif

#endif
