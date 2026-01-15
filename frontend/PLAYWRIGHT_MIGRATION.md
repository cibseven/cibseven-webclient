# Cypress to Playwright Migration Guide

This document provides an overview of the Cypress to Playwright migration for E2E tests in the CIB Seven Webclient frontend application.

## Migration Status

✅ **Complete** - All Cypress tests have been successfully converted to Playwright while maintaining the original Cypress test suite.

## Files Added

### Configuration
- `playwright.config.js` - Main Playwright configuration with browser settings, timeouts, and project setup

### Test Files
- `playwright/e2e/example.spec.js` - Converted from `cypress/e2e/example.cy.js`
- `playwright/e2e/variables.spec.js` - Converted from `cypress/e2e/variables.cy.js`

### Helpers
- `playwright/helpers/auth.js` - Authentication helper functions (equivalent to Cypress custom commands)

### Fixtures
- `playwright/fixtures/example.json` - Copied from Cypress fixtures

### Documentation
- `playwright/README.md` - Comprehensive guide for running Playwright tests

### Configuration Updates
- `package.json` - Added `@playwright/test` dependency and npm scripts
- `eslint.config.js` - Added Playwright-specific ESLint rules
- `.gitignore` - Added Playwright artifacts to ignore list

## Key Differences Between Cypress and Playwright

### 1. Selector Strategy

**Cypress (Before):**
```javascript
cy.get(':nth-child(3) > form > :nth-child(1) > .row > .col > .form-control').type(email)
```

**Playwright (After):**
```javascript
await page.locator('form input.form-control[type="text"]').first().fill(email)
```

**Improvements:**
- More robust selectors that are less likely to break with UI changes
- Use of semantic selectors (role, text, attributes)
- Better readability and maintainability

### 2. Async/Await Pattern

**Cypress (Before):**
```javascript
cy.visit('/')
cy.contains('h1', 'CIB seven')
```

**Playwright (After):**
```javascript
await page.goto('/')
await expect(page.getByRole('heading', { name: 'CIB seven', level: 1 })).toBeVisible()
```

**Improvements:**
- Explicit async/await for better control flow
- More predictable test behavior
- Standard JavaScript promises

### 3. Custom Commands → Helper Functions

**Cypress (Before):**
```javascript
Cypress.Commands.add('login', (username, password, displayName) => {
  cy.visit('#/seven/login')
  // ...
})

// Usage
cy.login('demo', 'demo')
```

**Playwright (After):**
```javascript
export async function login(page, username, password, displayName) {
  await page.goto('#/seven/login')
  // ...
}

// Usage
await login(page, 'demo', 'demo')
```

**Improvements:**
- Standard JavaScript modules (ES6 imports/exports)
- Better IDE support and type checking
- Explicit page context passing

### 4. Element Interactions

**Cypress (Before):**
```javascript
cy.get('.overflow-y-scroll > .table > tbody')
  .contains('tr', variableName)
  .within(() => {
    cy.get('.mdi-delete-outline').click()
  })
```

**Playwright (After):**
```javascript
const variableRow = tableBody.locator('tr').filter({ hasText: variableName })
await variableRow.locator('.mdi-delete-outline').click()
```

**Improvements:**
- Scoped locators for better test isolation
- Filter API for precise element selection
- Cleaner chaining syntax

### 5. Assertions

**Cypress (Before):**
```javascript
cy.get('.table > tbody').should('not.contain', variableName)
```

**Playwright (After):**
```javascript
await expect(tableBody).not.toContainText(variableName)
```

**Improvements:**
- Standard Jest-like expect syntax
- Better error messages
- Auto-waiting for assertions

## Environment Variables

Both frameworks support the same environment variables:

- `ENV` - Set to 'stage' for stage environment (defaults to local)
- `username` - Stage environment username
- `password` - Stage environment password

**Usage:**
```bash
# Cypress
ENV=stage username=user password=pass npm run test:e2e:dev

# Playwright
ENV=stage username=user password=pass npm run test:e2e:playwright
```

## Running Tests

### Cypress (Original)
```bash
npm run test:e2e              # Run tests in headless mode
npm run test:e2e:dev          # Open Cypress UI
```

