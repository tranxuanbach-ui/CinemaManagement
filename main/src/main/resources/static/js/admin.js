// =============================================================================
// ADMIN DASHBOARD JAVASCRIPT - PHIMMOI Cinema Management
// =============================================================================

const API_BASE = "/api";

// State
let currentUser = null;
let allMovies = [];
let allGenres = [];
let allSystems = [];
let allCinemas = [];
let allLinks = [];
let allShowtimes = [];
let allUsers = [];

// ==================== KHỞI CHẠY ỨNG DỤNG ====================
document.addEventListener("DOMContentLoaded", async function () {
    setupTabNavigation();
    setupForms();
    await checkAdminAuth();
});

// ==================== TIỆN ÍCH FETCH & THÔNG BÁO ====================
async function apiFetch(path, options = {}) {
    const fetchOpts = {
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        ...options
    };
    if (options.body && typeof options.body !== "string") {
        fetchOpts.body = JSON.stringify(options.body);
    }
    return await fetch(`${API_BASE}${path}`, fetchOpts);
}

function showToast(message, type = "info") {
    const container = document.getElementById("toast-container");
    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = "0";
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

function showLoadingToast(baseText = "Đang cập nhật phim") {
    const container = document.getElementById("toast-container");
    const toast = document.createElement("div");
    toast.className = "toast loading";

    const spinner = document.createElement("span");
    spinner.className = "toast-spinner";

    const textSpan = document.createElement("span");
    textSpan.textContent = baseText + "...";

    toast.appendChild(spinner);
    toast.appendChild(textSpan);
    container.appendChild(toast);

    let dotCount = 0;
    const intervalId = setInterval(() => {
        dotCount = (dotCount + 1) % 4; // 1, 2, 3, 0
        textSpan.textContent = baseText + ".".repeat(dotCount);
    }, 450);

    return {
        close: () => {
            clearInterval(intervalId);
            toast.style.opacity = "0";
            setTimeout(() => toast.remove(), 250);
        }
    };
}

// ==================== XÁC THỰC ADMIN ====================
async function checkAdminAuth() {
    try {
        const res = await apiFetch("/auth/me");
        if (res.ok) {
            const data = await res.json();
            const user = data.data;
            if (user && (user.role === "ADMIN" || user.role === "STAFF")) {
                currentUser = user;
                showDashboard();
                return;
            }
        }
    } catch (e) {
        console.warn("Chưa đăng nhập:", e);
    }
    showLoginView();
}

function showLoginView() {
    document.getElementById("admin-login-view").style.display = "flex";
    document.getElementById("admin-dashboard-view").style.display = "none";
}

function showDashboard() {
    document.getElementById("admin-login-view").style.display = "none";
    document.getElementById("admin-dashboard-view").style.display = "flex";

    // Set user info in topbar
    document.getElementById("current-user-name").textContent = currentUser.username;
    const roleBadge = document.getElementById("current-user-role");
    roleBadge.textContent = currentUser.role;
    if (currentUser.role === 'ADMIN') {
        roleBadge.style.background = "#f1800f";
        roleBadge.style.color = "#000";
        roleBadge.title = "Toàn quyền Quản trị viên (ADMIN)";
    } else {
        roleBadge.style.background = "#e67e22";
        roleBadge.style.color = "#fff";
        roleBadge.title = "Quyền Cộng tác viên (STAFF) - Giới hạn";
    }

    // Load initial data
    loadAllData();
}

// ==================== ĐIỀU HƯỚNG TABS ====================
function setupTabNavigation() {
    const menuLinks = document.querySelectorAll(".sidebar-menu a");
    menuLinks.forEach(link => {
        link.addEventListener("click", function (e) {
            e.preventDefault();
            const tabName = this.getAttribute("data-tab");
            switchTab(tabName);
        });
    });

    document.getElementById("admin-logout-btn").addEventListener("click", async () => {
        try {
            await apiFetch("/auth/logout", { method: "POST" });
        } catch (e) { }
        currentUser = null;
        showToast("Đã đăng xuất thành công", "info");
        showLoginView();
    });
}

function switchTab(tabName) {
    // Menu active class
    document.querySelectorAll(".sidebar-menu a").forEach(a => {
        a.classList.toggle("active", a.getAttribute("data-tab") === tabName);
    });

    // Content Pane
    document.querySelectorAll(".tab-pane").forEach(pane => {
        pane.style.display = "none";
    });
    const activePane = document.getElementById(`tab-${tabName}`);
    if (activePane) activePane.style.display = "block";

    // Title
    const titles = {
        overview: "Tổng quan hệ thống",
        movies: "Quản lý danh sách phim",
        genres: "Quản lý thể loại phim",
        "cinema-systems": "Quản lý hệ thống rạp chiếu",
        cinemas: "Quản lý cụm rạp",
        "movie-links": "Quản lý liên kết phim - hãng rạp",
        showtimes: "Quản lý lịch suất chiếu",
        users: "Quản lý tài khoản hệ thống"
    };
    document.getElementById("page-title").textContent = titles[tabName] || "Quản trị";
}

// ==================== TẢI TẤT CẢ DỮ LIỆU ====================
async function loadAllData() {
    await Promise.all([
        loadGenres(),
        loadSystems(),
        loadMovies(),
        loadCinemas(),
        loadLinks(),
        loadShowtimes(),
        loadUsers()
    ]);
    updateOverviewStats();
}

// ==================== MODAL HELPERS ====================
function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add("active");
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove("active");
}

let pendingConfirmAction = null;

function openConfirmModal({
    title = "Xác nhận hành động",
    message = "Bạn có chắc chắn muốn thực hiện thao tác này?",
    icon = "⚠️",
    btnText = "Xác nhận",
    btnColor = "#c0392b",
    onConfirm
}) {
    document.getElementById("confirm-modal-title").textContent = title;
    document.getElementById("confirm-modal-msg").textContent = message;
    document.getElementById("confirm-modal-icon").textContent = icon;

    const confirmBtn = document.getElementById("confirm-modal-btn");
    confirmBtn.textContent = btnText;
    confirmBtn.style.backgroundColor = btnColor;
    confirmBtn.style.borderColor = btnColor;

    pendingConfirmAction = onConfirm;
    openModal("confirm-modal");
}

function openDeleteConfirm(message, onConfirm) {
    openConfirmModal({
        title: "Xác nhận xóa?",
        message: message,
        icon: "⚠️",
        btnText: "Xác nhận Xóa",
        btnColor: "#c0392b",
        onConfirm: onConfirm
    });
}

document.addEventListener("DOMContentLoaded", function () {
    const confirmBtn = document.getElementById("confirm-modal-btn");
    if (confirmBtn) {
        confirmBtn.addEventListener("click", async () => {
            if (pendingConfirmAction) {
                closeModal("confirm-modal");
                await pendingConfirmAction();
                pendingConfirmAction = null;
            }
        });
    }
});

