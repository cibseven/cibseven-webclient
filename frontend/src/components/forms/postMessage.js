/* globals window */

var postMessageMixin = {
//	created: function() {
//		window.addEventListener('message', this.callMethodFromMessage)
//	},
	methods: {
		sendMessageToParent: function(data) {
			window.parent.postMessage(data, "*");
		}
//		callMethodFromMessage: function(e) {
//			if (e.source === window.parent && e.data.callback && this[e.data.callback]) {
//				this[e.data.callback](e.data.result)
//			}
//		}
	}
//	beforeDestroy: function() {
//		window.removeEventListener('message', this.callMethodFromMessage)
//	}
}

export default postMessageMixin
