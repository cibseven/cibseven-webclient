
const ActivityStore = {
  state: { processActivities: [] },
  mutations: {
    setProcessActivities: function (state, activities) {
      state.processActivities = activities
    }
  }
}

export default ActivityStore
