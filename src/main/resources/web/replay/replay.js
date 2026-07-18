const app = document.querySelector("#app");
const errorTemplate = document.querySelector("#error-template");
const replayIdPattern = /^[0-9a-f]{32}$/;

const phaseLabels = { DAY: "낮", NIGHT: "밤", DAWN: "새벽", VOTE: "투표", END: "게임 종료" };
const visibilityLabels = {
    PUBLIC: "공개", MAFIA_CHANNEL: "마피아", COUPLE_CHANNEL: "연인", DEAD_CHANNEL: "사망자",
    DIRECT_MESSAGE: "개인 DM", EPHEMERAL: "개인 응답", SYSTEM_INTERNAL: "시스템"
};

function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined && text !== null) node.textContent = text;
    return node;
}

function formatDate(value, withTime = true) {
    if (!value) return "기록 없음";
    return new Intl.DateTimeFormat("ko-KR", {
        dateStyle: "medium",
        ...(withTime ? { timeStyle: "short" } : {})
    }).format(new Date(value));
}

function formatTime(value) {
    return value ? new Intl.DateTimeFormat("ko-KR", { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false }).format(new Date(value)) : "--:--";
}

function formatDuration(value) {
    const totalSeconds = Math.max(0, Math.floor((value || 0) / 1000));
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    return [hours, minutes, seconds].filter((_, index) => index > 0 || hours > 0).map(part => String(part).padStart(2, "0")).join(":");
}

async function fetchJson(url) {
    const response = await fetch(url, { headers: { Accept: "application/json" } });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(payload.error || "리플레이 데이터를 불러오지 못했습니다.");
    return payload;
}

function showError(message) {
    const fragment = errorTemplate.content.cloneNode(true);
    fragment.querySelector(".empty-message").textContent = message;
    app.replaceChildren(fragment);
}

function resultName(replay) {
    return replay.winningTeamName || replay.endReason || "결과 기록 없음";
}

function makeMetric(label, value) {
    const metric = element("div", "metric");
    metric.append(element("small", "", label), element("strong", "", value));
    return metric;
}

function renderReplayCard(replay) {
    const link = element("a", "replay-card panel");
    link.href = `/history/${replay.replayUuid}`;

    const top = element("div", "card-top");
    const title = element("div");
    title.append(element("span", "eyebrow", "CASE RECORD"), element("h3", "", replay.guildName || "알 수 없는 서버"), element("span", "replay-date", formatDate(replay.replayStartedAtMillis)));
    top.append(title, element("span", "result-badge", resultName(replay)));

    const metrics = element("div", "replay-metrics");
    metrics.append(
        makeMetric("PLAYERS", `${replay.initialPlayerCount}명`),
        makeMetric("DAYS", `${replay.dayCount}일`),
        makeMetric("DURATION", formatDuration(replay.durationMillis)),
        makeMetric("LOGS", `${replay.logCount}개`)
    );

    const chips = element("div", "player-chips");
    (replay.players || []).slice(0, 8).forEach(player => chips.append(element("span", "player-chip", `${player.name} · ${player.jobName || "직업 미상"}`)));

    const uuid = element("div", "uuid-line");
    uuid.append(element("code", "", replay.replayUuid), element("span", "open-label", "기록 열기 →"));
    link.append(top, metrics, chips, uuid);
    return link;
}

function renderArchive(payload) {
    document.title = "게임 리플레이 아카이브";
    const section = element("div");
    const hero = element("section", "archive-hero panel");
    const copy = element("div");
    copy.append(
        element("span", "eyebrow", "REPLAY DATABASE"),
        element("h1", "", "모든 게임에는 기록이 남습니다."),
        element("p", "", "종료된 게임의 플레이어, 직업, 승리 결과와 전체 진행 로그를 한 화면에서 확인하세요.")
    );
    const count = element("div", "archive-count");
    count.append(element("strong", "", String(payload.count || 0)), document.createTextNode("개의 리플레이가 보관되어 있습니다."));
    copy.append(count);

    const form = element("form", "uuid-form");
    const label = element("label", "", "GAME UUID로 바로 이동");
    label.htmlFor = "uuid-input";
    const inputRow = element("div", "uuid-input-row");
    const input = element("input");
    input.id = "uuid-input";
    input.name = "uuid";
    input.placeholder = "32자리 리플레이 UUID";
    input.autocomplete = "off";
    input.maxLength = 32;
    const submit = element("button", "", "열기");
    submit.type = "submit";
    inputRow.append(input, submit);
    const formError = element("p", "form-error");
    form.append(label, inputRow, formError);
    form.addEventListener("submit", event => {
        event.preventDefault();
        const uuid = input.value.trim().toLowerCase();
        if (!replayIdPattern.test(uuid)) {
            formError.textContent = "영문 소문자와 숫자로 된 32자리 UUID를 입력해 주세요.";
            input.focus();
            return;
        }
        location.assign(`/history/${uuid}`);
    });
    hero.append(copy, form);

    const tools = element("div", "archive-tools");
    tools.append(element("h2", "", "최근 리플레이"));
    const search = element("input", "search-input");
    search.type = "search";
    search.placeholder = "서버, 플레이어, 직업 검색";
    search.setAttribute("aria-label", "리플레이 검색");
    tools.append(search);

    const grid = element("section", "replay-grid");
    const replays = payload.replays || [];
    const paint = query => {
        const normalized = query.trim().toLocaleLowerCase("ko-KR");
        const filtered = replays.filter(replay => !normalized || [replay.guildName, replay.winningTeamName, replay.replayUuid, ...(replay.players || []).flatMap(player => [player.name, player.jobName])].filter(Boolean).join(" ").toLocaleLowerCase("ko-KR").includes(normalized));
        grid.replaceChildren(...filtered.map(renderReplayCard));
        if (!filtered.length) grid.append(element("div", "no-results panel", replays.length ? "검색 조건에 맞는 리플레이가 없습니다." : "아직 저장된 리플레이가 없습니다."));
    };
    search.addEventListener("input", () => paint(search.value));
    paint("");
    section.append(hero, tools, grid);
    app.replaceChildren(section);
}

