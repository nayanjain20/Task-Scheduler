# Scheduler UI

Task cards refresh automatically every five seconds and after state actions.

- **Execution count** counts completed and failed execution records only, not
  pending, skipped, or discarded records (nor individual retry attempts).
- **Last finished run - scheduled time (IST)** is the latest scheduled timestamp
  among completed and failed records. The API does not record actual start/finish
  timestamps. All history timestamps use `Asia/Kolkata` regardless of browser timezone.
- Execution history is ordered newest to oldest by scheduled timestamp.
- Pause is available for active tasks; resume for paused tasks; cancel for active
  or paused tasks. Cancellation requires confirmation and cannot be resumed.
  These actions do not interrupt work already running.
- Failed requests are shown in the UI. An unavailable execution summary is not
  displayed as zero. Use Refresh to retry immediately.

The UI uses the existing API at `http://localhost:8080`. Run `npm run dev` to
start the frontend. Run `npm run build` and `npm run lint` to validate changes.
Run `npm test` with Node 22.6+ to check run counts, IST formatting, and API
success/failure handling using Node's built-in test runner.

## Creating tasks

The Create Task panel supports PRINT, WRITE, and DELETE with a one-time or
recurring schedule. Name and file path (for WRITE/DELETE) cannot be blank.
Recurring intervals must be positive whole seconds within the backend's integer
range. One-time tasks send interval `0`.

PRINT logs the task name, so no custom-message field is shown for it. WRITE
accepts an optional message; file paths refer to the backend machine. DELETE
shows a warning about its effect. Hidden payload fields are not submitted.

The form prevents duplicate submissions while waiting, preserves input on
failure, and resets only after a successful response containing a task ID.
Successful creation refreshes the task list. A null task ID is treated as a
failure even if the response says "Complete".

## React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```
