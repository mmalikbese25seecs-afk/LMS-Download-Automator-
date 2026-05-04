// JS file of LMS Calendar Page

// Type config
const TYPE_CONFIG = {
    assign: { label: 'Assignment', icon: '📝', color: '#3b82f6', bg: 'rgba(59,130,246,0.12)'  },
    quiz:   { label: 'Quiz',       icon: '📋', color: '#a78bfa', bg: 'rgba(167,139,250,0.12)' },
    lab:    { label: 'Lab Report', icon: '🔬', color: '#2dd4bf', bg: 'rgba(45,212,191,0.12)'  },
};

function getConfig(type) {
    return TYPE_CONFIG[type] || TYPE_CONFIG['assign'];
}

// State
let allEvents    = [];
let activeFilter = 'all';

// Bootstrap modal instance
let bsModal = null;

// Format helpers
function formatDate(iso) {
    return new Date(iso).toLocaleDateString('en-PK', {
        weekday: 'short', day: 'numeric', month: 'short', year: 'numeric'
    });
}

function formatTime(iso) {
    return new Date(iso).toLocaleTimeString('en-PK', {
        hour: '2-digit', minute: '2-digit', hour12: true
    });
}

function formatRemaining(sec) {
    if (sec <= 0) return 'Overdue';
    const d = Math.floor(sec / 86400);
    const h = Math.floor((sec % 86400) / 3600);
    const m = Math.floor((sec % 3600) / 60);
    if (d > 0) return d + 'd ' + h + 'h remaining';
    if (h > 0) return h + 'h ' + m + 'm remaining';
    return m + 'm remaining';
}

function getStatus(sec) {
    if (sec <= 0)       return { text: 'Overdue',   color: '#e74c3c' };
    if (sec <= 259200)  return { text: 'Urgent',    color: '#e74c3c' };
    if (sec <= 604800)  return { text: 'Due soon',  color: '#e67e22' };
    return                     { text: 'Upcoming',  color: '#22c55e' };
}

// Stats
function updateStats() {
    document.getElementById('statTotal').textContent  = allEvents.length;
    document.getElementById('statUrgent').textContent = allEvents.filter(e => e.timeUntilDue > 0 && e.timeUntilDue <= 259200).length;
    document.getElementById('statAssign').textContent = allEvents.filter(e => e.eventType === 'assign').length;
    document.getElementById('statQuiz').textContent   = allEvents.filter(e => e.eventType === 'quiz').length;
}

// Render cards
function renderEvents() {
    const list     = document.getElementById('eventList');
    const filtered = activeFilter === 'all'
        ? allEvents
        : allEvents.filter(e => e.eventType === activeFilter);

    const sorted = [...filtered].sort((a, b) => a.timeUntilDue - b.timeUntilDue);

    if (sorted.length === 0) {
        list.innerHTML = '<div class="cal-empty">No deadlines for this filter.</div>';
        return;
    }

    list.innerHTML = '';

    sorted.forEach(e => {
        const cfg    = getConfig(e.eventType);
        const status = getStatus(e.timeUntilDue);
        const card   = document.createElement('div');

        card.className = 'cal-event-card';
        card.style.borderLeftColor = e.color;

        card.innerHTML = `
            <div class="cal-card-top">
                <div class="cal-card-title">${e.title}</div>
                <span class="cal-card-badge"
                      style="background:${cfg.bg};color:${cfg.color};border:1px solid ${cfg.color}33">
                    ${cfg.label}
                </span>
            </div>
            <div class="cal-card-course" style="color:${e.color}">${e.courseName}</div>
            <div class="cal-card-bottom">
                <div class="cal-card-meta">
                    <span class="cal-meta-item">📅 ${formatDate(e.start)}</span>
                    <span class="cal-meta-item">🕐 ${formatTime(e.start)}</span>
                </div>
                <span class="cal-card-remaining"
                      style="background:${e.color}18;color:${e.color}">
                    ${formatRemaining(e.timeUntilDue)}
                </span>
            </div>`;

        card.addEventListener('click', () => openModal(e));
        list.appendChild(card);
    });
}

