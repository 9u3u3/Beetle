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

    // THIS RUNS AFTER - Loads Eruda with adaptive sizing for portrait/landscape
    val LATE_INJECTION = """
(function() {
    console.log('🚀 DevBrowser: Loading Eruda...');

    // ============================================
    // DETECT ORIENTATION & SET OPTIMAL SIZE
    // ============================================
    function getOptimalSize() {
        const isPortrait = window.innerHeight > window.innerWidth;
        const isLandscape = window.innerWidth > window.innerHeight;
        
        // Portrait: use less height (40%) since vertical space is limited
        // Landscape: use more height (50%) since we have more vertical space
        return {
            height: isPortrait ? 40 : 50,
            position: 'bottom' // Always at bottom for easier thumb access on mobile
        };
    }

    // ============================================
    // LOAD ERUDA
    // ============================================
    if (typeof eruda === 'undefined') {
        const script = document.createElement('script');
        script.src = 'https://cdn.jsdelivr.net/npm/eruda';
        script.onload = function() {
            const optimalSize = getOptimalSize();
            
            eruda.init({
                tool: ['console', 'elements', 'network', 'resources', 'sources', 'info', 'snippets'],
                useShadowDom: false,
                autoScale: true,
                defaults: {
                    displaySize: optimalSize.height,
                    transparency: 0.95,
                    theme: 'dark'
                }
            });

            eruda.hide();

            // Inject responsive CSS
            (function injectErudaStyles() {
                const isPortrait = window.innerHeight > window.innerWidth;
                const initialHeight = isPortrait ? 40 : 50;
                
                const css = `
/* Base Eruda positioning - responsive */
#eruda, .eruda {
    position: fixed !important;
    left: 0 !important;
    right: 0 !important;
    bottom: 0 !important;
    width: 100vw !important;
    height: ${'$'}{initialHeight}vh !important;
    margin: 0 !important;
    border-radius: 0 !important;
    z-index: 2147483647 !important;
    transform: none !important;
    transition: height 0.3s ease !important;
}

/* Dev-tools container - adaptive sizing */
#eruda .eruda-dev-tools, .eruda .eruda-dev-tools,
#eruda .eruda-wrapper, .eruda .eruda-wrapper {
    height: ${'$'}{initialHeight}% !important;
    max-height: 100% !important;
    width: 100% !important;
    transition: height 0.3s ease !important;
}

/* Make internal panels scroll correctly */
#eruda .eruda-panel, .eruda .eruda-panel,
#eruda .eruda-viewport, .eruda .eruda-viewport {
    height: 100% !important;
    max-height: 100% !important;
}

/* Hide Eruda's default resize handle */
#eruda .eruda-resize, .eruda .eruda-resize {
    display: none !important;
}

/* Hide entry button */
.eruda-entry-btn {
    display: none !important;
}

/* Panel scroll areas */
.eruda-console .eruda-logs,
.eruda-elements .eruda-section,
.eruda-network .eruda-requests,
.eruda-resources .eruda-section {
    height: calc(100% - 40px) !important;
    overflow-y: auto !important;
}

/* Keep pointer events */
#eruda, .eruda {
    pointer-events: auto !important;
}

/* Responsive button styling */
@media (max-width: 600px) {
    #eruda-resize-buttons button {
        width: 36px !important;
        height: 36px !important;
        font-size: 22px !important;
    }
}

@media (orientation: landscape) and (max-height: 500px) {
    #eruda-resize-buttons {
        top: 4px !important;
    }
    #eruda-resize-buttons button {
        width: 28px !important;
        height: 28px !important;
        font-size: 18px !important;
    }
}
`;
                const s = document.createElement('style');
                s.id = 'eruda-adaptive-style';
                s.innerHTML = css;
                document.head.appendChild(s);
            })();

            // Add enhanced resize buttons with orientation awareness
            (function addResizeButtons() {
                const isPortrait = window.innerHeight > window.innerWidth;
                let currentHeight = isPortrait ? 40 : 50; // Adaptive initial height

                // Wait for Eruda dev-tools wrapper to be in DOM
                const checkInterval = setInterval(() => {
                    const devToolsEl = document.querySelector('#eruda .eruda-dev-tools') ||
                                      document.querySelector('.eruda .eruda-dev-tools') ||
                                      document.querySelector('#eruda .eruda-wrapper') ||
                                      document.querySelector('.eruda .eruda-wrapper');
                    
                    const erudaContainer = document.getElementById('eruda') || document.querySelector('.eruda');

                    if (devToolsEl && erudaContainer) {
                        clearInterval(checkInterval);

                        // Create button container
                        const buttonContainer = document.createElement('div');
                        buttonContainer.id = 'eruda-resize-buttons';
                        buttonContainer.style.cssText = `
                            position: absolute;
                            top: 6px;
                            right: 45px;
                            display: flex;
                            gap: 6px;
                            z-index: 999999;
                            user-select: none;
                        `;

                        // Create - button (decrease height)
                        const minusBtn = document.createElement('button');
                        minusBtn.innerHTML = '−';
                        minusBtn.style.cssText = `
                            width: 32px;
                            height: 32px;
                            background: rgba(244, 67, 54, 0.85);
                            color: white;
                            border: none;
                            border-radius: 6px;
                            font-size: 20px;
                            font-weight: bold;
                            cursor: pointer;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            transition: all 0.2s ease;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                            padding: 0;
                            line-height: 1;
                        `;

                        // Create + button (increase height)
                        const plusBtn = document.createElement('button');
                        plusBtn.innerHTML = '+';
                        plusBtn.style.cssText = `
                            width: 32px;
                            height: 32px;
                            background: rgba(76, 175, 80, 0.85);
                            color: white;
                            border: none;
                            border-radius: 6px;
                            font-size: 20px;
                            font-weight: bold;
                            cursor: pointer;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            transition: all 0.2s ease;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                            padding: 0;
                            line-height: 1;
                        `;

                        // Function to update height with smooth animation
                        function updateHeight(newHeight) {
                            const isCurrentlyPortrait = window.innerHeight > window.innerWidth;
                            
                            // Different limits for portrait vs landscape
                            const minHeight = isCurrentlyPortrait ? 25 : 30;
                            const maxHeight = isCurrentlyPortrait ? 75 : 85;
                            
                            currentHeight = Math.max(minHeight, Math.min(maxHeight, newHeight));
                            
                            // Update both containers for smooth transition
                            devToolsEl.style.setProperty('height', currentHeight + '%', 'important');
                            erudaContainer.style.setProperty('height', currentHeight + 'vh', 'important');
                            
                            console.log('📏 Eruda height: ' + currentHeight + '% (orientation: ' + 
                                       (isCurrentlyPortrait ? 'portrait' : 'landscape') + ')');
                        }

                        // - button handlers
                        minusBtn.addEventListener('click', (e) => {
                            updateHeight(currentHeight - 10);
                            e.stopPropagation();
                        });

                        minusBtn.addEventListener('mouseenter', () => {
                            minusBtn.style.background = 'rgba(244, 67, 54, 1)';
                            minusBtn.style.transform = 'scale(1.1)';
                        });

                        minusBtn.addEventListener('mouseleave', () => {
                            minusBtn.style.background = 'rgba(244, 67, 54, 0.85)';
                            minusBtn.style.transform = 'scale(1)';
                        });

                        minusBtn.addEventListener('touchstart', () => {
                            minusBtn.style.background = 'rgba(244, 67, 54, 1)';
                            minusBtn.style.transform = 'scale(1.1)';
                        });

                        minusBtn.addEventListener('touchend', () => {
                            setTimeout(() => {
                                minusBtn.style.background = 'rgba(244, 67, 54, 0.85)';
                                minusBtn.style.transform = 'scale(1)';
                            }, 100);
                        });

                        // + button handlers
                        plusBtn.addEventListener('click', (e) => {
                            updateHeight(currentHeight + 10);
                            e.stopPropagation();
                        });

                        plusBtn.addEventListener('mouseenter', () => {
                            plusBtn.style.background = 'rgba(76, 175, 80, 1)';
                            plusBtn.style.transform = 'scale(1.1)';
                        });

                        plusBtn.addEventListener('mouseleave', () => {
                            plusBtn.style.background = 'rgba(76, 175, 80, 0.85)';
                            plusBtn.style.transform = 'scale(1)';
                        });

                        plusBtn.addEventListener('touchstart', () => {
                            plusBtn.style.background = 'rgba(76, 175, 80, 1)';
                            plusBtn.style.transform = 'scale(1.1)';
                        });

                        plusBtn.addEventListener('touchend', () => {
                            setTimeout(() => {
                                plusBtn.style.background = 'rgba(76, 175, 80, 0.85)';
                                plusBtn.style.transform = 'scale(1)';
                            }, 100);
                        });

                        buttonContainer.appendChild(minusBtn);
                        buttonContainer.appendChild(plusBtn);
                        devToolsEl.appendChild(buttonContainer);

                        // Set initial height
                        updateHeight(currentHeight);

                        // Handle orientation changes
                        window.addEventListener('orientationchange', () => {
                            setTimeout(() => {
                                const newIsPortrait = window.innerHeight > window.innerWidth;
                                const newOptimalHeight = newIsPortrait ? 40 : 50;
                                currentHeight = newOptimalHeight;
                                updateHeight(newOptimalHeight);
                                console.log('🔄 Orientation changed, adjusted to ' + newOptimalHeight + '%');
                            }, 300);
                        });

                        // Also handle resize events (for desktop)
                        let resizeTimeout;
                        window.addEventListener('resize', () => {
                            clearTimeout(resizeTimeout);
                            resizeTimeout = setTimeout(() => {
                                const newIsPortrait = window.innerHeight > window.innerWidth;
                                const wasPortrait = currentHeight <= 50;
                                
                                // Only auto-adjust if orientation actually changed
                                if (newIsPortrait !== wasPortrait) {
                                    const newOptimalHeight = newIsPortrait ? 40 : 50;
                                    currentHeight = newOptimalHeight;
                                    updateHeight(newOptimalHeight);
                                }
                            }, 300);
                        });

                        console.log('✅ Adaptive resize buttons added (portrait: 40%, landscape: 50%)');
                    }
                }, 100);

                setTimeout(() => clearInterval(checkInterval), 5000);
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

            console.log('✅ Eruda initialized with adaptive sizing');
        };

        script.onerror = function() {
            console.error('❌ Failed to load Eruda');
        };

        (document.head || document.documentElement).appendChild(script);
    }

    // ============================================
    // ENHANCED NETWORK INTERCEPTOR
    // ============================================
    (function() {
        const originalFetch = window.fetch;
        window.fetch = function(...args) {
            const startTime = Date.now();
            const url = typeof args[0] === 'string' ? args[0] : args[0]?.url;
            const method = args[1]?.method || 'GET';

            return originalFetch.apply(this, args)
                .then(response => {
                    const duration = Date.now() - startTime;
                    const clonedResponse = response.clone();

                    clonedResponse.text().then(body => {
                        try {
                            window.DevBridge.logNetwork(JSON.stringify({
                                url: url,
                                method: method,
                                status: response.status,
                                statusText: response.statusText,
                                duration: duration,
                                type: 'fetch',
                                contentType: response.headers.get('content-type') || 'unknown',
                                bodySize: body.length
                            }));
                        } catch(e) {}
                    }).catch(() => {});

                    return response;
                })
                .catch(error => {
                    console.error('❌ Fetch error:', error);
                    throw error;
                });
        };

        const XHR = XMLHttpRequest.prototype;
        const originalOpen = XHR.open;
        const originalSend = XHR.send;

        XHR.open = function(method, url) {
            this._devData = { method, url, startTime: Date.now() };
            return originalOpen.apply(this, arguments);
        };

        XHR.send = function(body) {
            const xhr = this;

            xhr.addEventListener('load', function() {
                const duration = Date.now() - xhr._devData.startTime;

                try {
                    window.DevBridge.logNetwork(JSON.stringify({
                        url: xhr._devData.url,
                        method: xhr._devData.method,
                        status: xhr.status,
                        statusText: xhr.statusText,
                        duration: duration,
                        type: 'xhr',
                        contentType: xhr.getResponseHeader('content-type') || 'unknown',
                        bodySize: xhr.responseText?.length || 0
                    }));
                } catch(e) {}
            });

            xhr.addEventListener('error', function() {
                console.error('❌ XHR error:', xhr._devData.url);
            });

            return originalSend.apply(this, arguments);
        };

        const OriginalWebSocket = window.WebSocket;
        window.WebSocket = function(url, protocols) {
            console.log('🔌 WebSocket connected:', url);
            const ws = new OriginalWebSocket(url, protocols);

            ws.addEventListener('open', () => {
                console.log('✅ WebSocket opened:', url);
            });

            ws.addEventListener('message', (event) => {
                const preview = typeof event.data === 'string' 
                    ? event.data.substring(0, 100) 
                    : '[Binary data]';
                console.log('📩 WS Message:', preview);
            });

            ws.addEventListener('error', () => {
                console.error('❌ WebSocket error:', url);
            });

            ws.addEventListener('close', () => {
                console.log('🔒 WebSocket closed:', url);
            });

            return ws;
        };

        console.log('✅ Enhanced network interceptor active');
    })();

    // ============================================
    // FRAMEWORK DETECTION
    // ============================================
    setTimeout(function() {
        try {
            const detected = [];

            if (window.React) {
                window.DevBridge.detectFramework('React', window.React.version || 'unknown');
                detected.push('React ' + (window.React.version || '?'));
            } else if (document.querySelector('[data-reactroot], [data-reactid]')) {
                window.DevBridge.detectFramework('React', 'detected');
                detected.push('React');
            }

            if (window.Vue) {
                window.DevBridge.detectFramework('Vue', window.Vue.version || 'unknown');
                detected.push('Vue ' + (window.Vue.version || '?'));
            } else if (document.querySelector('[data-v-]') || window.__VUE__) {
                window.DevBridge.detectFramework('Vue', 'detected');
                detected.push('Vue');
            }

            if (window.ng || window.getAllAngularRootElements) {
                window.DevBridge.detectFramework('Angular', window.ng?.version?.full || 'detected');
                detected.push('Angular');
            }

            if (window.__NEXT_DATA__) {
                window.DevBridge.detectFramework('Next.js', 'detected');
                detected.push('Next.js');
            }

            if (window.__SVELTE__) {
                window.DevBridge.detectFramework('Svelte', 'detected');
                detected.push('Svelte');
            }

            if (window.jQuery || window.$) {
                window.DevBridge.detectFramework('jQuery', window.jQuery?.fn?.jquery || 'detected');
                detected.push('jQuery ' + (window.jQuery?.fn?.jquery || '?'));
            }

            if (window.__APOLLO_CLIENT__) {
                window.DevBridge.detectFramework('Apollo', 'detected');
                detected.push('Apollo GraphQL');
            }

            if (detected.length > 0) {
                console.log('🎯 Frameworks detected: ' + detected.join(', '));
            } else {
                console.log('ℹ️ No major frameworks detected (vanilla JS)');
            }
        } catch(e) {
            console.error('❌ Framework detection error:', e);
        }
    }, 1500);

    console.log('🎉 DevBrowser: All tools loaded and optimized!');
})();
""".trimIndent()
}