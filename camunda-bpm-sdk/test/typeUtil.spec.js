// Adapted from legacy camunda-bpm-sdk-js/test/client/typeUtilSpec.js for ES modules and Vitest
import { describe, it, expect } from 'vitest';
import typeUtil from '../forms/type-util.js';
const { convertToType, isType, dateToString } = typeUtil;

describe('The type-util', () => {
  it('does convert Integer', () => {
    expect(convertToType('100', 'Integer')).toBe(100);
    expect(convertToType('-100', 'Integer')).toBe(-100);
    expect(() => convertToType('100.10', 'Integer')).toThrow("Value '100.10' is not of type Integer");
  });

  it('does convert Long', () => {
    expect(convertToType('100', 'Long')).toBe(100);
    expect(convertToType('-100', 'Long')).toBe(-100);
    expect(convertToType(' 0 ', 'Long')).toBe(0);
    expect(() => convertToType('100.10', 'Long')).toThrow("Value '100.10' is not of type Long");
  });

  it('does convert Short', () => {
    expect(convertToType('100', 'Short')).toBe(100);
    expect(convertToType('-100', 'Short')).toBe(-100);
    expect(convertToType(' 0 ', 'Short')).toBe(0);
    expect(() => convertToType('100.10', 'Short')).toThrow("Value '100.10' is not of type Short");
  });

  it('does convert Float', () => {
    expect(convertToType('100', 'Float')).toBe(100);
    expect(convertToType('-100', 'Float')).toBe(-100);
    expect(convertToType('100.10', 'Float')).toBeCloseTo(100.10);
    expect(convertToType('-100.10', 'Float')).toBeCloseTo(-100.10);
    expect(() => convertToType('100.10a', 'Float')).toThrow("Value '100.10a' is not of type Float");
  });

  it('does convert Double', () => {
    expect(convertToType('100', 'Double')).toBe(100);
    expect(convertToType('-100', 'Double')).toBe(-100);
    expect(convertToType('100.10', 'Double')).toBeCloseTo(100.10);
    expect(convertToType('-100.10', 'Double')).toBeCloseTo(-100.10);
    expect(() => convertToType('100.10a', 'Double')).toThrow("Value '100.10a' is not of type Double");
  });

  it('does convert Date', () => {
    const date = new Date('2016-05-09T08:56:00');
    expect(typeof convertToType(date, 'Date')).toBe('string');
    expect(convertToType('2013-01-23T13:42:42', 'Date')).toBe('2013-01-23T13:42:42');
    expect(convertToType(' 2013-01-23T13:42:42 ', 'Date')).toBe('2013-01-23T13:42:42');
    expect(() => convertToType('2013-01-23T13:42', 'Date')).toThrow("Value '2013-01-23T13:42' is not of type Date");
    expect(() => convertToType('2013-01-23T60:42:40', 'Date')).toThrow("Value '2013-01-23T60:42:40' is not of type Date");
  });

  it('does convert Boolean', () => {
    expect(convertToType('true', 'Boolean')).toBe(true);
    expect(convertToType(' true', 'Boolean')).toBe(true);
    expect(convertToType(' true ', 'Boolean')).toBe(true);
    expect(convertToType('false', 'Boolean')).toBe(false);
    expect(convertToType(' false', 'Boolean')).toBe(false);
    expect(convertToType(' false ', 'Boolean')).toBe(false);
    expect(convertToType('false ', 'Boolean')).toBe(false);
    expect(() => convertToType('strue', 'Boolean')).toThrow("Value 'strue' is not of type Boolean");
  });

  it('detects Integers', () => {
    expect(isType('100', 'Integer')).toBe(true);
    expect(isType('-100', 'Integer')).toBe(true);
    expect(isType('100-', 'Integer')).toBe(false);
  });

  it('detects Floats', () => {
    expect(isType('100', 'Float')).toBe(true);
    expect(isType('-100', 'Float')).toBe(true);
    expect(isType('-100e10', 'Float')).toBe(true);
    expect(isType('-100.01', 'Float')).toBe(true);
    expect(isType('100-', 'Float')).toBe(false);
  });

  it('detects Booleans', () => {
    expect(isType('true', 'Boolean')).toBe(true);
    expect(isType('false', 'Boolean')).toBe(true);
    expect(isType('wahr', 'Boolean')).toBe(false);
    expect(isType('1', 'Boolean')).toBe(false);
    expect(isType('0', 'Boolean')).toBe(false);
    expect(isType('', 'Boolean')).toBe(false);
  });

  it('detects Dates', () => {
    const date = new Date('2016-05-09T08:56:00');
    expect(isType(date, 'Date')).toBe(true);
    expect(isType('2013-01-23T13:42:42', 'Date')).toBe(true);
    expect(isType('2013-01-23T27:42:42', 'Date')).toBe(false);
    expect(isType('2013-13-23T13:42:42', 'Date')).toBe(false);
    expect(isType('tomorrow', 'Date')).toBe(false);
    expect(isType('2013-01-23D27:42:42', 'Date')).toBe(false);
  });
});
