<!--suppress JSAnnotator -->
<template id="dashboard">
    <div class="container">
        <h3 class="text-white">Latest Actions</h3>
        <div style="height: 50%; overflow-y: scroll">
            <table class="table table-dark mx-auto table-bg-dark table-hover" style="width: 75%;">
                <thead>
                <tr style="position: sticky; top: 0;">
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
                    <td>{{ action.time }}</td>
                    <td>{{ action.world }}</td>
                    <td>{{ action.x }} {{ action.y }} {{ action.z }}</td>
                    <td>{{ action.identifier }}</td>
                    <td v-if="action.sourceProfile != null">{{ action.sourceProfile }}</td>
                    <td v-else>{{ action.sourceName }}</td>
                    <td v-if="action.objectString != null">{{ action.objectString }}</td>
                    <td v-else>{{ action.oldObjectString }}</td>
                </tr>
                </tbody>
            </table>
        </div>
        <h3 class="text-white">Players</h3>
        <div style="height: 50%; overflow-y: scroll">
            <table class="table table-dark mx-auto table-bg-dark table-hover" style="width: 75%;">
                <thead>
                <tr style="position: sticky; top: 0;">
                    <th scope="col">Username</th>
                    <th scope="col">Last Join</th>
                    <th scope="col">First Join</th>
                    <th scope="col">Permissions</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="player in players">
                    <td>{{ player.name }}</td>
                    <td>{{ player.lastJoin }}</td>
                    <td>{{ player.firstJoin }}</td>
                    <td v-if="player.perms === 3">Admin</td>
                    <td v-else-if="player.perms === 2">Write</td>
                    <td v-else-if="player.perms === 1">Read</td>
                    <td v-else>None</td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
</template>
<script>
Vue.component("dashboard", {
    template: "#dashboard",
    data: () => ({
        actions: [],
        players: []
    }),
    created() {
        fetch("/api/overview")
                .then(res => res.json())
                .then(res => this.actions = res)
                .catch(() => alert("Error while fetching actions"));
        fetch("/api/players_overview")
                .then(res => res.json())
                .then(res => this.players = res)
                .catch(() => alert("Error while fetching players"))
    }
});
</script>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/css/bootstrap.min.css" rel="stylesheet"
      integrity="sha384-F3w7mX95PdgyTmZZMECAngseQB83DfGTowi0iMjiWaeVhAn4FJkqJByhZMI3AhiU" crossorigin="anonymous">
<link rel="stylesheet" type="text/css" href="../resources/ledger.css">