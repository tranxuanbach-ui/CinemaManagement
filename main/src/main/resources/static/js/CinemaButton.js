document.addEventListener("DOMContentLoaded", function () {
    const theaterBtn = document.getElementById("theaterBtn");
    const theaterModal = document.getElementById("theaterModal");
    const theaterClose = document.getElementById("theaterClose");

    const branchModal = document.getElementById("branchModal");
    const branchClose = document.getElementById("branchClose");
    const branchBack = document.getElementById("branchBack");

    const showtimeModal = document.getElementById("showtimeModal");
    const showtimeClose = document.getElementById("showtimeClose");
    const showtimeBack = document.getElementById("showtimeBack");

    if (theaterBtn && theaterModal) {
        theaterBtn.addEventListener("click", function () {
            theaterModal.classList.add("active");
        });
    }

    if (theaterClose && theaterModal) {
        theaterClose.addEventListener("click", function () {
            theaterModal.classList.remove("active");
        });
    }

    if (branchModal) {
        if (branchClose) {
            branchClose.addEventListener("click", function () {
                branchModal.classList.remove("active");
            });
        }
        if (branchBack) {
            branchBack.addEventListener("click", function () {
                branchModal.classList.remove("active");
                if (theaterModal) {
                    theaterModal.classList.add("active");
                }
            });
        }
    }

    if (showtimeModal) {
        if (showtimeClose) {
            showtimeClose.addEventListener("click", function () {
                showtimeModal.classList.remove("active");
            });
        }
        if (showtimeBack) {
            showtimeBack.addEventListener("click", function () {
                showtimeModal.classList.remove("active");
                if (branchModal) {
                    branchModal.classList.add("active");
                }
            });
        }
    }

    document.querySelectorAll(".watch-modal-overlay").forEach(function (overlay) {
        overlay.addEventListener("click", function () {
            overlay.parentElement.classList.remove("active");
        });
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            document.querySelectorAll(".watch-modal").forEach(function (modal) {
                modal.classList.remove("active");
            });
        }
    });
});