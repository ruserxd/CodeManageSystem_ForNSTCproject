import globals from 'globals';
import pluginJs from '@eslint/js';
import pluginReact from 'eslint-plugin-react';

export default [
	{ files: ['src/**/*.{js,mjs,cjs,jsx}'] },
	{ languageOptions: { globals: globals.browser } },
	pluginJs.configs.recommended,
	pluginReact.configs.flat.recommended,
	{
		settings: {
			react: {
				version: 'detect'
			}
		}
	},
	{
		rules: {
			'react/react-in-jsx-scope': 'off',
			// 檢查 hook 的問題
			'react-hooks/rules-of-hooks': 'error', // Checks rulse of Hooks
			'react-hooks/exhaustive-deps': 'warn' // Checks effect dependencies
		}
	}
];
