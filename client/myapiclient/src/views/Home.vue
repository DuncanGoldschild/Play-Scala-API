<template>
  <div class="home">
    <div>
      <h2 v-if="Object.entries(infos).length !== 0">Self Informations:</h2>
      <v-list-item v-for="[field, value] of Object.entries(infos)" v-bind:key="field">
        <v-list-item-content>
          <v-list-item-title>{{field}}:{{value}}</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
    </div>
    <div v-if="elements.length>0">
      <h2>Elements included:</h2>
      <v-list-item v-for="element in elements" v-bind:key="element.label">
        <v-list-item-content>
          <v-list-item-title v-for="[field, value] of Object.entries(element)" v-bind:key="field">{{field}}: {{value}}</v-list-item-title>
        </v-list-item-content>
      </v-list-item>
    </div>
    <div v-if="controls.length>0">
      <h2>Available controls:</h2>
      <v-container fluid grid-list-xs>
        <v-layout row wrap>
          <v-flex xs12 sm4 v-for="control in controls" v-bind:key="control.title">
            <GenericPage v-bind:control="control" />
          </v-flex>
        </v-layout>
      </v-container>
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
      elements: [],
      infos: {}
    };
  },
  mounted() {
    infoService
      .firstLink()
      .then(firstControls => {
        this.controls = firstControls;
      })
      .catch(error => {
        console.log(error);
      });
  }
};
</script>