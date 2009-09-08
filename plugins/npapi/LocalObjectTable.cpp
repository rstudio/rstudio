#include "mozincludes.h"
#include "LocalObjectTable.h"

LocalObjectTable::~LocalObjectTable() {
  if (!dontFree) {
    freeAll();
  }
}

void* LocalObjectTable::getIdentityFrom(NPObject* obj) {
  void** rawPtr = reinterpret_cast<void**>(reinterpret_cast<char*>(obj) + sizeof(NPClass*)
      + sizeof(uint32_t));
  Debug::log(Debug::Info) << "getIdentity(obj=" << (void*)obj << "): class=" << (void*)obj->_class
      << ", bytes:";
  for (int i = 0; i< 4; ++i) {
    Debug::log(Debug::Info) << " " << rawPtr[i];
  }
  Debug::log(Debug::Info) << Debug::flush;
  return obj;
}

