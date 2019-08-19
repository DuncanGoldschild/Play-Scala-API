<template>
  <div class="home">
    <img alt="Vue logo" src="../assets/logo.png" />
    <HelloWorld msg="Welcome to Your Vue.js App" />
  </div>
</template>

<script>
// @ is an alias to /src
import HelloWorld from "@/components/HelloWorld.vue";
import trelloService from "../services/trelloService";

export default {
  name: "home",
  components: {
    HelloWorld
  },
  created() {
    trelloService.getControls().then((response) => {
      console.log(response.data);
    })
    .catch((error) => {
      console.log(error);
    });
  }
};
</script>
