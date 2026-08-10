const state = {
    data: null,
    editing: false,
    activeTab: "profile",
    actionAbility: null,
    dialogAction: null,
    saveTimers: new Map(),
    toastTimer: null,
    lastEventSequence: 0,
    events: [],
    eventsInitialized: false,
    unreadEvents: 0,
    openJobPickerId: null,
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
    generalNoteInput: document.querySelector("#general-note-input"),
    generalNoteSaveStatus: document.querySelector("#general-note-save-status"),
    tabs: [...document.querySelectorAll(".workspace-tab")],
    panels: [...document.querySelectorAll(".tab-panel")],
    logUnread: document.querySelector("#log-unread"),
    eventList: document.querySelector("#event-list"),
    actionPanel: document.querySelector("#action-panel"),
    actionTitle: document.querySelector("#action-title"),
    actionHelp: document.querySelector("#action-help"),
    actionPlayerGrid: document.querySelector("#action-player-grid"),
    cancelAction: document.querySelector("#cancel-action"),
    dialog: document.querySelector("#ability-dialog"),
    dialogTitle: document.querySelector("#ability-dialog-title"),
    dialogSummary: document.querySelector("#ability-dialog-summary"),
    jobChoiceField: document.querySelector("#job-choice-field"),
    jobSelect: document.querySelector("#ability-job-select"),
    abilityConfirm: document.querySelector("#ability-confirm"),
    toast: document.querySelector("#toast"),
};

function createElement(tagName, className = "", text = "") {
    const node = document.createElement(tagName);
    if (className) node.className = className;
    if (text) node.textContent = text;
    return node;
}

function setAvatarImage(image, source) {
    image.alt = "";
    image.classList.remove("avatar-missing");
    image.onload = () => image.classList.remove("avatar-missing");
    image.onerror = () => {
        image.onload = null;
        image.onerror = null;
        image.removeAttribute("src");
        image.classList.add("avatar-missing");
    };
    if (source) image.src = source;
    else image.classList.add("avatar-missing");
}

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

async function fetchJson(url, options = {}) {
    const response = await fetch(url, { cache: "no-store", ...options });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
        const error = new Error(payload.error || payload.message || "요청을 처리하지 못했습니다.");
        error.status = response.status;
        throw error;
    }
    return payload;
}

async function fetchState({ forceRender = false } = {}) {
    try {
        const payload = await fetchJson("/api/state", { headers: { Accept: "application/json" } });
        state.data = payload;
        setConnection("online", "실시간 연결됨");
        showDashboard();
        if ((!state.editing && !state.actionAbility && !state.dialogAction) || forceRender) render(payload);
        await fetchEvents();
    } catch (error) {
        if (error.status === 401) {
            setConnection("offline", "인증 필요");
            showLogin(error.message || "Discord에서 메모장 링크를 다시 받아 주세요.");
            return;
        }
        setConnection("offline", "연결 끊김");
        if (!state.data) showLogin("메모장 서버에 연결할 수 없습니다. 봇 실행 상태를 확인해 주세요.");
    }
}

async function fetchEvents() {
    try {
        const payload = await fetchJson(`/api/events?after=${state.lastEventSequence}`, {
            headers: { Accept: "application/json" },
        });
        const incoming = payload.events || [];
        state.lastEventSequence = Math.max(state.lastEventSequence, payload.lastSequence || 0);
        if (incoming.length) {
            state.events.push(...incoming);
            if (state.events.length > 500) state.events.splice(0, state.events.length - 500);
            if (state.eventsInitialized && state.activeTab !== "log") {
                state.unreadEvents += incoming.length;
                updateUnread();
            }
            renderEvents();
        } else if (!state.eventsInitialized) {
            renderEvents();
        }
        state.eventsInitialized = true;
    } catch (error) {
        if (error.status !== 401) console.warn("게임 로그 갱신 실패", error);
    }
}

function render(data) {
    renderGame(data.game);
    renderIdentity(data.me);
    renderPlayers(data.players, data.jobs);
    renderGeneralNote(data.generalNote);
    activateTab(state.activeTab);
}

