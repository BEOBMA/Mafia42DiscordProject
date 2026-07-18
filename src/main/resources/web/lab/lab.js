const SESSION_KEY = "mafia42-laboratory-session";

const state = {
    token: sessionStorage.getItem(SESSION_KEY),
    data: null,
    setupDraft: [],
    toastTimer: null,
};

const elements = {
    connection: document.querySelector("#connection"),
    loading: document.querySelector("#loading-panel"),
    setupPanel: document.querySelector("#setup-panel"),
    gamePanel: document.querySelector("#game-panel"),
    playerCount: document.querySelector("#player-count"),
    applyCount: document.querySelector("#apply-count"),
    setupGrid: document.querySelector("#setup-grid"),
    saveSetup: document.querySelector("#save-setup"),
    startGame: document.querySelector("#start-game"),
    dayLabel: document.querySelector("#day-label"),
    phaseLabel: document.querySelector("#phase-label"),
    aliveLabel: document.querySelector("#alive-label"),
    resetGame: document.querySelector("#reset-game"),
    advancePhase: document.querySelector("#advance-phase"),
    rosterGrid: document.querySelector("#roster-grid"),
    abilitySection: document.querySelector("#ability-section"),
    abilityControls: document.querySelector("#ability-controls"),
    voteSection: document.querySelector("#vote-section"),
    voteControls: document.querySelector("#vote-controls"),
    prosConsSection: document.querySelector("#pros-cons-section"),
    prosConsControls: document.querySelector("#pros-cons-controls"),
    defenseTargetLabel: document.querySelector("#defense-target-label"),
    decisionGrid: document.querySelector("#decision-grid"),
    labLog: document.querySelector("#lab-log"),
    toast: document.querySelector("#toast"),
};

function node(tag, className = "", text = "") {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (text) element.textContent = text;
    return element;
}

function option(label, value) {
    return new Option(label, value);
}

async function request(path, options = {}) {
    const headers = { Accept: "application/json", ...(options.headers || {}) };
    if (state.token) headers["X-Lab-Session"] = state.token;
    const response = await fetch(path, { cache: "no-store", ...options, headers });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
        const error = new Error(payload.error || payload.message || "요청을 처리하지 못했습니다.");
        error.status = response.status;
        throw error;
    }
    return payload;
}

async function createSession() {
    const payload = await request("/api/lab/session", { method: "POST" });
    state.token = payload.token;
    sessionStorage.setItem(SESSION_KEY, state.token);
    acceptState(payload.state, true);
}

async function loadState() {
    try {
        if (!state.token) return await createSession();
        acceptState(await request("/api/lab/state"), !state.data);
    } catch (error) {
        if (error.status === 401) {
            sessionStorage.removeItem(SESSION_KEY);
            state.token = null;
            return createSession();
        }
        elements.connection.textContent = "연결 실패";
        showToast(error.message);
    }
}

async function mutate(path, method = "POST", body = null) {
    try {
        const payload = await request(path, {
            method,
            headers: body === null ? {} : { "Content-Type": "application/json" },
            body: body === null ? undefined : JSON.stringify(body),
        });
        if (payload.state) acceptState(payload.state, payload.state.phase === "SETUP");
        if (payload.message) showToast(payload.message);
        return true;
    } catch (error) {
        if (error.status === 401) {
            state.token = null;
            sessionStorage.removeItem(SESSION_KEY);
            await createSession();
        }
        showToast(error.message);
        return false;
    }
}

function acceptState(data, resetDraft = false) {
    state.data = data;
    elements.loading.hidden = true;
    elements.connection.textContent = "독립 세션 연결됨";
    elements.connection.classList.add("online");
    if (data.phase === "SETUP") {
        elements.setupPanel.hidden = false;
        elements.gamePanel.hidden = true;
        if (resetDraft || state.setupDraft.length === 0) {
            state.setupDraft = data.players.map(player => ({ name: player.name, jobName: player.jobName }));
        }
        renderSetup();
    } else {
        elements.setupPanel.hidden = true;
        elements.gamePanel.hidden = false;
        renderGame();
    }
}

function renderSetup() {
    const jobs = state.data?.jobs || [];
    elements.playerCount.value = state.setupDraft.length;
    elements.setupGrid.replaceChildren();
    state.setupDraft.forEach((player, index) => {
        const card = node("article", "setup-card");
        const head = node("div", "setup-card-head");
        head.append(node("span", "slot-number", String(index + 1)), node("strong", "", index === 0 ? "플레이어" : `봇 ${index}`));
        head.append(node("span", "kind-tag", index === 0 ? "본인" : "BOT"));

        const nameLabel = node("label", "", "표시 이름");
        const nameInput = document.createElement("input");
        nameInput.maxLength = 24;
        nameInput.value = player.name;
        nameInput.addEventListener("input", () => { player.name = nameInput.value; });
        nameLabel.append(nameInput);

        const jobLabel = node("label", "", "직업");
        const jobSelect = document.createElement("select");
        jobSelect.append(option("직업을 선택하세요", ""));
        jobs.forEach(job => jobSelect.append(option(job.name, job.name)));
        jobSelect.value = player.jobName || "";
        jobSelect.addEventListener("change", () => { player.jobName = jobSelect.value || null; });
        jobLabel.append(jobSelect);
        card.append(head, nameLabel, jobLabel);
        elements.setupGrid.append(card);
    });
}

