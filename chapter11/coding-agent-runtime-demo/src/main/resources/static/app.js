const state = {
    tasks: [],
    selectedTaskId: null,
    selectedTask: null,
    activeTab: 'timeline'
};

const el = {
    runtimeGrid: document.querySelector('#runtimeGrid'),
    taskList: document.querySelector('#taskList'),
    metrics: document.querySelector('#metrics'),
    emptyState: document.querySelector('#emptyState'),
    taskDetail: document.querySelector('#taskDetail'),
    createTaskButton: document.querySelector('#createTaskButton'),
    refreshButton: document.querySelector('#refreshButton'),
    approveButton: document.querySelector('#approveButton'),
    runButton: document.querySelector('#runButton'),
    loadSourceButton: document.querySelector('#loadSourceButton'),
    sourcePath: document.querySelector('#sourcePath'),
    sourceOutput: document.querySelector('#sourceOutput'),
    diffOutput: document.querySelector('#diffOutput'),
    steps: document.querySelector('#steps'),
    auditEvents: document.querySelector('#auditEvents'),
    toast: document.querySelector('#toast')
};

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: { 'Content-Type': 'application/json' },
        ...options
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.message || `HTTP ${response.status}`);
    }
    return data;
}

function showToast(message) {
    el.toast.textContent = message;
    el.toast.classList.add('show');
    window.setTimeout(() => el.toast.classList.remove('show'), 2600);
}

function statusText(status) {
    return {
        WAITING_APPROVAL: '待审批',
        APPROVED: '已审批',
        RUNNING: '运行中',
        COMPLETED: '已完成',
        FAILED: '失败'
    }[status] || status;
}

function formatTime(value) {
    if (!value) {
        return '-';
    }
    return new Intl.DateTimeFormat('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        month: '2-digit',
        day: '2-digit'
    }).format(new Date(value));
}

async function loadRuntime() {
    const runtime = await api('/api/coding-agent/runtime');
    el.runtimeGrid.innerHTML = `
        <dt>Provider</dt><dd>${runtime.provider}</dd>
        <dt>Model</dt><dd>${runtime.model}</dd>
        <dt>API Key</dt><dd>${runtime.apiKeyConfigured ? '已配置' : '未配置'}</dd>
        <dt>Tools</dt><dd>${runtime.tools.join(', ')}</dd>
    `;
}

async function loadTasks(keepSelection = true) {
    state.tasks = await api('/api/coding-agent/tasks');
    if (!keepSelection || !state.tasks.some(task => task.taskId === state.selectedTaskId)) {
        state.selectedTaskId = state.tasks[0]?.taskId || null;
    }
    renderMetrics();
    renderTaskList();
    if (state.selectedTaskId) {
        await selectTask(state.selectedTaskId);
    } else {
        renderEmptyState();
    }
}

function renderMetrics() {
    const total = state.tasks.length;
    const waiting = state.tasks.filter(task => task.status === 'WAITING_APPROVAL').length;
    const completed = state.tasks.filter(task => task.status === 'COMPLETED').length;
    const failed = state.tasks.filter(task => task.status === 'FAILED').length;
    const values = [total, waiting, completed, failed];
    el.metrics.querySelectorAll('.metric-value').forEach((node, index) => {
        node.textContent = values[index];
    });
}

function renderTaskList() {
    if (state.tasks.length === 0) {
        el.taskList.innerHTML = '<div class="task-item">暂无任务，先提交一个代码任务。</div>';
        return;
    }

    el.taskList.innerHTML = state.tasks.map(task => `
        <button class="task-item ${task.taskId === state.selectedTaskId ? 'active' : ''}" data-task-id="${task.taskId}">
            <div class="task-item-title">
                <span>${task.taskId}</span>
                <span class="small-badge status-${task.status}">${statusText(task.status)}</span>
            </div>
            <div class="task-item-desc">${task.repositoryName} / ${task.branchName}</div>
            <div class="task-item-desc">${task.description}</div>
            <div class="task-item-desc">更新：${formatTime(task.updatedAt)}</div>
        </button>
    `).join('');

    el.taskList.querySelectorAll('[data-task-id]').forEach(button => {
        button.addEventListener('click', () => selectTask(button.dataset.taskId));
    });
}

function renderEmptyState() {
    state.selectedTask = null;
    el.emptyState.classList.remove('hidden');
    el.taskDetail.classList.add('hidden');
}

async function selectTask(taskId) {
    state.selectedTaskId = taskId;
    state.selectedTask = await api(`/api/coding-agent/tasks/${taskId}`);
    renderTaskList();
    renderDetail();
}

