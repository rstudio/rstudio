import { defineConfig, globalIgnores } from "eslint/config";
import typescriptEslint from "@typescript-eslint/eslint-plugin";
import globals from "globals";
import tsParser from "@typescript-eslint/parser";
import path from "node:path";
import { fileURLToPath } from "node:url";
import js from "@eslint/js";
import { FlatCompat } from "@eslint/eslintrc";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const compat = new FlatCompat({
    baseDirectory: __dirname,
    recommendedConfig: js.configs.recommended,
    allConfig: js.configs.all
});

export default defineConfig([globalIgnores([
    "**/.eslintrc.js",
    "**/forge.config.js",
    "**/scripts/",
    "**/webpack.main.config.js",
    "**/webpack.plugins.js",
    "**/webpack.renderer.config.js",
    "**/webpack.rules.js",
    "**/.prettierrc",
    "**/.lintstagedrc",
]), {
    extends: compat.extends("eslint:recommended", "plugin:@typescript-eslint/recommended"),

    plugins: {
        "@typescript-eslint": typescriptEslint,
    },

    languageOptions: {
        globals: {
            ...globals.node,
        },

        parser: tsParser,
        ecmaVersion: 11,
        sourceType: "module",

        parserOptions: {
            project: "./tsconfig.json",
        },
    },

    rules: {
        "no-empty-function": 1,
        "@typescript-eslint/no-empty-function": 1,

        indent: ["error", 2, {
            SwitchCase: 1,
        }],

        quotes: ["error", "single", {
            avoidEscape: true,
        }],

        semi: ["error", "always"],

        "max-len": ["error", {
            code: 120,
            tabWidth: 2,
            ignoreUrls: true,
            ignoreStrings: true,
            ignoreTemplateLiterals: true,
        }],

        "@typescript-eslint/await-thenable": ["error"],
        "@typescript-eslint/no-base-to-string": ["error"],
        "@typescript-eslint/no-confusing-non-null-assertion": ["error"],
        "@typescript-eslint/no-floating-promises": ["warn"],
        "@typescript-eslint/no-invalid-void-type": ["error"],

        "@typescript-eslint/no-misused-promises": ["error", {
            checksVoidReturn: false,
        }],

        "@/no-throw-literal": ["error"],
        "@typescript-eslint/no-unnecessary-condition": ["error"],
        "@typescript-eslint/promise-function-async": ["warn"],
        "@typescript-eslint/require-array-sort-compare": ["error"],
        "@typescript-eslint/return-await": ["warn"],

        "@typescript-eslint/no-unused-vars": ["warn", {
            varsIgnorePattern: "^_",
            argsIgnorePattern: "^_",
            caughtErrorsIgnorePattern: '^_',
        }],

        "@typescript-eslint/strict-boolean-expressions": ["error", {
            allowString: true,
            allowNumber: true,
            allowNullableObject: true,
            allowNullableBoolean: true,
            allowNullableString: true,
            allowNullableNumber: true,
        }],
    },
}]);