function renderGame(game) {
    elements.guildName.textContent = game.guildName;
    elements.dayCount.textContent = `${game.dayCount}일차`;
    elements.phaseLabel.textContent = `${game.phaseLabel} · ${game.mode}`;
    elements.survivorCount.textContent = `${game.aliveCount} / ${game.playerCount}`;
}

function renderIdentity(me) {
    setAvatarImage(elements.myAvatar, me.avatarUrl);
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

    const actionByName = new Map((me.actionAbilities || []).map(ability => [ability.name, ability]));
    elements.abilityCount.textContent = `${me.abilities.length}개`;
    elements.abilityGrid.replaceChildren();
    if (me.abilities.length === 0) {
        elements.abilityGrid.append(createElement("p", "empty-abilities", "표시할 능력이 없습니다."));
        return;
    }

    for (const ability of me.abilities) {
        const action = actionByName.get(ability.name);
        const card = createElement(action ? "button" : "div", `ability-card${action ? " actionable" : ""}`);
        card.title = action ? `${ability.description} — 클릭하여 사용` : ability.description;
        if (action) {
            card.type = "button";
            card.addEventListener("click", () => selectAbility(action));
        }
        const icon = createElement("div", "ability-icon");
        const image = document.createElement("img");
        image.src = ability.image;
        image.alt = `${ability.name} 능력 아이콘`;
        icon.append(image);
        card.append(icon, createElement("span", "", ability.name));
        elements.abilityGrid.append(card);
    }
}

function selectAbility(ability) {
    state.actionAbility = ability;
    if (!ability.requiresTarget) {
        openAbilityDialog(ability, null);
        return;
    }
    elements.actionPanel.hidden = false;
    elements.actionTitle.textContent = `${ability.name} 대상 선택`;
    elements.actionHelp.textContent = ability.requiresJobSelection
        ? "플레이어를 선택한 다음 예상 직업을 선택합니다."
        : "능력을 사용할 플레이어의 초상화를 선택하세요.";
    renderActionTargets();
    elements.actionPanel.scrollIntoView({ behavior: "smooth", block: "start" });
}

function renderActionTargets() {
    elements.actionPlayerGrid.replaceChildren();
    for (const player of state.data?.players || []) {
        const button = createElement("button", `target-player${player.isDead ? " dead" : ""}`);
        button.type = "button";
        button.dataset.playerId = player.id;
        const jobIcon = createElement("div", "target-player-job-icon");
        updatePlayerJobIcon(jobIcon, player, state.data?.jobs || []);
        button.append(jobIcon, createElement("span", "", `${player.name}${player.isDead ? " · 사망" : ""}`));
        button.addEventListener("click", () => openAbilityDialog(state.actionAbility, player));
        elements.actionPlayerGrid.append(button);
    }
}

function cancelAbilitySelection() {
    state.actionAbility = null;
    state.dialogAction = null;
    elements.actionPanel.hidden = true;
    closeAbilityDialog();
}

function openAbilityDialog(ability, target) {
    state.dialogAction = { ability, target };
    elements.dialogTitle.textContent = ability.name;
    elements.dialogSummary.textContent = target
        ? `${target.name}님을 대상으로 ${ability.name} 능력을 사용합니다.`
        : `${ability.name} 능력을 사용합니다.`;
    elements.jobChoiceField.hidden = !ability.requiresJobSelection;
    elements.jobSelect.replaceChildren(new Option("직업을 선택하세요", ""));
    for (const jobName of ability.selectableJobNames || []) {
        elements.jobSelect.append(new Option(jobName, jobName));
    }
    elements.dialog.hidden = false;
    document.body.style.overflow = "hidden";
    if (ability.requiresJobSelection) elements.jobSelect.focus();
    else elements.abilityConfirm.focus();
}

function closeAbilityDialog() {
    state.dialogAction = null;
    elements.dialog.hidden = true;
    document.body.style.overflow = "";
}