// ==================== FORM SUBMITS SETUP ====================
function setupForms() {
    // 1. Login Form
    const rememberCb = document.getElementById("admin-remember-password");
    const savedUser = localStorage.getItem("cinema_admin_username");
    const savedPass = localStorage.getItem("cinema_admin_password");
    if (savedUser && savedPass) {
        const uInput = document.getElementById("login-username");
        const pInput = document.getElementById("login-password");
        if (uInput) uInput.value = savedUser;
        if (pInput) pInput.value = savedPass;
        if (rememberCb) rememberCb.checked = true;
    }

    document.getElementById("admin-login-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        const username = document.getElementById("login-username").value.trim();
        const password = document.getElementById("login-password").value;
        const errBox = document.getElementById("login-error-msg");
        errBox.style.display = "none";

        try {
            const res = await apiFetch("/auth/login", {
                method: "POST",
                body: { username, password }
            });
            const data = await res.json();
            if (res.ok && data.success) {
                const user = data.data;
                if (user.role === "ADMIN" || user.role === "STAFF") {
                    if (rememberCb && rememberCb.checked) {
                        localStorage.setItem("cinema_admin_username", username);
                        localStorage.setItem("cinema_admin_password", password);
                    } else {
                        localStorage.removeItem("cinema_admin_username");
                        localStorage.removeItem("cinema_admin_password");
                    }

                    currentUser = user;
                    showToast(`Chào mừng ${user.username} quay trở lại!`, "success");
                    showDashboard();
                } else {
                    errBox.textContent = "Tài khoản của bạn là USER thường, không có quyền truy cập Admin!";
                    errBox.style.display = "block";
                    await apiFetch("/auth/logout", { method: "POST" });
                }
            } else {
                errBox.textContent = data.message || "Tên đăng nhập hoặc mật khẩu không đúng!";
                errBox.style.display = "block";
            }
        } catch (err) {
            errBox.textContent = "Không thể kết nối đến máy chủ!";
            errBox.style.display = "block";
        }
    });

    // 2. Movie Form
    document.getElementById("movie-form").addEventListener("submit", saveMovie);

    // 3. Genre Form
    document.getElementById("genre-form").addEventListener("submit", saveGenre);

    // 4. Cinema System Form
    document.getElementById("system-form").addEventListener("submit", saveSystem);

    // 5. Cinema Form
    document.getElementById("cinema-form").addEventListener("submit", saveCinema);

    // 6. Movie Link Form
    document.getElementById("link-form").addEventListener("submit", saveLink);

    // 7. Showtime Form
    document.getElementById("showtime-form").addEventListener("submit", saveShowtime);

    // 8. User Form
    const userForm = document.getElementById("user-form");
    if (userForm) userForm.addEventListener("submit", saveUser);

    // Search & Filter Listeners
    document.getElementById("movie-search-input").addEventListener("input", renderMoviesTable);
    document.getElementById("movie-genre-filter").addEventListener("change", renderMoviesTable);
    document.getElementById("genre-search-input").addEventListener("input", renderGenresTable);
    document.getElementById("cinema-system-filter").addEventListener("change", renderCinemasTable);
    document.getElementById("cinema-search-input").addEventListener("input", renderCinemasTable);
    document.getElementById("link-system-filter").addEventListener("change", renderLinksTable);
    document.getElementById("showtime-movie-filter").addEventListener("change", renderShowtimesTable);
    document.getElementById("showtime-cinema-filter").addEventListener("change", renderShowtimesTable);
    document.getElementById("showtime-date-filter").addEventListener("change", renderShowtimesTable);

    const userSearchInput = document.getElementById("user-search-input");
    if (userSearchInput) userSearchInput.addEventListener("input", renderUsersTable);
    const userRoleFilter = document.getElementById("user-role-filter");
    if (userRoleFilter) userRoleFilter.addEventListener("change", renderUsersTable);
    const userStatusFilter = document.getElementById("user-status-filter");
    if (userStatusFilter) userStatusFilter.addEventListener("change", renderUsersTable);
}

