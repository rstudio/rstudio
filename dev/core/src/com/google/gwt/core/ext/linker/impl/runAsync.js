__MODULE_FUNC__.__startLoadingFragment = function(fragmentFile) {
  return computeUrlForResource(fragmentFile);
};

__MODULE_FUNC__.__installRunAsyncCode = function(code) {
  var docbody = getInstallLocation();
  var script = getInstallLocationDoc().createElement('script');
  script.language='javascript';
  script.text = code;
  docbody.appendChild(script);

  // Unless we're in pretty mode, remove the tags to shrink the DOM a little.
  // It should have installed its code immediately after being added.
  __START_OBFUSCATED_ONLY__
  docbody.removeChild(script);
  __END_OBFUSCATED_ONLY__
}
