document.addEventListener("DOMContentLoaded", function () {
    const filterBtn = document.getElementById("filter-button");
    const filterPopup = document.getElementById("filter-popup");
    const applyBtn = document.getElementById("apply-filter");

    if (filterBtn && filterPopup) {
        filterBtn.addEventListener("click", function (e) {
            e.stopPropagation();
            const isHidden = filterPopup.style.display === "none" || filterPopup.style.display === "";
            filterPopup.style.display = isHidden ? "block" : "none";
        });

        document.addEventListener("click", function (e) {
            if (!filterPopup.contains(e.target) && e.target !== filterBtn) {
                filterPopup.style.display = "none";
            }
        });
    }

    const minRuntime = document.getElementById("min-runtime");
    const maxRuntime = document.getElementById("max-runtime");
    const minRuntimeVal = document.getElementById("min-runtime-value");
    const maxRuntimeVal = document.getElementById("max-runtime-value");

    function updateSlider() {
        if (!minRuntime || !maxRuntime) return;
        let minVal = parseInt(minRuntime.value);
        let maxVal = parseInt(maxRuntime.value);

        if (minVal > maxVal) {
            let tmp = minVal;
            minVal = maxVal;
            maxVal = tmp;
        }

        if (minRuntimeVal) minRuntimeVal.textContent = minVal;
        if (maxRuntimeVal) maxRuntimeVal.textContent = maxVal;
    }

    if (minRuntime && maxRuntime) {
        minRuntime.addEventListener("input", updateSlider);
        maxRuntime.addEventListener("input", updateSlider);
    }

    // Giữ lại lựa chọn từ URL
    const params = new URLSearchParams(window.location.search);

    const urlRating = params.get("min_rating");
    if (urlRating) {
        const ratingSelect = document.getElementById("min-rating");
        if (ratingSelect) ratingSelect.value = urlRating;
    }

    const urlMinRuntime = params.get("min_runtime");
    const urlMaxRuntime = params.get("max_runtime");
    if (urlMinRuntime && minRuntime) minRuntime.value = urlMinRuntime;
    if (urlMaxRuntime && maxRuntime) maxRuntime.value = urlMaxRuntime;
    updateSlider();

    // Khi bấm nút ÁP DỤNG
    if (applyBtn) {
        applyBtn.addEventListener("click", function () {
            const baseUrl = this.getAttribute("data-url") || "/search.html";
            const searchParams = new URLSearchParams(window.location.search);

            searchParams.delete("genre");
            searchParams.delete("min_rating");
            searchParams.delete("min_runtime");
            searchParams.delete("max_runtime");
            searchParams.delete("page");

            const genreChecks = document.querySelectorAll('input[name="genre"]:checked');
            genreChecks.forEach((cb) => searchParams.append("genre", cb.value));

            const ratingSelect = document.getElementById("min-rating");
            if (ratingSelect && ratingSelect.value !== "0") {
                searchParams.append("min_rating", ratingSelect.value);
            }

            if (minRuntime && maxRuntime) {
                const minRt = Math.min(parseInt(minRuntime.value), parseInt(maxRuntime.value));
                const maxRt = Math.max(parseInt(minRuntime.value), parseInt(maxRuntime.value));
                if (minRt !== 30 || maxRt !== 240) {
                    searchParams.append("min_runtime", minRt);
                    searchParams.append("max_runtime", maxRt);
                }
            }

            window.location.href = baseUrl + "?" + searchParams.toString();
        });
    }
});