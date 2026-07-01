(function() {
    'use strict';

    function tryAutoLogin() {
        var btn = document.querySelector('button');
        if (!btn) return false;
        var text = btn.textContent.trim();
        if (text === '立即登录') {
            btn.click();
            return true;
        }
        return false;
    }

    // Try immediately
    if (tryAutoLogin()) return;

    // Try after a short delay in case page hasn't fully rendered
    setTimeout(function() {
        tryAutoLogin();
    }, 500);

    // Also try after full page load
    window.addEventListener('load', function() {
        setTimeout(function() {
            tryAutoLogin();
        }, 300);
    });
})();