async function confirmAbility() {
    const action = state.dialogAction;
    if (!action) return;
    const selectedJobName = action.ability.requiresJobSelection ? elements.jobSelect.value : null;
    if (action.ability.requiresJobSelection && !selectedJobName) {
        showToast("직업을 선택해 주세요.");
        elements.jobSelect.focus();
        return;
    }
    elements.abilityConfirm.disabled = true;
    try {
        const result = await fetchJson("/api/ability", {
            method: "POST",
            headers: { "Content-Type": "application/json", Accept: "application/json" },
            body: JSON.stringify({
                abilityName: action.ability.name,
                targetId: action.target?.id || null,
                selectedJobName,
            }),
        });
        showToast(result.message || "능력을 사용했습니다.");
        cancelAbilitySelection();
        await fetchState({ forceRender: true });
    } catch (error) {
        showToast(error.message);
    } finally {
        elements.abilityConfirm.disabled = false;
    }
}

function renderPlayers(players, jobs) {
    elements.playerGrid.replaceChildren();
    for (const player of players) elements.playerGrid.append(createPlayerCard(player, jobs));
}

function renderGeneralNote(content) {
    elements.generalNoteInput.value = content || "";
}

function displayedPlayerJob(player, jobs) {
    const displayedJobName = player.job?.name || player.note?.guessedJobName;
    if (!displayedJobName) return null;
    return jobs.find(job => job.name === displayedJobName) || player.job || null;
}

function updatePlayerJobIcon(container, player, jobs) {
    const job = displayedPlayerJob(player, jobs);
    container.replaceChildren();
    container.classList.toggle("unknown", !job?.image);
    container.title = job ? `${job.name} 직업 아이콘` : "직업 미확인";
    if (!job?.image) {
        container.textContent = "?";
        return;
    }

    const image = document.createElement("img");
    image.src = job.image;
    image.alt = `${job.name} 직업 아이콘`;
    image.addEventListener("error", () => {
        container.replaceChildren();
        container.classList.add("unknown");
        container.textContent = "?";
    }, { once: true });
    container.append(image);
}

function createPlayerCard(player, jobs) {
    const card = createElement("article", "player-card");
    card.dataset.playerId = player.id;
    if (player.isDead) card.classList.add("dead");
    if (player.isSelf) card.classList.add("self");

    const header = createElement("div", "player-header");
    const avatarWrap = createElement("div", "player-avatar-wrap");
    const jobIcon = createElement("div", "player-avatar player-job-icon");
    updatePlayerJobIcon(jobIcon, player, jobs);
    avatarWrap.append(jobIcon, createElement("span", "life-dot"));
    const headingCopy = createElement("div", "player-heading-copy");
    headingCopy.append(createElement("strong", "player-name", player.name), createElement("span", "player-status", player.isDead ? "사망" : "생존"));
    header.append(avatarWrap, headingCopy);
    if (player.isSelf) header.append(createElement("span", "self-tag", "나"));
    else if (player.isJobPublic) header.append(createElement("span", "public-tag", "공개"));
    card.append(header, createRoleStatus(player));

    if (!player.isSelf && !player.isJobPublic) {
        card.append(createMemoArea(player, jobs, () => updatePlayerJobIcon(jobIcon, player, jobs)));
    }
    else if (player.isJobPublic) card.append(createElement("p", "revealed-note", "공식적으로 공개된 직업입니다. 개인 추리 입력이 잠겼습니다."));
    else card.append(createElement("p", "revealed-note", "내 정보는 프로필 탭에서 자세히 확인할 수 있습니다."));
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
    } else icon.textContent = "?";
    const copy = createElement("div", "role-status-copy");
    copy.append(createElement("span", "", player.isJobPublic ? "PUBLIC ROLE" : player.isSelf ? "MY ROLE" : "ROLE UNKNOWN"), createElement("strong", "", player.job?.name || "아직 공개되지 않음"));
    status.append(icon, copy);
    return status;
}

