#include "mozincludes.h"
#include "LocalObjectTable.h"

// Mirrors of NPObjectWrapper and NPObjectProxy from Chrome
// TODO(jat): this is very fragile and may break if Chrome changes
struct ChromeNPObjectProxy {
  // IPC::Channel::Listener and IPC::Message::Sender are pure interfaces, so we don't need to
  // account for any data fields from their inheritance
  void* channel_; // scoped_refptr keeps only a single pointer
  void* unknown; // looks like another pointer before route id
  int route_id_;
  intptr_t npobject_ptr_;
};

struct ChromeNPObjectWrapper {
  NPObject object;
  ChromeNPObjectProxy* proxy;
};

NPClass* LocalObjectTable::wrappedObjectClass = 0;

LocalObjectTable::~LocalObjectTable() {
  if (!dontFree) {
    freeAll();
  }
}

void* LocalObjectTable::getIdentityFrom(NPObject* obj) {
  void* id = obj;
  if (obj->_class == wrappedObjectClass) {
    ChromeNPObjectWrapper* wrapper = reinterpret_cast<ChromeNPObjectWrapper*>(obj);
    ChromeNPObjectProxy* proxy = wrapper->proxy;
    id = reinterpret_cast<void*>(proxy->npobject_ptr_);
    Debug::log(Debug::Info) << "Mapped obj=" << (void*)obj << " to " << id << Debug::flush;
  }
  return id;
}
