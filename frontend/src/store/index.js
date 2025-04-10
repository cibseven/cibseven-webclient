
import { createStore } from 'vuex'

import ProcessStore from './ProcessStore.js'
import FilterStore from './FilterStore.js'
import AdvancedSearchStore from './AdvancedSearchStore.js'
import UserStore from './UserStore.js'
import DecisionStore from './DecisionStore.js'
import JobDefinitionStore from './JobDefinitionStore.js'
import ActivityStore from './ActivityStore.js'

const store = createStore({
  modules: {
    process: ProcessStore,
    filter: FilterStore,
    advancedSearch: AdvancedSearchStore,
    user: UserStore,
    decision: DecisionStore,
    jobDefinition: JobDefinitionStore,
    activity: ActivityStore
  }
})

export default store
