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
	
	const HoverStyle = {
	    inserted: function(el, binding) {
			var prevStyle = {}
	        el.addEventListener('mouseover', function() {
				if (binding.value.styles) {					
					prevStyle = JSON.parse(JSON.stringify(el.style))
					Object.keys(binding.value.styles).forEach(function(sk) {
						el.style[sk] = binding.value.styles[sk]
					})
				}
				if (binding.value.classes) {
					binding.value.classes.forEach(function(cls) { 
						el.classList.add(cls)
					})
				}
	        })
			el.addEventListener('mouseleave', function() {
				if (binding.value.styles) {						
					Object.keys(binding.value.styles).forEach(function(sk) {
						el.style[sk] = prevStyle[sk] ? prevStyle[sk] : null
					})
				}
				if (binding.value.classes) {
					binding.value.classes.forEach(function(cls) { 
						el.classList.remove(cls)
					})
				}
	        })
	    }
	}

	export { HoverStyle }