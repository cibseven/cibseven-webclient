
import { createStore } from 'vuex'

import ProcessStore from './ProcessStore.js'
import FilterStore from './FilterStore.js'
import AdvancedSearchStore from './AdvancedSearchStore.js'
import UserStore from './UserStore.js'

const store = createStore({
  modules: {
    process: ProcessStore,
    filter: FilterStore,
    advancedSearch: AdvancedSearchStore,
    user: UserStore
  }
})

export default store
