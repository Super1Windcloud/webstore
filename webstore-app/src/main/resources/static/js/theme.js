document.addEventListener("DOMContentLoaded", () => {
  const root = document.documentElement;
  const loadingOverlay = createLoadingOverlay();
  const toggleTheme = () => {
    const current = root.getAttribute("data-theme") || "dark";
    const next = current === "dark" ? "light" : "dark";
    root.setAttribute("data-theme", next);
    localStorage.setItem("webstore-theme", next);
  };

  document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
    button.addEventListener("click", toggleTheme);
  });

  const searchInput = document.querySelector("[data-app-store-search]");
  const appCards = Array.from(document.querySelectorAll("[data-app-store-card]"));
  const emptyState = document.querySelector("[data-app-store-empty]");
  const visibleCount = document.querySelector("[data-app-store-visible-count]");

  if (searchInput && appCards.length > 0) {
    const updateSearch = () => {
      const query = searchInput.value.trim().toLowerCase();
      let count = 0;

      appCards.forEach((card) => {
        const searchText = [
          card.querySelector("[data-app-store-name]")?.textContent || "",
          card.querySelector("[data-app-store-category]")?.textContent || "",
          card.querySelector("[data-app-store-description]")?.textContent || "",
        ]
          .join(" ")
          .toLowerCase();
        const isVisible = query === "" || searchText.includes(query);
        card.hidden = !isVisible;
        if (isVisible) {
          count += 1;
        }
      });

      if (visibleCount) {
        visibleCount.textContent = String(count);
      }

      if (emptyState) {
        emptyState.hidden = count > 0;
      }
    };

    searchInput.addEventListener("input", updateSearch);
    updateSearch();
  }

  document.querySelectorAll("[data-toast]").forEach((toast) => {
    const dismiss = () => {
      toast.classList.add("toast-leaving");
      window.setTimeout(() => toast.remove(), 220);
    };

    window.setTimeout(dismiss, 2800);
    toast.querySelector("[data-toast-close]")?.addEventListener("click", dismiss);
  });

  document.querySelectorAll("form[method='post']").forEach((form) => {
    if (!isRuntimeActionForm(form)) {
      return;
    }

    form.addEventListener("submit", (event) => {
      if (form.dataset.submitting === "true") {
        event.preventDefault();
        return;
      }

      form.dataset.submitting = "true";
      document.body.classList.add("is-loading");
      loadingOverlay.hidden = false;

      form.querySelectorAll("button, input[type='submit']").forEach((control) => {
        control.disabled = true;
      });
    });
  });
});

function isRuntimeActionForm(form) {
  const action = form.getAttribute("action") || "";
  return (
    /\/app-store\/[^/]+\/install$/.test(action) ||
    /\/my-apps\/[^/]+\/start$/.test(action) ||
    /\/my-apps\/[^/]+\/stop$/.test(action) ||
    /\/my-apps\/[^/]+\/uninstall$/.test(action) ||
    /\/my-apps\/start-all$/.test(action) ||
    /\/my-apps\/stop-all$/.test(action)
  );
}

function createLoadingOverlay() {
  const overlay = document.createElement("div");
  overlay.className = "loading-overlay";
  overlay.hidden = true;
  overlay.innerHTML = `
    <div class="loading-dialog" role="status" aria-live="polite" aria-label="页面正在加载">
      <div class="loading-spinner"></div>
      <div class="loading-text">正在处理中，请稍候...</div>
    </div>
  `;
  document.body.appendChild(overlay);
  return overlay;
}
