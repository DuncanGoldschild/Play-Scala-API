import Axios from 'axios';

export default {
  getControls() {
    return Axios.get("/");
  },

  getRoute(route) {
    return Axios.get(route)
  },

  create(route, data) {
    return Axios.post(route, data)
  },

  update(route,data) {
    return Axios.put(route,data)
  },

  delete(route) {
    return Axios.delete(route)
  },

  setToken(token){
    Axios.defaults.headers.common['Authorization'] = token
  }
};