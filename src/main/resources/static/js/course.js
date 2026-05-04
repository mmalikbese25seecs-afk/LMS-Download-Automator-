/* NUST LMS Course Page */

let completedIds = new Set();
let courseData = null;

// TYPE CONFIG
const TYPE_CONFIG = {
    lecture:    { icon: "fa-solid fa-chalkboard-user", label: "Lecture",    cssClass: "type-lecture"    },
    pdf:        { icon: "fa-solid fa-file-pdf",        label: "PDF",        cssClass: "type-pdf"        },
    assignment: { icon: "fa-solid fa-pen-to-square",   label: "Assignment", cssClass: "type-assignment" },
    quiz:       { icon: "fa-solid fa-circle-question", label: "Quiz",       cssClass: "type-quiz"       },
    word:       { icon: "fa-solid fa-file-word",       label: "Word Doc",   cssClass: "type-word"       },
};

function getTypeConfig(type) {
    return TYPE_CONFIG[type] || TYPE_CONFIG["pdf"];
}

// THEME
const themeToggle = document.getElementById("themeToggle");
const themeIcon   = document.getElementById("themeIcon");
const html        = document.documentElement;

function applyTheme(dark) {
    html.setAttribute("data-bs-theme", dark ? "dark" : "light");
    themeIcon.className = dark ? "fa-solid fa-moon" : "fa-solid fa-sun";
    themeToggle.checked = dark;
    localStorage.setItem("lmsTheme", dark ? "dark" : "light");
}

themeToggle.addEventListener("change", () => applyTheme(themeToggle.checked));

// Back button
document.querySelector(".cal-back-btn").addEventListener("click", () => {
    window.location.href = "/dashboard.html";
});

// POPULATE HERO
function populateHero(data) {
    const code = data.shortName || "UNKNOWN";
    const title = data.weeks && data.weeks.length > 0 ? data.weeks[0].sectionName : "Course";
    const description = "";

    document.title = `${code} — NUST LMS`;
    document.getElementById("breadcrumbCourse").textContent = `${code}: ${title}`;
    document.getElementById("heroCourseCode").textContent   = code;
    document.getElementById("heroCourseTitle").textContent  = title;
    document.getElementById("heroCourseDesc").textContent   = description;

    // Stats
    const totalWeeks       = data.weeks ? data.weeks.length : 0;
    const totalResources   = data.totalFiles || 0;
    const totalAssignments = data.totalAssignments || 0;
    const dueSoon = 0;

    document.getElementById("statWeeks").textContent       = totalWeeks;
    document.getElementById("statResources").textContent   = totalResources;
    document.getElementById("statAssignments").textContent = totalAssignments;
    document.getElementById("statDue").textContent         = dueSoon;

    // Progress ring
    const totalCount = data.totalCount || totalResources;
    const pct = totalCount > 0 ? Math.round((data.completedCount / totalCount) * 100) : 0;
    document.getElementById("progressPct").textContent = pct + "%";
    const circumference = 2 * Math.PI * 34;
    const offset = circumference - (pct / 100) * circumference;
    setTimeout(() => {
        document.getElementById("progressRing").style.strokeDashoffset = offset;
    }, 120);
}

// BUILD WEEK SECTIONS
function buildWeeks(data) {
    const container = document.getElementById("weekSections");
    container.innerHTML = "";

    if (!data.weeks || data.weeks.length === 0) {
        container.innerHTML = '<div class="empty-msg" style="padding: 20px; text-align: center;">No course content available.</div>';
        return;
    }

    data.weeks.forEach((week, wi) => {
        const section = document.createElement("div");
        section.className = "week-section";
        section.dataset.weekIndex = wi;

        // Header
        const header = document.createElement("div");
        header.className = "week-header";
        header.innerHTML = `
            <span class="week-label">${week.dateRange || week.sectionName}</span>
            <div class="week-line"></div>
            <span class="week-count" style="font-size:0.75rem;color:#555">${week.files ? week.files.length : 0} items</span>
            <i class="fa-solid fa-chevron-down week-toggle-icon"></i>
        `;
        header.addEventListener("click", () => {
            section.classList.toggle("collapsed");
        });

        // Items list
        const itemsList = document.createElement("div");
        itemsList.className = "week-items";

        if (week.files && week.files.length > 0) {
            week.files.forEach(res => {
                const cfg  = getTypeConfig(res.fileType || "pdf");
                const done = completedIds.has(res.fileName);
                const row  = document.createElement("div");
                row.className = `resource-row ${cfg.cssClass} ${done ? "done" : ""}`;
                row.dataset.type  = res.fileType || "pdf";
                row.dataset.title = (res.fileName || "").toLowerCase();
                row.dataset.id    = res.fileName;

                row.innerHTML = `
                    <div class="resource-icon-wrap">
                        <i class="${cfg.icon}"></i>
                    </div>
                    <span class="resource-name">${res.fileName}</span>
                    <span class="resource-type-label">${cfg.label}</span>
                    <div class="resource-check">
                        ${done ? '<i class="fa-solid fa-check"></i>' : ''}
                    </div>
                `;

                row.addEventListener("click", () => openModal(res, week.dateRange || week.sectionName, cfg));
                itemsList.appendChild(row);
            });
        } else {
            const emptyMsg = document.createElement("div");
            emptyMsg.className = "empty-msg";
            emptyMsg.textContent = "No files in this section";
            itemsList.appendChild(emptyMsg);
        }

        section.appendChild(header);
        section.appendChild(itemsList);
        container.appendChild(section);
    });
}