async function saveSetup() {
    return mutate("/api/lab/setup", "PUT", { players: state.setupDraft });
}

function renderGame() {
    const data = state.data;
    const living = data.players.filter(player => player.isAlive);
    elements.dayLabel.textContent = `${data.dayCount}일차`;
    elements.phaseLabel.textContent = data.phaseLabel;
    elements.aliveLabel.textContent = `${living.length} / ${data.players.length}`;
    elements.advancePhase.textContent = nextPhaseLabel(data.phase);
    renderRoster();
    renderAbilityControls();
    renderVoteControls();
    renderProsConsControls();
    renderDecisions();
    renderLog();
}

function nextPhaseLabel(phase) {
    return ({ NIGHT: "밤 결과 처리", DAY: "본투표 시작", MAIN_VOTE: "투표 집계", PROS_CONS: "찬반 집계" })[phase] || "다음 단계";
}

function renderRoster() {
    elements.rosterGrid.replaceChildren();
    for (const player of state.data.players) {
        const card = node("article", `roster-card${player.isAlive ? "" : " dead"}`);
        const head = node("div", "roster-head");
        const icon = node("div", "job-icon", player.jobImage ? "" : "?");
        if (player.jobImage) {
            const image = document.createElement("img");
            image.src = player.jobImage;
            image.alt = "";
            icon.append(image);
        }
        const copy = node("div", "roster-copy");
        copy.append(node("strong", "", player.name), node("span", "", `${player.jobName || "미배정"} · ${player.isHuman ? "본인" : "봇"}`));
        head.append(icon, copy);
        const button = node("button", "state-button", player.isAlive ? "사망 처리" : "생존 처리");
        button.type = "button";
        button.addEventListener("click", () => mutate("/api/lab/player-state", "PUT", { playerId: player.id, isAlive: !player.isAlive }));
        card.append(head, button);
        elements.rosterGrid.append(card);
    }
}

function renderAbilityControls() {
    const actors = state.data.players.filter(player => player.isAlive && player.abilities.length > 0);
    elements.abilitySection.hidden = actors.length === 0;
    elements.abilityControls.replaceChildren();
    if (!actors.length) return;
    for (const actor of actors) elements.abilityControls.append(createAbilityRow(actor));
}

function createAbilityRow(actor) {
    const existing = state.data.actions.find(action => action.actorId === actor.id);
    const row = node("div", "control-row");
    const playerLabel = node("div", "control-player");
    playerLabel.append(node("span", "mini-dot"), node("span", "", actor.name));
    const abilitySelect = document.createElement("select");
    abilitySelect.append(option("능력 선택", ""));
    actor.abilities.forEach(ability => abilitySelect.append(option(ability.name, ability.name)));
    abilitySelect.value = existing?.abilityName || "";
    const targetSelect = document.createElement("select");
    targetSelect.append(option("대상 선택", ""));
    state.data.players.forEach(player => targetSelect.append(option(`${player.name}${player.isAlive ? "" : " (사망)"}`, player.id)));
    targetSelect.value = existing?.targetId || "";
    const jobSelect = document.createElement("select");
    jobSelect.append(option("직업 선택", ""));
    state.data.jobs.forEach(job => jobSelect.append(option(job.name, job.name)));
    jobSelect.value = existing?.selectedJobName || "";
    const save = node("button", "primary-button", "행동 저장");

    const refreshFields = () => {
        const ability = actor.abilities.find(item => item.name === abilitySelect.value);
        targetSelect.hidden = !ability?.requiresTarget;
        jobSelect.hidden = !ability?.requiresJobSelection;
        save.disabled = !ability;
    };
    abilitySelect.addEventListener("change", refreshFields);
    save.addEventListener("click", () => mutate("/api/lab/action", "POST", {
        actorId: actor.id,
        abilityName: abilitySelect.value,
        targetId: targetSelect.hidden ? null : targetSelect.value || null,
        selectedJobName: jobSelect.hidden ? null : jobSelect.value || null,
    }));
    row.append(playerLabel, abilitySelect, targetSelect, jobSelect, save);
    refreshFields();
    return row;
}