function renderDetail() {
    const task = state.selectedTask;
    if (!task) {
        renderEmptyState();
        return;
    }

    el.emptyState.classList.add('hidden');
    el.taskDetail.classList.remove('hidden');
    document.querySelector('#detailTaskId').textContent = task.taskId;
    document.querySelector('#detailTitle').textContent = `${task.repositoryName} / ${task.branchName}`;
    document.querySelector('#detailDescription').textContent = task.description;
    document.querySelector('#detailStatus').textContent = statusText(task.status);
    document.querySelector('#detailStatus').className = `status-badge status-${task.status}`;
    document.querySelector('#detailRepo').textContent = `${task.repositoryName} · ${task.workspaceRoot}`;
    document.querySelector('#detailOperator').textContent = task.operator;

    el.approveButton.disabled = task.status !== 'WAITING_APPROVAL';
    el.runButton.disabled = task.status !== 'APPROVED';
    el.diffOutput.textContent = task.diff || '当前还没有 diff。审批并运行 Agent 后会生成交付差异。';

    renderSteps(task.steps);
    renderAudit(task.auditEvents);
    renderTabs();
}

function renderSteps(steps) {
    if (!steps || steps.length === 0) {
        el.steps.innerHTML = '<div class="step">还没有执行步骤。审批通过并运行 Agent 后会展示工具调用轨迹。</div>';
        return;
    }

    el.steps.innerHTML = steps.map(step => `
        <div class="step">
            <div class="step-title">
                <span>${step.index}. ${step.name}</span>
                <span class="small-badge ${step.success ? 'status-COMPLETED' : 'status-FAILED'}">${step.toolName}</span>
            </div>
            <div class="task-item-desc">${step.summary} · ${formatTime(step.startedAt)}</div>
            <pre class="step-output">${step.output || ''}</pre>
        </div>
    `).join('');
}

function renderAudit(events) {
    if (!events || events.length === 0) {
        el.auditEvents.innerHTML = '<li>暂无审计记录</li>';
        return;
    }
    el.auditEvents.innerHTML = events.map(event => `<li>${event}</li>`).join('');
}

function renderTabs() {
    document.querySelectorAll('.tab').forEach(tab => {
        tab.classList.toggle('active', tab.dataset.tab === state.activeTab);
    });
    document.querySelector('#timelinePanel').classList.toggle('hidden', state.activeTab !== 'timeline');
    document.querySelector('#diffPanel').classList.toggle('hidden', state.activeTab !== 'diff');
    document.querySelector('#sourcePanel').classList.toggle('hidden', state.activeTab !== 'source');
}

async function createTask() {
    const payload = {
        repositoryName: document.querySelector('#repositoryName').value,
        branchName: document.querySelector('#branchName').value,
        operator: document.querySelector('#operator').value,
        description: document.querySelector('#description').value
    };
    const task = await api('/api/coding-agent/tasks', {
        method: 'POST',
        body: JSON.stringify(payload)
    });
    state.selectedTaskId = task.taskId;
    showToast('任务已提交，等待审批');
    await loadTasks(true);
}

async function approveTask() {
    if (!state.selectedTaskId) {
        return;
    }
    await api(`/api/coding-agent/tasks/${state.selectedTaskId}/approve`, {
        method: 'POST',
        body: JSON.stringify({
            approver: 'tech-lead',
            comment: '确认需求明确，允许在隔离工作区写入代码并执行回归测试。'
        })
    });
    showToast('审批已通过');
    await loadTasks(true);
}

async function runTask() {
    if (!state.selectedTaskId) {
        return;
    }
    await api(`/api/coding-agent/tasks/${state.selectedTaskId}/run`, { method: 'POST' });
    state.activeTab = 'diff';
    showToast('Agent 执行完成，已生成 diff');
    await loadTasks(true);
}

async function loadSource() {
    if (!state.selectedTaskId) {
        return;
    }
    const file = await api(`/api/coding-agent/tasks/${state.selectedTaskId}/files?path=${encodeURIComponent(el.sourcePath.value)}`);
    el.sourceOutput.textContent = file.content;
}

function bindEvents() {
    el.createTaskButton.addEventListener('click', () => createTask().catch(error => showToast(error.message)));
    el.refreshButton.addEventListener('click', () => loadTasks(true).catch(error => showToast(error.message)));
    el.approveButton.addEventListener('click', () => approveTask().catch(error => showToast(error.message)));
    el.runButton.addEventListener('click', () => runTask().catch(error => showToast(error.message)));
    el.loadSourceButton.addEventListener('click', () => loadSource().catch(error => showToast(error.message)));
    document.querySelectorAll('.tab').forEach(tab => {
        tab.addEventListener('click', () => {
            state.activeTab = tab.dataset.tab;
            renderTabs();
            if (state.activeTab === 'source' && !el.sourceOutput.textContent.trim()) {
                loadSource().catch(error => showToast(error.message));
            }
        });
    });
}

async function init() {
    bindEvents();
    await loadRuntime();
    await loadTasks(false);
}

init().catch(error => showToast(error.message));
