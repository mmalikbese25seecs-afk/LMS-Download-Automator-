// ===== JS file of LMS Login Webpage =====

// ── Theme: load saved preference on page load ──────────────
if (localStorage.getItem('theme') === 'dark') {
    document.body.classList.add('dark');
    document.getElementById('darkToggle').textContent = '☀️ Light Mode';
}

// ── Dark Mode Toggle ───────────────────────────────────────
document.getElementById('darkToggle').addEventListener('click', function () {
    document.body.classList.toggle('dark');

    if (document.body.classList.contains('dark')) {
        localStorage.setItem('theme', 'dark');
        this.textContent = '☀️ Light Mode';
    } else {
        localStorage.setItem('theme', 'light');
        this.textContent = '🌙 Dark Mode';
    }
});

// ── Login Button ───────────────────────────────────────────
document.querySelector('.btn-login').addEventListener('click', handleLogin);

async function handleLogin() {
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();

    showError('');

    if (!username || !password) {
        showError('Please enter both username and password.');
        return;
    }

    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await res.json(); // ✅ IMPORTANT

        if (!res.ok) {
            showError(data.error || 'Login failed. Check your credentials.');
            return;
        }

        // ✅ STORE USER DATA
        localStorage.setItem('lmsUser', JSON.stringify({
            fullName: data.fullName,
            userId: data.userId
        }));

        // ✅ Redirect to dashboard (NOT calendar)
        window.location.href = 'dashboard.html';

    } catch (e) {
        showError('Cannot reach server. Is the backend running?');
    }
}

// ── Show error below the button ────────────────────────────
function showError(message) {
    const div = document.getElementById('loginError');
    if (!div) return;
    div.textContent = message;
    div.style.display = message ? 'block' : 'none';
}

// - Press Enter to Login -
document.addEventListener('keypress', function (e) {
    if (e.key === 'Enter') {
        handleLogin();
    }
});