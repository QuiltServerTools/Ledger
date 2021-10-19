<!--suppress ALL -->

<template id="search">
    <div class="container">
        <table class="table table-dark table-bg-dark">
            <form>
                <thead>
                <tr>
                    <th scope="col">Parameter</th>
                    <th scope="col">Value</th>
                    <th scope="col">Negative</th>
                    <th scope="col"></th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>Action</td>
                    <td>
                        <div class="form-check form-check-inline">
                            <input class="form-check-input" type="radio" name="action" id="actionTypeBreak"
                                   value="block-break">
                            <label class="form-check-label" for="actionTypeBreak">block-break</label>
                        </div>
                        <div class="form-check form-check-inline">
                            <input class="form-check-input" type="radio" name="action" id="actionTypePlace"
                                   value="block-place">
                            <label class="form-check-label" for="actionTypePlace">block-place</label>
                        </div>
                        <div class="form-check form-check-inline">
                            <input class="form-check-input" type="radio" name="action" id="actionTypeInsert"
                                   value="item-insert">
                            <label class="form-check-label" for="actionTypeInsert">item-insert</label>
                        </div>
                        <div class="form-check form-check-inline">
                            <input class="form-check-input" type="radio" name="action" id="actionTypeRemove"
                                   value="item-remove">
                            <label class="form-check-label" for="actionTypeRemove">item-remove</label>
                        </div>
                        <div class="form-check form-check-inline">
                            <input class="form-check-input" type="radio" name="action" id="actionTypeKill"
                                   value="entity-kill">
                            <label class="form-check-label" for="actionTypeKill">entity-kill</label>
                        </div>
                    </td>
                    <td>
                        <input class="form-check-input" type="checkbox" value="" id="actionNegative"></td>
                    <td>
                        <button type="button" class="btn btn-outline-info" v-on:click="addParam('action')">Add
                            parameter
                        </button>
                    </td>
                </tr>

                <tr>
                    <td>Dimension</td>
                    <td>
                        <input type="text" class="form-text" value="" id="world">
                    </td>
                    <td>
                        <input class="form-check-input" type="checkbox" value="" id="worldNegative"></td>
                    <td>
                        <button type="button" class="btn btn-outline-info" v-on:click="addParam('world')">Add
                            parameter
                        </button>
                    </td>
                </tr>

                <tr>
                    <td>Object</td>
                    <td>
                        <input type="text" class="form-text" value="" id="object">
                    </td>
                    <td>
                        <input class="form-check-input" type="checkbox" value="" id="objectNegative"></td>
                    <td>
                        <button type="button" class="btn btn-outline-info" v-on:click="addParam('object')">Add
                            parameter
                        </button>
                    </td>
                </tr>
                <tr>
                    <td>Source</td>
                    <td>
                        <input type="text" class="form-text" value="" id="source">
                    </td>
                    <td>
                        <input class="form-check-input" type="checkbox" value="" id="sourceNegative"></td>
                    <td>
                        <button type="button" class="btn btn-outline-info" v-on:click="addParam('source')">Add
                            parameter
                        </button>
                    </td>
                </tr>
                </tbody>
            </form>
        </table>
        <form>
            <div class="alert alert-ledger-light" ref="query-string" v-if="params"></div>
            <a class="btn btn-outline-info" v-on:click="sendSearch()">Search</a>
        </form>
    </div>
</template>

<script>
Vue.component("search", {
    template: "#search",
    data: () => ({
        actions: [],
        params: ""
    }),
    methods: {
        addParam: function (categoryName) {
            let input = document.getElementById(categoryName);
            let negative = document.getElementById(categoryName + "Negative")
            let isNegative = negative.checked
            if (input != null && input.type === "text") {
                this.addParsedParam(categoryName, input.value, isNegative)
                input.value = ""
            } else {
                let radios = document.getElementsByName(categoryName)
                for (let i = 0; i < radios.length; i++) {
                    if (radios[i].checked) {
                        this.addParsedParam(categoryName, radios[i].value, isNegative)
                        radios[i].checked = false
                    }
                }
            }
            negative.checked = false
        },
        sendSearch: function () {
            window.location.href = "/search/results?" + this.params
        },
        addParsedParam: function (key, value, negative) {
            if (this.params.length !== 0) {
                this.params += "&"
            }
            if (negative) {
                this.params += key + "=!" + value
            } else {
                this.params += key + "=" + value
            }

            Vue.nextTick(() => {
                this.$refs["query-string"].innerText = this.params.replaceAll("&", " ").replaceAll("=", ":")
            })
        }
    }
})
</script>

<script src="https://code.jquery.com/jquery-3.3.1.slim.min.js"
        integrity="sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo"
        crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.3/umd/popper.min.js"
        integrity="sha384-ZMP7rVo3mIykV+2+9J3UJ46jBk0WLaUAdn689aCwoqbBJiSnjAK/l8WvCWPIPm49"
        crossorigin="anonymous"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/js/bootstrap.min.js"
        integrity="sha384-ChfqqxuZUCnJSK3+MXmPNIyE6ZbWh2IMqE241rYiqJxyMiZ6OW/JmZQ5stwEULTy"
        crossorigin="anonymous"></script>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/css/bootstrap.min.css" rel="stylesheet"
      integrity="sha384-F3w7mX95PdgyTmZZMECAngseQB83DfGTowi0iMjiWaeVhAn4FJkqJByhZMI3AhiU" crossorigin="anonymous">
<link rel="stylesheet" type="text/css" href="../resources/ledger.css">