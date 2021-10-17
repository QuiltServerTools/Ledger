<!--suppress JSAnnotator -->
<template id="search_results">
    <div class="container">
        <table class="table table-dark mx-auto table-bg-dark table-hover" style="width: 75%;">
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
            <tr v-for="action in this.actions">
                <td>{{ action.time }}</td>
                <td>{{ action.world }}</td>
                <td>{{ action.x }} {{ action.y }} {{ action.z }}</td>
                <td>{{ action.identifier }}</td>
                <td v-if="action.sourceProfile !==''">{{ action.sourceProfile }}</td>
                <td v-else>{{ '@' + action.sourceName }}</td>
                <td v-if="action.objectString != null">{{ action.objectString }}</td>
                <td v-else>{{ action.oldObjectString }}</td>
            </tr>
            </tbody>
        </table>
        <div class="btn-toolbar" role="toolbar">
            <div class="btn-group" role="group" aria-label="Pages">
                <button class="btn btn-outline-info" type="button" v-on:click="showPage(getActivePage() - 1)">
                    Previous page
                </button>
                <button class="btn btn-outline-info" type="button" v-on:click="showPage(getActivePage() - 10)">&lt&lt
                </button>
                <button type="button" v-for="i in this.pages"
                        :class="{ 'active': (isActive(i)), 'btn': true, 'btn-outline-info': true }"
                        v-if="shouldShowPageSelector(i)"
                        v-on:click="showPage(i)"
                >{{ i }}
                </button>
                <button class="btn btn-outline-info" type="button" v-on:click="showPage(getActivePage() + 10)">&gt&gt
                </button>
                <button class="btn btn-outline-info" type="button" v-on:click="showPage(getActivePage() + 1)">Next
                    page
                </button>
            </div>
        </div>
    </div>
</template>
<script>

Vue.component("search_results", {
    template: "#search_results",
    data: () => ({
        actions: [],
        pages: 1,
        page: 1
    }),
    methods: {
        showPage: function (page) {
            this.page = page

            if (this.page <= 0) {
                this.page = 1
            } else if (this.page > this.pages) {
                this.page = this.pages
            }
            fetch("/api/search?page=" + this.page + "&" + window.location.href.substr(window.location.href.indexOf('?') + 1))
                    .then(res => res.json())
                    .then(res => this.actions = res)
                    .catch(() => alert("Error while fetching actions"));
        },
        // For whether to render a button as active
        isActive: function (i) {
            return i === this.page
        },
        // Ensures that only 10 pages are shown in selector bar at once
        shouldShowPageSelector: function (i) {
            let ten = Math.ceil(i / 10) * 10;
            return Math.ceil(this.page / 10) * 10 === ten
        },
        getActivePage: function () {
            return this.page
        }
    },
    created() {
        fetch("/api/searchinit?" + window.location.href.substr(window.location.href.indexOf('?') + 1))
                .then(res => res.json())
                .then(res => this.pages = res.pages)
                .then(() => this.showPage(1))
                .catch(() => alert("No results found"));
    }
});
</script>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.1/dist/css/bootstrap.min.css" rel="stylesheet"
      integrity="sha384-F3w7mX95PdgyTmZZMECAngseQB83DfGTowi0iMjiWaeVhAn4FJkqJByhZMI3AhiU" crossorigin="anonymous">
<link rel="stylesheet" type="text/css" href="../resources/ledger.css">