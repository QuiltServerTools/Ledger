<!--suppress JSAnnotator -->
<template id="account">
    <div class="container">
        <div class="card mx-auto" style="width: 30%;">
            <img src="../api/pfp" class="card-img-top" alt="...">
            <div class="card-header">
                {{ this.profile.name }}
            </div>
            <div class="card-body">
                <ul class="list-group list-group-flush">
                    <li class="list-group-item">Latest Join: {{ this.profile.lastJoin}}</li>
                    <li class="list-group-item">First Join: {{ this.profile.firstJoin }}</li>
                    <li class="list-group-item" v-if="this.profile.perms === 3">Permissions: Admin</li>
                    <li class="list-group-item" v-else-if="this.profile.webUiPerms === 2">Permissions: Write</li>
                    <li class="list-group-item" v-else>Permissions: Read</li>

                </ul>
            </div>
        </div>
    </div>
</template>

<script>
Vue.component("account", {
    template: "#account",
    data: () => ({
        profile: {}
    }),
    created() {
        fetch("/api/account")
        .then(res => res.json())
        .then(res => this.profile = res)
        .catch(() => "Unexpected error while loading profile")
    }})
</script>

<style scoped>

</style>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-F3w7mX95PdgyTmZZMECAngseQB83DfGTowi0iMjiWaeVhAn4FJkqJByhZMI3AhiU" crossorigin="anonymous">
<link rel="stylesheet" type="text/css" href="../resources/ledger.css">