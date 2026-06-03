// === Fixed header ===
// bannerHeader sits directly below the 6px red stripe (body::before).
// contentPanel padding-top is set once bannerHeader height has stabilised.
(function () {
    function applyFixedHeaders() {
        const bannerHeader = document.querySelector('.bannerHeader');
        const contentPanel = document.querySelector('.contentPanel');
        if (!bannerHeader || !contentPanel) return;

        bannerHeader.style.top = '6px';

        let lastBottom = 0;
        function measure() {
            void bannerHeader.offsetHeight;
            const bhBottom = Math.round(bannerHeader.getBoundingClientRect().bottom);
            if (bhBottom <= 6 || bhBottom !== lastBottom) {
                lastBottom = bhBottom;
                setTimeout(measure, 50);
                return;
            }
            contentPanel.style.paddingTop = bhBottom + 'px';
        }
        measure();
    }

    const headerObserver = new MutationObserver(() => {
        if (document.querySelector('.bannerHeader') &&
            document.querySelector('.contentPanel')) {
            headerObserver.disconnect();
            setTimeout(applyFixedHeaders, 0);
        }
    });

    document.addEventListener('DOMContentLoaded', () => {
        headerObserver.observe(document.body, { childList: true, subtree: true });
    });
})();

document.addEventListener('DOMContentLoaded', () => {
    let footerProcessed = false;

    const observer = new MutationObserver(() => {
        if (footerProcessed) return;

        const footer = document.querySelector('.footer');
        if (footer) {
            footerProcessed = true;
            observer.disconnect();
            loadVersionInfo();
        }
    });

    observer.observe(document.body, {
        childList: true,
        subtree: true
    });

    async function loadVersionInfo() {
        const pathname = window.location.pathname;
        try {
            const response = await fetch(pathname + "version.json");
            if (!response.ok) {
                throw new Error('Failed to load version.json');
            }
            const data = await response.json();
            if (data && data["git.build.version"]) {
                const version = data["git.build.version"];
                const versionDiv = document.querySelector('div#version');
                if (versionDiv) {
                    const versionElement = document.createElement('div');
                    versionElement.style.color = 'rgba(255, 255, 255, 0.5)';
                    versionElement.className = 'built_time';
                    versionElement.textContent = `Version ${version}`;
                    versionDiv.appendChild(versionElement);
                }
                const footerProduct = document.querySelector('span.eterna-footer__product');
                if (footerProduct) {
                    footerProduct.textContent = `ETERNA v${version}`;
                }
            }
        } catch (error) {
            console.warn("Failed to load version.json:", error);
        }
    }
});

