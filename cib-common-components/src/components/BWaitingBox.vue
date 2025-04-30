<!--

    Copyright CIB software GmbH and/or licensed to CIB software GmbH
    under one or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information regarding copyright
    ownership. CIB software licenses this file to you under the Apache License,
    Version 2.0; you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.

-->
<template>
  <div>
    <slot v-if="done"></slot>
    <svg v-else-if="show" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="-50 -50 100 100"  :style="styling">
      <defs>
        <line id="Line" x1="0" y1="-25" x2="0" y2="-45"/>
      </defs>
      <use class="element delay1" xlink:href="#Line" transform="rotate(0)"/>
      <use class="element delay2" xlink:href="#Line" transform="rotate(30)"/>
      <use class="element delay3" xlink:href="#Line" transform="rotate(60)"/>
      <use class="element delay4" xlink:href="#Line" transform="rotate(90)"/>
      <use class="element delay5" xlink:href="#Line" transform="rotate(120)"/>
      <use class="element delay6" xlink:href="#Line" transform="rotate(150)"/>
      <use class="element delay7" xlink:href="#Line" transform="rotate(180)"/>
      <use class="element delay8" xlink:href="#Line" transform="rotate(210)"/>
      <use class="element delay9" xlink:href="#Line" transform="rotate(240)"/>
      <use class="element delay10" xlink:href="#Line" transform="rotate(270)"/>
      <use class="element delay11" xlink:href="#Line" transform="rotate(300)"/>
      <use class="element delay12" xlink:href="#Line" transform="rotate(330)"/>
    </svg>
  </div>
</template>

<script>
export default {
  name: 'BWaitingBox',
  props: { styling: String },
  data: function() { return { done: false, show: true } },
  methods: {
    wait: function(prom, delay) {
      this.done = false
      this.show = !delay
      if (delay) setTimeout(function() { this.show = true }.bind(this), delay)
      return prom.then(function(res) {
        this.done = true
        return res
      }.bind(this), function() { this.done = true }.bind(this)) // any custom error handling needs to be attachend to prom before
    },
//			reset: function() { this.done = !(this.show = true) }
  }
}
</script>

<style scoped>
@-webkit-keyframes fadeLoop {
  0%{opacity:1;}
  20%{opacity:0.4;}
  100%{opacity:0.4;}
}
@keyframes fadeLoop {
  0%{opacity:1;}
  20%{opacity:0.4;}
  100%{opacity:0.4;}
}
.element {
  -webkit-animation: fadeLoop 1.2s infinite;
  animation: fadeLoop 1.2s infinite;
  opacity:0.4;
  stroke-width:10;
  stroke-linecap:round;
  stroke:#808080;
}
.delay2{
  -webkit-animation-delay:100ms;
  animation-delay:100ms;
}
.delay3{
  -webkit-animation-delay:200ms;
  animation-delay:200ms;
}
.delay4{
  -webkit-animation-delay:300ms;
  animation-delay:300ms;
}
.delay5{
  -webkit-animation-delay:400ms;
  animation-delay:400ms;
}
.delay6{
  -webkit-animation-delay:500ms;
  animation-delay:500ms;
}
.delay7{
  -webkit-animation-delay:600ms;
  animation-delay:600ms;
}
.delay8{
  -webkit-animation-delay:700ms;
  animation-delay:700ms;
}
.delay9{
  -webkit-animation-delay:800ms;
  animation-delay:800ms;
}
.delay10{
  -webkit-animation-delay:900ms;
  animation-delay:900ms;
}
.delay11{
  -webkit-animation-delay:1000ms;
  animation-delay:1000ms;
}
.delay12{
  -webkit-animation-delay:1100ms;
  animation-delay:1100ms;
}
</style>
