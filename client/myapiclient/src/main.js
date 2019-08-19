import Vue from "vue";
import App from "./App.vue";
import router from "./router";
import Axios from 'axios';

Vue.config.productionTip = false;

Axios.defaults.baseURL = 'http://localhost:9000';

new Vue({
  router,
  render: h => h(App)
}).$mount("#app");