// === Welcome page: personalized greeting + inline search ===
(function () {
    function escapeHtml(s) {
        return String(s)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function initWelcome() {
        const labelSv = document.getElementById('eterna-greeting-label');
        const labelEn = document.getElementById('eterna-greeting-label-en');
        const label = labelSv || labelEn;

        if (!label || label.dataset.welcomeInit) return;
        label.dataset.welcomeInit = '1';

        const isSv = !!labelSv;
        const submitId = isSv ? 'eterna-welcome-submit' : 'eterna-welcome-submit-en';
        const inputId  = isSv ? 'eterna-welcome-q'      : 'eterna-welcome-q-en';
        const resultsId = isSv ? 'eterna-welcome-results' : 'eterna-welcome-results-en';

        const btn     = document.getElementById(submitId);
        const inp     = document.getElementById(inputId);
        const results = document.getElementById(resultsId);

        // --- Personalized greeting ---
        function timeGreeting(isSv) {
            const h = new Date().getHours();
            if (isSv) {
                if (h >= 5  && h < 12) return 'Godmorgon';
                if (h >= 12 && h < 18) return 'Godmiddag';
                if (h >= 18 && h < 23) return 'Godkväll';
                return 'Godnatt';
            } else {
                if (h >= 5  && h < 12) return 'Good morning';
                if (h >= 12 && h < 18) return 'Good afternoon';
                if (h >= 18 && h < 23) return 'Good evening';
                return 'Good night';
            }
        }

        fetch('api/v2/members/users/authenticated', { credentials: 'same-origin' })
            .then(r => r.ok ? r.json() : null)
            .then(u => {
                if (u && !u.guest && u.fullName) {
                    const firstName = u.fullName.split(' ')[0];
                    label.textContent = timeGreeting(isSv) + ', ' + firstName;
                }
            })
            .catch(() => {});

        if (!btn || !inp) return;

        // --- Inline search ---
        function doSearch(q) {
            if (!results) return;
            if (!q) {
                results.innerHTML = '';
                results.classList.remove('is-open');
                return;
            }
            results.innerHTML = '<p class="eterna-welcome__results-status">'
                + (isSv ? 'Söker\u2026' : 'Searching\u2026') + '</p>';
            results.classList.add('is-open');

            fetch('api/v2/aips/find', {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    filter: {
                        parameters: [{ type: 'BasicSearchFilterParameter', name: 'search', value: q }]
                    },
                    onlyActive: true,
                    sublist: { firstElementIndex: 0, maximumElementCount: 6 }
                })
            })
            .then(r => r.ok ? r.json() : null)
            .then(data => {
                const objects = data && (data.objects || data.results);
                if (!objects || objects.length === 0) {
                    results.innerHTML = '<p class="eterna-welcome__results-status">'
                        + (isSv
                            ? 'Inga tr\u00e4ffar f\u00f6r \u201c' + escapeHtml(q) + '\u201d.'
                            : 'No results for \u201c' + escapeHtml(q) + '\u201d.')
                        + '</p>';
                    return;
                }
                let html = '<ul class="eterna-welcome__result-list">';
                objects.forEach(aip => {
                    const title = (aip.title && (Array.isArray(aip.title) ? aip.title[0] : aip.title)) || aip.id;
                    html += '<li class="eterna-welcome__result-item">'
                        + '<a href="#browse/' + encodeURIComponent(aip.id) + '">'
                        + escapeHtml(title) + '</a></li>';
                });
                html += '</ul>';
                const total = data.totalCount || data.total || objects.length;
                if (total > objects.length) {
                    html += '<a class="eterna-welcome__results-more" href="#search/q/'
                        + encodeURIComponent(q) + '">'
                        + (isSv ? 'Visa alla ' + total + ' tr\u00e4ffar \u2192' : 'Show all ' + total + ' results \u2192')
                        + '</a>';
                }
                results.innerHTML = html;
            })
            .catch(() => {
                if (results) {
                    results.innerHTML = '<p class="eterna-welcome__results-status">'
                        + (isSv ? 'S\u00f6kning misslyckades.' : 'Search failed.') + '</p>';
                }
            });
        }

        btn.addEventListener('click', e => {
            const q = inp.value.trim();
            if (q) { e.preventDefault(); doSearch(q); }
        });

        inp.addEventListener('keydown', e => {
            if (e.key === 'Enter') {
                const q = inp.value.trim();
                if (q) { e.preventDefault(); doSearch(q); }
            }
        });

        inp.addEventListener('input', () => {
            if (!inp.value.trim() && results) {
                results.textContent = '';
                results.classList.remove('is-open');
            }
        });
    }

    // Watch for welcome hero to be inserted into DOM (GWT loads it asynchronously)
    const welcomeObserver = new MutationObserver(() => {
        const label = document.getElementById('eterna-greeting-label')
                  || document.getElementById('eterna-greeting-label-en');
        if (label && !label.dataset.welcomeInit) {
            initWelcome();
            welcomeObserver.disconnect();
        }
    });

    document.addEventListener('DOMContentLoaded', () => {
        welcomeObserver.observe(document.body, { childList: true, subtree: true });
        initWelcome(); // in case it's already there
    });
})();

// === Aside: live archive statistics ===
(function () {
    const FIND_BODY = JSON.stringify({
        filter: { parameters: [{ type: 'AllFilterParameter' }] },
        sublist: { firstElementIndex: 0, maximumElementCount: 0 }
    });

    function fmt(n) {
        return typeof n === 'number' ? n.toLocaleString('sv-SE') : '—';
    }

    function fetchCount(endpoint) {
        return fetch(endpoint, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: FIND_BODY
        })
        .then(r => r.ok ? r.json() : null)
        .then(d => (d && typeof d.totalCount === 'number') ? d.totalCount : null)
        .catch(() => null);
    }

    function initAside() {
        const aipEl  = document.getElementById('eterna-stat-aips')
                    || document.getElementById('eterna-stat-aips-en');
        if (!aipEl || aipEl.dataset.asideInit) return;
        aipEl.dataset.asideInit = '1';

        const isSv = !!document.getElementById('eterna-stat-aips');
        const repEl  = document.getElementById(isSv ? 'eterna-stat-representations' : 'eterna-stat-representations-en');
        const fileEl = document.getElementById(isSv ? 'eterna-stat-files'            : 'eterna-stat-files-en');

        Promise.all([
            fetchCount('api/v2/aips/find'),
            fetchCount('api/v2/representations/find'),
            fetchCount('api/v2/files/find')
        ]).then(([aips, reps, files]) => {
            if (aipEl)  aipEl.textContent  = aips  !== null ? fmt(aips)  : '—';
            if (repEl)  repEl.textContent  = reps  !== null ? fmt(reps)  : '—';
            if (fileEl) fileEl.textContent = files !== null ? fmt(files) : '—';
        });
    }

    const asideObserver = new MutationObserver(initAside);
    document.addEventListener('DOMContentLoaded', () => {
        asideObserver.observe(document.body, { childList: true, subtree: true });
        initAside();
    });
})();

