<!--suppress JSAnnotator -->
<template id="login">
    <div class="container alert alert-ledger">
        <div class="alert alert-success" v-if="this.logout">
            Successfully logged out
        </div>
        <form method="POST" action="/api/login">
            <div class="mb-3">
                <label for="username" class="form-label">Username</label>
                <input type="text" class="form-control" id="username" aria-describedby="username" name="username">
                <div id="usernameHelp" class="form-text">The in-game username you want to use for authentication</div>
            </div>
            <div class="mb-3">
                <label for="userPassword" class="form-label">Password</label>
                <input type="password" class="form-control" id="userPassword" name="userPassword">
                <div id="passwordHelp" class="form-text">You can change your password in-game using /ledger password
                </div>
            </div>
            <div class="alert alert-danger" v-if="this.incorrectPass">Incorrect password</div>
            <div class="mb-3 form-check">
                <input type="checkbox" class="form-check-input" id="exampleCheck1">
                <label class="form-check-label" for="exampleCheck1">Check me out</label>
            </div>
            <button type="submit" class="btn btn-primary">Submit</button>
        </form>
    </div>
</template>
<script>
Vue.component("login", {
    template: "#login",
    created() {
        if (window.location.href.includes("?")) {
            const values = window.location.href.substr(window.location.href.indexOf("?"))
            if (values.includes("logout")) {
                this.logout = true
            }
            if (values.includes("password")) {
                this.incorrectPass = true
            }
        }
    },
    data: () => ({
        logout: false,
        incorrectPass: false
    })
})

</script>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/css/bootstrap.min.css" rel="stylesheet"
      integrity="sha384-F3w7mX95PdgyTmZZMECAngseQB83DfGTowi0iMjiWaeVhAn4FJkqJByhZMI3AhiU" crossorigin="anonymous">
<link rel="stylesheet" type="text/css" href="../resources/ledger.css">