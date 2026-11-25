const SITE_ORIGIN = "https://play-warcontry.com";
const SESSION_DURATION = 8 * 60 * 60;

const encoder = new TextEncoder();
const decoder = new TextDecoder();

function json(data, status = 200, origin = null, extraHeaders = {}) {
  const headers = {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
    "Vary": "Origin",
    ...extraHeaders
  };

  if (origin === SITE_ORIGIN) {
    headers["Access-Control-Allow-Origin"] = SITE_ORIGIN;
  }

  return new Response(JSON.stringify(data), {
    status,
    headers
  });
}

function getParisDate() {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Paris",
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).formatToParts(new Date());

  return Object.fromEntries(
    parts
      .filter(part => part.type !== "literal")
      .map(part => [part.type, Number(part.value)])
  );
}

function base64url(bytes) {
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }

  return btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function fromBase64url(value) {
  const base64 = value
    .replace(/-/g, "+")
    .replace(/_/g, "/");

  const binary = atob(
    base64 + "=".repeat((4 - base64.length % 4) % 4)
  );

  return Uint8Array.from(binary, c => c.charCodeAt(0));
}

async function getSessionKey(secret) {
  return crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign", "verify"]
  );
}

async function createSession(secret) {
  const payload = base64url(
    encoder.encode(JSON.stringify({
      exp: Math.floor(Date.now() / 1000) + SESSION_DURATION
    }))
  );

  const key = await getSessionKey(secret);

  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    encoder.encode(payload)
  );

  return payload + "." + base64url(new Uint8Array(signature));
}

async function verifySession(request, secret) {
  try {
    const cookie = request.headers.get("Cookie") || "";
    const match = cookie.match(/(?:^|;\s*)wc_admin=([^;]+)/);

    if (!match) return false;

    const parts = match[1].split(".");
    if (parts.length !== 2) return false;

    const key = await getSessionKey(secret);

    const valid = await crypto.subtle.verify(
      "HMAC",
      key,
      fromBase64url(parts[1]),
      encoder.encode(parts[0])
    );

    if (!valid) return false;

    const payload = JSON.parse(
      decoder.decode(fromBase64url(parts[0]))
    );

    return Number.isInteger(payload.exp) &&
      payload.exp > Math.floor(Date.now() / 1000);
  } catch {
    return false;
  }
}

function passwordMatches(input, expected) {
  const a = encoder.encode(input);
  const b = encoder.encode(expected);

  let difference = a.length ^ b.length;
  const length = Math.max(a.length, b.length);

  for (let i = 0; i < length; i++) {
    difference |= (a[i] || 0) ^ (b[i] || 0);
  }

  return difference === 0;
}

