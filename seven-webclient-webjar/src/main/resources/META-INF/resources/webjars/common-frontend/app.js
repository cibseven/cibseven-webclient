(function() { /* globals VueI18n, VueRouter, localStorage, console */
	"use strict";
	
	axios.interceptors.response.use(function(res) { return res.data }, function(error) { return Promise.reject(error) })
			
	var router = new VueRouter({
		routes: [] //TODO
	})
	
	var i18n = new VueI18n({ 
		locale: localStorage.getItem('language') || 'en', //TODO ? determine from browser
	})
	axios.defaults.headers.common['Accept-Language'] = i18n.locale
//	moment.locale(i18n.locale)
	var loadedLanguages = []	
	function fetchTranslation(lang) { // http://kazupon.github.io/vue-i18n/guide/lazy-loading.html
		return loadedLanguages.includes(lang) ? Promise.resolve() : axios.get('translations_' + lang + '.json').then(function(res) {
			i18n.setLocaleMessage(lang, res)
			loadedLanguages.push(lang)
		})   	
	}	
	fetchTranslation(i18n.locale)
	
	new Vue({ /*jshint nonew:false */
		el: '#app',		
		router: router,
		i18n: i18n,
		provide: {
			currentLanguage: function(lang) {
				if (!lang) return i18n.locale
				return fetchTranslation(lang).then(function() { 
					i18n.locale = lang
				//	moment.locale(lang)
					axios.defaults.headers.common['Accept-Language'] = lang
					localStorage.setItem('language', lang)
				})
			},
		}, 
		data: function() { //TODO that's just an example
			return {
				leftOpen: true,
				rightOpen: true
			}
		},
		methods: {			
			log: function(evt) {
				console && console.info(evt) 
			}, 
			onSelect: function(files, addTask) { /* globals setTimeout */
				console && console.debug('onSelect', files)
				var updaters = files.map(function(file) { return addTask(file.name) })
				updaters.forEach(function(updater) { updater(50) })
				var area = this.$refs.area
				setTimeout(function() {					
					updaters[0](true)
					updaters[1](100)
					updaters[2](false)
					area.clear() // only clear after we're done using fileList !
				}, 1000)
			}
		}
	})
	
})()