package com.bsbeetle.bananaspaghetti.ui.browser
object DevToolsInjector {

    // THIS RUNS FIRST - Captures early console logs
    val EARLY_INJECTION = """
(function() {
    window._earlyLogs = [];

    const original = {
        log: console.log,
        error: console.error,
        warn: console.warn,
        info: console.info,
        debug: console.debug
    };

    function wrap(level, originalFn) {
        return function(...args) {
            const msg = args.map(arg => {
                if (typeof arg === 'object') {
                    try { return JSON.stringify(arg); }
                    catch(e) { return String(arg); }
                }
                return String(arg);
            }).join(' ');

            window._earlyLogs.push({level: level, message: msg});
            try {
                if (window.DevBridge) {
                    window.DevBridge.logConsole(level, msg);
                }
            } catch(e) {}
            originalFn.apply(console, args);
        };
    }

    console.log = wrap('LOG', original.log);
    console.error = wrap('ERROR', original.error);
    console.warn = wrap('WARN', original.warn);
    console.info = wrap('INFO', original.info);
    console.debug = wrap('DEBUG', original.debug);

    console.log('✅ Early console capture active');
})();
""".trimIndent()

    // THIS RUNS AFTER - Loads Eruda (forced full-panel, no draggable)
//    val LATE_INJECTION = """
//(function() {
//    console.log('🚀 DevBrowser: Loading Eruda...');
//
//    // ============================================
//    // LOAD ERUDA
//    // ============================================
//    if (typeof eruda === 'undefined') {
//        const script = document.createElement('script');
//        script.src = 'https://cdn.jsdelivr.net/npm/eruda';
//        script.onload = function() {
//            eruda.init({
//                tool: ['console', 'elements', 'network', 'resources', 'sources', 'info', 'snippets'],
//                useShadowDom: false,
//                autoScale: true,
//                defaults: {
//                    displaySize: 100,     // request full viewport height
//                    transparency: 0.95,
//                    theme: 'dark'
//                }
//            });
//
//            // Ensure eruda is hidden initially, then style it when shown
//            eruda.hide();
//
//            // Inject CSS to force full-size panel and remove the resize handle
//            (function injectErudaStyles() {
//                const css = `
///* Force Eruda root to cover full viewport and dock to bottom without transform */
//#eruda, .eruda {
//    position: fixed !important;
//    top: 0 !important;
//    left: 0 !important;
//    right: 0 !important;
//    bottom: 0 !important;
//    width: 100vw !important;
//    height: 100vh !important;
//    max-height: 100vh !important;
//    transform: none !important;
//    margin: 0 !important;
//    border-radius: 0 !important;
//    z-index: 2147483647 !important; /* very high so it sits above page */
//}
//
///* Make the dev-tools container fill the root */
//#eruda .eruda-dev-tools, .eruda .eruda-dev-tools, #eruda .eruda-wrapper, .eruda .eruda-wrapper {
//    height: 100% !important;
//    max-height: 100% !important;
//    width: 100% !important;
//}
//
///* Make internal panels scroll correctly and occupy full height */
//#eruda .eruda-panel, .eruda .eruda-panel, #eruda .eruda-viewport, .eruda .eruda-viewport {
//    height: 100% !important;
//    max-height: 100% !important;
//}
//
///* Remove/Hide resize handle and any resize-related UI */
//#eruda .eruda-resize, .eruda .eruda-resize, #eruda .eruda-drag, .eruda .eruda-drag {
//    display: none !important;
//    pointer-events: none !important;
//    visibility: hidden !important;
//}
//
///* If Eruda uses transforms or inline styles to set height, override them */
//#eruda[style], .eruda[style] {
//    height: 100vh !important;
//    max-height: 100vh !important;
//}
//
///* Ensure entry button (floating) doesn't overlap awkwardly */
//.eruda-entry-btn {
//    display: none !important;
//}
//
///* Make sure panels' scroll areas use full height */
//.eruda-console .eruda-logs,
//.eruda-elements .eruda-section,
//.eruda-network .eruda-requests,
//.eruda-resources .eruda-section {
//    height: calc(100% - 40px) !important; /* leave small room for headers/tabs */
//    overflow-y: auto !important;
//}
//
///* keep pointer events for the panel itself */
//#eruda, .eruda {
//    pointer-events: auto !important;
//}
//`;
//                const s = document.createElement('style');
//                s.id = 'eruda-forced-fullstyle';
//                s.innerHTML = css;
//                document.head.appendChild(s);
//            })();
//
//            // Replay early logs
//            if (window._earlyLogs) {
//                window._earlyLogs.forEach(log => {
//                    const method = log.level.toLowerCase();
//                    if (console[method]) {
//                        console[method](log.message);
//                    } else {
//                        console.log('[' + log.level + '] ' + log.message);
//                    }
//                });
//                console.log('✅ Replayed ' + window._earlyLogs.length + ' early logs');
//            }
//
//            console.log('✅ Eruda initialized (forced full-panel)');
//        };
//
//        script.onerror = function() {
//            console.error('❌ Failed to load Eruda');
//        };
//
//        (document.head || document.documentElement).appendChild(script);
//    }
//
//    // ============================================
//    // NETWORK INTERCEPTOR
//    // ============================================
//    (function() {
//        const originalFetch = window.fetch;
//        window.fetch = function(...args) {
//            const startTime = Date.now();
//            const url = typeof args[0] === 'string' ? args[0] : args[0]?.url;
//            const method = args[1]?.method || 'GET';
//
//            return originalFetch.apply(this, args)
//                .then(response => {
//                    const duration = Date.now() - startTime;
//                    const clonedResponse = response.clone();
//
//                    clonedResponse.text().then(body => {
//                        try {
//                            window.DevBridge.logNetwork(JSON.stringify({
//                                url: url,
//                                method: method,
//                                status: response.status,
//                                statusText: response.statusText,
//                                duration: duration,
//                                type: 'fetch',
//                                contentType: response.headers.get('content-type') || 'unknown',
//                                bodySize: body.length
//                            }));
//                        } catch(e) {}
//                    }).catch(() => {});
//
//                    return response;
//                })
//                .catch(error => {
//                    console.error('Fetch error:', error);
//                    throw error;
//                });
//        };
//
//        const XHR = XMLHttpRequest.prototype;
//        const originalOpen = XHR.open;
//        const originalSend = XHR.send;
//
//        XHR.open = function(method, url) {
//            this._devData = { method, url, startTime: Date.now() };
//            return originalOpen.apply(this, arguments);
//        };
//
//        XHR.send = function(body) {
//            const xhr = this;
//
//            xhr.addEventListener('load', function() {
//                const duration = Date.now() - xhr._devData.startTime;
//
//                try {
//                    window.DevBridge.logNetwork(JSON.stringify({
//                        url: xhr._devData.url,
//                        method: xhr._devData.method,
//                        status: xhr.status,
//                        statusText: xhr.statusText,
//                        duration: duration,
//                        type: 'xhr',
//                        contentType: xhr.getResponseHeader('content-type') || 'unknown',
//                        bodySize: xhr.responseText?.length || 0
//                    }));
//                } catch(e) {}
//            });
//
//            return originalSend.apply(this, arguments);
//        };
//
//        const OriginalWebSocket = window.WebSocket;
//        window.WebSocket = function(url, protocols) {
//            console.log('🔌 WebSocket:', url);
//            const ws = new OriginalWebSocket(url, protocols);
//
//            ws.addEventListener('message', (event) => {
//                console.log('📩 WS Message:', event.data?.substring(0, 100));
//            });
//
//            return ws;
//        };
//
//        console.log('✅ Network interceptor active');
//    })();
//
//    // ============================================
//    // FRAMEWORK DETECTION
//    // ============================================
//    setTimeout(function() {
//        try {
//            if (window.React) {
//                window.DevBridge.detectFramework('React', window.React.version || 'unknown');
//            } else if (document.querySelector('[data-reactroot], [data-reactid]')) {
//                window.DevBridge.detectFramework('React', 'detected');
//            }
//
//            if (window.Vue) {
//                window.DevBridge.detectFramework('Vue', window.Vue.version || 'unknown');
//            } else if (document.querySelector('[data-v-]') || window.__VUE__) {
//                window.DevBridge.detectFramework('Vue', 'detected');
//            }
//
//            if (window.ng || window.getAllAngularRootElements) {
//                window.DevBridge.detectFramework('Angular', window.ng?.version?.full || 'detected');
//            }
//
//            if (window.__NEXT_DATA__) {
//                window.DevBridge.detectFramework('Next.js', 'detected');
//            }
//
//            if (window.__SVELTE__) {
//                window.DevBridge.detectFramework('Svelte', 'detected');
//            }
//
//            if (window.jQuery || window.$) {
//                window.DevBridge.detectFramework('jQuery', window.jQuery?.fn?.jquery || 'detected');
//            }
//
//            if (window.__APOLLO_CLIENT__) {
//                window.DevBridge.detectFramework('Apollo', 'detected');
//            }
//
//            console.log('✅ Framework detection complete');
//        } catch(e) {
//            console.error('Framework detection error:', e);
//        }
//    }, 1500);
//
//    console.log('🎉 DevBrowser: All tools loaded!');
//})();
//""".trimIndent()
    val LATE_INJECTION = """
(function() {
    console.log('🚀 DevBrowser: Loading Eruda...');

    // ============================================
    // LOAD ERUDA
    // ============================================
    if (typeof eruda === 'undefined') {
        const script = document.createElement('script');
        script.src = 'https://cdn.jsdelivr.net/npm/eruda';
        script.onload = function() {
            eruda.init({
                tool: ['console', 'elements', 'network', 'resources', 'sources', 'info', 'snippets'],
                useShadowDom: false,
                autoScale: true,
                defaults: {
                    displaySize: 50,     // 50% of viewport height
                    transparency: 0.95,
                    theme: 'dark'
                }
            });

            // Ensure eruda is hidden initially
            eruda.hide();

            // Inject CSS to make it cover half screen and keep it draggable
            (function injectErudaStyles() {
                const css = `
/* Position Eruda at bottom half of screen */
#eruda, .eruda {
    position: fixed !important;
    left: 0 !important;
    right: 0 !important;
    bottom: 0 !important;
    width: 100vw !important;
    height: 50vh !important;
    max-height: 50vh !important;
    margin: 0 !important;
    border-radius: 0 !important;
    z-index: 2147483647 !important;
}

/* Make the dev-tools container fill the root */
#eruda .eruda-dev-tools, .eruda .eruda-dev-tools, #eruda .eruda-wrapper, .eruda .eruda-wrapper {
    height: 100% !important;
    max-height: 100% !important;
    width: 100% !important;
}

/* Make internal panels scroll correctly and occupy full height */
#eruda .eruda-panel, .eruda .eruda-panel, #eruda .eruda-viewport, .eruda .eruda-viewport {
    height: 100% !important;
    max-height: 100% !important;
}

/* Keep resize handle visible and functional */
#eruda .eruda-resize, .eruda .eruda-resize {
    display: block !important;
    pointer-events: auto !important;
    visibility: visible !important;
    cursor: ns-resize !important;
    height: 10px !important;
    background: rgba(255, 255, 255, 0.1) !important;
}

/* Hide entry button */
.eruda-entry-btn {
    display: none !important;
}

/* Make sure panels' scroll areas use available height */
.eruda-console .eruda-logs,
.eruda-elements .eruda-section,
.eruda-network .eruda-requests,
.eruda-resources .eruda-section {
    height: calc(100% - 40px) !important;
    overflow-y: auto !important;
}

/* Keep pointer events for the panel itself */
#eruda, .eruda {
    pointer-events: auto !important;
}
`;
                const s = document.createElement('style');
                s.id = 'eruda-forced-fullstyle';
                s.innerHTML = css;
                document.head.appendChild(s);
            })();

            // Replay early logs
            if (window._earlyLogs) {
                window._earlyLogs.forEach(log => {
                    const method = log.level.toLowerCase();
                    if (console[method]) {
                        console[method](log.message);
                    } else {
                        console.log('[' + log.level + '] ' + log.message);
                    }
                });
                console.log('✅ Replayed ' + window._earlyLogs.length + ' early logs');
            }

            console.log('✅ Eruda initialized (half-screen, draggable)');
        };

        script.onerror = function() {
            console.error('❌ Failed to load Eruda');
        };

        (document.head || document.documentElement).appendChild(script);
    }

    // ... rest of the code stays the same (network interceptor, framework detection, etc.)
})();
""".trimIndent()
}
