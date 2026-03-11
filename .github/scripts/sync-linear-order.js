/**
 * sync-linear-order.js
 *
 * Läser kortens ordning per kolumn från GitHub Projects (v2)
 * och uppdaterar Linear-issuens sortOrder så att den matchar.
 *
 * GitHub Projects är "source of truth" – Linear skriver aldrig
 * tillbaka ordningen till GitHub.
 *
 * Konfigureras via miljövariabler (se workflow-filen):
 *   GITHUB_TOKEN          – Personal access token med scope: read:project
 *   LINEAR_API_KEY        – Linear API-nyckel
 *   GH_PROJECT_NUMBER – Siffran i URL:en för projektet (t.ex. 42)
 *   GH_ORG            – GitHub-organisationens namn
 */

const GITHUB_API = 'https://api.github.com/graphql';
const LINEAR_API = 'https://api.linear.app/graphql';

const GITHUB_TOKEN = process.env.GITHUB_TOKEN;
const LINEAR_API_KEY = process.env.LINEAR_API_KEY;
const PROJECT_NUMBER = parseInt(process.env.GH_PROJECT_NUMBER, 10);
const GITHUB_ORG = process.env.GH_ORG;

// Avstånd mellan sortOrder-värden – ger plats att lägga in
// nya issues utan att behöva räkna om allt direkt.
const SORT_STEP = 10_000;

// ─── Hjälpfunktioner ───────────────────────────────────────────────────────

async function githubQuery(query, variables = {}) {
  const res = await fetch(GITHUB_API, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `bearer ${GITHUB_TOKEN}`,
    },
    body: JSON.stringify({ query, variables }),
  });
  const json = await res.json();
  if (json.errors) {
    throw new Error(`GitHub GraphQL error: ${JSON.stringify(json.errors)}`);
  }
  return json.data;
}

async function linearQuery(query, variables = {}) {
  const res = await fetch(LINEAR_API, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: LINEAR_API_KEY,
    },
    body: JSON.stringify({ query, variables }),
  });
  const json = await res.json();
  if (json.errors) {
    throw new Error(`Linear GraphQL error: ${JSON.stringify(json.errors)}`);
  }
  return json.data;
}

// ─── Steg 1: Hämta alla items från GitHub Projects ────────────────────────

async function fetchGitHubProjectItems() {
  const allItems = [];
  let cursor = null;

  do {
    const data = await githubQuery(
      `query($org: String!, $number: Int!, $cursor: String) {
        organization(login: $org) {
          projectV2(number: $number) {
            items(first: 100, after: $cursor) {
              pageInfo { hasNextPage endCursor }
              nodes {
                # Status-kolumnen (anpassa fältnamnet om det heter något annat)
                fieldValueByName(name: "Status") {
                  ... on ProjectV2ItemFieldSingleSelectValue {
                    name
                  }
                }
                content {
                  ... on Issue {
                    number
                    url
                  }
                }
              }
            }
          }
        }
      }`,
      { org: GITHUB_ORG, number: PROJECT_NUMBER, cursor }
    );

    const page = data.organization.projectV2.items;
    allItems.push(...page.nodes);
    cursor = page.pageInfo.hasNextPage ? page.pageInfo.endCursor : null;
  } while (cursor);

  return allItems;
}

// ─── Steg 2: Bygg en map GitHub-issue-URL → önskad sortOrder ─────────────
//
// Items returneras från GitHub i den ordning de visas på brädan
// (per kolumn, uppifrån och ner). Vi grupperar per kolumn och
// tilldelar ett sortOrder-värde baserat på positionen inom kolumnen.

function buildSortOrderMap(items) {
  // Räkna positioner per kolumn
  const columnCounters = {};
  const map = new Map(); // githubIssueUrl → sortOrder

  for (const item of items) {
    // Hoppa över items som inte är Issues (t.ex. draft cards)
    if (!item.content?.url) continue;

    const column = item.fieldValueByName?.name ?? '__no_status__';

    if (columnCounters[column] === undefined) {
      columnCounters[column] = 0;
    }

    const position = columnCounters[column]++;
    // Position 0 (överst) → lägst sortOrder → hamnar högst i Linear
    map.set(item.content.url, position * SORT_STEP);
  }

  return map;
}

// ─── Steg 3: Hämta Linear-issues med GitHub-koppling ─────────────────────

async function fetchLinearIssuesWithGitHubUrls() {
  const allIssues = [];
  let cursor = null;

  do {
    const data = await linearQuery(
      `query($cursor: String) {
        issues(
          first: 250
          after: $cursor
          filter: { attachments: { url: { contains: "github.com" } } }
        ) {
          pageInfo { hasNextPage endCursor }
          nodes {
            id
            sortOrder
            attachments {
              nodes { url }
            }
          }
        }
      }`,
      { cursor }
    );

    const page = data.issues;
    allIssues.push(...page.nodes);
    cursor = page.pageInfo.hasNextPage ? page.pageInfo.endCursor : null;
  } while (cursor);

  return allIssues;
}

// ─── Steg 4: Uppdatera sortOrder i Linear ─────────────────────────────────

async function updateLinearSortOrder(issueId, sortOrder) {
  await linearQuery(
    `mutation($id: String!, $sortOrder: Float!) {
      issueUpdate(id: $id, input: { sortOrder: $sortOrder }) {
        success
      }
    }`,
    { id: issueId, sortOrder }
  );
}

// ─── Main ─────────────────────────────────────────────────────────────────

async function main() {
  console.log(`Hämtar items från GitHub Projects #${PROJECT_NUMBER} (${GITHUB_ORG})…`);
  const ghItems = await fetchGitHubProjectItems();
  console.log(`  ${ghItems.length} items hittades.`);

  const sortOrderMap = buildSortOrderMap(ghItems);
  console.log(`  ${sortOrderMap.size} issues med GitHub-URL mappade.`);

  console.log('Hämtar Linear-issues med GitHub-attachments…');
  const linearIssues = await fetchLinearIssuesWithGitHubUrls();
  console.log(`  ${linearIssues.length} Linear-issues hittades.`);

  let updated = 0;
  let skipped = 0;

  for (const issue of linearIssues) {
    // En issue kan ha flera attachments – leta upp den som matchar en GitHub-issue-URL
    const githubUrl = issue.attachments.nodes
      .map((a) => a.url)
      .find((url) => sortOrderMap.has(url));

    if (!githubUrl) {
      skipped++;
      continue;
    }

    const desiredSortOrder = sortOrderMap.get(githubUrl);

    // Hoppa över om sortOrder redan stämmer (undviker onödiga API-anrop)
    if (Math.abs(issue.sortOrder - desiredSortOrder) < 1) {
      skipped++;
      continue;
    }

    console.log(`  Uppdaterar issue ${issue.id}: sortOrder ${issue.sortOrder} → ${desiredSortOrder}`);
    await updateLinearSortOrder(issue.id, desiredSortOrder);
    updated++;

    // Enkel rate-limiting: vänta 100 ms mellan varje mutation
    await new Promise((r) => setTimeout(r, 100));
  }

  console.log(`\nKlart! Uppdaterade: ${updated}, hoppade över: ${skipped}.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
