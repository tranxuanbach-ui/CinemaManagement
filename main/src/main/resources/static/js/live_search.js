document.addEventListener("DOMContentLoaded", function () {
    const searchInput = document.getElementById("search-input");
    const searchDropdown = document.getElementById("search-dropdown");
    const resultsList = document.getElementById("search-results-list");

    if (!searchInput || !searchDropdown || !resultsList) return;

    let cachedMovies = null;
    let debounceTimer;

    async function fetchMoviesList() {
        if (cachedMovies) return cachedMovies;
        if (window.__ALL_MOVIES_CACHE__) {
            cachedMovies = window.__ALL_MOVIES_CACHE__;
            return cachedMovies;
        }
        let res = await fetch("/api/movies");
        if (!res.ok) res = await fetch("/movies");
        const data = await res.json();
        cachedMovies = Array.isArray(data) ? data : (data.data || []);
        window.__ALL_MOVIES_CACHE__ = cachedMovies;
        return cachedMovies;
    }

    searchInput.addEventListener("input", function () {
        clearTimeout(debounceTimer);
        const query = this.value.trim().toLowerCase();

        if (query.length === 0) {
            searchDropdown.style.display = "none";
            return;
        }

        debounceTimer = setTimeout(async () => {
            try {
                const movies = await fetchMoviesList();
                const matched = movies
                    .filter((m) => m.title && m.title.toLowerCase().includes(query))
                    .slice(0, 8);

                resultsList.innerHTML = "";

                if (matched.length > 0) {
                    matched.forEach((movie) => {
                        const poster = movie.posterUrl || movie.poster_url || "/media/logo.png";
                        const relDate = movie.releaseDate || movie.release_date || "";
                        const year = relDate ? relDate.toString().substring(0, 4) : "Mới";
                        const rating = movie.rating != null ? `★ ${movie.rating}` : "Chưa có điểm";  const item = document.createElement("a"); item.href = `/series_detail.html?id=${movie.id}`;
                        item.className = "search-result-item";
                        item.innerHTML = `
                            <img src="${poster}" class="search-result-img" alt="poster">
                            <div class="search-result-info">
                                <div class="search-result-title">${movie.title}</div>
                                <div class="search-result-meta">Phim • ${year} •${rating}</div>
                            </div>
                        `;
                        resultsList.appendChild(item);
                    });
                } else {
                    resultsList.innerHTML =
                        '<div style="padding: 15px; color: #888; font-size: 14px;">Không tìm thấy phim phù hợp.</div>';
                }
                searchDropdown.style.display = "block";
            } catch (err) {
                console.error("Lỗi Live Search:", err);
            }
        }, 250);
    });

    document.addEventListener("click", function (e) {
        if (!searchInput.contains(e.target) && !searchDropdown.contains(e.target)) {
            searchDropdown.style.display = "none";
        }
    });
});