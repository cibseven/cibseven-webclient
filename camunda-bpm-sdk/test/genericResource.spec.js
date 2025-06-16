// Adapted from legacy camunda-bpm-sdk-js/test/client/genericResourceSpec.js for ES modules and Vitest
import { describe, it, expect } from 'vitest';
import AbstractClientResource from '../api-client/abstract-client-resource.js';

describe('The AbstractClientResource', () => {
  let Extended1, Extended2, instance1, instance2;

  it('does not blow when loading', () => {
    expect(typeof AbstractClientResource.extend).toBe('function');
  });

  it('can be extend', () => {
    expect(() => {
      Extended1 = AbstractClientResource.extend({
        instanceMethod: function() {},
        instanceProperty: true
      }, {
        staticMethod: function() {},
        staticProperty: true
      });

      Extended2 = AbstractClientResource.extend({
        otherInstanceMethod: function() {},
        otherInstanceProperty: true
      }, {
        otherStaticMethod: function() {},
        otherStaticProperty: true
      });
    }).not.toThrow();
  });

  describe('generated resource class', () => {
    it('has a `static` properties', () => {
      expect(typeof Extended1.staticMethod).toBe('function');
      expect(typeof Extended1.staticProperty).toBe('boolean');
      expect(typeof Extended1.path).not.toBe('undefined');

      expect(typeof Extended2.staticMethod).toBe('undefined');
      expect(typeof Extended2.staticProperty).toBe('undefined');
      expect(typeof Extended2.path).not.toBe('undefined');
      expect(typeof Extended2.otherStaticMethod).toBe('function');
      expect(typeof Extended2.otherStaticProperty).toBe('boolean');
    });

    it('instanciates', () => {
      expect(() => {
        instance1 = new Extended1();
        instance2 = new Extended2();
      }).not.toThrow();
    });

    it('has a `instance` properties', () => {
      expect(typeof instance1.instanceMethod).toBe('function');
      expect(typeof instance1.instanceProperty).toBe('boolean');
      expect(typeof instance2.instanceMethod).toBe('undefined');
      expect(typeof instance2.instanceProperty).toBe('undefined');
      expect(typeof instance2.otherInstanceMethod).toBe('function');
      expect(typeof instance2.otherInstanceProperty).toBe('boolean');
    });
  });
});
