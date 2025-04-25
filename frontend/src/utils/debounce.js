export function debounce(delay, fn) {
    let timeoutID = null
    return function (...args) {
      clearTimeout(timeoutID)
      timeoutID = setTimeout(() => fn.apply(this, args), delay)
    }
}