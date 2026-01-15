# Playwright E2E Tests

This directory contains Playwright end-to-end tests for the CIB Seven Webclient frontend application.

## Directory Structure

```
playwright/
├── e2e/                    # Test files
│   ├── example.spec.js     # Basic tests (visit, login, failed login)
│   └── variables.spec.js   # Process instance variable tests
├── helpers/                # Helper functions
│   └── auth.js            # Authentication helper functions
└── fixtures/              # Test data fixtures
    └── example.json       # Example fixture data
```

## Running Tests

### Prerequisites

1. Install dependencies:
   ```bash
   npm install
   ```

2. Install Playwright browsers (first time only):
   ```bash
   npx playwright install
   ```

### Test Commands

Run all tests (builds app, starts server, and runs tests):
```bash
npm run test:e2e:playwright
```

Run tests with UI mode (interactive testing):
```bash
npm run test:e2e:playwright:ui
```

Run tests in debug mode:
```bash
npm run test:e2e:playwright:debug
```

Run tests directly (server must be running):
```bash
npx playwright test
```

Run specific test file:
```bash
npx playwright test playwright/e2e/example.spec.js
```

Run tests in headed mode (see browser):
```bash
npx playwright test --headed
```

### View Test Report

After running tests, view the HTML report:
```bash
npx playwright show-report
```

## Environment Configuration

### Local Environment (Default)

By default, tests run in local mode with demo credentials:
- Username: `demo`
- Password: `demo`

### Stage Environment

To run tests against the stage environment, set environment variables:

```bash
ENV=stage username=your_username password=your_password npm run test:e2e:playwright
```

Or export them:
```bash
export ENV=stage
export username=your_username
export password=your_password
npm run test:e2e:playwright
```

## Helper Functions

The `playwright/helpers/auth.js` file provides reusable authentication functions:

- `loginDefault(page)` - Login with environment-aware credentials
- `login(page, username, password, displayName)` - Login with specific credentials
- `logout(page)` - Logout from the application
- `loginFail(page, email, password)` - Test failed login attempt

### Example Usage

```javascript
import { test } from '@playwright/test'
import { login, logout } from '../helpers/auth.js'

test('my test', async ({ page }) => {
  await login(page, 'demo', 'demo')
  // ... perform test actions ...
  await logout(page)
})
```

## Test Coverage

### example.spec.js
- ✅ Visit app root URL and verify title
- ✅ Successful login for multiple users (demo, mary, john)
- ✅ Environment-based login (stage vs local)
- ✅ Failed login scenarios with wrong credentials

### variables.spec.js
- ✅ Add new variable to process instance
- ✅ Remove variable from process instance
- ✅ UI interactions with tables, modals, and forms
- ✅ Environment-specific behavior (stage filtering)

## Differences from Cypress Implementation

### Selector Improvements
- Replaced fragile `:nth-child()` selectors with more robust Playwright selectors
- Used `filter()` and `locator()` chaining for better element targeting
- Leveraged Playwright's auto-waiting capabilities

### Async/Await
- All test functions are properly async
- Proper use of await for all page interactions

### Error Handling
Playwright automatically handles many common errors that required explicit handling in Cypress:
- ResizeObserver loop warnings
- Timing issues with auto-waiting
- Element visibility checks

## Configuration

The Playwright configuration is defined in `playwright.config.js`:
- Base URL: `http://localhost:5173`
- Test directory: `./playwright/e2e`
- Browsers: Chromium, Firefox, WebKit
- Retries: 2 on CI, 0 locally
- Screenshots: On failure only
- Trace: On first retry

## Coexistence with Cypress

Both Playwright and Cypress test suites coexist in this repository:
- **Cypress tests**: `cypress/e2e/*.cy.js`
- **Playwright tests**: `playwright/e2e/*.spec.js`

This allows for gradual migration and comparison between the two frameworks.

## Best Practices

1. **Use Playwright's auto-waiting**: Avoid explicit waits when possible
2. **Write robust selectors**: Prefer role-based and text-based selectors over CSS
3. **Keep tests independent**: Each test should be able to run in isolation
4. **Use helper functions**: Reuse authentication and common actions via helpers
5. **Handle environment differences**: Use environment variables for stage vs local

## Troubleshooting

### Tests fail with "Browser not found"
Run: `npx playwright install`

### Server not starting
Ensure port 5173 is available and not in use

### Stage environment tests fail
Verify your stage credentials are correct:
```bash
echo $username
echo $ENV
```

### Element not found errors
Check if the application UI has changed and update selectors accordingly

## Future Improvements

- [ ] Add more comprehensive process instance tests
- [ ] Implement Page Object Model for better maintainability
- [ ] Add visual regression testing
- [ ] Add API testing alongside E2E tests
- [ ] Configure CI/CD pipeline for automated test runs