function logCategory(type) {
    if (["CHAT_PUBLIC", "CHAT_MAFIA", "CHAT_COUPLE", "CHAT_DEAD", "DIRECT_MESSAGE"].includes(type)) return "chat";
    if (["VOTE_CAST", "PROS_CONS_VOTE"].includes(type)) return "vote";
    if (type === "DEATH") return "death";
    if (type === "ABILITY_USED") return "ability";
    return "system";
}

function logIcon(type) {
    if (logCategory(type) === "chat") return "◌";
    if (logCategory(type) === "vote") return "✓";
    if (logCategory(type) === "death") return "†";
    if (logCategory(type) === "ability") return "✦";
    if (type === "GAME_START") return "▶";
    if (type === "GAME_END") return "■";
    return "•";
}

function renderLog(log) {
    const card = element("article", `log-card ${logCategory(log.type)}`);
    const side = element("div", "log-side");
    side.append(element("span", "log-icon", logIcon(log.type)), element("time", "log-time", formatTime(log.timestampMillis)));
    const content = element("div", "log-content");
    const heading = element("div", "log-heading");
    heading.append(element("h3", "", log.title || "게임 기록"), element("span", "visibility-badge", visibilityLabels[log.visibility] || log.visibility));
    content.append(heading);

    const contextParts = [];
    if (log.actorName) contextParts.push(`${log.actorName}${log.actorJobName ? ` · ${log.actorJobName}` : ""}`);
    if (log.recipients?.length) contextParts.push(`대상: ${log.recipients.map(recipient => recipient.name).join(", ")}`);
    if (contextParts.length) content.append(element("div", "log-context", contextParts.join("  /  ")));
    if (log.body) content.append(element("p", "log-body", log.body));
    if (log.imageUrls?.length) {
        const images = element("div", "log-images");
        log.imageUrls.forEach(url => {
            const image = element("img");
            image.src = url;
            image.alt = log.title || "리플레이 첨부 이미지";
            image.loading = "lazy";
            image.referrerPolicy = "no-referrer";
            images.append(image);
        });
        content.append(images);
    }
    card.append(side, content);
    return card;
}

function phaseKey(log) { return `${log.dayCount}:${log.phase}`; }

function phaseTitle(log) {
    if (log.phase === "END") return "게임 종료";
    const day = log.dayCount > 0 ? `${log.dayCount}일차 ` : "";
    return `${day}${phaseLabels[log.phase] || log.phase}`;
}

function renderTimeline(container, logs, category, selectedPhase) {
    const visible = logs.filter(log => (category === "all" || logCategory(log.type) === category) && (selectedPhase === "all" || phaseKey(log) === selectedPhase));
    const groups = [];
    visible.forEach(log => {
        const key = phaseKey(log);
        const latest = groups[groups.length - 1];
        if (!latest || latest.key !== key) groups.push({ key, sample: log, logs: [log] });
        else latest.logs.push(log);
    });
    container.replaceChildren();
    groups.forEach(group => {
        const phaseClass = group.sample.phase === "NIGHT" ? "night" : group.sample.phase === "VOTE" ? "vote" : "day";
        const section = element("section", `phase-group ${phaseClass}`);
        const header = element("div", "phase-header");
        header.append(element("span", "phase-dot"), element("h2", "", phaseTitle(group.sample)), element("span", "", `${group.logs.length}개 기록`));
        section.append(header, ...group.logs.map(renderLog));
        container.append(section);
    });
    if (!visible.length) container.append(element("div", "no-results panel", "선택한 조건에 맞는 기록이 없습니다."));
}

function makeSummaryStat(label, value) {
    const stat = element("div", "summary-stat");
    stat.append(element("small", "", label), element("strong", "", value));
    return stat;
}

