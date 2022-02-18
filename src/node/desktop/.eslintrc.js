module.exports = {
  root: true,

  env: {
    es2020: true,
    node: true,
  },

  parser: '@typescript-eslint/parser',

  parserOptions: {
    project: './tsconfig.json',
    ecmaVersion: 11,
    sourceType: 'module',
  },

  plugins: ['@typescript-eslint', 'prettier'],

  extends: ['eslint:recommended', 'plugin:@typescript-eslint/recommended'],

  rules: {
    'no-empty-function': 1, // will only show a warning
    '@typescript-eslint/no-empty-function': 1,

    'indent': ['error', 2, { SwitchCase: 1 }],
    'quotes': ['error', 'single'],
    'semi': ['error', 'always'],
    'max-len': [
      'error',
      {
        code: 120,
        tabWidth: 2,
        ignoreUrls: true,
      },
    ],
    'prettier/prettier': 'warn',

    '@typescript-eslint/await-thenable': ['error'],
    '@typescript-eslint/no-base-to-string': ['error'],
    '@typescript-eslint/no-confusing-non-null-assertion': ['error'],
    '@typescript-eslint/no-floating-promises': ['warn'],
    '@typescript-eslint/no-invalid-void-type': ['error'],
    '@typescript-eslint/no-misused-promises': ['error'],
    '@typescript-eslint/no-throw-literal': ['error'],
    '@typescript-eslint/no-unnecessary-condition': ['error'],
    '@typescript-eslint/promise-function-async': ['warn'],
    '@typescript-eslint/require-array-sort-compare': ['error'],
    '@typescript-eslint/return-await': ['warn'],
    '@typescript-eslint/no-implicit-any-catch': ['error'],
    '@typescript-eslint/no-unused-vars': ['warn', {"argsIgnorePattern": "^_"}],

    '@typescript-eslint/strict-boolean-expressions': [
      'error',
      {
        allowString: true,
        allowNumber: true,
        allowNullableObject: true,
        allowNullableBoolean: true,
        allowNullableString: true,
        allowNullableNumber: true,
      },
    ],
  },
};
