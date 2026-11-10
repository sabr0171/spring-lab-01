// Variant 6: loaded from this origin, so script-src 'self' allows it.
// It reports on the start page what the browser did with the inline script that follows it.
(function () {
    'use strict';

    var violations = [];

    // The browser fires this event every time the Content-Security-Policy refuses something.
    document.addEventListener('securitypolicyviolation', function (e) {
        var what = e.blockedURI === 'inline' ? 'an inline script' : e.blockedURI;
        violations.push('Blocked ' + what + ' (line ' + e.lineNumber + ') — directive ' + e.effectiveDirective);
        console.warn('CSP violation caught by csp-check.js:', e.effectiveDirective, e.blockedURI, 'line', e.lineNumber);
        render();
    });

    function render() {
        var list = document.getElementById('csp-violations');
        if (!list || violations.length === 0) {
            return;   // keep "none"
        }
        list.textContent = '';
        violations.forEach(function (text) {
            var item = document.createElement('li');
            item.textContent = text;
            list.appendChild(item);
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        var external = document.getElementById('external-status');
        if (external) {
            external.textContent = 'ran (allowed by script-src \'self\')';
            external.className = 'ok';
        }
        render();
    });
}());