// === Forum activity: fetch latest discussions from eterna.whitered.se ===
(function () {
    function timeAgo(isoString, isSv) {
        const diff = Math.floor((Date.now() - new Date(isoString)) / 1000);
        if (diff < 60)    return isSv ? 'just nu'        : 'just now';
        if (diff < 3600)  return Math.floor(diff / 60)   + (isSv ? ' min sedan'                          : ' min ago');
        if (diff < 86400) return Math.floor(diff / 3600) + (isSv ? ' tim sedan'                          : ' h ago');
        const d = Math.floor(diff / 86400);
        return d + (isSv ? (' dag' + (d !== 1 ? 'ar' : '') + ' sedan') : (' day' + (d !== 1 ? 's' : '') + ' ago'));
    }

    function renderForumActivity(listEl, isSv) {
        if (!listEl || listEl.dataset.forumInit) return;
        listEl.dataset.forumInit = '1';

        fetch('api/v2/forum/latest', { credentials: 'same-origin' })
            .then(r => r.ok ? r.json() : null)
            .then(posts => {
                if (!posts || posts.length === 0) {
                    listEl.innerHTML = '<li class="eterna-activity__item"><div class="eterna-activity__body">'
                        + '<p class="eterna-activity__title">' + (isSv ? 'Inga trådar hittades.' : 'No threads found.') + '</p>'
                        + '</div></li>';
                    return;
                }
                const frag = document.createDocumentFragment();
                posts.forEach(p => {
                    const replies = p.commentCount - 1; // commentCount includes the first post
                    const replyLabel = isSv
                        ? (replies === 1 ? '1 svar' : replies + ' svar')
                        : (replies === 1 ? '1 reply' : replies + ' replies');

                    let safeUrl = null;
                    try {
                        const u = new URL(p.url);
                        if (u.protocol === 'http:' || u.protocol === 'https:') safeUrl = u.href;
                    } catch (_) {}

                    const li    = document.createElement('li');
                    li.className = 'eterna-activity__item';
                    const body  = document.createElement('div');
                    body.className = 'eterna-activity__body';
                    const titleP = document.createElement('p');
                    titleP.className = 'eterna-activity__title';
                    const a = document.createElement('a');
                    if (safeUrl) a.setAttribute('href', safeUrl);
                    a.setAttribute('target', '_blank');
                    a.setAttribute('rel', 'noopener noreferrer');
                    a.textContent = p.title;
                    titleP.appendChild(a);
                    const descP = document.createElement('p');
                    descP.className = 'eterna-activity__desc';
                    descP.textContent = replyLabel + ' · ' + timeAgo(p.lastPostedAt, isSv);
                    body.appendChild(titleP);
                    body.appendChild(descP);
                    li.appendChild(body);
                    frag.appendChild(li);
                });
                listEl.innerHTML = '';
                listEl.appendChild(frag);
            })
            .catch(() => {
                listEl.innerHTML = '<li class="eterna-activity__item"><div class="eterna-activity__body">'
                    + '<p class="eterna-activity__title">' + (isSv ? 'Kunde inte ladda forumtrådar.' : 'Could not load forum threads.') + '</p>'
                    + '</div></li>';
            });
    }

    function initForumActivity() {
        const svEl = document.getElementById('eterna-forum-activity');
        const enEl = document.getElementById('eterna-forum-activity-en');
        if (svEl) renderForumActivity(svEl, true);
        if (enEl) renderForumActivity(enEl, false);
    }

    const forumObserver = new MutationObserver(initForumActivity);
    document.addEventListener('DOMContentLoaded', () => {
        forumObserver.observe(document.body, { childList: true, subtree: true });
        initForumActivity();
    });
})();
