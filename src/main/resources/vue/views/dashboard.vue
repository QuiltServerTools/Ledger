<!--suppress JSAnnotator -->
<template id="dashboard">
    <ul class="action-overview-list">
        <li v-for="action in actions">
            <a :href="`/actions/${action.identifier}`">{{action.identifier}}</a>
        </li>
    </ul>
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