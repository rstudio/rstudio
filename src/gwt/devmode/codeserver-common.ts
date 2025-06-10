import { platform } from "process";

export const ENDPOINT = platform === 'win32'
  ? "\\\\.\\pipe\\gwt-codeserver-daemon"
  : "/tmp/gwt-codeserver-daemon";
