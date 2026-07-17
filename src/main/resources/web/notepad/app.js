const state = {
    data: null,
    editing: false,
    saveTimers: new Map(),
    toastTimer: null,
};

const elements = {
    connection: document.querySelector("#connection"),
    loginPanel: document.querySelector("#login-panel"),
    loginMessage: document.querySelector("#login-message"),
    dashboard: document.querySelector("#dashboard"),
    guildName: document.querySelector("#guild-name"),
    dayCount: document.querySelector("#day-count"),
    phaseLabel: document.querySelector("#phase-label"),
    survivorCount: document.querySelector("#survivor-count"),
    refreshButton: document.querySelector("#refresh-button"),
    myAvatar: document.querySelector("#my-avatar"),
    myName: document.querySelector("#my-name"),
    myJobIcon: document.querySelector("#my-job-icon"),
    myJobFallback: document.querySelector("#my-job-fallback"),
    myJobName: document.querySelector("#my-job-name"),
    abilityCount: document.querySelector("#ability-count"),
    abilityGrid: document.querySelector("#ability-grid"),
    playerGrid: document.querySelector("#player-grid"),
    toast: document.querySelector("#toast"),
};

function setConnection(kind, label) {
    elements.connection.classList.remove("online", "offline");
    if (kind) elements.connection.classList.add(kind);
    elements.connection.lastElementChild.textContent = label;
}

function showLogin(message) {
    elements.dashboard.hidden = true;
    elements.loginPanel.hidden = false;
    if (message) elements.loginMessage.textContent = message;
}

function showDashboard() {
    elements.loginPanel.hidden = true;
    elements.dashboard.hidden = false;
}

async function fetchState({ forceRender = false } = {}) {
    try {
        const response = await fetch("/api/state", {
            headers: { Accept: "application/json" },
            cache: "no-store",
        });
        const payload = await response.json().catch(() => ({}));

        if (response.status === 401) {
            setConnection("offline", "인증 필요");
            showLogin(payload.error || "Discord에서 메모장 링크를 다시 받아 주세요.");
            return;
        }
        if (!response.ok) throw new Error(payload.error || "게임 정보를 가져오지 못했습니다.");

        state.data = payload;
        setConnection("online", "실시간 연결됨");
        showDashboard();
        if (!state.editing || forceRender) render(payload);
    } catch (error) {
        setConnection("offline", "연결 끊김");
        if (!state.data) showLogin("로컬 메모장 서버에 연결할 수 없습니다. 봇 실행 상태를 확인해 주세요.");
    }
}

function render(data) {
    renderGame(data.game);
    renderIdentity(data.me);
    renderPlayers(data.players, data.jobs);
}

function renderGame(game) {
    elements.guildName.textContent = game.guildName;
    elements.dayCount.textContent = `${game.dayCount}일차`;
    elements.phaseLabel.textContent = `${game.phaseLabel} · ${game.mode}`;
    elements.survivorCount.textContent = `${game.aliveCount} / ${game.playerCount}`;
}

function renderIdentity(me) {
    elements.myAvatar.src = me.avatarUrl;
    elements.myAvatar.alt = `${me.name} 프로필 이미지`;
    elements.myName.textContent = me.name;

    if (me.job) {
        elements.myJobName.textContent = me.job.name;
        elements.myJobIcon.hidden = !me.job.image;
        elements.myJobFallback.hidden = Boolean(me.job.image);
        if (me.job.image) {
            elements.myJobIcon.src = me.job.image;
            elements.myJobIcon.alt = `${me.job.name} 직업 아이콘`;
        }
    } else {
        elements.myJobName.textContent = "배정 대기 중";
        elements.myJobIcon.hidden = true;
        elements.myJobFallback.hidden = false;
    }

    elements.abilityCount.textContent = `${me.abilities.length}개`;
    elements.abilityGrid.replaceChildren();
    if (me.abilities.length === 0) {
        const empty = createElement("p", "empty-abilities", "표시할 능력이 없습니다.");
        elements.abilityGrid.append(empty);
        return;
    }

    for (const ability of me.abilities) {
        const card = createElement("div", "ability-card");
        card.title = ability.description;
        const icon = createElement("div", "ability-icon");
        const image = document.createElement("img");
        image.src = ability.image;
        image.alt = `${ability.name} 능력 아이콘`;
        icon.append(image);
        card.append(icon, createElement("span", "", ability.name));
        elements.abilityGrid.append(card);
    }
}

function renderPlayers(players, jobs) {
    elements.playerGrid.replaceChildren();
    for (const player of players) {
        elements.playerGrid.append(createPlayerCard(player, jobs));
    }
}

