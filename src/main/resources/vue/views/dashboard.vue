<!--suppress JSAnnotator -->
<template id="dashboard">
    <table class="table table-light mx-auto" style="width: 75%;">
        <thead>
        <tr>
            <th scope="col">Time</th>
            <th scope="col">World</th>
            <th scope="col">Position</th>
            <th scope="col">Action Type</th>
            <th scope="col">Source</th>
            <th scope="col">Object</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="action in actions">
            <td>{{action.time}}</td>
            <td>{{action.world}}</td>
            <td>{{action.x}} {{action.y}} {{action.z}}</td>
            <td>{{action.identifier}}</td>
            <td v-if="action.sourceProfile != null">{{action.sourceProfile}}</td>
            <td v-else>{{action.sourceName}}</td>
            <td v-if="action.objectString != null">{{action.objectString}}</td>
            <td v-else>{{action.oldObjectString}}</td>
        </tr>
        </tbody>
    </table>
</template>
<script>
Vue.component("dashboard", {
    template: "#dashboard",
    data: () => ({
        actions: [],
    }),
    created() {
        fetch("/api/overview")
                .then(res => res.json())
                .then(res => this.actions = res)
                .catch(() => alert("Error while fetching actions"));
    }});

</script>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-F3w7mX95PdgyTmZZMECAngseQB83DfGTowi0iMjiWaeVhAn4FJkqJByhZMI3AhiU" crossorigin="anonymous">
<link rel="stylesheet" type="text/css" href="../resources/ledger.css">