function renderReplay(replay) {
    document.title = `${replay.guildName || "게임"} · 리플레이`;
    const wrapper = element("div");
    const back = element("a", "back-link", "← 전체 리플레이로 돌아가기");
    back.href = "/";

    const summary = element("section", "replay-summary panel");
    summary.append(element("div", "summary-accent"));
    const summaryContent = element("div", "summary-content");
    const title = element("div", "replay-title");
    title.append(element("span", "eyebrow", "GAME REPLAY"), element("h1", "", replay.guildName || "알 수 없는 서버"));
    const titleMeta = element("div", "title-meta");
    titleMeta.append(element("span", "", formatDate(replay.replayStartedAtMillis)), element("span", "", `UUID ${replay.replayUuid}`));
    title.append(titleMeta);
    const winner = element("div", "winner");
    winner.append(element("small", "", "WINNING TEAM"), element("strong", "", resultName(replay)), element("span", "", replay.endReason || "게임 종료"));
    summaryContent.append(title, winner);
    summary.append(summaryContent);

    const logs = [...(replay.logs || [])].sort((a, b) => a.sequence - b.sequence);
    const endAt = logs.reduce((latest, log) => Math.max(latest, log.timestampMillis || 0), replay.generatedAtMillis || 0);
    const stats = element("div", "summary-stats");
    stats.append(
        makeSummaryStat("PLAYERS", `${replay.initialPlayerCount || replay.players?.length || 0}명`),
        makeSummaryStat("SURVIVORS", `${(replay.players || []).filter(player => !player.isDead).length}명`),
        makeSummaryStat("DURATION", formatDuration(endAt - replay.replayStartedAtMillis)),
        makeSummaryStat("RECORDS", `${logs.length}개`)
    );
    summary.append(stats);

    const playersPanel = element("section", "players-panel panel");
    const playerHead = element("div", "section-head");
    const playerTitle = element("div");
    playerTitle.append(element("span", "eyebrow", "FINAL ROSTER"), element("h2", "", "플레이어 최종 결과"));
    playerHead.append(playerTitle, element("p", "", "게임 종료 시점 기준"));
    const playersGrid = element("div", "players-grid");
    (replay.players || []).forEach((player, index) => {
        const card = element("div", `player-result ${player.isDead ? "dead" : "alive"}`);
        card.append(element("span", "player-number", String(index + 1)));
        const info = element("div", "player-info");
        info.append(element("strong", "", player.name || "알 수 없음"), element("small", "", `${player.jobName || "직업 미상"} · ${player.isDead ? "사망" : "생존"}`));
        card.append(info);
        playersGrid.append(card);
    });
    playersPanel.append(playerHead, playersGrid);

    const tools = element("section", "timeline-tools panel");
    const buttons = element("div", "filter-buttons");
    const categories = [["all", "전체"], ["system", "시스템"], ["chat", "대화/DM"], ["vote", "투표"], ["ability", "능력"], ["death", "사망"]];
    let activeCategory = "all";
    const timeline = element("section", "timeline");
    const phaseSelect = element("select", "phase-select");
    phaseSelect.setAttribute("aria-label", "진행 구간 선택");
    const allOption = element("option", "", "모든 진행 구간");
    allOption.value = "all";
    phaseSelect.append(allOption);
    const seenPhases = new Set();
    logs.forEach(log => {
        const key = phaseKey(log);
        if (seenPhases.has(key)) return;
        seenPhases.add(key);
        const option = element("option", "", phaseTitle(log));
        option.value = key;
        phaseSelect.append(option);
    });
    categories.forEach(([value, label]) => {
        const button = element("button", `filter-button${value === "all" ? " active" : ""}`, label);
        button.type = "button";
        button.addEventListener("click", () => {
            activeCategory = value;
            buttons.querySelectorAll("button").forEach(item => item.classList.toggle("active", item === button));
            renderTimeline(timeline, logs, activeCategory, phaseSelect.value);
        });
        buttons.append(button);
    });
    phaseSelect.addEventListener("change", () => renderTimeline(timeline, logs, activeCategory, phaseSelect.value));
    tools.append(buttons, phaseSelect);
    renderTimeline(timeline, logs, activeCategory, "all");

    wrapper.append(back, summary, playersPanel, tools, timeline);
    app.replaceChildren(wrapper);
}

async function start() {
    const match = location.pathname.match(/^\/history\/([^/]+)\/?$/);
    try {
        if (match) {
            const uuid = decodeURIComponent(match[1]).toLowerCase();
            if (!replayIdPattern.test(uuid)) throw new Error("UUID 형식이 올바르지 않습니다. 32자리 게임 UUID를 확인해 주세요.");
            renderReplay(await fetchJson(`/api/replays/${encodeURIComponent(uuid)}`));
        } else {
            renderArchive(await fetchJson("/api/replays"));
        }
    } catch (error) {
        showError(error.message);
    }
}

start();