function createPlayerCard(player, jobs) {
    const card = createElement("article", "player-card");
    card.dataset.playerId = player.id;
    if (player.isDead) card.classList.add("dead");
    if (player.isSelf) card.classList.add("self");

    const header = createElement("div", "player-header");
    const avatarWrap = createElement("div", "player-avatar-wrap");
    const avatar = document.createElement("img");
    avatar.className = "player-avatar";
    avatar.src = player.avatarUrl;
    avatar.alt = `${player.name} 프로필 이미지`;
    avatarWrap.append(avatar, createElement("span", "life-dot"));

    const headingCopy = createElement("div", "player-heading-copy");
    headingCopy.append(
        createElement("strong", "player-name", player.name),
        createElement("span", "player-status", player.isDead ? "사망" : "생존")
    );
    header.append(avatarWrap, headingCopy);
    if (player.isSelf) header.append(createElement("span", "self-tag", "나"));
    else if (player.isJobPublic) header.append(createElement("span", "public-tag", "공개"));

    card.append(header, createRoleStatus(player));

    if (!player.isSelf && !player.isJobPublic) {
        card.append(createMemoArea(player, jobs));
    } else if (player.isJobPublic) {
        card.append(createElement("p", "revealed-note", "공식적으로 공개된 직업입니다. 개인 추리 입력이 잠겼습니다."));
    } else {
        card.append(createElement("p", "revealed-note", "내 정보는 상단 프로필에서 자세히 확인할 수 있습니다."));
    }

    return card;
}

function createRoleStatus(player) {
    const status = createElement("div", "role-status");
    const icon = createElement("div", player.job ? "public-role-icon" : "unknown-role-icon");
    if (player.job?.image) {
        const image = document.createElement("img");
        image.src = player.job.image;
        image.alt = `${player.job.name} 직업 아이콘`;
        icon.append(image);
    } else {
        icon.textContent = "?";
    }

    const copy = createElement("div", "role-status-copy");
    copy.append(
        createElement("span", "", player.isJobPublic ? "PUBLIC ROLE" : player.isSelf ? "MY ROLE" : "ROLE UNKNOWN"),
        createElement("strong", "", player.job?.name || "아직 공개되지 않음")
    );
    status.append(icon, copy);
    return status;
}

function createMemoArea(player, jobs) {
    const area = createElement("div", "memo-area");
    const labelRow = createElement("div", "memo-label-row");
    const label = document.createElement("label");
    const selectId = `guess-${player.id}`;
    label.htmlFor = selectId;
    label.textContent = "예상 직업";
    const saveStatus = createElement("span", "save-status");
    labelRow.append(label, saveStatus);

    const select = document.createElement("select");
    select.className = "job-select";
    select.id = selectId;
    select.setAttribute("aria-label", `${player.name} 예상 직업`);
    select.append(new Option("직업을 선택하세요", ""));
    for (const job of jobs) select.append(new Option(job.name, job.name));
    select.value = player.note.guessedJobName || "";

    const textarea = document.createElement("textarea");
    textarea.className = "note-input";
    textarea.maxLength = 1000;
    textarea.placeholder = "발언, 투표, 능력 사용 등 추리 근거를 기록하세요.";
    textarea.setAttribute("aria-label", `${player.name}에 대한 개인 메모`);
    textarea.value = player.note.content || "";

    const scheduleSave = () => {
        saveStatus.textContent = "저장 중…";
        window.clearTimeout(state.saveTimers.get(player.id));
        state.saveTimers.set(player.id, window.setTimeout(async () => {
            const saved = await saveNote(player.id, select.value || null, textarea.value);
            saveStatus.textContent = saved ? "저장됨" : "저장 실패";
        }, 450));
    };
    select.addEventListener("change", scheduleSave);
    textarea.addEventListener("input", scheduleSave);
    area.append(labelRow, select, textarea);
    return area;
}

async function saveNote(targetId, guessedJobName, content) {
    try {
        const response = await fetch("/api/note", {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json",
            },
            body: JSON.stringify({ targetId, guessedJobName, content }),
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(payload.error || "메모를 저장하지 못했습니다.");
        return true;
    } catch (error) {
        showToast(error.message);
        return false;
    }
}

function createElement(tagName, className = "", text = "") {
    const element = document.createElement(tagName);
    if (className) element.className = className;
    if (text) element.textContent = text;
    return element;
}

function showToast(message) {
    elements.toast.textContent = message;
    elements.toast.classList.add("visible");
    window.clearTimeout(state.toastTimer);
    state.toastTimer = window.setTimeout(() => elements.toast.classList.remove("visible"), 2600);
}

document.addEventListener("focusin", (event) => {
    if (event.target.matches("textarea, select, input")) state.editing = true;
});

document.addEventListener("focusout", (event) => {
    if (!event.target.matches("textarea, select, input")) return;
    window.setTimeout(() => {
        state.editing = Boolean(document.activeElement?.matches("textarea, select, input"));
    }, 0);
});

elements.refreshButton.addEventListener("click", async () => {
    elements.refreshButton.disabled = true;
    await fetchState({ forceRender: !state.editing });
    elements.refreshButton.disabled = false;
});

fetchState();
window.setInterval(fetchState, 1800);
