/*
 * DO NOT EDIT.  THIS FILE IS GENERATED FROM IOOPHM.idl
 */

#ifndef __gen_IOOPHM_h__
#define __gen_IOOPHM_h__


#ifndef __gen_nsISupports_h__
#include "nsISupports.h"
#endif

/* For IDL files that don't want to include root IDL files. */
#ifndef NS_NO_VTABLE
#define NS_NO_VTABLE
#endif
class nsIDOMWindow; /* forward declaration */


/* starting interface:    IOOPHM */
#define IOOPHM_IID_STR "90cef17b-c3fe-4251-af68-4381b3d938a0"

#define IOOPHM_IID \
  {0x90cef17b, 0xc3fe, 0x4251, \
    { 0xaf, 0x68, 0x43, 0x81, 0xb3, 0xd9, 0x38, 0xa0 }}

class NS_NO_VTABLE NS_SCRIPTABLE IOOPHM : public nsISupports {
 public: 

  NS_DECLARE_STATIC_IID_ACCESSOR(IOOPHM_IID)

  /* boolean init (in nsIDOMWindow window); */
  NS_SCRIPTABLE NS_IMETHOD Init(nsIDOMWindow *window, bool *_retval NS_OUTPARAM) = 0;

  /* boolean connect (in ACString url, in ACString sessionKey, in ACString addr, in ACString moduleName, in ACString hostedHtmlVersion); */
  NS_SCRIPTABLE NS_IMETHOD Connect(const nsACString & url, const nsACString & sessionKey, const nsACString & addr, const nsACString & moduleName, const nsACString & hostedHtmlVersion, bool *_retval NS_OUTPARAM) = 0;

};

  NS_DEFINE_STATIC_IID_ACCESSOR(IOOPHM, IOOPHM_IID)

/* Use this macro when declaring classes that implement this interface. */
#define NS_DECL_IOOPHM \
  NS_SCRIPTABLE NS_IMETHOD Init(nsIDOMWindow *window, bool *_retval NS_OUTPARAM); \
  NS_SCRIPTABLE NS_IMETHOD Connect(const nsACString & url, const nsACString & sessionKey, const nsACString & addr, const nsACString & moduleName, const nsACString & hostedHtmlVersion, bool *_retval NS_OUTPARAM); 

/* Use this macro to declare functions that forward the behavior of this interface to another object. */
#define NS_FORWARD_IOOPHM(_to) \
  NS_SCRIPTABLE NS_IMETHOD Init(nsIDOMWindow *window, bool *_retval NS_OUTPARAM) { return _to Init(window, _retval); } \
  NS_SCRIPTABLE NS_IMETHOD Connect(const nsACString & url, const nsACString & sessionKey, const nsACString & addr, const nsACString & moduleName, const nsACString & hostedHtmlVersion, bool *_retval NS_OUTPARAM) { return _to Connect(url, sessionKey, addr, moduleName, hostedHtmlVersion, _retval); } 

/* Use this macro to declare functions that forward the behavior of this interface to another object in a safe way. */
#define NS_FORWARD_SAFE_IOOPHM(_to) \
  NS_SCRIPTABLE NS_IMETHOD Init(nsIDOMWindow *window, bool *_retval NS_OUTPARAM) { return !_to ? NS_ERROR_NULL_POINTER : _to->Init(window, _retval); } \
  NS_SCRIPTABLE NS_IMETHOD Connect(const nsACString & url, const nsACString & sessionKey, const nsACString & addr, const nsACString & moduleName, const nsACString & hostedHtmlVersion, bool *_retval NS_OUTPARAM) { return !_to ? NS_ERROR_NULL_POINTER : _to->Connect(url, sessionKey, addr, moduleName, hostedHtmlVersion, _retval); } 

#if 0
/* Use the code below as a template for the implementation class for this interface. */

/* Header file */
class _MYCLASS_ : public IOOPHM
{
public:
  NS_DECL_ISUPPORTS
  NS_DECL_IOOPHM

  _MYCLASS_();

private:
  ~_MYCLASS_();

protected:
  /* additional members */
};

/* Implementation file */
NS_IMPL_ISUPPORTS1(_MYCLASS_, IOOPHM)

_MYCLASS_::_MYCLASS_()
{
  /* member initializers and constructor code */
}

_MYCLASS_::~_MYCLASS_()
{
  /* destructor code */
}

/* boolean init (in nsIDOMWindow window); */
NS_IMETHODIMP _MYCLASS_::Init(nsIDOMWindow *window, bool *_retval NS_OUTPARAM)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* boolean connect (in ACString url, in ACString sessionKey, in ACString addr, in ACString moduleName, in ACString hostedHtmlVersion); */
NS_IMETHODIMP _MYCLASS_::Connect(const nsACString & url, const nsACString & sessionKey, const nsACString & addr, const nsACString & moduleName, const nsACString & hostedHtmlVersion, bool *_retval NS_OUTPARAM)
{
    return NS_ERROR_NOT_IMPLEMENTED;
}

/* End of implementation class template. */
#endif


#endif /* __gen_IOOPHM_h__ */
