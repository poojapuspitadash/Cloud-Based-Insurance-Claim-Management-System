'use strict';
const $ = (id) => document.getElementById(id);
let auth = null;   // base64("email:password") kept only for this browser tab
let me = null;

try { auth = sessionStorage.getItem('auth'); } catch (e) { /* storage may be blocked */ }

// Escape every piece of user-supplied text before putting it in HTML (prevents XSS).
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const inr = (n) => '₹' + Number(n).toLocaleString('en-IN');
const STATUS_BADGE = {SUBMITTED:'secondary', UNDER_REVIEW:'info', APPROVED:'success', REJECTED:'danger'};

function alertMsg(msg, type = 'danger') {
  $('alertBox').innerHTML = `<div class="alert alert-${type} alert-dismissible"><span>${esc(msg)}</span><button class="btn-close" data-bs-dismiss="alert" onclick="this.parentElement.remove()"></button></div>`;
  window.scrollTo(0, 0);
}

async function api(path, opts = {}) {
  const headers = Object.assign({}, opts.headers || {});
  if (auth) headers['Authorization'] = 'Basic ' + auth;
  if (opts.body && !(opts.body instanceof FormData)) headers['Content-Type'] = 'application/json';
  const res = await fetch(path, Object.assign({}, opts, { headers }));
  if (res.status === 401) { logout(); throw new Error('Invalid credentials or session expired'); }
  if (!res.ok) {
    let msg = res.statusText;
    try { const j = await res.json(); msg = j.error + (j.fields ? ': ' + Object.values(j.fields).join(', ') : ''); } catch (e) {}
    throw new Error(msg);
  }
  const ct = res.headers.get('content-type') || '';
  return ct.includes('json') ? res.json() : res;
}

// ---------- auth ----------
document.querySelectorAll('[data-tab]').forEach(b => b.onclick = () => {
  document.querySelectorAll('[data-tab]').forEach(x => x.classList.toggle('active', x === b));
  $('loginForm').classList.toggle('d-none', b.dataset.tab !== 'login');
  $('registerForm').classList.toggle('d-none', b.dataset.tab !== 'register');
});

$('loginForm').onsubmit = async (e) => {
  e.preventDefault();
  auth = btoa($('loginEmail').value.trim().toLowerCase() + ':' + $('loginPassword').value);
  try { await start(); try { sessionStorage.setItem('auth', auth); } catch (x) {} }
  catch (err) { alertMsg(err.message); }
};

$('registerForm').onsubmit = async (e) => {
  e.preventDefault();
  try {
    await api('/api/auth/register', { method: 'POST', body: JSON.stringify({
      name: $('regName').value, email: $('regEmail').value, password: $('regPassword').value, policyNumber: $('regPolicy').value }) });
    alertMsg('Account created. Please log in.', 'success');
    document.querySelector('[data-tab="login"]').click();
  } catch (err) { alertMsg(err.message); }
};

function logout() {
  auth = null; me = null;
  try { sessionStorage.removeItem('auth'); } catch (e) {}
  ['customerView', 'employeeView', 'navUser'].forEach(id => $(id).classList.add('d-none'));
  $('authView').classList.remove('d-none');
}
$('logoutBtn').onclick = logout;

async function start() {
  me = await api('/api/auth/me');
  $('authView').classList.add('d-none');
  $('navUser').classList.remove('d-none');
  $('whoami').textContent = `${me.name} (${me.role})`;
  $('alertBox').innerHTML = '';
  if (me.role === 'EMPLOYEE') { $('employeeView').classList.remove('d-none'); loadEmployee(); }
  else { $('customerView').classList.remove('d-none'); loadMine(); }
}

// ---------- rendering ----------
function docLinks(c) {
  if (!c.documents.length) return '<span class="text-muted">No documents</span>';
  return c.documents.map(d => `<a href="#" data-claim="${c.id}" data-doc="${d.id}" data-name="${esc(d.fileName)}" class="doc-link me-2">&#128196; ${esc(d.fileName)}</a>`).join('');
}

