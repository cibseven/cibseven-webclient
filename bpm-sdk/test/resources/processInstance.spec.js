// Adapted from legacy processInstanceSpec.js for ES modules and Vitest
import { describe, it, expect } from 'vitest';
import ProcessInstance from '../../api-client/resources/process-instance.js';

describe('The ProcessInstance resource', () => {
  it('does not blow when loading', () => {
    expect(() => {
      void ProcessInstance;
    }).not.toThrow();
  });
  it('has a `path` static property', () => {
    expect(ProcessInstance.path).toBe('process-instance');
  });
  describe('instance', () => {
    it('does not blow when instanciating', () => {
      expect(() => {
        new ProcessInstance();
      }).not.toThrow();
    });
  });
});