// Modal (Bootstrap 5 API)
function openModal(e) {
    const cfg    = getConfig(e.eventType);
    const status = getStatus(e.timeUntilDue);

    document.getElementById('modalIcon').textContent      = cfg.icon;
    document.getElementById('modalIcon').style.background = cfg.bg;
    document.getElementById('modalIcon').style.border     = `1px solid ${cfg.color}33`;
    document.getElementById('modalTitle').textContent     = e.title;
    document.getElementById('modalCourse').textContent    = e.courseName;
    document.getElementById('modalDate').textContent      = formatDate(e.start);
    document.getElementById('modalTime').textContent      = formatTime(e.start);
    document.getElementById('modalType').textContent      = cfg.label;
    document.getElementById('modalStatus').textContent    = status.text;
    document.getElementById('modalStatus').style.color    = status.color;

    const rem = document.getElementById('modalRemaining');
    rem.textContent = formatRemaining(e.timeUntilDue);
    rem.style.color = e.color;

    const block = document.getElementById('modalRemainingBlock');
    block.style.background = `${e.color}12`;
    block.style.border     = `1px solid ${e.color}25`;

    bsModal.show();
}

// Filter tabs
document.querySelectorAll('.cal-filter-btn').forEach(btn => {
    btn.addEventListener('click', function () {
        document.querySelectorAll('.cal-filter-btn').forEach(b => b.classList.remove('active'));
        this.classList.add('active');
        activeFilter = this.dataset.type;
        renderEvents();
    });
});

// Dark / Light toggle (Bootstrap data-bs-theme)
const darkToggle = document.getElementById('darkToggle');
const themeIcon  = document.getElementById('themeIcon');
const htmlEl     = document.documentElement;

function applyTheme(theme) {
    htmlEl.setAttribute('data-bs-theme', theme);
    localStorage.setItem('theme', theme);
    if (theme === 'light') {
        darkToggle.checked = true;
        themeIcon.className = 'fa fa-moon';
    } else {
        darkToggle.checked = false;
        themeIcon.className = 'fa fa-sun';
    }
}

// Apply saved theme on load
applyTheme(localStorage.getItem('theme') === 'light' ? 'light' : 'dark');

darkToggle.addEventListener('change', function () {
    applyTheme(this.checked ? 'light' : 'dark');
});

// Logout
document.getElementById('logoutBtn').addEventListener('click', () => {
    window.location.href = 'index.html';
});

// UI state helpers
function showEventList() {
    document.getElementById('calLoading').style.display = 'none';
    document.getElementById('calError').style.display   = 'none';
    document.getElementById('eventList').style.display  = 'flex';
}

function showError(msg) {
    document.getElementById('calLoading').style.display = 'none';
    document.getElementById('eventList').style.display  = 'none';
    document.getElementById('calError').style.display   = 'block';
    document.getElementById('calErrorMsg').textContent  = msg || 'Could not load deadlines.';
}

// Fetch from Backend API
async function loadCalendarData() {
    try {
        const res = await fetch('/api/calendar/events');
        if (!res.ok) throw new Error('Server returned ' + res.status);
        const data = await res.json();
        if (!Array.isArray(data)) throw new Error('Invalid data format');
        allEvents = data;
        showEventList();
        updateStats();
        renderEvents();
        renderMiniCal();          // redraw mini cal with event dots
    } catch (e) {
        showError(e.message);
        renderMiniCal();          // still show calendar on error
    }
}

document.getElementById('retryBtn').addEventListener('click', () => {
    document.getElementById('calError').style.display   = 'none';
    document.getElementById('calLoading').style.display = 'block';
    loadCalendarData();
});

// Mini Calendar

let miniCalYear  = new Date().getFullYear();
let miniCalMonth = new Date().getMonth();

const MONTH_NAMES = [
    'January','February','March','April','May','June',
    'July','August','September','October','November','December'
];

function buildEventDateMap() {
    const map = {};
    allEvents.forEach(e => {
        const d   = new Date(e.start);
        const key = d.getFullYear() + '-'
            + String(d.getMonth() + 1).padStart(2, '0') + '-'
            + String(d.getDate()).padStart(2, '0');
        if (!map[key]) map[key] = [];
        map[key].push(e);
    });
    return map;
}

