<template id="user-overview">
    <div>
        <ul class="user-overview-list">
            <li v-for="user in users">
                <a>{{user.name}} ({{user.email}})</a>
            </li>
        </ul>
    </div>
</template>
<script>
Vue.component("user-overview", {
    template: "#user-overview",
    data: () => ({
        users: [],
    }),
    created() {
        fetch("/api/users")
                .then(res => {
                    alert(res.json())
                    res.json()
                })
                .then(res => this.users = res)
                .catch(() => alert("Error while fetching users"));
    }
});
</script>
<style>
ul.user-overview-list {
    padding: 0;
    list-style: none;
}
ul.user-overview-list a {
    display: block;
    padding: 16px;
    border-bottom: 1px solid #ddd;
}
ul.user-overview-list a:hover {
    background: #00000010;
}
</style>