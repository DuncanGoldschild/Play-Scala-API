<template>
  <v-card class="mx-auto">
    <div id="GET" v-if="control.verb=='GET'">
      <v-form>
        <v-container>
          <h2>{{control.title}}</h2>
          <v-btn v-on:click.native="onSubmit">{{control.verb}}</v-btn>
        </v-container>
      </v-form>
    </div>
    <div id="POST" v-if="control.verb=='POST'">
      <v-form>
        <v-container>
          <h2>{{control.title}}</h2>
          <div v-for="[field] of Object.entries(control.schema)" v-bind:key="field">
            <h4>{{field}}</h4>
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
        .then(newPage => {
          this.$parent.controls = newPage.controlsHTTP;
          this.$parent.infos = newPage.infoHTTP;
          this.$parent.elements = newPage.elementsHTTP;
        })
        .catch(error => {
          console.log(error);
        });
    }
  }
};
</script>