function adminPage() {
  const html = `<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Administration - War Contry</title>
<style>
* { box-sizing: border-box; }
body {
  margin: 0;
  padding: 30px 16px;
  background: #101014;
  color: #f5f5f5;
  font-family: Arial, sans-serif;
}
main {
  max-width: 950px;
  margin: 35px auto;
}
.panel {
  background: #1c1c23;
  border: 1px solid #34343e;
  border-radius: 14px;
  padding: 24px;
  margin-bottom: 20px;
}
h1 { color: #e9b44c; }
h2 { font-size: 19px; }
input, button {
  width: 100%;
  padding: 13px;
  margin-top: 12px;
  border-radius: 8px;
  font-size: 15px;
}
input {
  background: #101014;
  border: 1px solid #484854;
  color: white;
}
button {
  background: #b98532;
  color: #17120a;
  border: 0;
  font-weight: bold;
  cursor: pointer;
}
button:hover { background: #d6a64f; }
.message { color: #ff9b9b; }
.muted { color: #aaa; }
.hidden { display: none; }
.table-wrap { overflow-x: auto; }
table {
  width: 100%;
  border-collapse: collapse;
  min-width: 500px;
}
th, td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #383842;
}
th { color: #e9b44c; }
#logout {
  width: auto;
  padding: 10px 18px;
}
</style>
</head>
<body>
<main>
  <h1>War Contry</h1>
  <p class="muted">Administration privée du calendrier de Noël</p>

  <section class="panel" id="loginPanel">
    <h2>Connexion administrateur</h2>
    <form id="loginForm">
      <input
        id="password"
        type="password"
        placeholder="Mot de passe administrateur"
        autocomplete="current-password"
        required
      >
      <button type="submit">Se connecter</button>
    </form>
    <p class="message" id="loginMessage"></p>
  </section>

  <section class="panel hidden" id="dashboard">
    <h2>Réclamations enregistrées</h2>
    <p id="count" class="muted"></p>
    <button id="logout" type="button">Se déconnecter</button>
    <p id="status" class="muted"></p>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Jour</th>
            <th>Pseudo Minecraft</th>
            <th>Date de réclamation</th>
          </tr>
        </thead>
        <tbody id="claims"></tbody>
      </table>
    </div>
  </section>
</main>

<script>
const loginPanel = document.getElementById("loginPanel");
const dashboard = document.getElementById("dashboard");
const loginMessage = document.getElementById("loginMessage");
const statusText = document.getElementById("status");
const claimsBody = document.getElementById("claims");

function showDashboard(show) {
  loginPanel.classList.toggle("hidden", show);
  dashboard.classList.toggle("hidden", !show);
}

async function loadClaims() {
  statusText.textContent = "Chargement...";

  try {
    const response = await fetch("/admin/api/claims", {
      credentials: "same-origin"
    });

    if (response.status === 401) {
      showDashboard(false);
      return;
    }

    if (!response.ok) {
      throw new Error("Erreur de chargement.");
    }

    const data = await response.json();

    claimsBody.replaceChildren();

    document.getElementById("count").textContent =
      data.claims.length + " réclamation(s)";

    for (const claim of data.claims) {
      const row = document.createElement("tr");

      const dayCell = document.createElement("td");
      dayCell.textContent = "Jour " + claim.day;

      const pseudoCell = document.createElement("td");
      pseudoCell.textContent = claim.pseudo;

      const dateCell = document.createElement("td");
      dateCell.textContent = claim.created_at;

      row.append(dayCell, pseudoCell, dateCell);
      claimsBody.appendChild(row);
    }

    statusText.textContent = data.claims.length
      ? "Données chargées."
      : "Aucune réclamation pour le moment.";

    showDashboard(true);
  } catch {
    statusText.textContent =
      "Impossible de charger les réclamations.";
  }
}

document.getElementById("loginForm").addEventListener(
  "submit",
  async event => {
    event.preventDefault();
    loginMessage.textContent = "";

    const password = document.getElementById("password").value;

    try {
      const response = await fetch("/admin/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "same-origin",
        body: JSON.stringify({ password })
      });

      if (!response.ok) {
        loginMessage.textContent =
          "Mot de passe incorrect ou connexion impossible.";
        return;
      }

      document.getElementById("password").value = "";
      await loadClaims();
    } catch {
      loginMessage.textContent =
        "Impossible de contacter le serveur.";
    }
  }
);

document.getElementById("logout").addEventListener(
  "click",
  async () => {
    await fetch("/admin/logout", {
      method: "POST",
      credentials: "same-origin"
    });

    showDashboard(false);
    loginMessage.textContent = "Tu es déconnecté.";
  }
);

loadClaims();
</script>
</body>
</html>`;

  return new Response(html, {
    headers: {
      "Content-Type": "text/html; charset=utf-8",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff",
      "X-Frame-Options": "DENY",
      "Referrer-Policy": "no-referrer"
    }
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const origin = request.headers.get("Origin");

    // Vérifier la connexion à D1
    if (url.pathname === "/health" && request.method === "GET") {
      try {
        await env.DB.prepare("SELECT 1").first();

        return json({
          ok: true,
          message: "Worker et base D1 opérationnels."
        }, 200, origin);
      } catch {
        return json({
          ok: false,
          message: "Base de données indisponible."
        }, 500, origin);
      }
    }

    // Page administrateur
    if (url.pathname === "/admin" && request.method === "GET") {
      return adminPage();
    }

    // Connexion administrateur
    if (url.pathname === "/admin/login" && request.method === "POST") {
      if (origin !== url.origin) {
        return json({ error: "Origine non autorisée." }, 403);
      }

      if (!env.ADMIN_PASSWORD || !env.SESSION_SECRET) {
        return json({ error: "Configuration administrateur incomplète." }, 500);
      }

      try {
        const text = await request.text();

        if (text.length > 2000) {
          return json({ error: "Demande trop longue." }, 400);
        }

        const body = JSON.parse(text);
        const password = typeof body.password === "string"
          ? body.password
          : "";

        if (!passwordMatches(password, env.ADMIN_PASSWORD)) {
          return json({ error: "Identifiants incorrects." }, 401);
        }

        const session = await createSession(env.SESSION_SECRET);

        return json(
          { ok: true },
          200,
          null,
          {
            "Set-Cookie":
              "wc_admin=" + session +
              "; HttpOnly; Secure; SameSite=Strict; Path=/admin; Max-Age=" +
              SESSION_DURATION
          }
        );
      } catch {
        return json({ error: "Erreur de connexion." }, 400);
      }
    }

    // Consulter les réclamations : administrateur uniquement
    if (
      url.pathname === "/admin/api/claims" &&
      request.method === "GET"
    ) {
      if (!env.SESSION_SECRET) {
        return json({ error: "Configuration indisponible." }, 500);
      }

      if (!(await verifySession(request, env.SESSION_SECRET))) {
        return json({ error: "Connexion requise." }, 401);
      }

      try {
        const result = await env.DB.prepare(`
          SELECT calendar_year, day, pseudo, created_at
          FROM claims
          WHERE calendar_year = 2026
          ORDER BY day ASC, created_at DESC
        `).all();

        return json({
          ok: true,
          claims: result.results || []
        });
      } catch {
        return json({ error: "Lecture impossible." }, 500);
      }
    }

    // Déconnexion
    if (url.pathname === "/admin/logout" && request.method === "POST") {
      if (origin !== url.origin) {
        return json({ error: "Origine non autorisée." }, 403);
      }

      return json(
        { ok: true },
        200,
        null,
        {
          "Set-Cookie":
            "wc_admin=; HttpOnly; Secure; SameSite=Strict; Path=/admin; Max-Age=0"
        }
      );
    }

    // Réclamations des joueurs
    if (url.pathname !== "/claim") {
      return json({ error: "Route introuvable." }, 404);
    }

    if (origin !== SITE_ORIGIN) {
      return json({ error: "Origine non autorisée." }, 403);
    }

    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": SITE_ORIGIN,
          "Access-Control-Allow-Methods": "POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type",
          "Access-Control-Max-Age": "86400",
          "Vary": "Origin"
        }
      });
    }

    if (request.method !== "POST") {
      return json({ error: "Méthode non autorisée." }, 405, origin);
    }

    if (!request.headers.get("Content-Type")?.includes("application/json")) {
      return json({ error: "Format invalide." }, 415, origin);
    }

    try {
      const bodyText = await request.text();

      if (bodyText.length > 1000) {
        return json({ error: "Demande trop longue." }, 400, origin);
      }

      const body = JSON.parse(bodyText);
      const day = Number(body.day);
      const year = Number(body.year);
      const pseudo = typeof body.pseudo === "string"
        ? body.pseudo.trim()
        : "";

      if (
        year !== 2026 ||
        !Number.isInteger(day) ||
        day < 1 ||
        day > 24 ||
        !/^[A-Za-z0-9_]{3,16}$/.test(pseudo)
      ) {
        return json({ error: "Informations invalides." }, 400, origin);
      }

      const today = getParisDate();

    const isTestClaim =
  env.TEST_MODE === "true" &&
  day === 1 &&
  pseudo === "TEST_WarContry";

if (
  !isTestClaim &&
  (
    today.year !== 2026 ||
    today.month !== 12 ||
    today.day !== day
  )
) {
  return json({
    error: "Ce cadeau ne peut pas être réclamé aujourd'hui."
  }, 403, origin);
}

      await env.DB.prepare(`
        INSERT OR IGNORE INTO claims
          (calendar_year, day, pseudo)
        VALUES (?, ?, ?)
      `).bind(2026, day, pseudo).run();

      return json({
        ok: true,
        message: "Demande traitée."
      }, 200, origin);

    } catch {
      return json({
        error: "Impossible de traiter la demande."
      }, 500, origin);
    }
  }
};
