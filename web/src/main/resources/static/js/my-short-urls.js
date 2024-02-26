(() => {
    'use strict'

    const apiBaseUrl = CONFIG.apiBaseURL
    const bffBaseUrl = CONFIG.bffBaseURL
    const debug = CONFIG.debug === true
    const xsrfToken = getCookieValue("XSRF-TOKEN")

    if (debug) console.log("Using config: " + JSON.stringify(CONFIG))

    // HTTP API
    async function getJwt() {
        const url = new URL(bffBaseUrl + "/jwt")
        return await fetch(url, {
            method: "GET"
        })
            .then(handleApiError)
            .then(response => response.json())
            .then(json => json["token_value"])
    }

    async function getUserShortUrls(page = 0, size = 20) {
        const bearerToken = await getJwt()
        const url = new URL(apiBaseUrl + "/api/v1/shorturl")
        url.searchParams.set("page", page.toString())
        url.searchParams.set("size", size.toString())
        if (debug) console.log("GET " + url)
        return await fetch(url, {
            method: "GET",
            headers: {"Authorization": "Bearer " + bearerToken},
        })
            .then(handleApiError)
            .then(response => response.json())
            .then(json => {
                if (debug) console.log(json)
                return json.data
            })
    }

    async function createShortUrl(obj) {
        const bearerToken = await getJwt()
        const url = new URL(apiBaseUrl + "/api/v1/shorturl")
        if (debug) console.log("POST " + url)
        return await fetch(url, {
            method: "POST",
            headers: {"Authorization": "Bearer " + bearerToken, "Content-Type": "application/json"},
            body: JSON.stringify(obj)
        })
            .then(handleApiError)
            .then(response => response.json())
            .then(json => {
                if (debug) console.log(json)
                return json.data
            })
    }

    async function deleteShortUrl(id) {
        const bearerToken = await getJwt()
        const url = new URL(apiBaseUrl + "/api/v1/shorturl/" + id)
        if (debug) console.log("DELETE " + url)
        return await fetch(url, {
            method: "DELETE",
            headers: {"Authorization": "Bearer " + bearerToken},
        })
            .then(handleApiError)
            .then(() => {
                if (debug) console.log("ok")
            })
    }

    async function handleApiError(response) {
        if (!response.ok) {
            console.log(response)
            throw new Error(response.statusText)
        }
        return response
    }

    // DOM
    const domParser = new DOMParser()

    function getCookieValue(name) {
        const regex = new RegExp(`(^| )${name}=([^;]+)`)
        const match = document.cookie.match(regex)
        if (match) {
            return match[2]
        }
    }

    async function refreshMyShortUrls(pageNumber, pageSize) {
        const size = document.getElementById("my-short-urls-pgsize").value
        const page = parseInt(document.querySelector(
            "#my-short-urls-nav .active .page-link").textContent) - 1
        return await getUserShortUrls(pageNumber === undefined ? page : pageNumber,
            pageSize === undefined ? size : pageSize)
            .then(json => Promise.all([
                showMyShortUrls(json, document.getElementById("my-short-urls")),
                showMyShortUrlsNav(json, document.getElementById("my-short-urls-nav")),
                showMyShortUrlsPgInfo(json, document.getElementById("my-short-urls-pginfo"))
            ]))
    }

    async function showMyShortUrls(json, table) {
        if (json === undefined || json.content === undefined) {
            return
        } else if (json.content.length === 0) {
            return await showNoDataRow()
        }

        let template = ""
        json.content.forEach(shortUrl => {
            template +=
`<tr>
    <td>${shortUrl.id}</td>
    <td>
        <a href="#" data-url="${bffBaseUrl + "/u/" + shortUrl.key}" data-bs-toggle="popover" data-bs-trigger="manual" 
          data-bs-content="Copied!" data-bs-placement="left" data-bs-custom-class="my-short-urls">
          <i class="bi bi-copy me-1"></i></a>
        <a href="${bffBaseUrl + "/u/" + shortUrl.key}" target="_blank">${bffBaseUrl + "/u/" + shortUrl.key}</a>
    </td>
    <td>
        <span class="text-truncate fit" data-bs-toggle="tooltip" data-bs-title="${shortUrl.url}" 
            data-bs-custom-class="my-short-urls">${shortUrl.url}</span>
    </td>
    <td>
        ${shortUrl.description === undefined ? "" : `
        <span class="text-truncate fit" data-bs-toggle="tooltip" data-bs-title="${shortUrl.description}" 
            data-bs-custom-class="my-short-urls">${shortUrl.description}</span>
        `}
    </td>
    <td>${new Date(shortUrl.created_at).toLocaleString()}</td>
    <td>${new Date(shortUrl.expires_at).toLocaleString()}</td>
    <td>
        <a href="#" data-id="${shortUrl.id}" data-bs-toggle="modal" data-bs-target="#short-url-delete-modal">
            <i class="bi bi-trash text-danger"></i>
        </a>
    </td>
</tr>`})
        const doc = domParser.parseFromString("<table>"+template+"</table>", "text/html")
        const elements = doc.querySelectorAll("tr")
        const tbody = table.querySelector("tbody")
        tbody.innerHTML = ""
        tbody.append(...elements)

        return [...document.querySelectorAll('[data-bs-toggle="tooltip"]')]
            .map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl))
    }

    async function showMyShortUrlsNav(json, el) {
        let template = ""
        template += `
<li class="page-item${json.first ? " disabled" : ""}">
    <a class="page-link previous" href="#" aria-label="Previous">
        <span aria-hidden="true">&laquo;</span>
    </a>
</li>`
        for (let i = 0; i < json.total_pages; i++) {
            const active = (i === json.number ? " active" : "")
            template += `<li class="page-item${active}"><a class="page-link" href="#">${i+1}</a></li>`
        }
        template += `
<li class="page-item${json.last ? " disabled" : ""}">
    <a class="page-link next" href="#" aria-label="Next">
        <span aria-hidden="true">&raquo;</span>
    </a>
</li>`
        const doc = domParser.parseFromString("<ul>"+template+"</ul>", "text/html")
        const elements = doc.querySelectorAll("li")
        el.innerHTML = ""
        el.append(...elements)
    }

    async function showMyShortUrlsPgInfo(json, el) {
        const x = json.number * json.size + 1
        const y = x + json.number_of_elements - 1
        el.innerText = x + " - " + y + " of " + json.total_elements
    }

    async function showNoDataRow() {
        const template = `
<tr id="no-short-urls">
    <td colspan=7>
        <div class="container bg-white">
            <div class="position-relative p-5 text-center text-muted">
                <h1 class="text-body-emphasis">No Short URLs</h1>
                <div class="my-3"><i class="bi bi-table h1"></i></div>
                <p class="col-lg-6 mx-auto mb-4">
                    Add a Short URL to begin!
                </p>
            </div>
        </div>
    </td>
</tr>`
        const table = document.getElementById("my-short-urls")
        const doc = domParser.parseFromString("<table>"+template+"</table>", "text/html")
        const el = doc.querySelector("tr")
        const tbody = table.querySelector("tbody")
        tbody.innerHTML = ""
        tbody.append(el)
    }

    async function showTimedTooltip(el, callback) {
        const popover = await new bootstrap.Popover(el)
        popover.show()
        setTimeout(function() {
            popover.hide()
            if (callback !== undefined) {
                callback()
            }
        }, 1500)
    }

    async function showToast(msg) {
        const template = `
<div class="toast fade bg-white" role="alert" aria-live="assertive" aria-atomic="true">
    <div class="toast-header">
        <img src="/images/favicon.ico" width="30" height="30" class="rounded me-2" alt="URL Shortener">
        <strong class="me-auto">URL Shortener</strong>
        <small class="text-body-secondary">Just now</small>
        <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
    </div>
    <div class="toast-body">${msg}</div>
</div>`
        const doc = domParser.parseFromString(template, "text/html")
        const el = doc.body.firstChild
        document.getElementById("notifications").appendChild(el)
        return await bootstrap.Toast.getOrCreateInstance(el).show()
    }

    document.getElementById("apidocs-link").setAttribute("href",
        apiBaseUrl + "/api/v1/docs/swagger-ui/index.html")

    document.getElementById("logout-link").setAttribute("href",
        bffBaseUrl + "/logout")

    const createUrlInputUrl = document.getElementById("create-url-input-url");
    async function customValidateUrl(e) {
        const VALID_URL_REGEX = "^((((https?|ftps?|gopher|telnet|nntp)://)|(mailto:|news:))" +
            "(%[0-9A-Fa-f]{2}|[-()_.!~*';/?:@&=+$,A-Za-z0-9])+)([).!';/?:,][[:blank:]])?$" // Same as BE
        const validUrlRegex = new RegExp(VALID_URL_REGEX)

        let valid
        try {
            valid = validUrlRegex.test(createUrlInputUrl.value)
        } catch (err) {
            valid = false
        }

        if (valid) {
            createUrlInputUrl.setCustomValidity("")
        } else {
            createUrlInputUrl.setCustomValidity("Invalid")
            e.preventDefault()
        }
        return valid
    }
    createUrlInputUrl.addEventListener("input", customValidateUrl);

    const createShortUrlForm = document.getElementById("create-short-url-form")
    createShortUrlForm.addEventListener("submit", async function(e) {
        e.preventDefault()

        await customValidateUrl(e)
        if (!this.checkValidity()) {
            e.stopPropagation()
            this.classList.add("was-validated")
            return
        }
        this.classList.add("was-validated")

        const url = document.getElementById("create-url-input-url").value
        const description = document.getElementById("create-url-input-desc").value
        return await createShortUrl({url: url, description: !description?null:description})
            .then(json => showToast("Created Short URL " + json.id))
            .then(() => refreshMyShortUrls())
            .then(() => this.classList.remove("was-validated"))
            .then(() => document.getElementById("create-short-url-form").reset())
    }, false)

    document.getElementById("short-url-delete").addEventListener("click", async function() {
        const modalEl = document.getElementById("short-url-delete-modal")
        const id = modalEl.getElementsByClassName("short-url-id")[0].textContent
        return await deleteShortUrl(id)
            .then(() => bootstrap.Modal.getOrCreateInstance(modalEl).hide())
            .then(() => showToast("Short URL " + id + " has been deleted"))
            .then(() => {
                if (document.querySelectorAll("#my-short-urls tr").length === 2) {
                    // Go back a page
                    const activeEl = document.querySelector("#my-short-urls-nav .active")
                    activeEl.previousElementSibling.classList.add("active")
                    activeEl.classList.remove("active")
                }
                return refreshMyShortUrls()
            })
    })

    document.getElementById("my-short-urls").addEventListener("click", async function(e) {
        if (e.target && e.target.nodeName === "I" && e.target.classList.contains("bi-copy")) {
            e.preventDefault()
            const linkTarget = e.target.parentElement
            const text = linkTarget.dataset.url
            if (text !== undefined) {
                return await navigator.clipboard.writeText(text)
                    .then(() => {
                        e.target.classList.replace("bi-copy","bi-check")
                        e.target.classList.add("text-success")
                        showTimedTooltip(linkTarget, function () {
                            e.target.classList.replace("bi-check","bi-copy")
                            e.target.classList.remove("text-success")
                        })
                    })
            }
        } else if (e.target && e.target.nodeName === "I" && e.target.classList.contains("bi-trash")) {
            e.preventDefault()
            const linkTarget = e.target.parentElement
            const id = linkTarget.dataset.id
            if (id !== undefined) {
                const modalEl = document.getElementById("short-url-delete-modal")
                modalEl.getElementsByClassName("short-url-id")[0].textContent = id
            }
        }
    })

    document.getElementById("my-short-urls-nav").addEventListener("click", async function(e) {
        const target = e.target.closest(".page-link")
        if (target) {
            e.preventDefault()

            const activeLink = document.querySelector("#my-short-urls-nav .active .page-link")
            const active = parseInt(activeLink.textContent) - 1
            const rows = document.getElementById("my-short-urls-pgsize").value

            if (target.classList.contains("previous")) {
                return await refreshMyShortUrls(active - 1, rows)
            } else if (target.classList.contains("next")) {
                return await refreshMyShortUrls(active + 1, rows)
            } else {
                const toPage = parseInt(target.textContent) - 1
                return await refreshMyShortUrls(toPage, rows)
            }
        }
    })

    document.getElementById("my-short-urls-pgsize").addEventListener("change", async function() {
        return await refreshMyShortUrls(0, this.value)
    })

    refreshMyShortUrls().then()
})()
