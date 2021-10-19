<!--suppress JSAnnotator -->
<template id="user_manager">
    <div class="container">
        <table class="table table-dark mx-auto table-bg-dark table-hover" style="width: 75%">
            <thead>
            <tr style="position: sticky; top: 0;">
                <th scope="col">Name</th>
                <th scope="col">First Join</th>
                <th scope="col">Latest Join</th>
                <th scope="col">Permissions</th>
                <th scope="col">Manage</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="user in this.users">
                <td> {{ user.name }} </td>
                <td> {{user.firstJoin}} </td>
                <td> {{user.lastJoin}} </td>
                <td v-if="user.perms === 3">Admin</td>
                <td v-else-if="user.perms === 2">Write</td>
                <td v-else-if="user.perms === 1">Read</td>
                <td v-else>None</td>
                <td>
                    <a class="btn btn-outline-warning" v-on:click="showBox(user.uuid)">Modify</a>
                </td>
            </tr>
            <tr v-if="this.users.length === 0">
                <td>
                    No results found
                </td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
            </tr>
            </tbody>
        </table>
        <div class="fixed-bottom alert alert-ledger mx-auto" style="width: 75%; display: none" ref="permissionsBox">
            Update Permissions
            <label for="userPermRange" class="form-label">Range (0 - 3)</label>
            <input type="range" class="form-range" id="userPermRange" ref="userPermRange" min="0" max="3">
            <button class="btn btn-outline-warning" v-on:click="updateUser()">
                Update
            </button>
        </div>
    </div>
</template>

<script>
Vue.component("user_manager", {
    template: "#user_manager",
    data: () => ({
        users: [],
        userBeingModified: ""
    }),
    created() {
        fetch("/api/users")
                .then(res => res.json())
                .then(res => this.users = res)
                .catch(() => "Unexpected error fetching users")
    },
    methods: {
        updateUser: function () {
            let permissionLevel = this.$refs.userPermRange.value
            this.$refs["permissionsBox"].style.display = "none"
            fetch("/api/updateuser?" + "uuid=" + this.userBeingModified + "&level=" + permissionLevel)
            this.userBeingModified = ""
            fetch("/api/users")
                    .then(res => res.json())
                    .then(res => this.users = res)
                    .catch(() => "Unexpected error fetching users")
        },
        showBox: function (uuid) {
            this.userBeingModified = uuid
            this.$refs["permissionsBox"].style.display = "inline"
        }
    }
});
</script>
<style scoped>

</style>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/css/bootstrap.min.css" rel="stylesheet"
      integrity="sha384-F3w7mX95PdgyTmZZMECAngseQB83DfGTowi0iMjiWaeVhAn4FJkqJByhZMI3AhiU" crossorigin="anonymous">
<link rel="stylesheet" type="text/css" href="../resources/ledger.css">