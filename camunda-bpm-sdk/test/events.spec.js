// Adapted from legacy camunda-bpm-sdk-js/test/client/eventsSpec.js for ES modules and Vitest
import { describe, it, expect, beforeEach } from 'vitest';
import Events from '../events.js';

const isArray = Array.isArray;

describe('The events system', () => {
  let obj;
  let counters;

  function onEventCB() { counters.on++; }
  function onceEventCB() { counters.once++; }
  function otherEventCB() { counters.other++; }

  beforeEach(() => {
    obj = {};
    counters = { on: 0, once: 0, other: 0 };
    Events.attach(obj);
  });

  it('does not blow when loading', () => {
    expect(typeof Events.attach).toBe('function');
  });

  describe('`attach` function', () => {
    it('is used to provide events to an object', () => {
      expect(typeof obj.on).toBe('function');
      expect(typeof obj.once).toBe('function');
      expect(typeof obj.off).toBe('function');
      expect(typeof obj.trigger).toBe('function');
      expect(typeof obj._events).toBe('object');
    });
  });

  describe('`on` function', () => {
    it('is a function', () => {
      expect(typeof Events.on).toBe('function');
      expect(typeof obj.on).toBe('function');
    });

    it('adds an event', () => {
      expect(obj).not.toBeUndefined();
      expect(typeof obj._events).toBe('object');
      expect(() => obj.on('some:event:name', onEventCB)).not.toThrow();
      expect(isArray(obj._events['some:event:name'])).toBe(true);
      expect(obj._events['some:event:name'].indexOf(onEventCB)).toBeGreaterThan(-1);
    });
  });

  describe('`trigger` function', () => {
    it('is a function', () => {
      expect(typeof Events.trigger).toBe('function');
    });

    it('calls the functions assigned to the event', () => {
      obj.on('some:event:name', onEventCB);
      expect(() => {
        obj.trigger('some:event:name');
        obj.trigger('some:event:name');
      }).not.toThrow();
      expect(counters.on).toBe(2);
    });
  });

  describe('`once` function', () => {
    it('is a function', () => {
      expect(typeof Events.once).toBe('function');
    });

    it('adds a function', () => {
      expect(() => obj.once('other:event:name', onceEventCB)).not.toThrow();
    });

    it('calls the added function', () => {
      obj.once('other:event:name', onceEventCB);
      expect(() => {
        obj.trigger('other:event:name');
      }).not.toThrow();
    });

    it('removes the function after it has been called', () => {
      obj.once('other:event:name', onceEventCB);
      obj.trigger('other:event:name');
      obj.trigger('other:event:name');
      expect(counters.once).toBe(1);
    });
  });

  describe('`off` function', () => {
    it('is a function', () => {
      expect(typeof Events.off).toBe('function');
    });

    it('removes a function assigned to an event', () => {
      obj.on('some:event:name', onEventCB);
      obj.on('some:event:name', otherEventCB);
      expect(isArray(obj._events['some:event:name'])).toBe(true);
      expect(obj._events['some:event:name'].length).toBe(2);
      expect(() => obj.off('some:event:name', otherEventCB)).not.toThrow();
      expect(isArray(obj._events['some:event:name'])).toBe(true);
      expect(obj._events['some:event:name'].length).toBe(1);
    });

    it('removes all the functions assigned to an event', () => {
      obj.on('some:event:name', onEventCB);
      obj.on('some:event:name', otherEventCB);
      expect(() => obj.off('some:event:name')).not.toThrow();
      expect(obj._events['some:event:name']).toBeUndefined();
    });
  });
});
