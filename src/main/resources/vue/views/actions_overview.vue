<template id="action-overview">
    <div>
        <ul class="action-overview-list">
            <li v-for="action in actions">
                <a :href="`/actions/${action.id}`">{{action.name}} ({{action.email}})</a>
            </li>
        </ul>
    </div>
</template>
<script>
Vue.component("action-overview", {

});
</script>
<style>
ul.action-overview-list {
    padding: 0;
    list-style: none;
}
ul.action-overview-list a {
    display: block;
    padding: 16px;
    border-bottom: 1px solid #ddd;
}
ul.action-overview-list a:hover {
    background: #00000010;
}
</style>