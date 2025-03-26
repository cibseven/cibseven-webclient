
import { DecisionService } from '@/services.js'

const DecisionStore = {
  state: { list: [] },
  mutations: {
    setDecisions: function (state, param) {
      state.list = param.decisions
    }
  },
  actions: {
    async getDecisionList() {
      return DecisionService.getDecisionList()
    },
    async getDecisionByKey({ state, dispatch }, key) {
        if (state.list && state.list.length > 0) {
          const found = state.list.find(decision => decision.key === key)
          if (found) {
            return found
          }
        }
        const newList = await dispatch("getDecisionList")
        const foundAfterReload = newList.find(decision => decision.key === key)
        if (foundAfterReload) {
          return foundAfterReload
        }
        debugger
        return DecisionService.getDecisionByKey(key)
      }
    }
}

export default DecisionStore