// MODAL
const bsModal = new bootstrap.Modal(document.getElementById("resourceModal"));

function openModal(res, weekLabel, cfg) {
    document.getElementById("modalIcon").className = `cal-modal-icon`;
    document.getElementById("modalIcon").style.background = getIconBg(res.fileType || "pdf");
    document.getElementById("modalIcon").style.color = getIconColor(res.fileType || "pdf");
    document.getElementById("modalIcon").innerHTML = `<i class="${cfg.icon}"></i>`;
    document.getElementById("modalTitle").textContent = res.fileName;
    document.getElementById("modalWeek").textContent  = weekLabel;
    document.getElementById("modalType").textContent  = cfg.label;
    document.getElementById("modalSize").textContent  = formatBytes(res.fileSize || 0);

    const dueDateRow = document.getElementById("modalDueDateRow");
    dueDateRow.style.display = "none";

    document.getElementById("modalOpenBtn").href = res.fileUrl || "#";
    document.getElementById("modalOpenBtn").target = "_blank";
    bsModal.show();
}

function formatBytes(bytes) {
    if (!bytes) return "—";
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
    return (bytes / (1024 * 1024)).toFixed(1) + " MB";
}

function getIconBg(type) {
    const map = {
        lecture: "rgba(59,130,246,0.15)", pdf: "rgba(231,76,60,0.15)",
        assignment: "rgba(167,139,250,0.15)", quiz: "rgba(245,158,11,0.15)", word: "rgba(34,197,94,0.15)"
    };
    return map[type] || "rgba(255,255,255,0.08)";
}

function getIconColor(type) {
    const map = {
        lecture: "#3b82f6", pdf: "#e74c3c",
        assignment: "#a78bfa", quiz: "#f59e0b", word: "#22c55e"
    };
    return map[type] || "#ccc";
}

// FILTER & SEARCH
let activeFilter = "all";

document.querySelectorAll(".cal-filter-btn").forEach(btn => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".cal-filter-btn").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        activeFilter = btn.dataset.filter;
        applyFilterSearch();
    });
});

document.getElementById("searchInput").addEventListener("input", applyFilterSearch);

function applyFilterSearch() {
    const query = document.getElementById("searchInput").value.toLowerCase().trim();

    document.querySelectorAll(".week-section").forEach(section => {
        let visibleCount = 0;
        section.querySelectorAll(".resource-row").forEach(row => {
            const typeMatch  = activeFilter === "all" || row.dataset.type === activeFilter;
            const queryMatch = !query || row.dataset.title.includes(query);
            const visible    = typeMatch && queryMatch;
            row.classList.toggle("hidden-by-filter", !visible);
            if (visible) visibleCount++;
        });
        section.classList.toggle("all-hidden", visibleCount === 0);
    });
}

// LOGOUT
function logout() {
    fetch('/api/auth/logout', { method: 'POST' })
        .then(() => {
            window.location.href = '/index.html';
        })
        .catch(e => console.error('Logout failed:', e));
}

// INIT
async function initCoursePage() {
    const savedTheme = localStorage.getItem("lmsTheme") || "dark";
    applyTheme(savedTheme === "dark");

    // Get course ID from URL or localStorage
    const params = new URLSearchParams(window.location.search);
    let courseId = params.get('id');
    let shortName = params.get('name');

    if (!courseId) {
        const stored = localStorage.getItem('selectedCourse');
        if (stored) {
            try {
                const data = JSON.parse(stored);
                courseId = data.courseId;
                shortName = data.shortName;
            } catch (e) {
                console.error('Invalid stored course data');
            }
        }
    }

    if (!courseId || !shortName) {
        document.getElementById('weekSections').innerHTML =
            '<div class="empty-msg" style="padding: 20px; text-align: center;">No course selected. <a href="/dashboard.html">Go back to dashboard</a></div>';
        return;
    }

    // Fetch course details from API
    try {
        const response = await fetch(`/api/courses/${courseId}/detail?shortName=${encodeURIComponent(shortName)}`);
        if (!response.ok) {
            throw new Error('Failed to load course details');
        }
        courseData = await response.json();
        populateHero(courseData);
        buildWeeks(courseData);
    } catch (e) {
        console.error('Error loading course:', e);
        document.getElementById('weekSections').innerHTML =
            `<div class="empty-msg" style="padding: 20px; text-align: center; color: #e74c3c;">Error loading course: ${e.message}</div>`;
    }
}

// Start initialization when page loads
document.addEventListener('DOMContentLoaded', initCoursePage);
