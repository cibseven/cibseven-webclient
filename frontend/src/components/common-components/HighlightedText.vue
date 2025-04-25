<template>
  <div v-html="highlightedText"></div>
</template>

<script>
export default {
  name: "HighlightedText",
  props: {
    text: {
      type: String,
      required: true
    },
    keyword: {
      type: String,
      required: true
    }
  },
  computed: {
    highlightedText() {
      if (!this.keyword) return this.text

      // Create a regular expression to match the keyword
      const regex = new RegExp(`(${this.keyword})`, 'gi')
      // Replace the matched keyword with a span element
      return this.text.replace(regex, '<span class="mark p-0">$1</span>')
    }
  }
}
</script>