### Playwright (New)
```bash
npm run test:e2e:playwright        # Run tests in headless mode
npm run test:e2e:playwright:ui     # Open Playwright UI
npm run test:e2e:playwright:debug  # Run in debug mode
```

## Test Coverage Comparison

All test scenarios from Cypress have been migrated:

| Test | Cypress | Playwright | Status |
|------|---------|------------|--------|
| Visit app root URL | ✅ | ✅ | Migrated |
| Login (demo user) | ✅ | ✅ | Migrated |
| Login (mary user) | ✅ | ✅ | Migrated |
| Login (john user) | ✅ | ✅ | Migrated |
| Failed login (wrong password) | ✅ | ✅ | Migrated |
| Failed login (non-existing user) | ✅ | ✅ | Migrated |
| Add variable to process instance | ✅ | ✅ | Migrated |
| Remove variable from process instance | ✅ | ✅ | Migrated |

## Error Handling

### Cypress (Before)
Explicit error suppression in `cypress/support/e2e.js`:
```javascript
Cypress.on('uncaught:exception', (err) => {
  if (err.message.includes('ResizeObserver loop')) {
    return false
  }
  // ...
})
```

### Playwright (After)
Playwright's auto-waiting and retry logic handles most timing issues automatically. No explicit error handling needed for common issues like:
- ResizeObserver warnings
- Timing race conditions
- Element visibility

## Advantages of Playwright

1. **Multi-browser support**: Test against Chromium, Firefox, and WebKit
2. **Auto-waiting**: Built-in smart waiting for elements
3. **Better parallelization**: Run tests in parallel by default
4. **Network interception**: More powerful API mocking
5. **TypeScript support**: First-class TypeScript support
6. **Modern API**: Clean, promise-based API
7. **Better debugging**: Powerful debug tools and trace viewer

## Coexistence Strategy

Both test suites currently coexist in the repository:

```
frontend/
├── cypress/              # Original Cypress tests (preserved)
│   ├── e2e/
│   ├── support/
│   └── fixtures/
├── playwright/           # New Playwright tests
│   ├── e2e/
│   ├── helpers/
│   └── fixtures/
├── cypress.config.js     # Cypress configuration
└── playwright.config.js  # Playwright configuration
```

This allows for:
- Gradual migration and validation
- Side-by-side comparison
- Rollback option if needed
- Team training and adaptation period

## Future Deprecation Path

Once the Playwright tests are fully validated and the team is comfortable:

1. ✅ Phase 1: Add Playwright tests (Current)
2. ⏳ Phase 2: Run both test suites in parallel (2-4 weeks)
3. ⏳ Phase 3: Gradually deprecate Cypress tests
4. ⏳ Phase 4: Remove Cypress dependencies and configuration

## Maintenance Guidelines

### For New Tests

**Prefer Playwright** for all new E2E tests:
- Use helper functions from `playwright/helpers/`
- Follow the patterns established in existing Playwright tests
- Use robust selectors (role, text, data-testid)

### For Existing Tests

**Keep both in sync** during the transition period:
- If updating a test scenario, update both Cypress and Playwright versions
- Document any differences in behavior

## Known Issues & Workarounds

### 1. Modal Dialog Refresh
Both frameworks have the same workaround for the modal dialog issue:
```javascript
// TODO: dialog is still shown, so refresh the page
await page.reload()
```

This should be fixed in the application code rather than the tests.

### 2. Stage Environment Sorting
Both frameworks include stage-specific behavior for sorting:
```javascript
if (process.env.ENV === 'stage') {
  // Sort logic
}
```

## Resources

### Playwright Documentation
- [Official Docs](https://playwright.dev)
- [API Reference](https://playwright.dev/docs/api/class-playwright)
- [Best Practices](https://playwright.dev/docs/best-practices)

### Migration Guides
- [Cypress to Playwright Migration](https://playwright.dev/docs/test-runners#cypress)
- [Selector Best Practices](https://playwright.dev/docs/selectors)

## Questions & Support

For questions about the migration:
1. Check `playwright/README.md` for usage instructions
2. Review this migration guide
3. Compare equivalent Cypress and Playwright test files
4. Consult the Playwright documentation

## Contributors

This migration maintains the same test coverage while improving:
- Selector robustness
- Code maintainability
- Framework capabilities
- Developer experience