function claimCard(c, employee) {
  const actions = employee ? employeeActions(c) : '';
  return `<div class="card claim-card ${esc(c.priority)} mb-3"><div class="card-body">
    <div class="d-flex justify-content-between">
      <h6 class="mb-1">${esc(c.claimNumber)} &middot; ${esc(c.type)}</h6>
      <span class="badge text-bg-${STATUS_BADGE[c.status]}">${esc(c.status.replace('_', ' '))}</span>
    </div>
    <div class="small text-muted mb-2">${employee ? esc(c.customerName) + ' (' + esc(c.policyNumber) + ') &middot; ' : ''}Incident ${esc(c.incidentDate)} &middot; ${inr(c.amount)}${employee ? ` &middot; Priority <strong>${esc(c.priority)}</strong> (risk ${esc(c.riskScore)}%)` : ''}</div>
    <p class="mb-2">${esc(c.description)}</p>
    <div class="mb-2">${docLinks(c)}</div>
    ${c.reviewerNote ? `<div class="alert alert-light py-1 mb-2"><strong>Reviewer note:</strong> ${esc(c.reviewerNote)}</div>` : ''}
    ${actions}
  </div></div>`;
}

function employeeActions(c) {
  const next = { SUBMITTED: ['UNDER_REVIEW', 'REJECTED'], UNDER_REVIEW: ['APPROVED', 'REJECTED'] }[c.status];
  if (!next) return '';
  return `<div class="input-group input-group-sm">
    <input class="form-control" placeholder="Reviewer note (optional)" id="note-${c.id}" maxlength="500">
    ${next.map(s => `<button class="btn btn-outline-${s === 'REJECTED' ? 'danger' : 'success'} status-btn" data-id="${c.id}" data-status="${s}">${s.replace('_', ' ')}</button>`).join('')}
  </div>`;
}

document.addEventListener('click', async (e) => {
  const link = e.target.closest('.doc-link');
  if (link) {
    e.preventDefault();
    try {
      const res = await api(`/api/claims/${link.dataset.claim}/documents/${link.dataset.doc}`);
      const url = URL.createObjectURL(await res.blob());
      window.open(url, '_blank');
    } catch (err) { alertMsg(err.message); }
  }
  const btn = e.target.closest('.status-btn');
  if (btn) {
    try {
      await api(`/api/employee/claims/${btn.dataset.id}/status`, { method: 'PATCH',
        body: JSON.stringify({ status: btn.dataset.status, note: $('note-' + btn.dataset.id).value || null }) });
      loadEmployee();
    } catch (err) { alertMsg(err.message); }
  }
});

// ---------- customer ----------
async function loadMine() {
  const list = await api('/api/claims');
  $('myClaims').innerHTML = list.length ? list.map(c => claimCard(c, false)).join('') : '<p class="text-muted">No claims yet.</p>';
}

$('claimForm').onsubmit = async (e) => {
  e.preventDefault();
  try {
    const claim = await api('/api/claims', { method: 'POST', body: JSON.stringify({
      type: $('cType').value, description: $('cDesc').value, amount: $('cAmount').value, incidentDate: $('cDate').value }) });
    for (const f of $('cFiles').files) {
      const fd = new FormData(); fd.append('file', f);
      try { await api(`/api/claims/${claim.id}/documents`, { method: 'POST', body: fd }); }
      catch (err) { alertMsg(`Claim saved, but "${f.name}" failed: ${err.message}`, 'warning'); }
    }
    e.target.reset();
    if (!$('alertBox').innerHTML) alertMsg(`Claim ${claim.claimNumber} submitted.`, 'success');
    loadMine();
  } catch (err) { alertMsg(err.message); }
};

// ---------- employee ----------
async function loadEmployee() {
  const params = new URLSearchParams();
  if ($('statusFilter').value) params.set('status', $('statusFilter').value);
  if ($('searchBox').value.trim()) params.set('q', $('searchBox').value.trim());
  try {
    const [list, stats] = await Promise.all([api('/api/employee/claims?' + params), api('/api/employee/claims/stats')]);
    $('stats').innerHTML = Object.entries(stats).map(([k, v]) =>
      `<div class="col"><div class="stat-box"><strong>${esc(v)}</strong><small>${esc(k.replace('_', ' '))}</small></div></div>`).join('');
    $('allClaims').innerHTML = list.length ? list.map(c => claimCard(c, true)).join('') : '<p class="text-muted">No claims match.</p>';
  } catch (err) { alertMsg(err.message); }
}
$('refreshBtn').onclick = loadEmployee;
$('statusFilter').onchange = loadEmployee;
let t; $('searchBox').oninput = () => { clearTimeout(t); t = setTimeout(loadEmployee, 300); };

$('cDate').max = new Date().toISOString().slice(0, 10);
if (auth) start().catch(() => logout());
