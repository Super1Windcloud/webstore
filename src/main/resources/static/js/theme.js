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
});
