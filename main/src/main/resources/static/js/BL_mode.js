document.addEventListener("DOMContentLoaded", function () {
    const themeButtons = document.querySelectorAll(".theme-toggle");
    const body = document.body;

    function updateThemeUI(isLight) {
        if (isLight) {
            body.classList.add("light-mode");
            document.documentElement.classList.add("light-mode");
        } else {
            body.classList.remove("light-mode");
            document.documentElement.classList.remove("light-mode");
        }
        themeButtons.forEach(btn => {
            btn.textContent = isLight ? "Mode: Tối" : "Mode: Sáng";
        });
    }

    // 1. Kiểm tra trạng thái đã lưu từ lần truy cập trước
    const savedTheme = localStorage.getItem("theme");
    updateThemeUI(savedTheme === "light");

    // 2. Bắt sự kiện khi người dùng bấm nút
    themeButtons.forEach(btn => {
        btn.addEventListener("click", function () {
            const isLight = !body.classList.contains("light-mode");
            localStorage.setItem("theme", isLight ? "light" : "dark");
            updateThemeUI(isLight);
        });
    });
});
