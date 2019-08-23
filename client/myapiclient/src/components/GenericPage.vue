<template>
  <v-card height="100%" class="mx-auto" color="#b1b1ff">
    <div>
      <v-form>
        <h2>{{control.title}}</h2>
        <v-container v-if="control.verb=='GET'||control.verb == 'DELETE'">
          <v-btn v-on:click.native="onSubmit">{{control.verb}}</v-btn>
        </v-container>
        <v-container v-if="control.verb=='POST'||control.verb == 'PUT' ">
          <div v-for="[field] of Object.entries(control.schema)" v-bind:key="field">
            <h4>{{field}}:</h4>
            <v-text-field v-model="control.schema[field]" type="text" v-bind:placeholder="field"></v-text-field>
          </div>
          <v-btn v-on:click.native="onSubmit">{{control.verb}}</v-btn>
        </v-container>
      </v-form>
    </div>
  </v-card>
</template>

<script>
import infoService from "../services/infoService";
import Home from "@/views/Home.vue";
import * as Vue from "vue/dist/vue.common.js";
import GenericPage from "@/components/GenericPage.vue";
import App from "@/App.vue";

export default {
  name: "generic-page",
  props: {
    control: {
      title: {
        type: String,
        required: true
      },
      href: {
        type: String,
        required: true
      },
      verb: {
        type: String,
        required: true
      },
      schema: {
        type: String
      },
      required: true
    }
  },
  methods: {
    onSubmit() {
      infoService
        .httpAction(this.control.href, this.control.verb, this.control.schema)
        .then(
          newPage => {
            var ComponentGeneric = new Vue({
              name: "homie",
              el: "#app",
              template: `<div>{{infos}}</div>`,
              components: {
                GenericPage
              },
              appName: "DDDDDD",
              data: {
                  controls: newPage.controlsHTTP,
                  elements: newPage.elementsHTTP,
                  infos: newPage.infoHTTP
              },
              mounted: function() {
                console.log("new generic");
              }
            });
            this.$router.addRoutes([
              {
                path: this.control.href,
                name: "foo",
                component: ComponentGeneric.default,
                props: true
              }
            ]);
            this.$router.push({
              path: this.control.href,
              params: {
                controls: newPage.controlsHTTP,
                infos: newPage.infoHTTP,
                elements: newPage.elementsHTTP
              }
            });
            this.$parent.controls = newPage.controlsHTTP;
            this.$parent.infos = newPage.infoHTTP;
            this.$parent.elements = newPage.elementsHTTP;
          },
          reject => console.log(reject)
        )
        .catch(error => {
          console.log(error);
        });
    }
  }
};
</script>