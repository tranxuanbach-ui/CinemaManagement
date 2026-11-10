let API_BASE = "/api";

async function apiFetch(path, options = {}) {
    const fetchOpts = { credentials: "same-origin", ...options };
    return await fetch(`${API_BASE}${path}`, fetchOpts);
}

document.addEventListener("DOMContentLoaded", async function () {
    await initAuthUI();

    try {
        const res = await apiFetch("/movies");
        if (!res.ok) return;
        const rawData = await res.json();
        const allMovies = Array.isArray(rawData) ? rawData : (rawData.data || []);
        window.__ALL_MOVIES_CACHE__ = allMovies;

        renderHeaderGenres(allMovies);
        setupRandomMovieButton(allMovies);

        if (document.getElementById("hot-slider")) {
            renderHomePage(allMovies);
        }

        if (document.getElementById("catalog-movies-grid")) {
            renderCatalogOrSearchPage(allMovies);
        }

        if (document.querySelector(".detail-page")) {
            await renderMovieDetailPage();
        }

        if (document.getElementById("following-movies-grid")) {
            renderFollowingPage();
        }
    } catch (error) {
        console.error("Lỗi tải dữ liệu ứng dụng:", error);
    }

    // Xử lý nút Back / Forward trên trình duyệt mà không reload trang
    window.addEventListener("popstate", () => {
        const p = parseInt(new URLSearchParams(window.location.search).get("page") || "1");
        const grid = document.getElementById("new-movies-grid") || document.getElementById("catalog-movies-grid");
        if (grid && window.__ALL_MOVIES_CACHE__) {
            const pageSize = grid.id === "new-movies-grid" ? 20 : 15;
            const movies = (grid.id === "new-movies-grid")
                ? (window.__NOW_SHOWING_MOVIES__ || window.__ALL_MOVIES_CACHE__)
                : window.__ALL_MOVIES_CACHE__;
            paginateAndRender(movies, grid, pageSize, p);
            scrollToSection("danh-sach-phim");
        }
    });
});

// ==================== HỆ THỐNG ĐĂNG NHẬP / ĐĂNG KÝ ====================

async function initAuthUI() {
    const authSection = document.getElementById("auth-section");
    if (!authSection) return;

    injectAuthModals();

    let currentUser = null;
    try {
        const res = await apiFetch("/auth/me");
        if (res.ok) {
            const data = await res.json();
            currentUser = data.data || data;
        }
    } catch (_) {}

    window.__CURRENT_USER__ = currentUser;

    const headerContainer = document.querySelector(".header-container");

    if (currentUser && (currentUser.username || currentUser.email)) {
        const displayName = currentUser.username || currentUser.email;
        const isAdmin = currentUser.role === "ADMIN" || currentUser.role === "STAFF";

        if (headerContainer) {
            if (isAdmin) {
                headerContainer.classList.add("has-admin");
                headerContainer.classList.remove("no-admin");
            } else {
                headerContainer.classList.remove("has-admin");
                headerContainer.classList.add("no-admin");
            }
        }

        authSection.innerHTML = `
            <div style="display: flex; align-items: center; gap: 10px;">
                ${isAdmin ? `<a href="/admin" class="filter-button" style="text-decoration: none; display: inline-flex; align-items: center; gap: 4px; padding: 5px 12px; border-radius: 14px; background: #f1800f; color: #fff; font-size: 12px; font-weight: bold; white-space: nowrap;" title="Vào trang quản trị">⚙ Quản trị</a>` : ""}
                <span style="font-size: 13px; font-weight: 600; color: var(--text-main); white-space: nowrap; display: inline-flex; align-items: center; gap: 4px;">
                    👤 ${displayName}
                </span>
                <button type="button" id="logout-btn" class="theme-toggle" style="width: auto; min-width: unset; padding: 5px 12px; border-radius: 14px; font-size: 12px; font-weight: 600;">
                    Thoát
                </button>
            </div>
        `;

        document.getElementById("logout-btn").addEventListener("click", async () => {
            await apiFetch("/auth/logout", { method: "POST" });
            window.location.reload();
        });
    } else {
        if (headerContainer) {
            headerContainer.classList.remove("has-admin");
            headerContainer.classList.add("no-admin");
        }
        authSection.innerHTML = `
            <button type="button" id="open-auth-modal-btn" class="filter-button" style="padding: 7px 16px; border-radius: 15px;">
                Đăng nhập
            </button>
        `;
        document.getElementById("open-auth-modal-btn").addEventListener("click", () => {
            document.getElementById("auth-modal-backdrop").style.display = "flex";
        });
    }
}