function renderMiniCal() {
    const label    = document.getElementById('miniCalLabel');
    const daysEl   = document.getElementById('miniCalDays');
    const eventMap = buildEventDateMap();

    label.textContent = MONTH_NAMES[miniCalMonth] + ' ' + miniCalYear;
    daysEl.innerHTML  = '';

    const today    = new Date();
    const todayKey = today.getFullYear() + '-'
        + String(today.getMonth() + 1).padStart(2, '0') + '-'
        + String(today.getDate()).padStart(2, '0');

    // Convert Sunday-first to Monday-first offset
    const firstDay    = new Date(miniCalYear, miniCalMonth, 1).getDay();
    const offset      = (firstDay === 0) ? 6 : firstDay - 1;
    const daysInMonth = new Date(miniCalYear, miniCalMonth + 1, 0).getDate();

    // Blank leading cells
    for (let i = 0; i < offset; i++) {
        const empty = document.createElement('div');
        empty.className = 'mini-cal-day empty';
        daysEl.appendChild(empty);
    }

    // Day cells
    for (let d = 1; d <= daysInMonth; d++) {
        const key  = miniCalYear + '-'
            + String(miniCalMonth + 1).padStart(2, '0') + '-'
            + String(d).padStart(2, '0');
        const evts = eventMap[key] || [];

        const cell = document.createElement('div');
        cell.className = 'mini-cal-day';
        if (key === todayKey) cell.classList.add('today');
        if (evts.length > 0)  cell.classList.add('has-event');

        // Day number circle
        const num = document.createElement('div');
        num.className   = 'mini-cal-day-num';
        num.textContent = d;
        cell.appendChild(num);

        // Colored dots (max 3)
        if (evts.length > 0) {
            const dotsRow = document.createElement('div');
            dotsRow.className = 'mini-cal-dots';
            evts.slice(0, 3).forEach(ev => {
                const dot = document.createElement('div');
                dot.className        = 'mini-cal-dot';
                dot.style.background = getConfig(ev.eventType).color;
                dotsRow.appendChild(dot);
            });
            cell.appendChild(dotsRow);
        }

        // Click: filter event list to this day
        if (evts.length > 0) {
            cell.addEventListener('click', () => {
                document.querySelectorAll('.mini-cal-day.selected')
                    .forEach(c => c.classList.remove('selected'));
                cell.classList.add('selected');
                filterByDate(key);
            });
        }

        daysEl.appendChild(cell);
    }
}

function filterByDate(key) {
    const list = document.getElementById('eventList');

    const eventsOnDay = allEvents.filter(e => {
        const d = new Date(e.start);
        const k = d.getFullYear() + '-'
            + String(d.getMonth() + 1).padStart(2, '0') + '-'
            + String(d.getDate()).padStart(2, '0');
        return k === key;
    });

    list.style.display = 'flex';
    list.innerHTML = '';

    if (eventsOnDay.length === 0) {
        list.innerHTML = '<div class="cal-empty">No deadlines on this day.</div>';
        list.scrollIntoView({ behavior: 'smooth', block: 'start' });
        return;
    }

    eventsOnDay.forEach(e => {
        const cfg  = getConfig(e.eventType);
        const card = document.createElement('div');
        card.className = 'cal-event-card';
        card.style.borderLeftColor = cfg.color;
        card.innerHTML = `
            <div class="cal-card-top">
                <div class="cal-card-title">${e.title}</div>
                <span class="cal-card-badge"
                      style="background:${cfg.bg};color:${cfg.color};border:1px solid ${cfg.color}33">
                    ${cfg.label}
                </span>
            </div>
            <div class="cal-card-course" style="color:${cfg.color}">${e.courseName}</div>
            <div class="cal-card-bottom">
                <div class="cal-card-meta">
                    <span class="cal-meta-item">📅 ${formatDate(e.start)}</span>
                    <span class="cal-meta-item">🕐 ${formatTime(e.start)}</span>
                </div>
                <span class="cal-card-remaining"
                      style="background:${cfg.color}18;color:${cfg.color}">
                    ${formatRemaining(e.timeUntilDue)}
                </span>
            </div>`;
        card.addEventListener('click', () => openModal(e));
        list.appendChild(card);
    });

    list.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// Prev / Next month navigation
document.getElementById('miniCalPrev').addEventListener('click', () => {
    miniCalMonth--;
    if (miniCalMonth < 0) { miniCalMonth = 11; miniCalYear--; }
    renderMiniCal();
});

document.getElementById('miniCalNext').addEventListener('click', () => {
    miniCalMonth++;
    if (miniCalMonth > 11) { miniCalMonth = 0; miniCalYear++; }
    renderMiniCal();
});

// Init
document.addEventListener('DOMContentLoaded', () => {
    bsModal = new bootstrap.Modal(document.getElementById('eventModal'));
    renderMiniCal(); // render immediately (dots added after API fetch)
    loadCalendarData();
});