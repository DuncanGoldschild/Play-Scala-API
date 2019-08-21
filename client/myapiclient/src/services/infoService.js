import trelloService from "./trelloService";

export default {
    controls: [],
    infos: {},
    addControl(json) {
        this.controls.push(json)
    },
    firstLink() {
        return new Promise((resolve, reject) => {
            var controlsBuffer = [];
            trelloService
                .getControls()
                .then(response => {
                    response.data["@controls"].forEach(function (element) {
                        var action = Object.values(element)[0];
                        trelloService
                            .getRoute(action.schema)
                            .then(responseSchema => {
                                action.schema = responseSchema.data;
                                controlsBuffer.push(action);
                            })
                            .catch(error => {
                                console.log(error);
                            });
                    });
                })
                .then(() => {
                    console.log("service")
                    console.log(controlsBuffer)
                    resolve(controlsBuffer)
                })
                .catch(error => {
                    console.log(error);
                });
        })
    }
    ,
    httpAction(route, verb, data) {
        return new Promise((resolve, reject) => {
            var controlsBuffer = [];
            var infoBuffer = {};
            var elementsBuffer = [];
            switch (verb) {
                case "POST":
                    trelloService.create(route, data)
                        .then(createResponse => {
                            console.log(createResponse)
                            if (createResponse.data["info"]) {
                                infoBuffer = (createResponse.data["info"])
                            }
                            if (createResponse.data["elements"]) {
                                elementsBuffer = createResponse.data["elements"]
                            }
                            createResponse.data["@controls"].forEach(function (element) {
                                var action = Object.values(element)[0];
                                trelloService
                                    .getRoute(action.schema)
                                    .then(responseSchema => {
                                        action.schema = responseSchema.data;
                                        controlsBuffer.push(action);
                                    })
                                    .catch(error => {
                                        reject(error);
                                    });
                            });


                        })
                        .then(() => {
                            resolve(
                                {
                                    infoHTTP: infoBuffer,
                                    controlsHTTP: controlsBuffer,
                                    elementsHTTP: elementsBuffer
                                }
                            )
                        })
                        .catch(error => {
                            reject(error);
                        });
                    break;
                case "GET":
                    trelloService.getRoute(route)
                        .then(response => {
                            console.log(response)
                            if (response.data["info"]) {
                                infoBuffer = response.data["info"]
                            }
                            if (response.data["elements"]) {
                                elementsBuffer = response.data["elements"]
                            }
                            if (response.data["@controls"]) {
                                response.data["@controls"].forEach(function (element) {
                                    var action = Object.values(element)[0];
                                    trelloService
                                        .getRoute(action.schema)
                                        .then(responseSchema => {
                                            action.schema = responseSchema.data;
                                            controlsBuffer.push(action);
                                        })
                                        .catch(error => {
                                            reject(error);
                                        });
                                });
                            }
                            else if (response.data["info"]) {
                                resolve(
                                    {
                                        infoHTTP: response.data["info"],
                                        controlsHTTP: controlsBuffer
                                    }
                                )
                            }
                        })
                        .then(() => {
                            console.log(elementsBuffer)
                            resolve(
                                {
                                    infoHTTP: infoBuffer,
                                    controlsHTTP: controlsBuffer,
                                    elementsHTTP: elementsBuffer
                                }
                            )
                        })
                        .catch(error => {
                            reject(error);
                        });
                    break;
                default:
                    resolve(
                        {
                            infoHTTP: infoBuffer,
                            controlsHTTP: controlsBuffer
                        }
                    )
            }
        })
    }
}