function injectAuthModals() {
    if (document.getElementById("auth-modal-backdrop")) return;

    const modalHTML = `
        <div id="auth-modal-backdrop" style="display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.75); z-index: 9999; align-items: center; justify-content: center;">
            <div style="background: var(--bg-card); color: var(--text-main); width: min(400px, 92%); padding: 28px; border-radius: 12px; position: relative; box-shadow: 0 15px 40px rgba(0,0,0,0.6); border: 1px solid #444;">
                <button type="button" id="close-auth-modal" style="position: absolute; top: 10px; right: 14px; background: transparent; border: none; color: var(--text-muted); font-size: 26px; cursor: pointer;">×</button>
                
                <div style="display: flex; gap: 15px; margin-bottom: 20px; border-bottom: 1px solid #444; padding-bottom: 10px;">
                    <button type="button" id="tab-login-btn" style="background: none; border: none; color: var(--accent-color); font-size: 16px; font-weight: bold; cursor: pointer;">Đăng nhập</button>
                    <button type="button" id="tab-register-btn" style="background: none; border: none; color: var(--text-muted); font-size: 16px; font-weight: bold; cursor: pointer;">Đăng ký</button>
                </div>

                <div id="auth-error-msg" style="display: none; background: rgba(231, 76, 60, 0.2); border: 1px solid #e74c3c; color: #ff6b6b; padding: 10px; border-radius: 6px; font-size: 13px; margin-bottom: 15px;"></div>

                <form id="login-form" style="display: flex; flex-direction: column; gap: 14px;">
                    <input type="text" id="login-username" placeholder="Tên đăng nhập hoặc Email" required
                           style="padding: 11px 14px; border-radius: 6px; border: 1px solid #555; background: var(--bg-main); color: var(--text-main); outline: none;" />
                    <input type="password" id="login-password" placeholder="Mật khẩu" required
                           style="padding: 11px 14px; border-radius: 6px; border: 1px solid #555; background: var(--bg-main); color: var(--text-main); outline: none;" />
                    <button type="submit" class="apply-filter" style="margin-top: 5px;">ĐĂNG NHẬP</button>
                </form>

                <form id="register-form" style="display: none; flex-direction: column; gap: 14px;">
                    <input type="text" id="reg-username" placeholder="Tên đăng nhập" required
                           style="padding: 11px 14px; border-radius: 6px; border: 1px solid #555; background: var(--bg-main); color: var(--text-main); outline: none;" />
                    <input type="email" id="reg-email" placeholder="Email" required
                           style="padding: 11px 14px; border-radius: 6px; border: 1px solid #555; background: var(--bg-main); color: var(--text-main); outline: none;" />
                    <input type="password" id="reg-password" placeholder="Mật khẩu (tối thiểu 6 ký tự)" required
                           style="padding: 11px 14px; border-radius: 6px; border: 1px solid #555; background: var(--bg-main); color: var(--text-main); outline: none;" />
                    <button type="submit" class="apply-filter" style="margin-top: 5px;">TẠO TÀI KHOẢN</button>
                </form>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML("beforeend", modalHTML);

    const backdrop = document.getElementById("auth-modal-backdrop");
    const loginForm = document.getElementById("login-form");
    const registerForm = document.getElementById("register-form");
    const tabLogin = document.getElementById("tab-login-btn");
    const tabRegister = document.getElementById("tab-register-btn");
    const errBox = document.getElementById("auth-error-msg");

    const showError = (msg) => {
        errBox.textContent = msg;
        errBox.style.display = "block";
    };

    document.getElementById("close-auth-modal").addEventListener("click", () => {
        backdrop.style.display = "none";
    });

    tabLogin.addEventListener("click", () => {
        errBox.style.display = "none";
        loginForm.style.display = "flex";
        registerForm.style.display = "none";
        tabLogin.style.color = "var(--accent-color)";
        tabRegister.style.color = "var(--text-muted)";
    });

    tabRegister.addEventListener("click", () => {
        errBox.style.display = "none";
        loginForm.style.display = "none";
        registerForm.style.display = "flex";
        tabRegister.style.color = "var(--accent-color)";
        tabLogin.style.color = "var(--text-muted)";
    });

    loginForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        errBox.style.display = "none";
        const username = document.getElementById("login-username").value.trim();
        const password = document.getElementById("login-password").value;

        try {
            const res = await apiFetch("/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    username: username,
                    email: username,
                    usernameOrEmail: username,
                    password: password
                })
            });
            const data = await res.json();
            if (res.ok && data.success !== false) {
                window.location.reload();
            } else {
                showError(data.message || "Sai thông tin đăng nhập!");
            }
        } catch (_) {
            showError("Lỗi kết nối tới máy chủ!");
        }
    });

    registerForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        errBox.style.display = "none";
        const username = document.getElementById("reg-username").value.trim();
        const email = document.getElementById("reg-email").value.trim();
        const password = document.getElementById("reg-password").value;

        try {
            const res = await apiFetch("/auth/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, email, password })
            });
            const data = await res.json();
            if (res.ok && data.success !== false) {
                alert("Đăng ký thành công! Vui lòng đăng nhập.");
                tabLogin.click();
                document.getElementById("login-username").value = username;
            } else {
                const detail = data.data && typeof data.data === "object"
                    ? Object.values(data.data).join(", ")
                    : data.message;
                showError(detail || "Đăng ký không thành công!");
            }
        } catch (_) {
            showError("Lỗi kết nối tới máy chủ!");
        }
    });
}

// ==================== TRANG THEO DÕI (FOLLOWING.HTML) ====================

async function renderFollowingPage() {
    const grid = document.getElementById("following-movies-grid");
    if (!grid) return;

    if (!window.__CURRENT_USER__) {
        grid.innerHTML = `<p style="color: #e74c3c; grid-column: 1 / -1; padding: 20px 0; font-size: 16px;">Vui lòng <a href="#" onclick="document.getElementById('auth-modal-backdrop').style.display='flex'; return false;" style="color: var(--accent-color); text-decoration: underline;">Đăng nhập</a> để xem danh sách phim đang theo dõi.</p>`;
        return;
    }

    try {
        const res = await apiFetch("/user/wishlist/my");
        const data = await res.json();
        const items = data.data || [];

        if (items.length === 0) {
            grid.innerHTML = `<p style="color: var(--text-muted); grid-column: 1 / -1; padding: 20px 0; font-size: 16px;">Bạn chưa theo dõi phim nào.</p>`;
            return;
        }

        grid.innerHTML = items.map(item => {
            const mId = item.movieId || item.movie_id;
            const title = item.movieTitle || item.movie_title || "Phim";
            const poster = item.moviePosterUrl || item.movie_poster_url || "/media/logo.png";
            
            return `
                <div class="movie-card" style="position: relative;">
                    <a href="/series_detail.html?id=${mId}" style="text-decoration: none; color: inherit; display: flex; flex-direction: column; height: 100%;">
                        <img src="${poster}" alt="${title}" class="poster-img" />
                        <div class="movie-info">
                            <h3>${title}</h3>
                        </div>
                    </a>
                    <button onclick="removeFollowingPageItem(event, ${mId})" style="position: absolute; top: 10px; right: 10px; background: rgba(231, 76, 60, 0.9); color: white; border: none; padding: 6px 12px; border-radius: 6px; cursor: pointer; font-size: 12px; font-weight: bold; z-index: 10; transition: 0.2s;">Bỏ theo dõi</button>
                </div>
            `;
        }).join("");
    } catch (err) {
        grid.innerHTML = `<p style="color: #e74c3c; grid-column: 1 / -1; padding: 20px 0;">Không thể tải danh sách theo dõi lúc này.</p>`;
    }
}

window.removeFollowingPageItem = async function(event, movieId) {
    event.preventDefault();
    if(confirm("Bạn có chắc chắn muốn bỏ theo dõi phim này?")) {
        await apiFetch(`/user/wishlist/movie/${movieId}`, { method: "DELETE" });
        renderFollowingPage(); 
    }
};

// ==================== HIỂN THỊ PHIM & CHI TIẾT (Trang chủ, Tìm kiếm...) ====================

function normalizeMovie(m) {
    const rawRating = typeof m.rating === "number" ? m.rating : parseFloat(m.rating);
    const validRating = !isNaN(rawRating) && rawRating > 0 ? Math.round(rawRating * 10) / 10 : null;
    return {
        id: m.id,
        title: m.title || "Chưa có tên",
        posterUrl: m.posterUrl || m.poster_url || "/media/logo.png",
        trailer: m.trailer || null,
        rating: validRating,
        releaseDate: m.releaseDate || m.release_date || null,
        runtimeMinutes: m.runtimeMinutes || m.runtime_minutes || null,
        description: m.description || "Đang cập nhật nội dung...",
        genres: Array.isArray(m.genres)
            ? m.genres.map((g) => (typeof g === "string" ? g : g.name))
            : []
    };
}

function formatDateVN(dateStr) {
    if (!dateStr) return "Chưa cập nhật";
    const parts = dateStr.toString().split("-");
    if (parts.length === 3 && parts[0].length === 4) {
        return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    return dateStr;
}

function renderDetailMovieRating(rating) {
    const ratingValueBox = document.querySelector(".rating-value");
    if (!ratingValueBox) return;
    const num = typeof rating === "number" ? rating : parseFloat(rating);
    if (!isNaN(num) && num > 0) {
        ratingValueBox.innerHTML = `<span class="star-icon">★</span> <span id="movie-avg-rating">${Math.round(num * 10) / 10}/10</span>`;
    } else {
        ratingValueBox.innerHTML = `<span id="movie-avg-rating" style="color: var(--text-muted); font-size: 15px;">Không có đánh giá</span>`;
    }
}

function createMovieCardHTML(movie, showReleaseDate = false) {
    const m = normalizeMovie(movie);
    const ratingDisplay = m.rating != null
        ? `Rating: ★ ${m.rating}`
        : "Không có đánh giá";

    const subText =
        showReleaseDate && m.releaseDate
            ? `Ngày chiếu: ${formatDateVN(m.releaseDate)}`
            : ratingDisplay;

    return `
        <div class="movie-card">
            <a href="/series_detail.html?id=${m.id}" style="text-decoration: none; color: inherit; display: flex; flex-direction: column; height: 100%;">
                <img src="${m.posterUrl}" alt="${m.title}" class="poster-img" />
                <div class="movie-info">
                    <h3>${m.title}</h3>
                    <p>${subText}</p>
                </div>
            </a>
        </div>
    `;
}

function setupRandomMovieButton(allMovies) {
    const randomBtn = document.getElementById("random-movie-btn");
    if (randomBtn && allMovies.length > 0) {
        randomBtn.addEventListener("click", function (e) {
            e.preventDefault();
            const randomMovie = allMovies[Math.floor(Math.random() * allMovies.length)];
            window.location.href = `/series_detail.html?id=${randomMovie.id}`;
        });
    }
}

function renderHeaderGenres(movies) {
    const genreSet = new Set();
    movies.forEach((m) => {
        normalizeMovie(m).genres.forEach((g) => {
            if (g) genreSet.add(g);
        });
    });

    const sortedGenres = Array.from(genreSet).sort();

    document.querySelectorAll(".dropdown-content").forEach((dd) => {
        dd.innerHTML = sortedGenres
            .map((g) => `<a href="/the_loai.html?ten=${encodeURIComponent(g)}">${g}</a>`)
            .join("");
    });

    const filterGenreList = document.getElementById("filter-genre-list");
    if (filterGenreList) {
        const urlGenres = new URLSearchParams(window.location.search).getAll("genre");
        filterGenreList.innerHTML = sortedGenres
            .map(
                (g) => `
                <label class="filter-checkbox">
                    <input type="checkbox" name="genre" value="${g}" ${urlGenres.includes(g) ? "checked" : ""} />
                    ${g}
                </label>
            `
            )
            .join("");
    }
}

function renderHomePage(allMovies) {
    const today = new Date().toISOString().split("T")[0];

    const hotMovies = [...allMovies]
        .sort((a, b) => (b.rating || 0) - (a.rating || 0))
        .slice(0, 12);
    const hotSlider = document.getElementById("hot-slider");
    if (hotSlider) {
        hotSlider.innerHTML = hotMovies.map((m) => createMovieCardHTML(m, false)).join("");
    }

    const upcomingMovies = allMovies.filter((m) => {
        const d = m.releaseDate || m.release_date;
        return d && d > today;
    });
    const upcomingSlider = document.getElementById("upcoming-slider");
    const upcomingSection = document.getElementById("upcoming-section");
    if (upcomingSlider) {
        if (upcomingMovies.length > 0) {
            upcomingSlider.innerHTML = upcomingMovies.map((m) => createMovieCardHTML(m, true)).join("");
        } else if (upcomingSection) {
            upcomingSection.style.display = "none";
        }
    }

    // Phim mới cập nhật / mới ra mắt: chỉ hiện các phim đã/đang chiếu (<= today)
    // Tuyệt đối không bao gồm phim sắp chiếu (> today)
    // Sắp xếp theo thứ tự ngày chiếu mới nhất lên đầu
    const nowShowingMovies = allMovies
        .filter((m) => {
            const d = m.releaseDate || m.release_date;
            return !d || d <= today;
        })
        .sort((a, b) => {
            const da = a.releaseDate || a.release_date || "";
            const db = b.releaseDate || b.release_date || "";
            return db.localeCompare(da);
        });

    window.__NOW_SHOWING_MOVIES__ = nowShowingMovies;

    const grid = document.getElementById("new-movies-grid");
    if (grid) {
        paginateAndRender(nowShowingMovies, grid, 20);

        // Nếu mở trang đã có sẵn param ?page= lớn hơn 1 hoặc hash #danh-sach-phim thì cuộn thẳng xuống phần phim mới
        const initPage = parseInt(new URLSearchParams(window.location.search).get("page") || "1");
        if (window.location.hash === "#danh-sach-phim" || initPage > 1) {
            setTimeout(() => {
                scrollToSection("danh-sach-phim");
            }, 150);
        }
    }

    // Hiển thị danh sách phim đề xuất bên sidebar
    renderRecommendSidebar(allMovies);

    if (typeof updateFadeEdges === "function") {
        setTimeout(updateFadeEdges, 100);
    }
}

// ==================== PHIM ĐỀ XUẤT (RECOMMENDATION) ====================

async function fetchRecommendedMovies(allMovies) {
    if (!allMovies || allMovies.length === 0) return [];

    // 1. Thử gọi API đề xuất từ backend nếu người khác đã làm xong hoặc triển khai dở
    try {
        const res = await apiFetch("/movies/recommendations");
        if (res.ok) {
            const data = await res.json();
            const list = Array.isArray(data) ? data : (data.data || []);
            if (list.length > 0) return list;
        }
    } catch (_) {}

    try {
        const res = await apiFetch("/user/recommendations");
        if (res.ok) {
            const data = await res.json();
            const list = Array.isArray(data) ? data : (data.data || []);
            if (list.length > 0) return list;
        }
    } catch (_) {}

    // 2. Thuật toán gợi ý frontend dựa trên lượt đánh giá và theo dõi của user
    const favoriteGenreCount = {};
    const interactedMovieIds = new Set();

    if (window.__CURRENT_USER__) {
        try {
            // Lấy danh sách phim user đã theo dõi (wishlist)
            const wishRes = await apiFetch("/user/wishlist/my");
            if (wishRes.ok) {
                const wishData = await wishRes.json();
                const wishList = Array.isArray(wishData) ? wishData : (wishData.data || []);
                wishList.forEach((w) => {
                    const mId = w.movieId || w.movie_id || (w.movie && w.movie.id);
                    if (mId) interactedMovieIds.add(Number(mId));
                    const movie = allMovies.find((m) => m.id == mId);
                    if (movie && movie.genres) {
                        movie.genres.forEach((g) => {
                            const name = typeof g === "string" ? g : (g.name || "");
                            if (name) {
                                favoriteGenreCount[name] = (favoriteGenreCount[name] || 0) + 3;
                            }
                        });
                    }
                });
            }
        } catch (_) {}

        try {
            // Lấy danh sách phim user đã đánh giá
            const rateRes = await apiFetch("/user/ratings/my");
            if (rateRes.ok) {
                const rateData = await rateRes.json();
                const rateList = Array.isArray(rateData) ? rateData : (rateData.data || []);
                rateList.forEach((r) => {
                    const mId = r.movieId || r.movie_id;
                    const ratingScore = r.rating || 0;
                    if (mId) interactedMovieIds.add(Number(mId));
                    const movie = allMovies.find((m) => m.id == mId);
                    if (movie && movie.genres && ratingScore >= 5) {
                        movie.genres.forEach((g) => {
                            const name = typeof g === "string" ? g : (g.name || "");
                            if (name) {
                                favoriteGenreCount[name] = (favoriteGenreCount[name] || 0) + Math.round(ratingScore / 2);
                            }
                        });
                    }
                });
            }
        } catch (_) {}
    }

    // Chấm điểm từng phim dựa trên thể loại yêu thích và rating
    const scoredMovies = allMovies.map((movie) => {
        let score = typeof movie.rating === "number" ? movie.rating : parseFloat(movie.rating) || 0;

        if (movie.genres && Array.isArray(movie.genres)) {
            movie.genres.forEach((g) => {
                const name = typeof g === "string" ? g : (g.name || "");
                if (favoriteGenreCount[name]) {
                    score += favoriteGenreCount[name] * 2.5;
                }
            });
        }

        // Ưu tiên phim mới chưa đánh giá/theo dõi
        if (interactedMovieIds.has(Number(movie.id))) {
            score -= 1.0;
        }

        return { movie, score };
    });

    scoredMovies.sort((a, b) => b.score - a.score || (b.movie.rating || 0) - (a.movie.rating || 0));

    // Lấy đúng 15 phim đề xuất theo yêu cầu
    return scoredMovies.slice(0, 15).map((item) => item.movie);
}

function createRecommendItemHTML(movie) {
    const m = normalizeMovie(movie);
    const rating = m.rating != null && m.rating > 0 ? m.rating.toFixed(1) : "N/A";
    const runtime = m.runtimeMinutes ? `${m.runtimeMinutes} ph` : "105 ph";

    return `
        <a href="/series_detail.html?id=${m.id}" class="recommend-item" title="${m.title}">
            <div class="recommend-poster-wrap">
                <img src="${m.posterUrl}" alt="${m.title}" class="recommend-poster-img" loading="lazy" />
                <div class="recommend-play-icon">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                        <path d="M8 5v14l11-7z"/>
                    </svg>
                </div>
            </div>
            <div class="recommend-info">
                <h4 class="recommend-movie-title">${m.title}</h4>
                <div class="recommend-meta-row">
                    <span class="recommend-rating">★ ${rating}</span>
                    <span class="recommend-meta-item">
                        <svg class="meta-icon" viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="10"></circle>
                            <polyline points="12 6 12 12 16 14"></polyline>
                        </svg>
                        ${runtime}
                    </span>
                </div>
            </div>
        </a>
    `;
}

async function renderRecommendSidebar(allMovies) {
    const box = document.getElementById("recommend-movies-list");
    if (!box) return;

    box.innerHTML = `
        <div class="recommend-loading">
            <span class="recommend-spinner"></span> Đang tải gợi ý phim...
        </div>
    `;

    try {
        const list = await fetchRecommendedMovies(allMovies);
        if (!list || list.length === 0) {
            box.innerHTML = `<p style="color: var(--text-muted); font-size: 13px; text-align: center; padding: 15px;">Chưa có phim đề xuất.</p>`;
            return;
        }

        box.innerHTML = list.map((m) => createRecommendItemHTML(m)).join("");
    } catch (e) {
        console.error("Lỗi renderRecommendSidebar:", e);
        const topMovies = [...allMovies]
            .sort((a, b) => (b.rating || 0) - (a.rating || 0))
            .slice(0, 15);
        box.innerHTML = topMovies.map((m) => createRecommendItemHTML(m)).join("");
    }
}

function scrollToSection(targetId) {
    const target = (targetId && document.getElementById(targetId)) ||
                   document.getElementById("danh-sach-phim") ||
                   document.querySelector("main .movie-list-container")?.previousElementSibling ||
                   document.querySelector("main h2");
    if (target) {
        const headerHeight = 70;
        const targetTop = target.getBoundingClientRect().top + window.pageYOffset - headerHeight;
        window.scrollTo({
            top: Math.max(0, targetTop),
            behavior: "smooth"
        });
    }
}

function renderCatalogOrSearchPage(allMovies) {
    const params = new URLSearchParams(window.location.search);
    const q = (params.get("q") || "").trim().toLowerCase();
    const tenTheLoai = params.get("ten");
    const filterGenres = params.getAll("genre");
    const minRating = parseFloat(params.get("min_rating") || "0");
    const minRuntime = parseInt(params.get("min_runtime") || "0");
    const maxRuntime = parseInt(params.get("max_runtime") || "9999");

    const searchInput = document.getElementById("search-input");
    if (searchInput && params.get("q")) {
        searchInput.value = params.get("q");
    }

    const heading = document.querySelector("main h2");
    if (heading) {
        if (q) heading.textContent = `Kết quả tìm kiếm: "${params.get("q")}"`;
        else if (tenTheLoai) heading.textContent = `Thể loại: ${tenTheLoai}`;
    }

    const filtered = allMovies.filter((raw) => {
        const m = normalizeMovie(raw);
        if (q && !m.title.toLowerCase().includes(q)) return false;
        if (tenTheLoai && !m.genres.includes(tenTheLoai)) return false;
        if (filterGenres.length > 0 && !filterGenres.some((fg) => m.genres.includes(fg))) return false;
        if (minRating > 0 && (typeof m.rating !== "number" || m.rating < minRating)) return false;
        if (m.runtimeMinutes != null && (m.runtimeMinutes < minRuntime || m.runtimeMinutes > maxRuntime)) return false;
        return true;
    });

    const grid = document.getElementById("catalog-movies-grid");
    if (grid) {
        paginateAndRender(filtered, grid, 15);
    }
}

function paginateAndRender(moviesList, gridElement, pageSize, targetPage) {
    const params = new URLSearchParams(window.location.search);
    const isHomeGrid = gridElement.id === "new-movies-grid";

    let totalPages;
    if (isHomeGrid) {
        const p1Size = 16;
        const otherSize = 20;
        if (moviesList.length <= p1Size) {
            totalPages = 1;
        } else {
            totalPages = 1 + Math.ceil((moviesList.length - p1Size) / otherSize);
        }
    } else {
        totalPages = Math.max(1, Math.ceil(moviesList.length / pageSize));
    }

    let currentPage = targetPage !== undefined ? targetPage : parseInt(params.get("page") || "1");
    if (currentPage < 1) currentPage = 1;
    if (currentPage > totalPages) currentPage = totalPages;

    let pageItems;
    if (isHomeGrid) {
        const sidebar = document.getElementById("recommend-sidebar");
        const container = document.getElementById("danh-sach-phim");
        if (currentPage === 1) {
            pageItems = moviesList.slice(0, 16);
            gridElement.className = "movie-grid-4";
            if (sidebar) sidebar.style.display = "";
            if (container) container.classList.remove("hide-sidebar");
        } else {
            const start = 16 + (currentPage - 2) * 20;
            pageItems = moviesList.slice(start, start + 20);
            gridElement.className = "movie-grid-5";
            if (sidebar) sidebar.style.display = "none";
            if (container) container.classList.add("hide-sidebar");
        }
    } else {
        const start = (currentPage - 1) * pageSize;
        pageItems = moviesList.slice(start, start + pageSize);
    }

    if (pageItems.length === 0) {
        gridElement.innerHTML = `<p style="color: var(--text-muted); grid-column: 1 / -1; padding: 20px 0;">Không tìm thấy phim nào phù hợp.</p>`;
    } else {
        gridElement.innerHTML = pageItems.map((m) => createMovieCardHTML(m, false)).join("");
    }

    const pageNumbersBox = document.getElementById("pagination-controls");
    if (!pageNumbersBox) return;

    const buildPageUrl = (p) => {
        const nextParams = new URLSearchParams(window.location.search);
        nextParams.set("page", p);
        const anchor = document.getElementById("danh-sach-phim") ? "#danh-sach-phim" : "";
        return `?${nextParams.toString()}${anchor}`;
    };

    let html = `<span class="page-info">Trang ${currentPage} / ${totalPages}</span>`;
    if (currentPage > 1) {
        html += `<a href="${buildPageUrl(1)}" data-page="1" class="page-link">Trang Đầu</a>`;
    }
    for (let num = Math.max(1, currentPage - 2); num <= Math.min(totalPages, currentPage + 2); num++) {
        if (num === currentPage) {
            html += `<span class="page-link current-page">${num}</span>`;
        } else {
            html += `<a href="${buildPageUrl(num)}" data-page="${num}" class="page-link">${num}</a>`;
        }
    }
    if (currentPage < totalPages) {
        html += `<a href="${buildPageUrl(totalPages)}" data-page="${totalPages}" class="page-link">Trang Cuối</a>`;
    }
    pageNumbersBox.innerHTML = html;

    // Bắt sự kiện chuyển trang client-side mượt mà: KHÔNG tải lại trang và KHÔNG nhảy lên đầu trang
    pageNumbersBox.querySelectorAll(".page-link[data-page]").forEach((btn) => {
        btn.addEventListener("click", function (e) {
            e.preventDefault();
            const p = parseInt(this.getAttribute("data-page"));
            if (p && p !== currentPage) {
                const nextUrl = buildPageUrl(p);
                window.history.pushState({ page: p }, "", nextUrl);

                paginateAndRender(moviesList, gridElement, pageSize, p);

                // Cuộn mượt đến ngay phần Phim Mới Cập Nhật (#danh-sach-phim)
                scrollToSection("danh-sach-phim");
            }
        });
    });
}

async function renderMovieDetailPage() {
    const movieId = new URLSearchParams(window.location.search).get("id");
    if (!movieId) return;

    const res = await apiFetch(`/movies/${movieId}`);
    if (!res.ok) return;
    const rawMovie = await res.json();
    const movie = normalizeMovie(rawMovie.data || rawMovie);

    document.title = `${movie.title} - WebPHIMMOI`;

    const posterBox = document.querySelector(".detail-poster-box");
    if (posterBox) {
        posterBox.innerHTML = `<img src="${movie.posterUrl}" alt="${movie.title}" class="detail-poster">`;
    }

    const titleEl = document.querySelector(".detail-title");
    if (titleEl) titleEl.textContent = movie.title;

    const releaseEl = document.getElementById("detail-release-date");
    if (releaseEl) releaseEl.textContent = formatDateVN(movie.releaseDate);

    renderDetailMovieRating(movie.rating);

    const runtimeEl = document.getElementById("detail-runtime");
    if (runtimeEl) {
        runtimeEl.textContent = movie.runtimeMinutes ? `${movie.runtimeMinutes} phút` : "Chưa cập nhật";
    }

    const genresBox = document.querySelector(".detail-genres");
    if (genresBox) {
        genresBox.innerHTML =
            `<span class="detail-label">Thể loại:</span>` +
            (movie.genres.length > 0
                ? movie.genres.map((g) => `<a href="/the_loai.html?ten=${encodeURIComponent(g)}" class="genre-tag">${g}</a>`).join("")
                : `<span>Chưa cập nhật</span>`);
    }

    const descBox = document.querySelector(".detail-description p");
    if (descBox) descBox.textContent = movie.description;

    const trailerBox = document.querySelector(".detail-trailer");
    if (trailerBox) {
        const ytMatch = movie.trailer ? movie.trailer.match(/(?:v=|youtu\.be\/|embed\/)([^&\n?#]+)/) : null;
        if (ytMatch && ytMatch[1]) {
            trailerBox.innerHTML = `
                <h2>Trailer</h2>
                <div class="youtube-player">
                    <iframe src="https://www.youtube.com/embed/${ytMatch[1]}"
                        title="Trailer ${movie.title}" frameborder="0" allowfullscreen>
                    </iframe>
                </div>
            `;
        } else {
            trailerBox.innerHTML = `<h2>Trailer</h2><p class="no-trailer">Phim này chưa có trailer.</p>`;
        }
    }

    loadCinemasIntoModal(movie.id);
    setupFollowingAndRatingUI(movie.id);
}



async function loadCinemasIntoModal(movieId) {
    console.log("BẮT ĐẦU loadCinemasIntoModal, movieId =", movieId);
    const theaterListBox = document.getElementById("theater-list-box");
    if (!theaterListBox) return;

    theaterListBox.innerHTML = `<p class="watch-empty">Đang tìm rạp chiếu phim này...</p>`;

    try {
        // Tải danh sách toàn bộ suất chiếu thực tế của riêng bộ phim này
        const res = await apiFetch(`/showtimes?movieId=${movieId}`);
        if (!res.ok) {
            theaterListBox.innerHTML = `<p class="watch-empty">Hiện chưa có thông tin rạp chiếu phim này.</p>`;
            return;
        }

        const raw = await res.json();
        const movieShowtimes = Array.isArray(raw) ? raw : (raw.data || []);
        console.log("DANH SÁCH SUẤT CHIẾU CỦA PHIM:", movieShowtimes.length);

        // Nếu phim không có lịch chiếu ở bất kỳ rạp nào: ẩn toàn bộ, không đề xuất rạp nào!
        if (movieShowtimes.length === 0) {
            theaterListBox.innerHTML = `<p class="watch-empty">Phim hiện chưa có lịch chiếu tại các rạp.</p>`;
            return;
        }

        // Gom các suất chiếu theo hệ thống rạp (cinema_system) và chi nhánh rạp (cinema)
        // Hệ thống hoặc chi nhánh nào KHÔNG chiếu phim này sẽ KHÔNG có trong cinemaGroups -> Tự động bị ẩn!
        const cinemaGroups = {};

        movieShowtimes.forEach((st) => {
            const systemId = st.system_id || st.systemId;
            const systemName = st.system_name || st.systemName || "Rạp khác";
            const cinemaId = st.cinema_id || st.cinemaId;
            const cinemaName = st.cinema_name || st.cinemaName || "Chi nhánh";

            if (!systemId || !cinemaId) return;

            const sKey = String(systemId);
            if (!cinemaGroups[sKey]) {
                cinemaGroups[sKey] = {
                    id: systemId,
                    name: systemName,
                    branchesMap: {},
                    branches: []
                };
            }

            const cKey = String(cinemaId);
            if (!cinemaGroups[sKey].branchesMap[cKey]) {
                const branchObj = {
                    id: cinemaId,
                    name: cinemaName,
                    showtimes: []
                };
                cinemaGroups[sKey].branchesMap[cKey] = branchObj;
                cinemaGroups[sKey].branches.push(branchObj);
            }

            cinemaGroups[sKey].branchesMap[cKey].showtimes.push(st);
        });

        const activeSystemEntries = Object.entries(cinemaGroups);
        if (activeSystemEntries.length === 0) {
            theaterListBox.innerHTML = `<p class="watch-empty">Phim hiện chưa có lịch chiếu tại các rạp.</p>`;
            return;
        }

        // Chỉ hiển thị các hệ thống rạp THỰC SỰ CÓ chiếu phim này
        theaterListBox.innerHTML = activeSystemEntries
            .map(([systemId, group]) => `
                <div
                    class="watch-item cinema-system-item"
                    data-system-id="${systemId}"
                >
                    ${group.name}
                </div>
            `)
            .join("");

        // Bấm hệ thống rạp để mở popup chi nhánh (chỉ hiện các chi nhánh thực sự có lịch chiếu của phim)
        theaterListBox.querySelectorAll(".cinema-system-item")
            .forEach((item) => {
                item.addEventListener("click", function () {
                    console.log("ĐÃ CHỌN HỆ THỐNG RẠP:", this.dataset.systemId);
                    const systemId = this.dataset.systemId;
                    const group = cinemaGroups[systemId];

                    const branchModal = document.getElementById("branchModal");
                    const branchListBox = document.getElementById("branch-list-box");
                    const branchSystemName = document.getElementById("branchSystemName");
                    const theaterModal = document.getElementById("theaterModal");

                    if (!branchModal || !branchListBox || !branchSystemName || !group) return;

                    branchSystemName.textContent = group.name;

                    // Chỉ hiển thị các chi nhánh CÓ lịch chiếu của phim này
                    branchListBox.innerHTML = group.branches
                        .map(cinema => `
                            <div
                                class="watch-item cinema-branch"
                                data-cinema-id="${cinema.id}"
                                data-cinema-name="${cinema.name}"
                                data-system-id="${systemId}"
                            >
                                ${cinema.name}
                            </div>
                        `)
                        .join("");

                    // Đóng popup hệ thống rạp, mở popup chi nhánh
                    theaterModal.classList.remove("active");
                    branchModal.classList.add("active");

                    // Bấm chi nhánh để mở popup lịch chiếu
                    branchListBox.querySelectorAll(".cinema-branch")
                        .forEach((branch) => {
                            branch.addEventListener("click", async function () {
                                console.log("ĐÃ CLICK CHI NHÁNH:", this.dataset.cinemaId);
                                const showtimeModal = document.getElementById("showtimeModal");
                                const showtimeCinemaName = document.getElementById("showtimeCinemaName");

                                if (!showtimeModal || !showtimeCinemaName) return;

                                const cinemaId = this.dataset.cinemaId;
                                const systemId = this.dataset.systemId;

                                showtimeCinemaName.textContent = this.dataset.cinemaName;
                                showtimeModal.dataset.cinemaId = cinemaId;

                                branchModal.classList.remove("active");
                                showtimeModal.classList.add("active");

                                // Suất chiếu của chi nhánh này đã được lọc chính xác từ trước
                                const branchData = group.branchesMap[String(cinemaId)];
                                const preloadedShowtimes = branchData ? branchData.showtimes : null;

                                const [showtimes, detailLink] = await Promise.all([
                                    preloadedShowtimes && preloadedShowtimes.length > 0
                                        ? Promise.resolve(preloadedShowtimes)
                                        : loadShowtimes(movieId, cinemaId),
                                    loadMovieSystemLink(movieId, systemId)
                                ]);

                                if (!showtimes || showtimes.length === 0) {
                                    document.getElementById("showtime-list-box").innerHTML = `
                                        <p class="watch-empty">Chi nhánh này chưa có lịch chiếu trực tuyến.</p>
                                    `;
                                    return;
                                }

                                renderShowtimes(showtimes, detailLink);
                            });
                        });
                });
            });

    } catch (error) {
        console.error("Lỗi loadCinemasIntoModal:", error);
        theaterListBox.innerHTML = `<p class="watch-empty">Có lỗi khi tải danh sách rạp.</p>`;
    }
}

const CINEMA_SYSTEM_HOMEPAGES = {
    "3": "https://betacinemas.vn",
    "4": "https://www.bhdstar.vn",
    "5": "https://www.lottecinemavn.com",
    "6": "https://www.cgv.vn",
    "7": "https://cinestar.com.vn",
    "8": "https://www.galaxycine.vn",
    "9": "https://chieuphimquocgia.com.vn",
    "beta": "https://betacinemas.vn",
    "bhd": "https://www.bhdstar.vn",
    "lotte": "https://www.lottecinemavn.com",
    "cgv": "https://www.cgv.vn",
    "cinestar": "https://cinestar.com.vn",
    "galaxy": "https://www.galaxycine.vn",
    "ncc": "https://chieuphimquocgia.com.vn"
};

async function loadMovieSystemLink(movieId, systemId) {
    const sysKey = String(systemId || "").trim().toLowerCase();
    const fallbackUrl = CINEMA_SYSTEM_HOMEPAGES[sysKey] || CINEMA_SYSTEM_HOMEPAGES[String(systemId)] || null;

    try {
        if (!movieId || !systemId || systemId === "unknown") return fallbackUrl;

        const response = await apiFetch(
            `/movie-system-links?movieId=${movieId}&systemId=${systemId}`
        );

        if (!response.ok) {
            return fallbackUrl;
        }

        const result = await response.json();
        const links = result.data || [];

        if (links.length > 0) {
            const link = links[0].detail_link || links[0].detailLink;
            if (link && typeof link === "string" && link.trim().length > 0) {
                return link.trim();
            }
        }

        // Nếu database chưa có bản ghi liên kết cụ thể của phim tại rạp này,
        // fallback về trang chủ chính thức của hệ thống rạp để người dùng vẫn luôn bấm được nút "Đặt vé"
        return fallbackUrl;

    } catch (error) {
        console.error("Lỗi tải đường dẫn đặt vé:", error);
        return fallbackUrl;
    }
}

async function loadShowtimes(movieId, cinemaId) {
    try {
        const response = await apiFetch(
            `/showtimes?movieId=${movieId}&cinemaId=${cinemaId}`
        );

        if (!response.ok) {
            throw new Error("Không thể tải lịch chiếu");
        }

        const result = await response.json();
        console.log("PHẢN HỒI API SHOWTIMES:", result);

        if (!result.success) {
            throw new Error(result.message || "Lỗi lấy lịch chiếu");
        }

        return result.data || [];

    } catch (error) {
        console.error("Lỗi tải lịch chiếu:", error);
        return null;
    }
}


function renderShowtimes(showtimes, detailLink) {
    const box = document.getElementById("showtime-list-box");
    if (!box) return;

    // Lấy danh sách phiên bản phim không trùng nhau
    const versions = [...new Set(
        showtimes.map(item => item.movie_version)
    )];

    // Lấy danh sách ngày chiếu không trùng nhau
    const dates = [...new Set(
        showtimes.map(item => item.show_date)
    )].sort();

    box.innerHTML = `
        <div class="showtime-version-list">
            ${versions.map(version => `
                <button class="showtime-version-btn" data-version="${version}">
                    ${version}
                </button>
            `).join("")}
        </div>

        <div class="showtime-date-list">
            ${dates.map(date => `
                <button class="showtime-date-btn" data-date="${date}">
                    ${date.split("-").reverse().join("/")}
                </button>
            `).join("")}
        </div>

        <div id="showtime-results">
            <p class="watch-empty">Chọn phiên bản và ngày chiếu.</p>
        </div>

        <button id="booking-btn" class="booking-btn" disabled>
            Đặt vé
        </button>
    `;

    let selectedVersion = versions[0];
    let selectedDate = null;

    const versionButtons = box.querySelectorAll(".showtime-version-btn");
    const dateButtons = box.querySelectorAll(".showtime-date-btn");
    const bookingBtn = document.getElementById("booking-btn");

    // Kiểm tra đường dẫn có hợp lệ không
    let validDetailLink = false;

    if (detailLink) {
        try {
            const url = new URL(detailLink);
            validDetailLink = ["http:", "https:"].includes(url.protocol);
        } catch (error) {
            validDetailLink = false;
        }
    }

    function updateShowtimes() {
        const results = document.getElementById("showtime-results");

        const filtered = showtimes.filter(item =>
            item.movie_version === selectedVersion &&
            item.show_date === selectedDate
        );

        // Chỉ bật nút đặt vé khi có suất chiếu và link hợp lệ
        bookingBtn.disabled = filtered.length === 0 || !validDetailLink;
        if (bookingBtn.disabled) {
            bookingBtn.title = !validDetailLink ? "Chưa có đường dẫn đặt vé cho rạp này" : "Không có suất chiếu vào ngày đã chọn";
        } else {
            bookingBtn.title = "Chuyển đến trang đặt vé của rạp";
        }

        if (!selectedDate) {
            results.innerHTML = `
                <p class="watch-empty">Vui lòng chọn ngày chiếu.</p>
            `;
            return;
        }

        if (filtered.length === 0) {
            results.innerHTML = `
                <p class="watch-empty">Không có suất chiếu vào ngày này.</p>
            `;
            return;
        }

        results.innerHTML = `
            <div class="showtime-time-list">
                ${filtered.map(item => `
                    <button class="showtime-time-btn" data-id="${item.id}">
                        ${item.show_time}
                    </button>
                `).join("")}
            </div>
        `;

        // Gắn sự kiện click cho từng khung giờ
        const timeButtons = results.querySelectorAll(".showtime-time-btn");

        timeButtons.forEach(button => {
            button.addEventListener("click", function () {
                timeButtons.forEach(btn => btn.classList.remove("selected"));

                this.classList.add("selected");

                console.log("Đã chọn suất chiếu ID:", this.dataset.id);
            });
        });

        // Tự động chọn khung giờ đầu tiên
        if (timeButtons.length > 0) {
            timeButtons[0].classList.add("selected");
        }
    }

    versionButtons.forEach(button => {
        button.addEventListener("click", function () {
            selectedVersion = this.dataset.version;

            versionButtons.forEach(btn => btn.classList.remove("selected"));
            this.classList.add("selected");

            updateShowtimes();
        });
    });

    dateButtons.forEach(button => {
        button.addEventListener("click", function () {
            selectedDate = this.dataset.date;

            dateButtons.forEach(btn => btn.classList.remove("selected"));
            this.classList.add("selected");

            updateShowtimes();
        });
    });

    // Xử lý nút đặt vé
    bookingBtn.addEventListener("click", function () {
        if (this.disabled || !validDetailLink) return;

        window.open(detailLink, "_blank", "noopener,noreferrer");
    });

    // Mặc định chọn phiên bản đầu tiên
    if (versionButtons.length > 0) {
        versionButtons[0].classList.add("selected");
    }

    // Chọn ngày chiếu đầu tiên
    if (dateButtons.length > 0) {
        dateButtons[0].click();
    }
}


async function setupFollowingAndRatingUI(movieId) {
    const watchOptions = document.querySelector(".watch-options");
    if (watchOptions) {
        const actionHTML = `
            <button type="button" class="watch-option-btn" id="followBtn" style="background-color: #444;">
                📌 Theo dõi
            </button>
        `;
        watchOptions.insertAdjacentHTML("beforeend", actionHTML);

        const followBtn = document.getElementById("followBtn");
        let isFollowing = false;

        function updateFollowButtonState() {
            if (isFollowing) {
                followBtn.textContent = "✓ Đang theo dõi";
                followBtn.style.backgroundColor = "#27ae60";
            } else {
                followBtn.textContent = "📌 Theo dõi";
                followBtn.style.backgroundColor = "#444";
            }
        }

        if (window.__CURRENT_USER__) {
            try {
                const checkRes = await apiFetch(`/user/wishlist/check/${movieId}`);
                if (checkRes.ok) {
                    const checkData = await checkRes.json();
                    isFollowing = checkData.data === true;
                    updateFollowButtonState();
                }
            } catch (_) {}
        }

        followBtn.addEventListener("click", async () => {
            if (!window.__CURRENT_USER__) {
                document.getElementById("auth-modal-backdrop").style.display = "flex";
                return;
            }
            try {
                if (!isFollowing) {
                    const res = await apiFetch("/user/wishlist", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ movieId: Number(movieId), movie_id: Number(movieId) })
                    });
                    const data = await res.json();
                    if (res.ok && data.success) {
                        isFollowing = true;
                        updateFollowButtonState();
                    } else {
                        alert(data.message || "Không thể theo dõi phim này!");
                    }
                } else {
                    const res = await apiFetch(`/user/wishlist/movie/${movieId}`, {
                        method: "DELETE"
                    });
                    const data = await res.json();
                    if (res.ok && data.success) {
                        isFollowing = false;
                        updateFollowButtonState();
                    }
                }
            } catch (_) {
                alert("Lỗi kết nối khi cập nhật trạng thái theo dõi!");
            }
        });
    }

    // ==================== PHẦN ĐÁNH GIÁ PHIM BẰNG NGÔI SAO ====================
    // 5 ngôi sao, mỗi nửa sao = 1 điểm (tổng cộng 10 điểm)
    const starRatingWidget = document.getElementById("starRatingWidget");
    const userRatingText = document.getElementById("user-rating-text");
    const deleteRatingBtn = document.getElementById("delete-rating-btn");

    if (starRatingWidget) {
        let starsHtml = "";
        for (let i = 1; i <= 5; i++) {
            const leftPoint = (i - 1) * 2 + 1;
            const rightPoint = i * 2;
            starsHtml += `
                <div class="star-item" data-star="${i}">
                    <svg class="star-svg star-empty" viewBox="0 0 24 24">
                        <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
                    </svg>
                    <div class="star-fill" id="star-fill-${i}">
                        <svg class="star-svg star-filled" viewBox="0 0 24 24">
                            <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
                        </svg>
                    </div>
                    <div class="star-half left" data-point="${leftPoint}" title="${leftPoint}/10 điểm"></div>
                    <div class="star-half right" data-point="${rightPoint}" title="${rightPoint}/10 điểm"></div>
                </div>
            `;
        }
        starRatingWidget.innerHTML = starsHtml;

        let currentUserRating = 0;

        function setStarDisplay(point) {
            for (let i = 1; i <= 5; i++) {
                const fillEl = document.getElementById(`star-fill-${i}`);
                if (!fillEl) continue;
                if (point >= i * 2) {
                    fillEl.style.width = "100%";
                } else if (point === i * 2 - 1) {
                    fillEl.style.width = "50%";
                } else {
                    fillEl.style.width = "0%";
                }
            }
        }

        function updateRatingLabel(point, isTemporary = false) {
            if (!userRatingText) return;
            if (point > 0) {
                if (isTemporary) {
                    userRatingText.textContent = `${point}/10 điểm`;
                } else {
                    userRatingText.textContent = `✓ Đánh giá của bạn: ${point}/10 điểm`;
                    if (deleteRatingBtn) deleteRatingBtn.style.display = "inline-block";
                }
            } else {
                userRatingText.textContent = "Bạn chưa đánh giá (Nhấp vào sao để chấm điểm)";
                if (deleteRatingBtn) deleteRatingBtn.style.display = "none";
            }
        }

        // Tải điểm đánh giá hiện tại của user nếu đã đăng nhập
        if (window.__CURRENT_USER__) {
            try {
                const myRatingRes = await apiFetch(`/user/ratings/movie/${movieId}`);
                if (myRatingRes.ok) {
                    const myRatingData = await myRatingRes.json();
                    if (myRatingData.data && myRatingData.data.rating != null) {
                        currentUserRating = Math.round(myRatingData.data.rating);
                        setStarDisplay(currentUserRating);
                        updateRatingLabel(currentUserRating, false);
                    }
                }
            } catch (_) {}
        } else {
            setStarDisplay(0);
            updateRatingLabel(0, false);
        }

        // Gắn sự kiện hover và click cho từng nửa sao
        starRatingWidget.querySelectorAll(".star-half").forEach(half => {
            const point = parseInt(half.getAttribute("data-point"));

            half.addEventListener("mouseenter", () => {
                setStarDisplay(point);
                updateRatingLabel(point, true);
            });

            half.addEventListener("click", async () => {
                if (!window.__CURRENT_USER__) {
                    document.getElementById("auth-modal-backdrop").style.display = "flex";
                    return;
                }

                try {
                    const res = await apiFetch("/user/ratings", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({
                            movieId: Number(movieId),
                            movie_id: Number(movieId),
                            rating: point
                        })
                    });
                    const data = await res.json();
                    if (res.ok && data.success) {
                        currentUserRating = point;
                        setStarDisplay(currentUserRating);
                        updateRatingLabel(currentUserRating, false);

                        const updatedMovieRes = await apiFetch(`/movies/${movieId}`);
                        if (updatedMovieRes.ok) {
                            const updatedMovie = await updatedMovieRes.json();
                            const newAvg = (updatedMovie.data || updatedMovie).rating;
                            renderDetailMovieRating(newAvg);
                        }
                    } else {
                        alert(data.message || "Không thể gửi đánh giá!");
                    }
                } catch (_) {
                    alert("Lỗi kết nối khi gửi đánh giá!");
                }
            });
        });

        starRatingWidget.addEventListener("mouseleave", () => {
            setStarDisplay(currentUserRating);
            updateRatingLabel(currentUserRating, false);
        });

        if (deleteRatingBtn) {
            deleteRatingBtn.addEventListener("click", async () => {
                if (!window.__CURRENT_USER__) return;
                try {
                    const res = await apiFetch(`/user/ratings/movie/${movieId}`, { method: "DELETE" });
                    const data = await res.json();
                    if (res.ok && data.success) {
                        currentUserRating = 0;
                        setStarDisplay(0);
                        updateRatingLabel(0, false);

                        const updatedMovieRes = await apiFetch(`/movies/${movieId}`);
                        if (updatedMovieRes.ok) {
                            const updatedMovie = await updatedMovieRes.json();
                            const newAvg = (updatedMovie.data || updatedMovie).rating;
                            renderDetailMovieRating(newAvg);
                        }
                    }
                } catch (_) {}
            });
        }
    }
}