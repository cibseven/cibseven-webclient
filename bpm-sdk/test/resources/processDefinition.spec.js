// Adapted from legacy processDefinitionSpec.js for ES modules and Vitest
import { describe, it, expect } from 'vitest';
import ProcessDefinition from '../../api-client/resources/process-definition.js';

describe('The ProcessDefinition resource', () => {
  it('does not blow when loading', () => {
    expect(() => {
      void ProcessDefinition;
    }).not.toThrow();
  });
  it('has a `path` static property', () => {
    expect(ProcessDefinition.path).toBe('process-definition');
  });
});