function renderVoteControls() {
    const visible = state.data.phase === "MAIN_VOTE";
    elements.voteSection.hidden = !visible;
    elements.voteControls.replaceChildren();
    if (!visible) return;
    const living = state.data.players.filter(player => player.isAlive);
    for (const voter of living) {
        const row = node("div", "control-row vote-row");
        const label = node("div", "control-player");
        label.append(node("span", "mini-dot"), node("span", "", voter.name));
        const select = document.createElement("select");
        select.append(option("기권 / 미지정", ""));
        living.forEach(target => select.append(option(target.name, target.id)));
        select.value = state.data.votes[voter.id] || "";
        select.addEventListener("change", () => mutate("/api/lab/vote", "POST", { voterId: voter.id, targetId: select.value || null }));
        row.append(label, select);
        elements.voteControls.append(row);
    }
}

function renderProsConsControls() {
    const visible = state.data.phase === "PROS_CONS";
    elements.prosConsSection.hidden = !visible;
    elements.prosConsControls.replaceChildren();
    if (!visible) return;
    const target = state.data.players.find(player => player.id === state.data.defenseTargetId);
    elements.defenseTargetLabel.textContent = `${target?.name || "대상"}님의 처형 여부를 모든 플레이어별로 결정하세요.`;
    for (const voter of state.data.players.filter(player => player.isAlive)) {
        const row = node("div", "control-row pros-row");
        const label = node("div", "control-player");
        label.append(node("span", "mini-dot"), node("span", "", voter.name));
        const buttons = node("div", "pros-buttons");
        const current = state.data.prosConsVotes[voter.id];
        const pros = node("button", `pros-button${current === true ? " active" : ""}`, "찬성");
        const cons = node("button", `cons-button${current === false ? " active" : ""}`, "반대");
        pros.addEventListener("click", () => mutate("/api/lab/pros-cons", "POST", { voterId: voter.id, isPros: true }));
        cons.addEventListener("click", () => mutate("/api/lab/pros-cons", "POST", { voterId: voter.id, isPros: false }));
        buttons.append(pros, cons);
        row.append(label, buttons);
        elements.prosConsControls.append(row);
    }
}

function renderDecisions() {
    elements.decisionGrid.replaceChildren();
    const cards = [];
    state.data.actions.forEach(action => cards.push([action.actorName, `${action.abilityName}${action.targetName ? ` → ${action.targetName}` : ""}${action.selectedJobName ? ` [${action.selectedJobName}]` : ""}`]));
    Object.entries(state.data.votes).forEach(([voterId, targetId]) => {
        const voter = playerName(voterId);
        cards.push([voter, `본투표 → ${targetId ? playerName(targetId) : "기권"}`]);
    });
    Object.entries(state.data.prosConsVotes).forEach(([voterId, isPros]) => cards.push([playerName(voterId), isPros ? "찬성" : "반대"]));
    if (!cards.length) {
        elements.decisionGrid.append(node("div", "empty", "아직 저장된 결정이 없습니다."));
        return;
    }
    cards.forEach(([title, body]) => {
        const card = node("div", "decision-card");
        card.append(node("strong", "", title), document.createTextNode(body));
        elements.decisionGrid.append(card);
    });
}

function renderLog() {
    elements.labLog.replaceChildren();
    if (!state.data.events.length) {
        elements.labLog.append(node("div", "empty", "게임을 시작하면 실험 기록이 쌓입니다."));
        return;
    }
    state.data.events.slice().reverse().forEach(event => {
        const entry = node("article", "log-entry");
        const meta = node("div", "log-meta", `${event.dayCount}일차 · ${event.phaseLabel}`);
        const copy = node("div", "log-copy");
        copy.append(node("strong", "", event.title), node("p", "", event.body));
        entry.append(meta, copy);
        elements.labLog.append(entry);
    });
}

function playerName(id) {
    return state.data.players.find(player => player.id === id)?.name || id;
}

function showToast(message) {
    elements.toast.textContent = message || "처리했습니다.";
    elements.toast.classList.add("visible");
    clearTimeout(state.toastTimer);
    state.toastTimer = setTimeout(() => elements.toast.classList.remove("visible"), 3000);
}

elements.applyCount.addEventListener("click", () => {
    const count = Math.max(4, Math.min(16, Number(elements.playerCount.value) || 6));
    const next = state.setupDraft.slice(0, count);
    while (next.length < count) {
        const index = next.length;
        next.push({ name: index === 0 ? "나" : `봇 ${index}`, jobName: null });
    }
    state.setupDraft = next;
    renderSetup();
});
elements.saveSetup.addEventListener("click", saveSetup);
elements.startGame.addEventListener("click", async () => {
    if (await saveSetup()) await mutate("/api/lab/start");
});
elements.advancePhase.addEventListener("click", () => mutate("/api/lab/advance"));
elements.resetGame.addEventListener("click", async () => {
    if (window.confirm("현재 실험 기록과 진행 상태를 초기화하고 설정 화면으로 돌아갈까요?")) {
        await mutate("/api/lab/reset");
    }
});

loadState();
