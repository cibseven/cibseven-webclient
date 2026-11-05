/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import copyToClipboardMixin from '@/mixins/copyToClipboardMixin.js'

const { copyValueToClipboard } = copyToClipboardMixin.methods

function context() {
  return { $refs: { messageCopy: { show: vi.fn() } } }
}

/**
 * Stubs execCommand (jsdom does not implement it) and records the temporary textarea
 * as it is at the moment of copying, since it is removed from the DOM right after.
 */
function stubExecCommand(result = true) {
  const snapshot = {}
  document.execCommand = vi.fn(() => {
    const textarea = document.querySelector('textarea')
    Object.assign(snapshot, {
      parent: textarea?.parentElement,
      value: textarea?.value,
      readonly: textarea?.hasAttribute('readonly'),
      position: textarea?.style.position
    })
    return result
  })
  return snapshot
}

function addButton(parent) {
  const button = document.createElement('button')
  parent.appendChild(button)
  button.focus()
  return button
}

describe('copyToClipboardMixin', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  afterEach(() => {
    delete document.execCommand
  })

  it('copies a string value and shows the success message', () => {
    const snapshot = stubExecCommand()
    const vm = context()

    copyValueToClipboard.call(vm, 'hello')

    expect(document.execCommand).toHaveBeenCalledWith('copy')
    expect(snapshot.value).toBe('hello')
    expect(snapshot.parent).toBe(document.body)
    expect(snapshot.readonly).toBe(true)
    expect(snapshot.position).toBe('fixed')
    expect(document.querySelector('textarea')).toBeNull()
    expect(vm.$refs.messageCopy.show).toHaveBeenCalledTimes(1)
  })

  it('serialises an object value as JSON', () => {
    const snapshot = stubExecCommand()

    copyValueToClipboard.call(context(), { a: 1 })

    expect(snapshot.value).toBe('{"a":1}')
  })

  it.each([[''], [null], [undefined]])('does nothing for the empty value %j', (val) => {
    stubExecCommand()
    const vm = context()

    copyValueToClipboard.call(vm, val)

    expect(document.execCommand).not.toHaveBeenCalled()
    expect(vm.$refs.messageCopy.show).not.toHaveBeenCalled()
  })

  // Regression: a textarea appended to <body> lies outside an open modal, whose focus trap
  // pulls focus back into the modal, so nothing got copied while the success alert still showed.
  it('places the textarea inside the open modal and restores focus afterwards', () => {
    const modal = document.createElement('div')
    modal.className = 'modal fade show'
    document.body.appendChild(modal)
    const button = addButton(modal)
    const snapshot = stubExecCommand()

    copyValueToClipboard.call(context(), 'in modal')

    expect(snapshot.parent).toBe(modal)
    expect(modal.querySelector('textarea')).toBeNull()
    expect(document.activeElement).toBe(button)
  })

  it('restores focus to the clicked element outside a modal', () => {
    const button = addButton(document.body)
    stubExecCommand()

    copyValueToClipboard.call(context(), 'value')

    expect(document.activeElement).toBe(button)
  })

  // The success alert used to be shown unconditionally, hiding failed copies from the user.
  it('does not show the success message when the browser refuses to copy', () => {
    stubExecCommand(false)
    const vm = context()

    copyValueToClipboard.call(vm, 'value')

    expect(vm.$refs.messageCopy.show).not.toHaveBeenCalled()
    expect(document.querySelector('textarea')).toBeNull()
  })
})
