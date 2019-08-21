import Axios from 'axios';

var config = {
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyAidXNlcm5hbWUiIDogIkplYW4iIH0.bwqUlQj0qYtSPwS6xWFFXu0FkNTFh1P0LpeJXwoXuv0'
  }
};

export default {
  getControls() {
    return Axios.get("/");
  },

  getRoute(route) {
    return Axios.get(route, config)
  },

  create(route, data) {
    return Axios.post(route, data)
  }
};