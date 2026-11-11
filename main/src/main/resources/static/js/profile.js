// ==========================================================================
// TRANG QUẢN LÝ HỒ SƠ CÁ NHÂN (USER PROFILE JAVASCRIPT)
// ==========================================================================

let currentUserProfile = null;
let userRatingsData = [];

document.addEventListener("DOMContentLoaded", async () => {
    await loadUserProfile();
    setupProfileForms();
});

// Chuyển đổi giữa các tab
window.switchProfileTab = function (tabName) {
    document.querySelectorAll(".profile-tab-btn").forEach(btn => {
        btn.classList.toggle("active", btn.dataset.tab === tabName);
    });

    document.querySelectorAll(".profile-tab-content").forEach(pane => {
        pane.classList.toggle("active", pane.id === `tab-${tabName}`);
    });
};

// 1. Tải thông tin người dùng từ máy chủ
async function loadUserProfile() {
    try {
        const res = await apiFetch("/auth/me");
        if (!res.ok) {
            showNotLoggedInState();
            return;
        }

        const data = await res.json();
        currentUserProfile = data.data || data;

        if (!currentUserProfile || !currentUserProfile.username) {
            showNotLoggedInState();
            return;
        }

        renderUserInfo(currentUserProfile);
        await loadUserRatings();

    } catch (err) {
        console.error("Lỗi tải thông tin hồ sơ:", err);
        showNotLoggedInState();
    }
}

function showNotLoggedInState() {
    const notLoggedIn = document.getElementById("not-logged-in-container");
    const profileWrap = document.getElementById("profile-container");
    if (notLoggedIn) notLoggedIn.style.display = "block";
    if (profileWrap) profileWrap.style.display = "none";

    const triggerBtn = document.getElementById("profile-login-trigger-btn");
    if (triggerBtn) {
        triggerBtn.addEventListener("click", () => {
            const authModal = document.getElementById("auth-modal-backdrop");
            if (authModal) authModal.style.display = "flex";
        });
    }
}

// 2. Hiển thị thông tin người dùng lên banner & form
function renderUserInfo(user) {
    const notLoggedIn = document.getElementById("not-logged-in-container");
    const profileWrap = document.getElementById("profile-container");
    if (notLoggedIn) notLoggedIn.style.display = "none";
    if (profileWrap) profileWrap.style.display = "block";

    // Username & Email
    document.getElementById("profile-username").textContent = user.username;
    document.getElementById("profile-email").textContent = user.email || "Chưa cập nhật email";
    
    // Ngày tham gia
    const joinDateEl = document.getElementById("profile-joined-date");
    if (joinDateEl) {
        if (user.createdAt) {
            const d = new Date(user.createdAt);
            joinDateEl.textContent = `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}/${d.getFullYear()}`;
        } else {
            joinDateEl.textContent = "Thành viên thân thiết";
        }
    }

    // Role badge
    const roleBadge = document.getElementById("profile-role-badge");
    if (roleBadge) {
        const role = user.role || "USER";
        roleBadge.textContent = role;
        roleBadge.className = "profile-role-badge";
        if (role === "ADMIN") roleBadge.classList.add("role-admin");
        else if (role === "STAFF") roleBadge.classList.add("role-staff");
        else roleBadge.classList.add("role-user");
    }

    // Avatar: Mặc định là logo web (/media/logo.png) nếu chưa có hoặc mới tạo
    const avatarImg = document.getElementById("profile-avatar-img");
    const avatar = (user.avatarUrl && user.avatarUrl.trim() !== "") ? user.avatarUrl.trim() : "/media/logo.png";
    if (avatarImg) {
        avatarImg.src = avatar;
        avatarImg.onerror = () => {
            avatarImg.src = "/media/logo.png";
        };
    }

    // Điền dữ liệu vào form cập nhật
    const inputUser = document.getElementById("input-username");
    const inputEmail = document.getElementById("input-email");
    const inputAvatar = document.getElementById("input-avatar");
    if (inputUser) inputUser.value = user.username;
    if (inputEmail) inputEmail.value = user.email || "";
    if (inputAvatar) {
        inputAvatar.value = (user.avatarUrl && user.avatarUrl.trim() !== "/media/logo.png") ? user.avatarUrl.trim() : "";
    }
}

// 3. Tải lịch sử đánh giá phim
async function loadUserRatings() {
    try {
        const res = await apiFetch("/user/ratings/my");
        const data = await res.json();
        userRatingsData = data.data || [];

        // Cập nhật số lượng
        const count = userRatingsData.length;
        document.getElementById("profile-stat-ratings").textContent = count;
        document.getElementById("tab-ratings-count").textContent = count;

        renderRatingsList(userRatingsData);
    } catch (err) {
        console.error("Lỗi loadUserRatings:", err);
    }
}

