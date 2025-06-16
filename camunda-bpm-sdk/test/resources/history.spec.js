// Adapted from legacy historySpec.js for ES modules and Vitest
import { describe, it, expect } from 'vitest';
import History from '../../api-client/resources/history.js';

describe('The History resource usage', () => {
  it('does not blow when loading', () => {
    expect(() => {
      // Import already done at top, just check usage
      void History;
    }).not.toThrow();
  });

  it('has a `path` static property', () => {
    expect(History.path).toBe('history');
  });

  it('has a `userOperation` method', () => {
    expect(typeof History.userOperation).toBe('function');
  });

  it('has a `processInstance` method', () => {
    expect(typeof History.processInstance).toBe('function');
  });

  it('has a `processInstanceCount` method', () => {
    expect(typeof History.processInstanceCount).toBe('function');
  });

  it('has a `decisionInstance` method', () => {
    expect(typeof History.decisionInstance).toBe('function');
  });

  it('has a `decisionInstanceCount` method', () => {
    expect(typeof History.decisionInstanceCount).toBe('function');
  });

  it('has a `batch` method', () => {
    expect(typeof History.batch).toBe('function');
  });

  it('has a `singleBatch` method', () => {
    expect(typeof History.singleBatch).toBe('function');
  });
});
