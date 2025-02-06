	
	const BLink = {
	    template: `
	        <component
	            :is="tag"
	            :href="computedHref"
	            :to="isRouterLink ? to : null"
	            :class="linkClasses"
	            :target="target"
	            :rel="rel"
	            :disabled="disabled ? 'disabled' : null"
	            @click="handleClick"
	        >
	            <slot></slot>
	        </component>
	    `,
	    props: {
	        href: { type: String, default: null },
	        to: { type: [String, Object], default: null },
	        target: { type: String, default: null },
	        rel: { type: String, default: null },
	        disabled: { type: Boolean, default: false },
	        variant: { type: String, default: 'primary' },
	        active: { type: Boolean, default: false },
	        underline: { type: Boolean, default: true },
	        buttonStyle: { type: Boolean, default: false }
	    },
	    computed: {
	        isRouterLink() {
	            return !!this.to
	        },
	        tag() {
	            if (this.isRouterLink) {
	                return 'router-link'
	            }
	            if (this.href) {
	                return 'a'
	            }
	            return 'button'
	        },
	        computedHref() {
	            return this.href && !this.isRouterLink ? this.href : null;
	        },
	        linkClasses() {
	            return [
	                this.buttonStyle ? 'btn' : 'nav-link',
	                this.variant && this.buttonStyle ? `btn-${this.variant}` : null,
	                this.active ? 'active' : null,
	                this.disabled ? 'disabled' : null,
	                !this.underline && !this.buttonStyle ? 'text-decoration-none' : null
	            ]
	        }
	    },
	    methods: {
	        handleClick(event) {
	            if (this.disabled) {
	                event.preventDefault()
	                event.stopPropagation()
	            }
	        }
	    }
	}

	const BCollapse = {
		template: `
			<div class="collapse"
				:class="{ 'show': isOpen, 'navbar-collapse': isNav }"
				:id="id"
				role="region"
				:aria-expanded="isOpen.toString()"
				@transitionend="handleTransitionEnd">
				<slot></slot>
			</div>
		`,
		props: {
			id: {
				type: String,
				required: false,
			},
			isNav: {
				type: Boolean,
				default: false,
			},
			modelValue: {
				type: Boolean,
				default: false,
			},
			accordion: {
				type: String,
				default: null,
			}
		},
		data() {
			return {
				isOpen: this.modelValue
			}
		},
		watch: {
			modelValue(newVal) {
				this.isOpen = newVal
			},
			isOpen(newVal) {
				if (newVal !== this.modelValue) {
					this.$emit('update:modelValue', newVal)
				}
				if (this.accordion && newVal) {
					this.$eventBus.emit('toggle-accordion', { accordion: this.accordion, id: this.id })
				}
			}
		},
		methods: {
			toggle() {
				this.isOpen = !this.isOpen
				this.$emit('update:modelValue', this.isOpen)
			},
			handleTransitionEnd() { },
			close() {
				this.isOpen = false
				this.$emit('update:modelValue', false)
			},
		},
		mounted() {
			this.$eventBus.on('bv::toggle::collapse', (id) => {
				if (this.id === id) {
					this.toggle()
				}
			})
			if (this.accordion) {
				this.$eventBus.on('toggle-accordion', (obj) => {
					if (obj.accordion === this.accordion && this.id !== obj.id) {
						this.close()
					}
				})
			}
			
		},
		beforeUnmount() {
			this.$eventBus.off('bv::toggle::collapse')
			if (this.accordion) {
				this.$eventBus.off('toggle-accordion')
			}
		}
	}


	
	const BNavbar = { 
		template: `
			<nav class="navbar fixed-top navbar-expand-md" role="navigation" :aria-label="$t('bcomponents.navbar.mainNavigation')">
				<slot></slot>
			</nav>
		`
	}
	
	const BNavbarToggle = { 
		template: `
			<button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#nav_collapse"
	    		aria-controls="nav_collapse" aria-expanded="false" :aria-label="$t('bcomponents.navbar.toggleNavigation')">
	    		<span class="navbar-toggler-icon" aria-hidden="true"></span>
	    	</button>
		`
	}
	
	const BNavbarBrand = { 
		template: `
			<a class="navbar-brand" :href="'#' + to" :aria-label="$t('bcomponents.navbar.brand')"> 
				<slot></slot> 
			</a>
		`,
		props: { to: String }
	}
	
	const BNavbarNav = { 
		template: '<ul class="navbar-nav" role="menu"> <slot></slot> </ul>'
	}

	const BNavItemDropdown = {
		template: `
			<li class="nav-item dropdown">
				<a class="nav-link dropdown-toggle" href="javascript:void(0)" role="button" data-bs-toggle="dropdown"
					aria-expanded="false" aria-haspopup="true" :aria-label="$t('bcomponents.navbar.toggleDropdown')">
					<slot name="button-content"></slot>
				</a>
			    <ul class="dropdown-menu dropdown-menu-end" role="menu"> <slot></slot> </ul>
		    </li>
		`
	}
	
	const BPagination = {
		template: `
			<nav :class="['pagination-wrapper', wrapperClass]" role="navigation" :aria-label="$t('bcomponents.pagination.label')">
				<ul class="pagination justify-content-center">
					<li class="page-item" :class="{ disabled: currentPage === 1 }">
						<button class="page-link" @click="goToPage(currentPage - 1)" 
							:aria-label="$t('bcomponents.pagination.prevPage')" :aria-disabled="currentPage === 1">
							<span aria-hidden="true">&laquo;</span>
						</button>
					</li>

					<li v-for="page in pages" :key="page" class="page-item" 
						:class="{ active: page === currentPage }">
						<button class="page-link" @click="goToPage(page)" 
							:aria-current="page === currentPage ? 'page' : null">
							{{ page }}
						</button>
					</li>

					<li class="page-item" :class="{ disabled: currentPage === totalPages }">
						<button class="page-link" @click="goToPage(currentPage + 1)" 
							:aria-label="$t('bcomponents.pagination.nextPage')" :aria-disabled="currentPage === totalPages">
							<span aria-hidden="true">&raquo;</span>
						</button>
					</li>
				</ul>
			</nav>
		`,
		props: {
			modelValue: { type: Number, default: 1 },
			totalRows: { type: Number, required: true },
			perPage: { type: Number, default: 10 },
			wrapperClass: { type: String, default: '' }
		},
		data: function() {
			return {
				currentPage: this.modelValue
			}
		},
		computed: {
			totalPages: function() {
				return Math.ceil(this.totalRows / this.perPage)
			},
			pages: function() {
				const half = Math.floor(5 / 2)
				let start = Math.max(1, this.currentPage - half)
				let end = Math.min(this.totalPages, start + 5 - 1)

				if (end - start < 5 - 1) {
					start = Math.max(1, end - 5 + 1)
				}

				return Array.from({ length: end - start + 1 }, (v, i) => start + i)
			}
		},
		watch: {
			modelValue: function(newValue) { this.currentPage = newValue },
			currentPage: function(newValue) { 
				this.$emit('update:modelValue', newValue)
			}
		},
		methods: {
			goToPage: function(page) {
				if (page >= 1 && page <= this.totalPages) {
					this.currentPage = page
				}
			}
		}
	}
	
	const BTabs = {
		template: `
			<div :class="customClass">
		        <nav>
		            <div class="nav nav-tabs" role="tablist">
		                <button v-for="(tab, index) in tabs" :key="tab.id"
		                    class="nav-link" :class="[titleLinkClass, { active: activeTab === tab.id }]"
		                    :id="'nav-' + tab.id + '-tab'"
		                    data-bs-toggle="tab" :data-bs-target="'#nav-' + tab.id"
		                    type="button" role="tab" 
							:aria-controls="'nav-' + tab.id" :aria-selected="activeTab === tab.id"
		                    @click="selectTab(tab.id)">
		                    {{ tab.title }}
		                </button>
		            </div>
		        </nav>
		        <div class="tab-content" :class="contentClass">
		            <slot></slot>
		        </div>
			</div>
	    `,
		props: {
			class: {
				type: String,
				default: ''
			},
			contentClass: {
				type: String,
				default: ''
			},
			titleLinkClass: {
				type: String,
				default: ''
			}
		},
		data: function() {
			return {
				tabs: [],
				activeTab: null,
				tabClickHandlers: []
			};
		},
		computed: {
			customClass: function() {
				return this.class
			}
		},
		methods: {
			selectTab: function(tabId) {
				this.activeTab = tabId
				this.notifyTabClick(tabId)
			},
			registerTab: function(tab) {
				this.tabs.push(tab)
				if (this.tabs.length === 1) {
					this.activeTab = tab.id
				}
			},
			registerClickHandler: function(callback) {
				this.tabClickHandlers.push(callback)
			},
			notifyTabClick: function(tabId) {
				this.tabClickHandlers.forEach(callback => callback(tabId))
			}
		},
		provide() {
			return {
				registerTab: this.registerTab,
				activeTab: () => this.activeTab,
				registerClickHandler: this.registerClickHandler
			}
		},
		mounted() {
			if (this.tabs.length > 0) {
				this.activeTab = this.tabs[0].id
			}
		}
	}
	
	const BTab = {
		template: `
	        <div class="tab-pane fade" :class="{ 'show active': isActive }"
	            :id="'nav-' + id" role="tabpanel" :aria-labelledby="'nav-' + id + '-tab'">
	            <slot></slot>
	        </div>
	    `,
		props: {
			id: { type: String, required: true },
			title: { type: String, required: true }
		},
		inject: ['registerTab', 'activeTab', 'registerClickHandler'],
		computed: {
			isActive() {
				return this.activeTab() === this.id;
			}
		},
		methods: {
			handleClick: function() {
				this.$emit('click')
			},
			externalClick: function(tabId) {
				if (tabId === this.id) {
					this.handleClick()
				}
			}
		},
		created() {
			this.registerTab({ id: this.id, title: this.title })
			this.registerClickHandler(this.externalClick)
		}
	}
	
	export { BLink, BCollapse, BNavbar, BNavbarBrand, BNavbarNav, BNavbarToggle, BNavItemDropdown, BPagination, BTab, BTabs }