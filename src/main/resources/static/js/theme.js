document.addEventListener("DOMContentLoaded", () => {
  const root = document.documentElement;
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
});