function createMemoArea(player, jobs, onJobChanged) {
    const area = createElement("div", "memo-area");
    const labelRow = createElement("div", "memo-label-row");
    const label = createElement("span", "memo-label", "예상 직업");
    const saveStatus = createElement("span", "save-status");
    labelRow.append(label, saveStatus);

    let selectedJobName = player.note.guessedJobName || "";
    const selectedJob = () => jobs.find(job => job.name === selectedJobName);
    const trigger = createElement("button", "memo-job-trigger");
    trigger.type = "button";
    trigger.setAttribute("aria-expanded", String(state.openJobPickerId === player.id));
    trigger.setAttribute("aria-controls", `job-picker-${player.id}`);

    const triggerIcon = createElement("span", "memo-selected-icon", selectedJobName ? "" : "?");
    const triggerImage = document.createElement("img");
    triggerImage.alt = "";
    const triggerName = createElement("strong", "memo-selected-name");
    const triggerHint = createElement("span", "memo-trigger-hint", "클릭하여 직업 아이콘 선택");
    trigger.append(triggerIcon, createElement("span", "memo-trigger-copy"));
    trigger.lastElementChild.append(triggerName, triggerHint);

    const updateTrigger = () => {
        const job = selectedJob();
        triggerName.textContent = job?.name || "직업 미선택";
        triggerIcon.textContent = job ? "" : "?";
        triggerImage.remove();
        if (job) {
            triggerImage.src = job.image;
            triggerImage.alt = `${job.name} 직업 아이콘`;
            triggerIcon.append(triggerImage);
        }
    };
    updateTrigger();

    const picker = createElement("div", "job-icon-picker");
    picker.id = `job-picker-${player.id}`;
    picker.hidden = state.openJobPickerId !== player.id;
    picker.setAttribute("aria-label", `${player.name} 예상 직업 선택`);

    const buttons = [];
    const selectJob = jobName => {
        selectedJobName = selectedJobName === jobName ? "" : jobName;
        player.note.guessedJobName = selectedJobName || null;
        for (const button of buttons) {
            const selected = button.dataset.jobName === selectedJobName;
            button.classList.toggle("selected", selected);
            button.setAttribute("aria-pressed", String(selected));
        }
        updateTrigger();
        onJobChanged?.();
        state.openJobPickerId = null;
        picker.hidden = true;
        trigger.setAttribute("aria-expanded", "false");
        scheduleSave();
        trigger.focus();
    };

    const jobsByRow = new Map();
    for (const job of jobs) {
        const rowNumber = job.memoRow || 4;
        if (!jobsByRow.has(rowNumber)) jobsByRow.set(rowNumber, []);
        jobsByRow.get(rowNumber).push(job);
    }
    for (const [, rowJobs] of [...jobsByRow.entries()].sort(([left], [right]) => left - right)) {
        const row = createElement("div", "job-icon-row");
        for (const job of rowJobs.slice(0, 6)) {
            const button = createElement("button", "job-icon-button");
            button.type = "button";
            button.dataset.jobName = job.name;
            button.title = job.name;
            button.setAttribute("aria-label", job.name);
            button.setAttribute("aria-pressed", String(job.name === selectedJobName));
            button.classList.toggle("selected", job.name === selectedJobName);
            const image = document.createElement("img");
            image.src = job.image;
            image.alt = "";
            image.loading = "lazy";
            image.addEventListener("error", () => button.classList.add("image-error"), { once: true });
            button.append(image, createElement("span", "job-icon-name", job.name));
            button.addEventListener("click", () => selectJob(job.name));
            buttons.push(button);
            row.append(button);
        }
        picker.append(row);
    }

    trigger.addEventListener("click", () => {
        const willOpen = picker.hidden;
        document.querySelectorAll(".job-icon-picker").forEach(node => { node.hidden = true; });
        document.querySelectorAll(".memo-job-trigger[aria-expanded='true']")
            .forEach(node => node.setAttribute("aria-expanded", "false"));
        picker.hidden = !willOpen;
        trigger.setAttribute("aria-expanded", String(willOpen));
        state.openJobPickerId = willOpen ? player.id : null;
    });

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
            const saved = await saveNote(player.id, selectedJobName || null, textarea.value);
            saveStatus.textContent = saved ? "저장됨" : "저장 실패";
        }, 450));
    };
    textarea.addEventListener("input", scheduleSave);
    area.append(labelRow, trigger, picker, textarea);
    return area;
}

async function saveNote(targetId, guessedJobName, content) {
    try {
        await fetchJson("/api/note", {
            method: "PUT",
            headers: { "Content-Type": "application/json", Accept: "application/json" },
            body: JSON.stringify({ targetId, guessedJobName, content }),
        });
        return true;
    } catch (error) {
        showToast(error.message);
        return false;
    }
}

