<!--suppress JSAnnotator -->
<template id="inspect">
    <div class="container">
        <table class="table table-dark table-bg-dark">
            <thead>
            <tr>
                <th scope="col">Parameter</th>
                <th scope="col">Value</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td>World</td>
                <td>
                    <input type="text" class="form-text" value="minecraft:overworld" ref="world">
                </td>
            </tr>
            <tr>
                <td>X</td>
                <td>
                    <input type="number" class="form-number" ref="x">
                </td>
            </tr>
            <tr>
                <td>Y</td>
                <td>
                    <input type="number" class="form-text" ref="y">
                </td>
            </tr>
            <tr>
                <td>Z</td>
                <td>
                    <input type="number" class="form-text" ref="z">
                </td>
            </tr>
            </tbody>
        </table>
        <form>
            <a class="btn btn-outline-info" v-on:click="inspect()">Inspect</a>
        </form>
    </div>
</template>

<script>
Vue.component("inspect", {
    template: "#inspect",
    methods: {
        inspect: function () {
            let x = 0
            let y = 64
            let z = 0
            let world = this.$refs["world"].value
            if (world === "") {
                world = "minecraft:overworld"
            }
            console.log(world)
            window.location.href = "../inspect/results?x=" + x + "&y=" + y + "&z=" + z + "&world=" + world
        }
    }
});
</script>
<style scoped>

</style>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/css/bootstrap.min.css" rel="stylesheet"
      integrity="sha384-F3w7mX95PdgyTmZZMECAngseQB83DfGTowi0iMjiWaeVhAn4FJkqJByhZMI3AhiU" crossorigin="anonymous">
<link rel="stylesheet" type="text/css" href="../resources/ledger.css">