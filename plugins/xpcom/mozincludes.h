#ifndef _H_mozincludes
#define _H_mozincludes

// Defines private prototypes for copy constructor and assigment operator. Do
// not implement these methods.
#define DISALLOW_EVIL_CONSTRUCTORS(CLASS) \
 private:                                 \
  CLASS(const CLASS&);                    \
  CLASS& operator=(const CLASS&)

#include "xpcom-config.h"
#include "mozilla-config.h"

#endif