async function saveGeneralNote(content) {
    try {
        await fetchJson("/api/general-note", {
            method: "PUT",
            headers: { "Content-Type": "application/json", Accept: "application/json" },
            body: JSON.stringify({ content }),
        });
        return true;
    } catch (error) {
        showToast(error.message);
        return false;
    }
}

function activateTab(tabName) {
    state.activeTab = tabName;
    for (const tab of elements.tabs) {
        const active = tab.dataset.tab === tabName;
        tab.classList.toggle("active", active);
        tab.setAttribute("aria-selected", String(active));
    }
    for (const panel of elements.panels) {
        const active = panel.dataset.panel === tabName;
        panel.hidden = !active;
        panel.classList.toggle("active", active);
    }
    if (tabName === "log") {
        state.unreadEvents = 0;
        updateUnread();
        renderEvents();
    }
}

function updateUnread() {
    elements.logUnread.hidden = state.unreadEvents === 0;
    elements.logUnread.textContent = String(Math.min(state.unreadEvents, 99));
}

function renderEvents() {
    elements.eventList.replaceChildren();
    if (!state.events.length) {
        elements.eventList.append(createElement("div", "event-empty", "아직 표시할 시스템 메시지가 없습니다."));
        return;
    }
    for (const event of state.events) {
        const card = createElement("article", `game-event ${event.type}`);
        const time = new Intl.DateTimeFormat("ko-KR", { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false }).format(new Date(event.timestampMillis));
        const meta = createElement("div", "event-meta", time);
        const copy = createElement("div", "event-copy");
        copy.append(createElement("h3", "", event.title || "시스템 메시지"));
        if (event.actorName) copy.append(createElement("p", "event-context", event.actorName));
        if (event.body) copy.append(createElement("p", "event-body", event.body));
        if (event.imageUrls?.length) {
            const images = createElement("div", "event-images");
            for (const url of event.imageUrls) {
                const image = document.createElement("img");
                image.src = url;
                image.alt = event.title || "시스템 메시지 이미지";
                image.loading = "lazy";
                image.referrerPolicy = "no-referrer";
                images.append(image);
            }
            copy.append(images);
        }
        card.append(meta, copy);
        elements.eventList.append(card);
    }
    if (state.activeTab === "log") elements.eventList.lastElementChild?.scrollIntoView({ block: "nearest" });
}

function showToast(message) {
    elements.toast.textContent = message;
    elements.toast.classList.add("visible");
    window.clearTimeout(state.toastTimer);
    state.toastTimer = window.setTimeout(() => elements.toast.classList.remove("visible"), 3200);
}

document.addEventListener("focusin", event => {
    if (event.target.matches("textarea, select, input")) state.editing = true;
});

document.addEventListener("focusout", event => {
    if (!event.target.matches("textarea, select, input")) return;
    window.setTimeout(() => {
        state.editing = Boolean(document.activeElement?.matches("textarea, select, input"));
    }, 0);
});

elements.tabs.forEach(tab => tab.addEventListener("click", () => activateTab(tab.dataset.tab)));
elements.cancelAction.addEventListener("click", cancelAbilitySelection);
document.querySelectorAll("[data-close-dialog]").forEach(button => button.addEventListener("click", closeAbilityDialog));
elements.abilityConfirm.addEventListener("click", confirmAbility);
document.addEventListener("keydown", event => {
    if (event.key === "Escape" && !elements.dialog.hidden) closeAbilityDialog();
});
elements.refreshButton.addEventListener("click", async () => {
    elements.refreshButton.disabled = true;
    await fetchState({ forceRender: !state.editing && !state.dialogAction });
    elements.refreshButton.disabled = false;
});

elements.generalNoteInput.addEventListener("input", () => {
    elements.generalNoteSaveStatus.textContent = "저장 중…";
    window.clearTimeout(state.saveTimers.get("general-note"));
    state.saveTimers.set("general-note", window.setTimeout(async () => {
        const saved = await saveGeneralNote(elements.generalNoteInput.value);
        elements.generalNoteSaveStatus.textContent = saved ? "저장됨" : "저장 실패";
    }, 450));
});

fetchState();
window.setInterval(fetchState, 1800);