function renderRatingsList(items) {
    const list = document.getElementById("profile-ratings-list");
    const empty = document.getElementById("profile-ratings-empty");

    if (!items || items.length === 0) {
        list.style.display = "none";
        empty.style.display = "block";
        return;
    }

    list.style.display = "flex";
    empty.style.display = "none";

    list.innerHTML = items.map(item => {
        const rId = item.id;
        const mId = item.movieId || item.movie_id;
        const title = item.movieTitle || item.movie_title || "Phim";
        const poster = item.moviePosterUrl || item.movie_poster_url || "/media/logo.png";
        const score = typeof item.rating === "number" ? item.rating.toFixed(1) : item.rating;

        let dateStr = "";
        if (item.updatedAt || item.createdAt) {
            const d = new Date(item.updatedAt || item.createdAt);
            dateStr = `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}/${d.getFullYear()}`;
        }

        return `
            <div class="rating-history-item">
                <div class="rating-movie-meta-wrap">
                    <img src="${poster}" alt="${title}" class="rating-movie-poster" />
                    <div class="rating-movie-info">
                        <h4><a href="/series_detail.html?id=${mId}">${title}</a></h4>
                        <div style="display: flex; align-items: center; margin-top: 4px;">
                            <span class="rating-given-score">★ ${score} / 10</span>
                            ${dateStr ? `<span class="rating-date">🕒 ${dateStr}</span>` : ""}
                        </div>
                    </div>
                </div>
                <div class="rating-actions">
                    <a href="/series_detail.html?id=${mId}" class="filter-button" style="padding: 6px 14px; font-size: 12px; text-decoration: none;">Xem phim</a>
                    <button type="button" class="btn-remove-rating" onclick="removeRatingItem(${rId})">Xóa đánh giá</button>
                </div>
            </div>
        `;
    }).join("");
}

window.removeRatingItem = async function (ratingId) {
    if (!confirm("Bạn có chắc chắn muốn xóa đánh giá cho bộ phim này không?")) return;
    try {
        const res = await apiFetch(`/user/ratings/${ratingId}`, { method: "DELETE" });
        if (res.ok) {
            await loadUserRatings();
        } else {
            alert("Không thể xóa đánh giá!");
        }
    } catch (e) {
        alert("Lỗi kết nối máy chủ!");
    }
};

// 5. Cài đặt các biểu mẫu cập nhật thông tin & đổi mật khẩu
function setupProfileForms() {
    // Form 1: Cập nhật Email & Avatar
    const updateForm = document.getElementById("update-profile-form");
    const updateAlert = document.getElementById("profile-update-alert");

    if (updateForm) {
        updateForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            updateAlert.className = "profile-alert";
            updateAlert.style.display = "none";

            const email = document.getElementById("input-email").value.trim();
            const avatarUrl = document.getElementById("input-avatar").value.trim();

            try {
                const res = await apiFetch("/user/profile", {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email, avatarUrl })
                });

                const data = await res.json();
                if (res.ok && data.success !== false) {
                    updateAlert.textContent = "✓ Cập nhật thông tin hồ sơ thành công!";
                    updateAlert.classList.add("success");
                    currentUserProfile = data.data || currentUserProfile;
                    currentUserProfile.email = email;
                    currentUserProfile.avatarUrl = avatarUrl;
                    renderUserInfo(currentUserProfile);
                } else {
                    updateAlert.textContent = data.message || "Không thể cập nhật hồ sơ!";
                    updateAlert.classList.add("error");
                }
            } catch (err) {
                updateAlert.textContent = "Lỗi kết nối tới máy chủ!";
                updateAlert.classList.add("error");
            }
        });
    }

    // Form 2: Đổi mật khẩu
    const pwdForm = document.getElementById("change-password-form");
    const pwdAlert = document.getElementById("change-password-alert");

    if (pwdForm) {
        pwdForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            pwdAlert.className = "profile-alert";
            pwdAlert.style.display = "none";

            const oldPassword = document.getElementById("input-old-password").value;
            const newPassword = document.getElementById("input-new-password").value;
            const confirmPassword = document.getElementById("input-confirm-password").value;

            if (newPassword !== confirmPassword) {
                pwdAlert.textContent = "Mật khẩu mới và xác nhận mật khẩu không khớp!";
                pwdAlert.classList.add("error");
                return;
            }

            if (newPassword.length < 6) {
                pwdAlert.textContent = "Mật khẩu mới phải có tối thiểu 6 ký tự!";
                pwdAlert.classList.add("error");
                return;
            }

            try {
                const res = await apiFetch("/user/profile/change-password", {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ oldPassword, newPassword, confirmPassword })
                });

                const data = await res.json();
                if (res.ok && data.success !== false) {
                    pwdAlert.textContent = "✓ Đổi mật khẩu thành công!";
                    pwdAlert.classList.add("success");
                    pwdForm.reset();
                } else {
                    pwdAlert.textContent = data.message || "Không thể đổi mật khẩu!";
                    pwdAlert.classList.add("error");
                }
            } catch (err) {
                pwdAlert.textContent = "Lỗi kết nối tới máy chủ!";
                pwdAlert.classList.add("error");
            }
        });
    }
}

