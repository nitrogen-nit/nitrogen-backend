// Nitrogen Design Docs — hành vi phía client. Trang vẫn đọc được khi JS tắt.

const root = document.documentElement;
const article = document.querySelector(".article");

function storedTheme() {
  try {
    return localStorage.getItem("theme");
  } catch {
    return null;
  }
}

function currentTheme() {
  return root.dataset.theme === "dark" ? "dark" : "light";
}

// ── Theme toggle ──────────────────────────────────────────

document.querySelector("[data-theme-toggle]")?.addEventListener("click", () => {
  const next = currentTheme() === "dark" ? "light" : "dark";
  root.dataset.theme = next;
  try {
    localStorage.setItem("theme", next);
  } catch {
    // Không lưu được (private mode) — theme vẫn đổi cho lần xem này.
  }
  renderDiagrams();
});

window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", (event) => {
  if (!storedTheme()) {
    root.dataset.theme = event.matches ? "dark" : "light";
    renderDiagrams();
  }
});

// ── Mobile navigation ─────────────────────────────────────

const menuButton = document.querySelector("[data-menu-toggle]");
menuButton?.addEventListener("click", () => {
  const open = document.body.classList.toggle("nav-open");
  menuButton.setAttribute("aria-expanded", String(open));
});

document.querySelector(".sidebar")?.addEventListener("click", (event) => {
  if (event.target.closest("a")) {
    document.body.classList.remove("nav-open");
    menuButton?.setAttribute("aria-expanded", "false");
  }
});

document.querySelector('.nav-list a[aria-current="page"]')?.scrollIntoView({ block: "center" });

// ── Tables: cuộn ngang thay vì tràn trang ─────────────────

article?.querySelectorAll("table").forEach((table) => {
  const wrap = document.createElement("div");
  wrap.className = "table-wrap";
  table.parentNode.insertBefore(wrap, table);
  wrap.appendChild(table);
});

// ── Heading anchors + table of contents ───────────────────

const headings = article ? [...article.querySelectorAll("h2[id], h3[id]")] : [];

headings.forEach((heading) => {
  const anchor = document.createElement("a");
  anchor.className = "heading-anchor";
  anchor.href = `#${heading.id}`;
  anchor.setAttribute("aria-label", "Liên kết tới mục này");
  anchor.textContent = "#";
  heading.prepend(anchor);
});

const toc = document.querySelector(".toc");
if (toc && headings.length >= 2) {
  const list = document.createElement("ul");
  const links = new Map();

  headings.forEach((heading) => {
    const link = document.createElement("a");
    link.href = `#${heading.id}`;
    link.className = heading.tagName === "H3" ? "depth-3" : "depth-2";
    link.textContent = heading.textContent.replace(/^#/, "").trim();
    const item = document.createElement("li");
    item.appendChild(link);
    list.appendChild(item);
    links.set(heading, link);
  });

  const title = document.createElement("p");
  title.className = "toc-title";
  title.textContent = "Trên trang này";
  toc.append(title, list);

  const observer = new IntersectionObserver(
    (entries) => {
      const visible = entries.filter((entry) => entry.isIntersecting);
      if (visible.length === 0) return;
      links.forEach((link) => link.classList.remove("active"));
      links.get(visible[0].target)?.classList.add("active");
    },
    { rootMargin: "-70px 0px -70% 0px" }
  );
  headings.forEach((heading) => observer.observe(heading));
}

// ── Copy button cho code block ────────────────────────────

article?.querySelectorAll("pre").forEach((pre) => {
  if (pre.closest(".diagram") || pre.querySelector("code.language-mermaid")) return;
  const button = document.createElement("button");
  button.type = "button";
  button.className = "copy-button";
  button.textContent = "Copy";
  button.addEventListener("click", async () => {
    try {
      await navigator.clipboard.writeText(pre.querySelector("code")?.innerText ?? pre.innerText);
      button.textContent = "Đã copy";
    } catch {
      button.textContent = "Lỗi";
    }
    setTimeout(() => (button.textContent = "Copy"), 1500);
  });
  pre.appendChild(button);
});

// ── Mermaid ───────────────────────────────────────────────
// Kramdown xuất ```mermaid thành <pre><code class="language-mermaid">, không phải
// <div class="mermaid">; đổi lại ở đây rồi render theo theme hiện tại.

const diagrams = [];
article?.querySelectorAll("code.language-mermaid").forEach((code) => {
  const block = code.closest("div.highlighter-rouge, pre") ?? code;
  const container = document.createElement("div");
  container.className = "diagram";
  container.dataset.source = code.textContent;
  block.replaceWith(container);
  diagrams.push(container);
});

let mermaidModule;

// Không tải hoặc không parse được diagram ⇒ hiện nguyên văn nguồn, vẫn đọc được.
function showSource(container) {
  const pre = document.createElement("pre");
  pre.textContent = container.dataset.source;
  container.replaceChildren(pre);
}

async function renderDiagrams() {
  if (diagrams.length === 0) return;
  try {
    mermaidModule ??= (await import("https://cdn.jsdelivr.net/npm/mermaid@11.4.1/dist/mermaid.esm.min.mjs")).default;
  } catch {
    diagrams.forEach(showSource);
    return;
  }

  mermaidModule.initialize({
    startOnLoad: false,
    theme: currentTheme() === "dark" ? "dark" : "neutral",
    fontFamily: getComputedStyle(document.body).fontFamily,
    securityLevel: "strict",
  });

  for (const [index, container] of diagrams.entries()) {
    try {
      const { svg } = await mermaidModule.render(`diagram-${index}-${Date.now()}`, container.dataset.source);
      container.innerHTML = svg;
    } catch {
      showSource(container);
    }
  }
}

renderDiagrams();