// ==================== TAB 1: OVERVIEW ====================
function updateOverviewStats() {
    document.getElementById("stat-movies-count").textContent = allMovies.length;
    document.getElementById("stat-genres-count").textContent = allGenres.length;
    document.getElementById("stat-systems-count").textContent = allSystems.length;
    document.getElementById("stat-cinemas-count").textContent = allCinemas.length;
    document.getElementById("stat-showtimes-count").textContent = allShowtimes.length;

    // Phân loại thống kê người dùng theo các vai trò
    const totalUsers = allUsers.length;
    const adminCount = allUsers.filter(u => u.role === "ADMIN").length;
    const staffCount = allUsers.filter(u => u.role === "STAFF").length;
    const regularUserCount = allUsers.filter(u => u.role === "USER" || (!u.role || (u.role !== "ADMIN" && u.role !== "STAFF"))).length;

    const elTotal = document.getElementById("stat-users-total");
    if (elTotal) elTotal.textContent = totalUsers;
    const elOld = document.getElementById("stat-users-count");
    if (elOld) elOld.textContent = totalUsers;

    const elAdmin = document.getElementById("stat-users-admin");
    if (elAdmin) elAdmin.textContent = adminCount;

    const elStaff = document.getElementById("stat-users-staff");
    if (elStaff) elStaff.textContent = staffCount;

    const elRegular = document.getElementById("stat-users-regular");
    if (elRegular) elRegular.textContent = regularUserCount;

    // Render 5 recent movies
    const tbody = document.getElementById("overview-recent-movies");
    const recents = allMovies.slice(0, 5);
    if (recents.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="empty-state">Chưa có phim nào trong hệ thống.</td></tr>`;
        return;
    }
    tbody.innerHTML = recents.map(m => {
        const poster = m.poster_url || m.posterUrl || "/media/logo.png";
        const genres = Array.isArray(m.genres) ? m.genres.map(g => `<span class="badge-tag">${typeof g === "string" ? g : g.name}</span>`).join("") : "";
        return `
            <tr>
                <td><img src="${poster}" class="poster-thumb" alt="poster"></td>
                <td style="font-weight:600; color:var(--text-primary);">${m.title}</td>
                <td>${genres || "—"}</td>
                <td>${m.runtime_minutes || m.runtimeMinutes || "—"} phút</td>
                <td><span style="color:var(--gold); font-weight:700;">★ ${m.rating ?? "—"}</span></td>
                <td>${m.release_date || m.releaseDate || "—"}</td>
            </tr>
        `;
    }).join("");
}

// ==================== TAB 2: QUẢN LÝ PHIM ====================
async function loadMovies() {
    try {
        const res = await apiFetch("/movies");
        if (res.ok) {
            allMovies = await res.json();
            renderMoviesTable();
            populateMovieFilterDropdowns();
        }
    } catch (e) {
        console.error("Lỗi tải phim:", e);
    }
}

function renderMoviesTable() {
    const searchVal = document.getElementById("movie-search-input").value.trim().toLowerCase();
    const genreVal = document.getElementById("movie-genre-filter").value;
    const tbody = document.getElementById("movies-table-body");

    let filtered = allMovies.filter(m => {
        const matchTitle = !searchVal || (m.title && m.title.toLowerCase().includes(searchVal));
        let matchGenre = true;
        if (genreVal) {
            const mGenres = Array.isArray(m.genres) ? m.genres.map(g => (typeof g === "string" ? g : g.name)) : [];
            matchGenre = mGenres.includes(genreVal);
        }
        return matchTitle && matchGenre;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="empty-state">Không tìm thấy bộ phim nào phù hợp.</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(m => {
        const poster = m.poster_url || m.posterUrl || "/media/logo.png";
        const genres = Array.isArray(m.genres) ? m.genres.map(g => `<span class="badge-tag">${typeof g === "string" ? g : g.name}</span>`).join("") : "";
        return `
            <tr>
                <td>#${m.id}</td>
                <td><img src="${poster}" class="poster-thumb" alt="poster"></td>
                <td style="font-weight:600; color:var(--text-primary);">${m.title}</td>
                <td>${genres || "—"}</td>
                <td><span style="color:var(--gold); font-weight:700;">★ ${m.rating ?? "N/A"}</span></td>
                <td>${m.runtime_minutes || m.runtimeMinutes || "—"}p</td>
                <td>${m.release_date || m.releaseDate || "—"}</td>
                <td style="text-align: right; white-space: nowrap;">
                    <button class="admin-btn btn-sm btn-edit" onclick="openEditMovieModal(${m.id})">Sửa</button>
                    <button class="admin-btn btn-sm btn-delete" onclick="confirmDeleteMovie(${m.id}, '${escapeHtml(m.title)}')">Xóa</button>
                </td>
            </tr>
        `;
    }).join("");
}

function populateMovieFilterDropdowns() {
    const movieFilter = document.getElementById("showtime-movie-filter");
    const linkMovieSelect = document.getElementById("link-form-movie");
    const showtimeMovieSelect = document.getElementById("showtime-form-movie");

    const movieOpts = allMovies.map(m => `<option value="${m.id}">${m.title} (ID: ${m.id})</option>`).join("");

    if (movieFilter) movieFilter.innerHTML = `<option value="">Tất cả phim</option>` + movieOpts;
    if (linkMovieSelect) linkMovieSelect.innerHTML = `<option value="">-- Chọn phim --</option>` + movieOpts;
    if (showtimeMovieSelect) showtimeMovieSelect.innerHTML = `<option value="">-- Chọn phim --</option>` + movieOpts;
}

function openCreateMovieModal() {
    document.getElementById("movie-modal-title").textContent = "Thêm Phim Mới";
    document.getElementById("movie-form").reset();
    document.getElementById("movie-id").value = "";
    renderGenreCheckboxes([]);
    openModal("movie-modal");
}

function openEditMovieModal(id) {
    const m = allMovies.find(item => item.id === id);
    if (!m) return;
    document.getElementById("movie-modal-title").textContent = `Chỉnh Sửa Phim: #${m.id}`;
    document.getElementById("movie-id").value = m.id;
    document.getElementById("movie-title").value = m.title || "";
    document.getElementById("movie-release-date").value = m.release_date || m.releaseDate || "";
    document.getElementById("movie-runtime").value = m.runtime_minutes || m.runtimeMinutes || "";
    document.getElementById("movie-rating").value = m.rating != null ? m.rating : "";
    document.getElementById("movie-poster").value = m.poster_url || m.posterUrl || "";
    document.getElementById("movie-trailer").value = m.trailer || "";
    document.getElementById("movie-description").value = m.description || "";

    const selectedGenreIds = Array.isArray(m.genres) ? m.genres.map(g => (typeof g === "object" ? g.id : null)).filter(Boolean) : [];
    renderGenreCheckboxes(selectedGenreIds);
    openModal("movie-modal");
}

function renderGenreCheckboxes(selectedIds = []) {
    const container = document.getElementById("movie-genre-checkboxes");
    container.innerHTML = allGenres.map(g => {
        const isChecked = selectedIds.includes(g.id);
        return `
            <label class="genre-label">
                <input type="checkbox" name="movie-genres" value="${g.id}" ${isChecked ? "checked" : ""}>
                <span>${g.name}</span>
            </label>
        `;
    }).join("");
}

async function saveMovie(e) {
    e.preventDefault();
    const id = document.getElementById("movie-id").value;
    const selectedGenreIds = Array.from(document.querySelectorAll('input[name="movie-genres"]:checked')).map(cb => parseInt(cb.value));

    const payload = {
        title: document.getElementById("movie-title").value.trim(),
        releaseDate: document.getElementById("movie-release-date").value || null,
        runtimeMinutes: parseInt(document.getElementById("movie-runtime").value) || null,
        rating: parseFloat(document.getElementById("movie-rating").value) || null,
        posterUrl: document.getElementById("movie-poster").value.trim() || null,
        trailer: document.getElementById("movie-trailer").value.trim() || null,
        description: document.getElementById("movie-description").value.trim() || null,
        genreIds: selectedGenreIds
    };

    try {
        const method = id ? "PUT" : "POST";
        const url = id ? `/movies/${id}` : "/movies";
        const res = await apiFetch(url, { method, body: payload });
        if (res.ok) {
            closeModal("movie-modal");
            showToast(id ? "Cập nhật phim thành công!" : "Thêm phim mới thành công!", "success");
            await loadMovies();
            updateOverviewStats();
        } else {
            const err = await res.json();
            showToast(err.message || "Lỗi lưu phim!", "error");
        }
    } catch (err) {
        showToast("Không thể kết nối đến máy chủ!", "error");
    }
}

function confirmDeleteMovie(id, title) {
    openDeleteConfirm(`Bạn có chắc chắn muốn xóa phim "${title}" (ID: #${id}) không?`, async () => {
        try {
            const res = await apiFetch(`/movies/${id}`, { method: "DELETE" });
            if (res.ok) {
                showToast("Đã xóa phim thành công!", "success");
                await loadMovies();
                updateOverviewStats();
            } else {
                const data = await res.json().catch(() => null);
                showToast((data && data.message) ? data.message : "Bạn không đủ quyền thực hiện thao tác này!", "error");
            }
        } catch (e) {
            showToast("Lỗi hệ thống khi xóa!", "error");
        }
    });
}

async function triggerDataImport() {
    openConfirmModal({
        title: "Đồng bộ dữ liệu từ JSON?",
        message: "Hệ thống sẽ cập nhật toàn bộ phim và lịch chiếu từ file data_phim_java.json. Các phim không có trong file sẽ bị xóa (tự động xóa khỏi danh sách theo dõi của người dùng), các phim có sẵn sẽ được cập nhật lại toàn bộ lịch chiếu. Bạn có chắc muốn tiếp tục?",
        icon: "📥",
        btnText: "Bắt đầu Đồng bộ",
        btnColor: "#27ae60",
        onConfirm: async () => {
            closeModal("confirm-modal");

            const syncBtn = document.getElementById("btn-sync-json");
            const syncBtnText = document.getElementById("btn-sync-json-text");
            const syncBtnIcon = document.getElementById("btn-sync-json-icon");

            // 1. Khóa tạm nút bấm và hiện trạng thái
            if (syncBtn) {
                syncBtn.disabled = true;
                syncBtn.style.opacity = "0.7";
                syncBtn.style.cursor = "not-allowed";
                if (syncBtnIcon) syncBtnIcon.innerHTML = `<span class="btn-loading-spinner"></span>`;
                if (syncBtnText) syncBtnText.textContent = "Đang đồng bộ...";
            }

            // 2. Hiện thông báo Toast kèm vòng xoay loading và dấu 3 chấm "Đang cập nhật phim..." lặp đi lặp lại
            const loadingToast = showLoadingToast("Đang cập nhật phim");

            try {
                const res = await apiFetch("/movies/import-data", { method: "POST" });
                const data = await res.json().catch(() => null);

                // Tắt thông báo đang cập nhật
                loadingToast.close();

                // 3. Hiện thông báo kết quả như trước
                if (res.ok && data && data.success) {
                    showToast(data.message || "Đồng bộ dữ liệu thành công!", "success");
                    await loadAllData();
                } else {
                    const errMsg = (data && data.message) ? data.message : "Đồng bộ thất bại!";
                    showToast(errMsg, "error");
                }
            } catch (e) {
                loadingToast.close();
                console.error("Lỗi đồng bộ JSON:", e);
                showToast("Lỗi kết nối máy chủ khi đồng bộ!", "error");
            } finally {
                // 4. Mở khóa và khôi phục nút bấm
                if (syncBtn) {
                    syncBtn.disabled = false;
                    syncBtn.style.opacity = "1";
                    syncBtn.style.cursor = "pointer";
                    if (syncBtnIcon) syncBtnIcon.textContent = "📥";
                    if (syncBtnText) syncBtnText.textContent = "Đồng bộ từ JSON";
                }
            }
        }
    });
}

// ==================== TAB 3: QUẢN LÝ THỂ LOẠI ====================
async function loadGenres() {
    try {
        const res = await apiFetch("/genres");
        if (res.ok) {
            allGenres = await res.json();
            renderGenresTable();
            populateGenreDropdowns();
        }
    } catch (e) {
        console.error("Lỗi tải thể loại:", e);
    }
}

function renderGenresTable() {
    const searchVal = document.getElementById("genre-search-input").value.trim().toLowerCase();
    const tbody = document.getElementById("genres-table-body");

    let filtered = allGenres.filter(g => !searchVal || g.name.toLowerCase().includes(searchVal));

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="3" class="empty-state">Không có thể loại nào.</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(g => `
        <tr>
            <td>#${g.id}</td>
            <td style="font-weight:600; color:var(--text-primary);">${g.name}</td>
            <td style="text-align: right;">
                <button class="admin-btn btn-sm btn-edit" onclick="openEditGenreModal(${g.id}, '${escapeHtml(g.name)}')">Sửa</button>
                <button class="admin-btn btn-sm btn-delete" onclick="confirmDeleteGenre(${g.id}, '${escapeHtml(g.name)}')">Xóa</button>
            </td>
        </tr>
    `).join("");
}

function populateGenreDropdowns() {
    const filter = document.getElementById("movie-genre-filter");
    if (!filter) return;
    filter.innerHTML = `<option value="">Tất cả thể loại</option>` + allGenres.map(g => `<option value="${g.name}">${g.name}</option>`).join("");
}

function openCreateGenreModal() {
    document.getElementById("genre-modal-title").textContent = "Thêm Thể Loại Mới";
    document.getElementById("genre-form").reset();
    document.getElementById("genre-id").value = "";
    openModal("genre-modal");
}

function openEditGenreModal(id, name) {
    document.getElementById("genre-modal-title").textContent = `Sửa Thể Loại: #${id}`;
    document.getElementById("genre-id").value = id;
    document.getElementById("genre-name").value = name;
    openModal("genre-modal");
}

async function saveGenre(e) {
    e.preventDefault();
    const id = document.getElementById("genre-id").value;
    const name = document.getElementById("genre-name").value.trim();

    try {
        const method = id ? "PUT" : "POST";
        const url = id ? `/genres/${id}` : "/genres";
        const res = await apiFetch(url, { method, body: { name } });
        if (res.ok) {
            closeModal("genre-modal");
            showToast(id ? "Cập nhật thể loại thành công!" : "Tạo thể loại thành công!", "success");
            await loadGenres();
            updateOverviewStats();
        } else {
            const err = await res.json();
            showToast(err.message || "Lỗi lưu thể loại!", "error");
        }
    } catch (err) {
        showToast("Lỗi kết nối máy chủ!", "error");
    }
}

function confirmDeleteGenre(id, name) {
    openDeleteConfirm(`Bạn có chắc muốn xóa thể loại "${name}" không?`, async () => {
        try {
            const res = await apiFetch(`/genres/${id}`, { method: "DELETE" });
            if (res.ok) {
                showToast("Đã xóa thể loại!", "success");
                await loadGenres();
                updateOverviewStats();
            } else {
                const data = await res.json().catch(() => null);
                showToast((data && data.message) ? data.message : "Bạn không đủ quyền thực hiện thao tác này!", "error");
            }
        } catch (e) {
            showToast("Lỗi kết nối máy chủ!", "error");
        }
    });
}

// ==================== TAB 4: HỆ THỐNG RẠP (CINEMA SYSTEMS) ====================
async function loadSystems() {
    try {
        const res = await apiFetch("/cinema-systems");
        if (res.ok) {
            const json = await res.json();
            allSystems = json.data || [];
            renderSystemsTable();
            populateSystemDropdowns();
        }
    } catch (e) {
        console.error("Lỗi tải hệ thống rạp:", e);
    }
}

function renderSystemsTable() {
    const tbody = document.getElementById("systems-table-body");
    if (allSystems.length === 0) {
        tbody.innerHTML = `<tr><td colspan="3" class="empty-state">Chưa có hệ thống rạp nào.</td></tr>`;
        return;
    }
    tbody.innerHTML = allSystems.map(s => `
        <tr>
            <td>#${s.id}</td>
            <td style="font-weight:600; color:var(--text-primary);">${s.name}</td>
            <td style="text-align: right;">
                <button class="admin-btn btn-sm btn-edit" onclick="openEditSystemModal(${s.id}, '${escapeHtml(s.name)}')">Sửa</button>
                <button class="admin-btn btn-sm btn-delete" onclick="confirmDeleteSystem(${s.id}, '${escapeHtml(s.name)}')">Xóa</button>
            </td>
        </tr>
    `).join("");
}

function populateSystemDropdowns() {
    const filterSelect = document.getElementById("cinema-system-filter");
    const linkFilterSelect = document.getElementById("link-system-filter");
    const cinemaFormSystem = document.getElementById("cinema-form-system");
    const linkFormSystem = document.getElementById("link-form-system");

    const options = allSystems.map(s => `<option value="${s.id}">${s.name}</option>`).join("");

    if (filterSelect) filterSelect.innerHTML = `<option value="">Tất cả hệ thống rạp</option>` + options;
    if (linkFilterSelect) linkFilterSelect.innerHTML = `<option value="">Tất cả hệ thống</option>` + options;
    if (cinemaFormSystem) cinemaFormSystem.innerHTML = `<option value="">-- Chọn hệ thống rạp --</option>` + options;
    if (linkFormSystem) linkFormSystem.innerHTML = `<option value="">-- Chọn hệ thống rạp --</option>` + options;
}

function openCreateSystemModal() {
    document.getElementById("system-modal-title").textContent = "Thêm Hệ Thống Rạp Mới";
    document.getElementById("system-form").reset();
    document.getElementById("system-id").value = "";
    openModal("system-modal");
}

function openEditSystemModal(id, name) {
    document.getElementById("system-modal-title").textContent = `Sửa Hệ Thống Rạp: #${id}`;
    document.getElementById("system-id").value = id;
    document.getElementById("system-name").value = name;
    openModal("system-modal");
}

async function saveSystem(e) {
    e.preventDefault();
    const id = document.getElementById("system-id").value;
    const name = document.getElementById("system-name").value.trim();

    try {
        const method = id ? "PUT" : "POST";
        const url = id ? `/cinema-systems/${id}` : "/cinema-systems";
        const res = await apiFetch(url, { method, body: { name } });
        const data = await res.json();
        if (res.ok && data.success) {
            closeModal("system-modal");
            showToast(id ? "Cập nhật hệ thống rạp thành công!" : "Tạo hệ thống rạp thành công!", "success");
            await loadSystems();
            updateOverviewStats();
        } else {
            showToast(data.message || "Lỗi lưu hệ thống rạp!", "error");
        }
    } catch (err) {
        showToast("Lỗi kết nối máy chủ!", "error");
    }
}

function confirmDeleteSystem(id, name) {
    openDeleteConfirm(`CẢNH BÁO: Xóa hệ thống "${name}" sẽ xóa toàn bộ các cụm rạp và lịch chiếu liên quan! Bạn có chắc không?`, async () => {
        try {
            const res = await apiFetch(`/cinema-systems/${id}`, { method: "DELETE" });
            const data = await res.json().catch(() => null);
            if (res.ok && data && data.success) {
                showToast("Đã xóa hệ thống rạp!", "success");
                await loadSystems();
                await loadCinemas();
                updateOverviewStats();
            } else {
                showToast((data && data.message) ? data.message : "Bạn không đủ quyền thực hiện thao tác này!", "error");
            }
        } catch (e) {
            showToast("Lỗi kết nối máy chủ!", "error");
        }
    });
}

// ==================== TAB 5: CỤM RẠP (CINEMAS) ====================
async function loadCinemas() {
    try {
        const res = await apiFetch("/cinemas");
        if (res.ok) {
            const json = await res.json();
            allCinemas = json.data || [];
            renderCinemasTable();
            populateCinemaDropdowns();
        }
    } catch (e) {
        console.error("Lỗi tải cụm rạp:", e);
    }
}

function renderCinemasTable() {
    const sysVal = document.getElementById("cinema-system-filter").value;
    const searchVal = document.getElementById("cinema-search-input").value.trim().toLowerCase();
    const tbody = document.getElementById("cinemas-table-body");

    let filtered = allCinemas.filter(c => {
        const matchSys = !sysVal || (c.system_id || c.systemId) == sysVal;
        const matchName = !searchVal || c.name.toLowerCase().includes(searchVal);
        return matchSys && matchName;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" class="empty-state">Không có cụm rạp nào phù hợp.</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(c => `
        <tr>
            <td>#${c.id}</td>
            <td style="font-weight:600; color:var(--text-primary);">${c.name}</td>
            <td><span class="badge-tag" style="background:#2980b9;">${c.system_name || c.systemName || "—"}</span></td>
            <td style="text-align: right;">
                <button class="admin-btn btn-sm btn-edit" onclick="openEditCinemaModal(${c.id})">Sửa</button>
                <button class="admin-btn btn-sm btn-delete" onclick="confirmDeleteCinema(${c.id}, '${escapeHtml(c.name)}')">Xóa</button>
            </td>
        </tr>
    `).join("");
}

function populateCinemaDropdowns() {
    const showtimeCinemaSelect = document.getElementById("showtime-form-cinema");
    const showtimeFilterCinema = document.getElementById("showtime-cinema-filter");

    const options = allCinemas.map(c => `<option value="${c.id}">${c.name} (${c.system_name || c.systemName || "Rạp"})</option>`).join("");

    if (showtimeCinemaSelect) showtimeCinemaSelect.innerHTML = `<option value="">-- Chọn rạp --</option>` + options;
    if (showtimeFilterCinema) showtimeFilterCinema.innerHTML = `<option value="">Tất cả rạp</option>` + options;
}

function openCreateCinemaModal() {
    document.getElementById("cinema-modal-title").textContent = "Thêm Cụm Rạp Mới";
    document.getElementById("cinema-form").reset();
    document.getElementById("cinema-id").value = "";
    openModal("cinema-modal");
}

function openEditCinemaModal(id) {
    const c = allCinemas.find(item => item.id === id);
    if (!c) return;
    document.getElementById("cinema-modal-title").textContent = `Sửa Cụm Rạp: #${c.id}`;
    document.getElementById("cinema-id").value = c.id;
    document.getElementById("cinema-form-system").value = c.system_id || c.systemId || "";
    document.getElementById("cinema-name").value = c.name;
    openModal("cinema-modal");
}

async function saveCinema(e) {
    e.preventDefault();
    const id = document.getElementById("cinema-id").value;
    const systemId = parseInt(document.getElementById("cinema-form-system").value);
    const name = document.getElementById("cinema-name").value.trim();

    try {
        const method = id ? "PUT" : "POST";
        const url = id ? `/cinemas/${id}` : "/cinemas";
        const res = await apiFetch(url, { method, body: { systemId, name } });
        const data = await res.json();
        if (res.ok && data.success) {
            closeModal("cinema-modal");
            showToast(id ? "Cập nhật cụm rạp thành công!" : "Thêm cụm rạp thành công!", "success");
            await loadCinemas();
            updateOverviewStats();
        } else {
            showToast(data.message || "Lỗi lưu cụm rạp!", "error");
        }
    } catch (err) {
        showToast("Lỗi kết nối máy chủ!", "error");
    }
}

function confirmDeleteCinema(id, name) {
    openDeleteConfirm(`Bạn có chắc chắn muốn xóa rạp "${name}" không?`, async () => {
        try {
            const res = await apiFetch(`/cinemas/${id}`, { method: "DELETE" });
            const data = await res.json().catch(() => null);
            if (res.ok && data && data.success) {
                showToast("Đã xóa cụm rạp!", "success");
                await loadCinemas();
                updateOverviewStats();
            } else {
                showToast((data && data.message) ? data.message : "Bạn không đủ quyền thực hiện thao tác này!", "error");
            }
        } catch (e) {
            showToast("Lỗi kết nối máy chủ!", "error");
        }
    });
}

// ==================== TAB 6: LIÊN KẾT PHIM - RẠP ====================
async function loadLinks() {
    try {
        const res = await apiFetch("/movie-system-links");
        if (res.ok) {
            const json = await res.json();
            allLinks = json.data || [];
            renderLinksTable();
        }
    } catch (e) {
        console.error("Lỗi tải liên kết:", e);
    }
}

function renderLinksTable() {
    const sysVal = document.getElementById("link-system-filter").value;
    const tbody = document.getElementById("links-table-body");

    let filtered = allLinks.filter(l => !sysVal || (l.system_id || l.systemId) == sysVal);

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="empty-state">Chưa có liên kết đặt vé nào.</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(l => `
        <tr>
            <td>#${l.id}</td>
            <td style="font-weight:600; color:var(--text-primary);">${l.movie_title || l.movieTitle || "Phim #" + (l.movie_id || l.movieId)}</td>
            <td><span class="badge-tag" style="background:#27ae60;">${l.system_name || l.systemName || "Hệ thống"}</span></td>
            <td><a href="${l.detail_link || l.detailLink}" target="_blank" style="color:var(--gold); text-decoration:underline; font-size:13px; word-break:break-all;">${l.detail_link || l.detailLink}</a></td>
            <td style="text-align: right;">
                <button class="admin-btn btn-sm btn-edit" onclick="openEditLinkModal(${l.id})">Sửa</button>
                <button class="admin-btn btn-sm btn-delete" onclick="confirmDeleteLink(${l.id})">Xóa</button>
            </td>
        </tr>
    `).join("");
}

function openCreateLinkModal() {
    document.getElementById("link-modal-title").textContent = "Thêm Liên Kết Đặt Vé";
    document.getElementById("link-form").reset();
    document.getElementById("link-id").value = "";
    openModal("link-modal");
}

function openEditLinkModal(id) {
    const l = allLinks.find(item => item.id === id);
    if (!l) return;
    document.getElementById("link-modal-title").textContent = `Sửa Liên Kết: #${l.id}`;
    document.getElementById("link-id").value = l.id;
    document.getElementById("link-form-movie").value = l.movie_id || l.movieId || "";
    document.getElementById("link-form-system").value = l.system_id || l.systemId || "";
    document.getElementById("link-detail-url").value = l.detail_link || l.detailLink || "";
    openModal("link-modal");
}

async function saveLink(e) {
    e.preventDefault();
    const id = document.getElementById("link-id").value;
    const movieId = parseInt(document.getElementById("link-form-movie").value);
    const systemId = parseInt(document.getElementById("link-form-system").value);
    const detailLink = document.getElementById("link-detail-url").value.trim();

    try {
        const method = id ? "PUT" : "POST";
        const url = id ? `/movie-system-links/${id}` : "/movie-system-links";
        const res = await apiFetch(url, { method, body: { movieId, systemId, detailLink } });
        const data = await res.json();
        if (res.ok && data.success) {
            closeModal("link-modal");
            showToast(id ? "Cập nhật liên kết thành công!" : "Tạo liên kết thành công!", "success");
            await loadLinks();
        } else {
            showToast(data.message || "Lỗi lưu liên kết!", "error");
        }
    } catch (err) {
        showToast("Lỗi kết nối máy chủ!", "error");
    }
}

function confirmDeleteLink(id) {
    openDeleteConfirm(`Bạn có chắc muốn xóa liên kết đặt vé #${id} không?`, async () => {
        try {
            const res = await apiFetch(`/movie-system-links/${id}`, { method: "DELETE" });
            const data = await res.json().catch(() => null);
            if (res.ok && data && data.success) {
                showToast("Đã xóa liên kết!", "success");
                await loadLinks();
            } else {
                showToast((data && data.message) ? data.message : "Bạn không đủ quyền thực hiện thao tác này!", "error");
            }
        } catch (e) {
            showToast("Lỗi kết nối máy chủ!", "error");
        }
    });
}

// ==================== TAB 7: LỊCH CHIẾU (SHOWTIMES) ====================
async function loadShowtimes() {
    try {
        const res = await apiFetch("/showtimes");
        if (res.ok) {
            const json = await res.json();
            allShowtimes = json.data || [];
            renderShowtimesTable();
        }
    } catch (e) {
        console.error("Lỗi tải suất chiếu:", e);
    }
}

function renderShowtimesTable() {
    const movieVal = document.getElementById("showtime-movie-filter").value;
    const cinemaVal = document.getElementById("showtime-cinema-filter").value;
    const dateVal = document.getElementById("showtime-date-filter").value;
    const tbody = document.getElementById("showtimes-table-body");

    let filtered = allShowtimes.filter(s => {
        const matchMovie = !movieVal || (s.movie_id || s.movieId) == movieVal;
        const matchCinema = !cinemaVal || (s.cinema_id || s.cinemaId) == cinemaVal;
        const matchDate = !dateVal || (s.show_date || s.showDate) === dateVal;
        return matchMovie && matchCinema && matchDate;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-state">Không có suất chiếu nào phù hợp.</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(s => {
        const date = s.show_date || s.showDate || "—";
        const time = s.show_time || s.showTime || "—";
        const version = s.movie_version || s.movieVersion || "2D";
        return `
            <tr>
                <td>#${s.id}</td>
                <td style="font-weight:600; color:var(--text-primary);">${s.movie_title || s.movieTitle || "Phim #" + (s.movie_id || s.movieId)}</td>
                <td>${s.cinema_name || s.cinemaName || "Rạp #" + (s.cinema_id || s.cinemaId)}</td>
                <td><span style="color:#3498db; font-weight:600;">📅 ${date}</span></td>
                <td><span style="color:var(--gold); font-weight:700;">⏰ ${time}</span></td>
                <td><span class="badge-tag" style="background:#8e44ad;">${version}</span></td>
                <td style="text-align: right;">
                    <button class="admin-btn btn-sm btn-edit" onclick="openEditShowtimeModal(${s.id})">Sửa</button>
                    <button class="admin-btn btn-sm btn-delete" onclick="confirmDeleteShowtime(${s.id})">Xóa</button>
                </td>
            </tr>
        `;
    }).join("");
}

function openCreateShowtimeModal() {
    document.getElementById("showtime-modal-title").textContent = "Thêm Suất Chiếu Mới";
    document.getElementById("showtime-form").reset();
    document.getElementById("showtime-id").value = "";
    openModal("showtime-modal");
}

function openEditShowtimeModal(id) {
    const s = allShowtimes.find(item => item.id === id);
    if (!s) return;
    document.getElementById("showtime-modal-title").textContent = `Sửa Suất Chiếu: #${s.id}`;
    document.getElementById("showtime-id").value = s.id;
    document.getElementById("showtime-form-movie").value = s.movie_id || s.movieId || "";
    document.getElementById("showtime-form-cinema").value = s.cinema_id || s.cinemaId || "";
    document.getElementById("showtime-date").value = s.show_date || s.showDate || "";

    // Format time to HH:mm
    let rawTime = s.show_time || s.showTime || "";
    if (rawTime.length > 5) rawTime = rawTime.substring(0, 5);
    document.getElementById("showtime-time").value = rawTime;
    document.getElementById("showtime-version").value = s.movie_version || s.movieVersion || "";
    openModal("showtime-modal");
}

async function saveShowtime(e) {
    e.preventDefault();
    const id = document.getElementById("showtime-id").value;
    const movieId = parseInt(document.getElementById("showtime-form-movie").value);
    const cinemaId = parseInt(document.getElementById("showtime-form-cinema").value);
    const showDate = document.getElementById("showtime-date").value;
    let showTime = document.getElementById("showtime-time").value;
    if (showTime.length === 5) showTime += ":00"; // Định dạng HH:mm:ss
    const movieVersion = document.getElementById("showtime-version").value.trim();

    const payload = { movieId, cinemaId, showDate, showTime, movieVersion };

    try {
        const method = id ? "PUT" : "POST";
        const url = id ? `/showtimes/${id}` : "/showtimes";
        const res = await apiFetch(url, { method, body: payload });
        const data = await res.json();
        if (res.ok && data.success) {
            closeModal("showtime-modal");
            showToast(id ? "Cập nhật suất chiếu thành công!" : "Tạo suất chiếu thành công!", "success");
            await loadShowtimes();
            updateOverviewStats();
        } else {
            showToast(data.message || "Lỗi lưu suất chiếu!", "error");
        }
    } catch (err) {
        showToast("Lỗi kết nối máy chủ!", "error");
    }
}

function confirmDeleteShowtime(id) {
    openDeleteConfirm(`Bạn có chắc muốn xóa suất chiếu #${id} không?`, async () => {
        try {
            const res = await apiFetch(`/showtimes/${id}`, { method: "DELETE" });
            const data = await res.json().catch(() => null);
            if (res.ok && data && data.success) {
                showToast("Đã xóa suất chiếu!", "success");
                await loadShowtimes();
                updateOverviewStats();
            } else {
                showToast((data && data.message) ? data.message : "Bạn không đủ quyền thực hiện thao tác này!", "error");
            }
        } catch (e) {
            showToast("Lỗi kết nối máy chủ!", "error");
        }
    });
}

// ==================== TAB 8: QUẢN LÝ NGƯỜI DÙNG (USERS) ====================
async function loadUsers() {
    try {
        const res = await apiFetch("/users");
        if (res.ok) {
            const json = await res.json();
            allUsers = json.data || [];
            renderUsersTable();
        }
    } catch (e) {
        console.error("Lỗi tải danh sách người dùng:", e);
    }
}

function renderUsersTable() {
    const searchVal = document.getElementById("user-search-input") ? document.getElementById("user-search-input").value.trim().toLowerCase() : "";
    const roleVal = document.getElementById("user-role-filter") ? document.getElementById("user-role-filter").value : "";
    const statusVal = document.getElementById("user-status-filter") ? document.getElementById("user-status-filter").value : "";
    const tbody = document.getElementById("users-table-body");
    if (!tbody) return;

    const isCurrentAdmin = currentUser && currentUser.role === "ADMIN";

    let filtered = allUsers.filter(u => {
        const matchSearch = !searchVal ||
            (u.username && u.username.toLowerCase().includes(searchVal)) ||
            (u.email && u.email.toLowerCase().includes(searchVal));
        const matchRole = !roleVal || u.role === roleVal;
        let matchStatus = true;
        if (statusVal === "active") matchStatus = Boolean(u.enabled);
        if (statusVal === "banned") matchStatus = !Boolean(u.enabled);
        return matchSearch && matchRole && matchStatus;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="empty-state">Không tìm thấy tài khoản nào phù hợp.</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(u => {
        const isMe = currentUser && currentUser.id === u.id;
        const isTargetAdmin = u.role === "ADMIN";
        const isTargetStaff = u.role === "STAFF";
        const isTargetUser = u.role === "USER";
        const roleColor = isTargetAdmin ? "background:#f1800f; color:#000;" : (isTargetStaff ? "background:#e67e22; color:#fff;" : "background:#34495e;");
        const statusBadge = u.enabled
            ? `<span style="background:rgba(46,204,113,0.2); color:#2ecc71; padding:3px 8px; border-radius:12px; font-size:12px; font-weight:bold;">● Hoạt động</span>`
            : `<span style="background:rgba(231,76,60,0.2); color:#e74c3c; padding:3px 8px; border-radius:12px; font-size:12px; font-weight:bold;">● Đã khóa</span>`;

        const avatar = (u.avatarUrl && u.avatarUrl.trim() !== "")
            ? u.avatarUrl.trim()
            : (u.avatar_url && u.avatar_url.trim() !== "" ? u.avatar_url.trim() : "/media/logo.png");
        const createdDate = u.createdAt || u.created_at || "—";
        const formattedDate = createdDate !== "—" ? createdDate.substring(0, 10) : "—";

        // Phân quyền nút thao tác:
        // - ADMIN: toàn quyền quản lý staff, user (sửa, khóa, xóa).
        // - STAFF: quản lý user (sửa info, khóa/mở khóa); KHÔNG ĐƯỢC khóa/hạ cấp staff khác hoặc admin; KHÔNG ĐƯỢC xóa user (chỉ khóa).
        let actionButtons = "";
        if (!isCurrentAdmin) {
            // Đang đăng nhập tài khoản STAFF
            if (isTargetAdmin) {
                actionButtons = `<span style="color:var(--text-muted); font-size:12px; font-style:italic;" title="Không đủ quyền thao tác trên Quản trị viên">🔒 Quản trị viên</span>`;
            } else if (isTargetStaff && !isMe) {
                actionButtons = `<span style="color:var(--text-muted); font-size:12px; font-style:italic;" title="Chỉ Quản trị viên (ADMIN) mới có quyền quản lý Cộng tác viên khác">🔒 Cộng tác viên</span>`;
            } else {
                // Là USER thường hoặc chính tài khoản STAFF của mình
                actionButtons = `<button class="admin-btn btn-sm btn-edit" onclick="openEditUserModal(${u.id})">Sửa</button>`;
                if (!isMe && isTargetUser) {
                    // STAFF có quyền khóa tài khoản USER thường
                    actionButtons += `
                        <button class="admin-btn btn-sm" style="background:${u.enabled ? '#e67e22' : '#27ae60'}; color:#fff;" onclick="toggleUserStatus(${u.id}, '${escapeHtml(u.username)}', ${Boolean(u.enabled)})">
                            ${u.enabled ? 'Khóa' : 'Kích hoạt'}
                        </button>
                    `;
                }
            }
        } else {
            // Đang đăng nhập tài khoản ADMIN (Toàn quyền)
            actionButtons = `<button class="admin-btn btn-sm btn-edit" onclick="openEditUserModal(${u.id})">Sửa</button>`;
            if (!isMe) {
                actionButtons += `
                    <button class="admin-btn btn-sm" style="background:${u.enabled ? '#e67e22' : '#27ae60'}; color:#fff;" onclick="toggleUserStatus(${u.id}, '${escapeHtml(u.username)}', ${Boolean(u.enabled)})">
                        ${u.enabled ? 'Khóa' : 'Kích hoạt'}
                    </button>
                    <button class="admin-btn btn-sm btn-delete" onclick="confirmDeleteUser(${u.id}, '${escapeHtml(u.username)}')">Xóa</button>
                `;
            }
        }

        return `
            <tr>
                <td>#${u.id}</td>
                <td><img src="${avatar}" alt="avatar" style="width:36px; height:36px; border-radius:50%; object-fit:cover; border:1px solid var(--border-color); background: var(--bg-card);" onerror="this.src='/media/logo.png'"></td>
                <td style="font-weight:600; color:var(--text-primary);">
                    ${u.username}
                    ${isMe ? `<span style="font-size:11px; background:#2980b9; color:#fff; padding:1px 6px; border-radius:4px; margin-left:6px;">Bạn</span>` : ""}
                </td>
                <td style="color:var(--text-secondary);">${u.email}</td>
                <td><span class="badge-tag" style="${roleColor} font-weight:bold;">${u.role}</span></td>
                <td>${statusBadge}</td>
                <td><span style="color:var(--text-muted); font-size:13px;">${formattedDate}</span></td>
                <td style="text-align: right; white-space: nowrap;">
                    ${actionButtons}
                </td>
            </tr>
        `;
    }).join("");
}

function openCreateUserModal() {
    const isCurrentAdmin = currentUser && currentUser.role === "ADMIN";
    document.getElementById("user-modal-title").textContent = "Thêm Tài Khoản Mới";
    document.getElementById("user-form").reset();
    document.getElementById("user-id").value = "";
    document.getElementById("user-username").disabled = false;
    document.getElementById("form-group-username").style.opacity = "1";
    document.getElementById("user-password").required = true;
    document.getElementById("user-password-label").textContent = "Mật khẩu *";
    document.getElementById("user-password-hint").style.display = "none";

    // Phân quyền chọn Role: Chỉ ADMIN mới có quyền cấp vai trò STAFF hoặc ADMIN
    const roleSelect = document.getElementById("user-role");
    const adminOpt = roleSelect.querySelector('option[value="ADMIN"]');
    const staffOpt = roleSelect.querySelector('option[value="STAFF"]');
    if (!isCurrentAdmin) {
        roleSelect.disabled = true;
        roleSelect.title = "Chỉ Quản trị viên (ADMIN) mới có quyền cấp vai trò STAFF hoặc ADMIN";
        roleSelect.value = "USER";
    } else {
        roleSelect.disabled = false;
        roleSelect.title = "";
        roleSelect.value = "USER";
        if (adminOpt) adminOpt.disabled = false;
        if (staffOpt) staffOpt.disabled = false;
    }

    openModal("user-modal");
}

function openEditUserModal(id) {
    const u = allUsers.find(item => item.id === id);
    if (!u) return;

    const isCurrentAdmin = currentUser && currentUser.role === "ADMIN";
    if (!isCurrentAdmin) {
        if (u.role === "ADMIN") {
            showToast("Bạn không đủ quyền: Không thể chỉnh sửa tài khoản Quản trị viên (ADMIN)!", "error");
            return;
        }
        if (u.role === "STAFF" && (!currentUser || currentUser.id !== u.id)) {
            showToast("Bạn không đủ quyền: Chỉ Quản trị viên mới có thể quản lý Cộng tác viên khác!", "error");
            return;
        }
    }

    document.getElementById("user-modal-title").textContent = `Chỉnh Sửa Tài Khoản: ${u.username} (#${u.id})`;
    document.getElementById("user-id").value = u.id;
    document.getElementById("user-username").value = u.username;
    document.getElementById("user-username").disabled = true; // Không cho đổi username
    document.getElementById("form-group-username").style.opacity = "0.7";
    document.getElementById("user-email").value = u.email;
    document.getElementById("user-password").value = "";
    document.getElementById("user-password").required = false; // Đổi mật khẩu là tùy chọn
    document.getElementById("user-password-label").textContent = "Mật khẩu mới (Tùy chọn)";
    document.getElementById("user-password-hint").style.display = "block";

    // Phân quyền chọn Role: Chỉ ADMIN mới có quyền cấp role cho user
    const roleSelect = document.getElementById("user-role");
    roleSelect.value = u.role;
    if (!isCurrentAdmin) {
        roleSelect.disabled = true;
        roleSelect.title = "Chỉ Quản trị viên (ADMIN) mới có quyền cấp hoặc đổi vai trò (Role)";
    } else {
        roleSelect.disabled = false;
        roleSelect.title = "";
    }

    document.getElementById("user-enabled").value = String(Boolean(u.enabled));
    document.getElementById("user-avatar").value = u.avatarUrl || u.avatar_url || "";
    openModal("user-modal");
}

async function saveUser(e) {
    e.preventDefault();
    const isCurrentAdmin = currentUser && currentUser.role === "ADMIN";
    const id = document.getElementById("user-id").value;
    const email = document.getElementById("user-email").value.trim();
    const roleSelect = document.getElementById("user-role");
    let role = roleSelect.value;
    const enabled = document.getElementById("user-enabled").value === "true";
    const password = document.getElementById("user-password").value;
    const avatarUrl = document.getElementById("user-avatar").value.trim() || null;

    // Kiểm tra phân quyền client-side
    if (!isCurrentAdmin) {
        if (id) {
            const originalUser = allUsers.find(item => item.id == id);
            if (originalUser && originalUser.role === "ADMIN") {
                showToast("Bạn không đủ quyền: Không thể chỉnh sửa tài khoản Quản trị viên (ADMIN)!", "error");
                return;
            }
            if (originalUser && originalUser.role === "STAFF" && (!currentUser || currentUser.id != id)) {
                showToast("Bạn không đủ quyền: Không thể chỉnh sửa tài khoản của Cộng tác viên khác!", "error");
                return;
            }
            if (originalUser && role !== originalUser.role) {
                showToast("Bạn không đủ quyền: Chỉ Quản trị viên (ADMIN) mới có quyền cấp hoặc đổi vai trò!", "error");
                return;
            }
        } else {
            // Khi STAFF tạo mới, luôn mặc định là role USER
            role = "USER";
        }
    }

    let payload;
    let method;
    let url;

    if (id) {
        // Cập nhật người dùng
        method = "PUT";
        url = `/users/${id}`;
        payload = { email, role, enabled, avatarUrl };
        if (password) payload.password = password;
    } else {
        // Thêm người dùng mới
        method = "POST";
        url = "/users";
        const username = document.getElementById("user-username").value.trim();
        payload = { username, email, password, role, enabled, avatarUrl };
    }

    try {
        const res = await apiFetch(url, { method, body: payload });
        const data = await res.json().catch(() => null);
        if (res.ok && data && data.success) {
            closeModal("user-modal");
            showToast(id ? "Cập nhật tài khoản thành công!" : "Tạo tài khoản mới thành công!", "success");
            await loadUsers();
            updateOverviewStats();
        } else {
            showToast((data && data.message) ? data.message : "Bạn không đủ quyền thực hiện thao tác này!", "error");
        }
    } catch (err) {
        showToast("Lỗi kết nối máy chủ!", "error");
    }
}

async function toggleUserStatus(id, username, currentStatus) {
    const isCurrentAdmin = currentUser && currentUser.role === "ADMIN";
    const targetUser = allUsers.find(item => item.id === id);

    // STAFF chỉ có quyền khóa tài khoản USER, không có quyền khóa staff khác hay admin
    if (!isCurrentAdmin && targetUser && targetUser.role !== "USER") {
        showToast("Bạn không đủ quyền: Cộng tác viên chỉ có thể khóa tài khoản Người dùng (USER), không thể khóa Cộng tác viên khác hoặc Quản trị viên!", "error");
        return;
    }

    if (currentStatus) {
        // Hành động: KHÓA TÀI KHOẢN
        openConfirmModal({
            title: "Khóa tài khoản?",
            message: `Bạn có chắc chắn muốn khóa tài khoản "${username}" không? Người dùng này sẽ không thể đăng nhập vào hệ thống.`,
            icon: "🔒",
            btnText: "Khóa Tài Khoản",
            btnColor: "#e67e22", // Màu cam cảnh báo
            onConfirm: async () => {
                await executeToggleStatus(id);
            }
        });
    } else {
        // Hành động: KÍCH HOẠT TÀI KHOẢN
        openConfirmModal({
            title: "Kích hoạt tài khoản?",
            message: `Bạn có chắc chắn muốn kích hoạt lại tài khoản "${username}" không? Người dùng sẽ có thể đăng nhập bình thường.`,
            icon: "🔓",
            btnText: "Kích Hoạt Tài Khoản",
            btnColor: "#27ae60", // Màu xanh lá tích cực
            onConfirm: async () => {
                await executeToggleStatus(id);
            }
        });
    }
}

async function executeToggleStatus(id) {
    try {
        const res = await apiFetch(`/users/${id}/toggle-status`, { method: "PUT" });
        const data = await res.json().catch(() => null);
        if (res.ok && data && data.success) {
            showToast(data.message, "success");
            await loadUsers();
        } else {
            showToast((data && data.message) ? data.message : "Bạn không đủ quyền thực hiện thao tác này!", "error");
        }
    } catch (e) {
        showToast("Lỗi kết nối máy chủ!", "error");
    }
}

function confirmDeleteUser(id, username) {
    const isCurrentAdmin = currentUser && currentUser.role === "ADMIN";
    if (!isCurrentAdmin) {
        showToast("Bạn không đủ quyền: Cộng tác viên không có quyền xóa tài khoản. Bạn chỉ có thể khóa tài khoản Người dùng!", "error");
        return;
    }
    openDeleteConfirm(`CẢNH BÁO: Bạn có chắc muốn xóa vĩnh viễn tài khoản "${username}" (ID: #${id})? Tất cả dữ liệu đánh giá, theo dõi của tài khoản này sẽ bị xóa.`, async () => {
        try {
            const res = await apiFetch(`/users/${id}`, { method: "DELETE" });
            const data = await res.json().catch(() => null);
            if (res.ok && data && data.success) {
                showToast("Đã xóa tài khoản thành công!", "success");
                await loadUsers();
                updateOverviewStats();
            } else {
                showToast((data && data.message) ? data.message : "Bạn không đủ quyền thực hiện thao tác này!", "error");
            }
        } catch (e) {
            showToast("Lỗi kết nối máy chủ!", "error");
        }
    });
}

// Utility: Escape HTML
function escapeHtml(text) {
    if (!text) return "";
    return text.toString().replace(/'/g, "\\'").replace(/"/g, "&quot;");
}

