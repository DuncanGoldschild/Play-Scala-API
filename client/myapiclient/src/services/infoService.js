import trelloService from "./trelloService";

export default {
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
                                reject(error);
                            });
                    });
                })
                .then(() => {
                    resolve(controlsBuffer)
                })
                .catch(error => {
                    reject(error);
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
                                if (infoBuffer.token) {
                                    trelloService.setToken(infoBuffer.token)
                                }
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
                case "PUT":
                    trelloService.update(route, data)
                        .then(updateResponse => {
                            console.log(updateResponse)
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
                    case "DELETE":
                        trelloService.delete(route)
                            .then(deleteResponse => {
                                console.log(deleteResponse)
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
                                response.data["elements"]
                                    .forEach(function (element) {
                                        var action = Object.values(element["@controls"])[0]
                                        if (action.schema) {
                                            trelloService
                                                .getRoute(action.schema)
                                                .then(responseSchema => {
                                                    action.schema = responseSchema.data;
                                                    controlsBuffer.push(action);
                                                })
                                                .catch(error => {
                                                    reject(error);
                                                });
                                        }
                                        else {
                                            controlsBuffer.push(action)
                                        }
                                    })
                            }
                            if (response.data["@controls"]) {
                                response.data["@controls"].forEach(function (control) {
                                    var action = Object.values(control)[0];
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
                    console.log(verb + "invalid")
            }
        })
    }
}