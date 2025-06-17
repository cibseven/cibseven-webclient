import { describe, it, expect, beforeEach } from 'vitest';
import CamSDK from '../index-browser.js';

// Example: adapt one basic test to validate setup

describe('Camunda BPM SDK Core', () => {
  it('should expose a Client constructor', () => {
    expect(typeof CamSDK.Client).toBe('function');
  });

  it('should expose a Form constructor', () => {
    expect(typeof CamSDK.Form).toBe('function');
  });

  // Add more migrated/adapted tests from coreSpec.js here
});
