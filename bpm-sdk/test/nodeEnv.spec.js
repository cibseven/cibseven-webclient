// Adapted from legacy camunda-bpm-sdk-js/test/client/nodeEnvSpec.js for ES modules and Vitest
import { describe, it, expect } from 'vitest';
import CamundaClient from '../index-browser.js';

describe('The node.js usage', () => {
  it('does not blow when loading', () => {
    expect(typeof CamundaClient).toBe('object');
  });

  it('has to be configured', () => {
    expect(() => {
      // The SDK expects config; calling as a function should fail
      // (simulate legacy expectation)
      CamundaClient();
    }).toThrow();
  });

  describe('configuration', () => {
    it('needs a apiUri property', () => {
      expect(() => {
        // The SDK expects config with apiUri
        new CamundaClient.Client({});
      }).toThrow();
    });
  });
});
