export const TIMEOUTS = {
  processCleanup: 1000,
  rstudioStartup: 10000,
  consoleReady: 15000,
  settleDelay: 1000,
  fileOpen: 20000,
  ghostText: 30000,
  nesApply: 30000,
  displayOutput: 2000,
};

export async function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export const RSTUDIO_EXTRA_ARGS: string[] = process.env.RSTUDIO_EXTRA_ARGS
  ? process.env.RSTUDIO_EXTRA_ARGS.split(' ').filter(Boolean)
  : [];

export const CODE_SUGGESTION_PROVIDERS: Record<string, string> = {
  'copilot': 'GitHub Copilot',
  'posit-assistant': 'Posit AI',
};

export const CHAT_PROVIDERS: Record<string, string> = {
  'posit-assistant': 'Posit Assistant',
};
