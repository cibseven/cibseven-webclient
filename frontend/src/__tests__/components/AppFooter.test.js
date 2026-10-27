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
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import AppFooter from '@/components/common-components/AppFooter.vue'
import { InfoService } from '@/services.js'

vi.mock('@/services.js', () => ({ InfoService: { getVersion: vi.fn() } }))

beforeEach(() => { vi.clearAllMocks() })

describe('AppFooter', () => {
  // Test 1: Kommt bei Erfolgsfall das richtige Ergebnis?
  // ein einzelner Test
  it('shows the version returned by the backend', async () => {
    // So tun, als würde getVersion() den Wert '1.2.3' zurückgeben (Rückgabewert ist ein Promise)
    InfoService.getVersion.mockResolvedValue('1.2.3')
    // vm (view model) dient als das "this" aus AppFooter
    const vm = AppFooter.data()
    // created() ausführen und abwarten
    await AppFooter.created.call(vm)
    // Auf das Promise warten
    await flushPromises()
    // Prüfen, ob version jetzt auch wirklich '1.2.3' ist
    expect(vm.version).toBe('1.2.3')
  })

  // Test 2: Funktioniert das Fallback auf 'n/a' bei Fehlern?
  // mehrere Tests mit dem gleichen Schema. Vorlage ist eine Tabelle
  // Test A: Es kommt vom Backend kein Wert zurück
  // Test B: Das Backend antwortet nicht
  it.each([[undefined], [new Error('boom')]])('falls back to n/a (%s)', async (result) => {
    if (result instanceof Error)
      // getVersion() ersetzen, sodass der Ausgabewert abgelehnt wird
      InfoService.getVersion.mockRejectedValue(result)
    else
      // getVersion() ersetzen, sodass undefined zurückgegeben wird
      InfoService.getVersion.mockResolvedValue(result)
    // Den Fehleroutput in der Konsole unterdrücken (überwachen, aber durch nix ersetzen)
    vi.spyOn(console, 'error').mockImplementation(() => {})
    // Wie oben: "this" erzeugen
    const vm = AppFooter.data()
    // created() ausführen
    await AppFooter.created.call(vm)
    // Promises abarbeiten
    await flushPromises()
    // Prüfen, ob version jetzt auch wirklich 'n/a' ist
    expect(vm.version).toBe('n/a')
  })
})