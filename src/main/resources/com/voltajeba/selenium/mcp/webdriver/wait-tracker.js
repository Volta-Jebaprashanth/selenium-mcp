// Tracks in-flight fetch/XHR requests and CSS/Web animations, so a caller can wait until the page
// has settled. The MCP server's auto-waits run this exact script, and the Waits helper that
// getWaitCodeSnippets hands out embeds it too, so a wait that passes in the server passes in the
// generated test. Idempotent: running it again on a page that has it only refreshes the ignore list.
// Set window.__seleniumMcpWaitIgnore to an array of URL substrings beforehand to leave requests
// (analytics, long polling, ...) out of the in-flight count.
(function () {
    var existing = window.__seleniumMcpWait;
    if (existing) {
        existing.ignore = window.__seleniumMcpWaitIgnore || existing.ignore;
        return;
    }
    var t = {
        ignore: window.__seleniumMcpWaitIgnore || [],
        pending: 0,
        requests: 0,
        animations: 0,
        lastNetwork: Date.now(),
        lastAnimation: Date.now()
    };

    function ignored(url) {
        url = String(url || '');
        for (var i = 0; i < t.ignore.length; i++) {
            if (t.ignore[i] && url.indexOf(t.ignore[i]) >= 0) return true;
        }
        return false;
    }

    function started(url) {
        if (ignored(url)) return false;
        t.pending++;
        t.requests++;
        t.lastNetwork = Date.now();
        return true;
    }

    function finished() {
        t.pending = Math.max(0, t.pending - 1);
        t.lastNetwork = Date.now();
    }

    if (window.fetch) {
        var originalFetch = window.fetch;
        window.fetch = function (input) {
            var tracked = started(input && input.url ? input.url : input);
            var result;
            try {
                result = originalFetch.apply(this, arguments);
            } catch (e) {
                if (tracked) finished();
                throw e;
            }
            if (tracked) result.then(finished, finished);
            return result;
        };
    }

    if (window.XMLHttpRequest) {
        var originalOpen = XMLHttpRequest.prototype.open;
        var originalSend = XMLHttpRequest.prototype.send;
        XMLHttpRequest.prototype.open = function (method, url) {
            this.__seleniumMcpUrl = url;
            return originalOpen.apply(this, arguments);
        };
        XMLHttpRequest.prototype.send = function () {
            var tracked = started(this.__seleniumMcpUrl);
            if (tracked) this.addEventListener('loadend', finished);
            try {
                return originalSend.apply(this, arguments);
            } catch (e) {
                if (tracked) {
                    this.removeEventListener('loadend', finished);
                    finished();
                }
                throw e;
            }
        };
    }

    // Repeating animations (spinners, pulsing badges) never finish, so they are left out; a loading
    // spinner is covered by the network wait instead.
    function repeatsForever(event) {
        if (event.type !== 'animationstart' || !event.target || !window.getComputedStyle) return false;
        return String(window.getComputedStyle(event.target).animationIterationCount).indexOf('infinite') >= 0;
    }

    function animationStarted(event) {
        if (repeatsForever(event)) return;
        t.animations++;
        t.lastAnimation = Date.now();
    }

    function animationEnded() {
        t.lastAnimation = Date.now();
    }

    document.addEventListener('animationstart', animationStarted, true);
    document.addEventListener('transitionrun', animationStarted, true);
    ['animationend', 'animationcancel', 'transitionend', 'transitioncancel'].forEach(function (type) {
        document.addEventListener(type, animationEnded, true);
    });

    function runningAnimations() {
        if (!document.getAnimations) return 0;
        return document.getAnimations().filter(function (a) {
            if (a.playState !== 'running') return false;
            var timing = a.effect && a.effect.getComputedTiming ? a.effect.getComputedTiming() : null;
            return !timing || timing.iterations !== Infinity;
        }).length;
    }

    // Starts a new step: counts restart, and the page must stay quiet for the full idle time from now.
    t.mark = function () {
        t.requests = 0;
        t.animations = 0;
        t.lastNetwork = Date.now();
        t.lastAnimation = Date.now();
    };

    // Called as a wait begins, so the page must stay quiet for the full idle time from then on: a
    // request an action sets off a moment later still counts.
    t.restartIdle = function (what) {
        var now = Date.now();
        if (what !== 'animations') t.lastNetwork = Math.max(t.lastNetwork, now);
        if (what !== 'network') t.lastAnimation = Math.max(t.lastAnimation, now);
    };

    t.networkIdle = function (idleMillis) {
        return t.pending === 0 && Date.now() - t.lastNetwork >= idleMillis;
    };

    t.animationsIdle = function (idleMillis) {
        var running = runningAnimations();
        if (running > 0) {
            // Web Animations API animations (element.animate) fire no events; count the ones seen here.
            t.animations = Math.max(t.animations, running);
            t.lastAnimation = Date.now();
            return false;
        }
        return Date.now() - t.lastAnimation >= idleMillis;
    };

    t.status = function (networkIdleMillis, animationIdleMillis) {
        return {
            networkIdle: t.networkIdle(networkIdleMillis),
            animationsIdle: t.animationsIdle(animationIdleMillis),
            pending: t.pending,
            requests: t.requests,
            animations: t.animations
        };
    };

    window.__seleniumMcpWait = t;
})();
