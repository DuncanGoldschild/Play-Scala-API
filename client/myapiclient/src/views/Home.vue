<template>
  <div class="home">
    <v-list-item v-for="[field, value] of Object.entries(infos)" v-bind:key="field">
      <v-list-item-content>
        <v-list-item-title>{{field}}:{{value}}</v-list-item-title>
      </v-list-item-content>
    </v-list-item>
    <v-list-item v-for="element in elements" v-bind:key="element.label">
      <v-list-item-content>
        <v-list-item-title>{{element}}</v-list-item-title>
      </v-list-item-content>
    </v-list-item>
    <div v-for="control in controls" v-bind:key="control.title">
      <GenericPage v-bind:control="control" />
    </div>
  </div>
</template>

<script>
// @ is an alias to /src
import GenericPage from "@/components/GenericPage.vue";
import infoService from "../services/infoService";

export default {
  name: "home",
  components: {
    GenericPage
  },
  data() {
    return {
      controls: [],
      elements: [{ label: "Element 1" }],
      infos: { Welcome: "Decide what you want to do" }
    };
  },

  mounted() {
    infoService
      .firstLink()
      .then(firstControls => {
        console.log("home");
        this.controls = firstControls;
      })
      .catch(error => {
        console.log(error);
      });
  }
};
</script>