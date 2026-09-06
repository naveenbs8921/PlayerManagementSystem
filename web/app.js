/**
 * Arcadia — Minimalist Frontend & Combat Controller
 */

let state = {
  player: { id: 0, name: 'Player', level: 1, xp: 0, coins: 100, rank: 0, highScore: 0 },
  characters: [],
  inventory: [],
  board: [],
  achievements: []
};

let combatState = {
  active: false,
  characterId: null,
  character: null,
  enemyType: 'Goblin Scout',
  playerHp: 100,
  playerMaxHp: 100,
  enemyHp: 60,
  enemyMaxHp: 60,
  isDefending: false,
  turn: 1
};

const enemyConfigs = {
  'Training Dummy': { hp: 40, threat: 'Easy Training Partner' },
  'Goblin Scout': { hp: 60, threat: 'Normal Dungeon Scavenger' },
  'Stone Golem': { hp: 120, threat: 'Hard Ancient Automaton' },
  'Dread Knight': { hp: 180, threat: 'Elite Armored Champion' },
  'Archdemon': { hp: 260, threat: 'Boss Calamity Entity' }
};

let playerId = localStorage.getItem('arcadia-player-id');
let registering = false;

const $ = s => document.querySelector(s);
const $$ = s => document.querySelectorAll(s);
const esc = s => String(s ?? '').replace(/[&<>'"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[c]));

// --- API Helper ---
async function api(path, values) {
  const r = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams(values)
  });
  const d = await r.json();
  if (!r.ok) throw new Error(d.error || 'Request failed.');
  return d;
}

// --- Load Player Dashboard ---
async function load(id) {
  const r = await fetch(`/api/dashboard?playerId=${encodeURIComponent(id)}`);
  const d = await r.json();
  if (!r.ok) throw new Error(d.error || 'Could not load player.');
  state = d;
  playerId = String(id);
  localStorage.setItem('arcadia-player-id', playerId);
  $('#auth-screen').classList.add('hidden');
  render();
}

// --- Toast Notification ---
function toast(msg) {
  const t = $('#toast');
  t.textContent = msg;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 2800);
}

// --- Logout ---
function logout() {
  localStorage.removeItem('arcadia-player-id');
  playerId = null;
  state = { player: { id: 0, name: 'Player', level: 1, xp: 0, coins: 100, rank: 0, highScore: 0 }, characters: [], inventory: [], board: [], achievements: [] };
  resetCombat();
  const f = $('#auth-form');
  if (f) f.reset();
  $('#auth-error').textContent = '';
  $('#auth-screen').classList.remove('hidden');
  toast('Logged out.');
}

// --- View Switching ---
function switchView(viewId) {
  $$('.view').forEach(v => v.classList.toggle('active', v.id === viewId));
  $$('.nav-link').forEach(btn => btn.classList.toggle('active', btn.dataset.view === viewId));
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

$$('[data-view]').forEach(btn => {
  btn.addEventListener('click', () => switchView(btn.dataset.view));
});

// --- Main Render ---
function render() {
  const p = state.player;
  const boardSorted = [...state.board].sort((a, b) => b[1] - a[1]);

  // Header & User Profile
  $('#header-name').textContent = p.name;
  $('#header-level').textContent = p.level;
  $('#header-coins').textContent = p.coins.toLocaleString();
  const initials = p.name.split(' ').filter(Boolean).map(n => n[0]).join('').substring(0, 2).toUpperCase() || 'PL';
  $('#header-avatar').textContent = initials;
  $('#page-title').textContent = `Welcome, ${p.name.split(' ')[0]}`;

  // Metrics Strip
  $('#stat-level').textContent = p.level;
  $('#stat-xp-copy').textContent = `${p.xp} / 100 XP`;
  $('#stat-coins').textContent = p.coins.toLocaleString();
  $('#stat-score').textContent = p.highScore.toLocaleString();
  $('#stat-rank').textContent = p.rank > 0 ? `#${p.rank}` : 'Unranked';
  $('#stat-roster-count').textContent = state.characters.length;

  // Overview Active Hero Preview
  const heroPreview = $('#overview-hero-preview');
  if (state.characters.length > 0) {
    const c = state.characters[0];
    heroPreview.innerHTML = `
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
        <strong style="font-size: 15px;">${esc(c.name)}</strong>
        <span class="pill-badge" style="font-size: 11px;">${esc(c.className)} · Lv.${c.level || 1}</span>
      </div>
      <div style="display: flex; gap: 16px; font-family: var(--font-mono); font-size: 12px; color: var(--text-muted);">
        <span>HP: <b style="color: var(--text);">${c.hp}</b></span>
        <span>ATK: <b style="color: var(--text);">${c.atk}</b></span>
        <span>DEF: <b style="color: var(--text);">${c.def}</b></span>
      </div>
    `;
  } else {
    heroPreview.innerHTML = `
      <p class="muted" style="margin-bottom: 8px;">You haven't created any characters yet.</p>
      <button class="btn-secondary" data-view="characters">+ Create your first character</button>
    `;
    heroPreview.querySelector('button')?.addEventListener('click', () => switchView('characters'));
  }

  // Overview Leaderboard Top 3
  const top3 = boardSorted.slice(0, 3);
  $('#overview-board-list').innerHTML = top3.length ? top3.map((entry, idx) => `
    <div style="display: flex; justify-content: space-between; padding: 7px 0; border-bottom: 1px solid var(--border-subtle); font-size: 13px;">
      <span><b style="font-family: var(--font-mono); color: var(--text-muted); margin-right: 8px;">#${idx + 1}</b> ${esc(entry[0])}</span>
      <span style="font-family: var(--font-mono); font-weight: 600;">${entry[1].toLocaleString()} pts</span>
    </div>
  `).join('') : '<p class="muted">No scores logged yet.</p>';

  // Arena Character Dropdown
  const select = $('#arena-character-select');
  select.innerHTML = state.characters.length
    ? state.characters.map(c => `<option value="${c.id}">${esc(c.name)} (${esc(c.className)} · HP ${c.hp} · ATK ${c.atk} · DEF ${c.def})</option>`).join('')
    : '<option value="">No characters available. Create one first.</option>';

  // Characters View Roster
  const charList = $('#character-list');
  charList.innerHTML = state.characters.length ? state.characters.map(c => `
    <div class="item-card">
      <div class="item-card-header">
        <span class="item-card-title">${esc(c.name)}</span>
        <span class="pill-badge" style="font-size: 11px;">${esc(c.className)}</span>
      </div>
      <div class="item-card-desc">Trained combatant ready for arena engagement.</div>
      <div class="item-card-stats">
        <span>HP <b>${c.hp}</b></span>
        <span>ATK <b>${c.atk}</b></span>
        <span>DEF <b>${c.def}</b></span>
      </div>
    </div>
  `).join('') : '<p class="muted">No characters created yet.</p>';

  // Inventory View Armory
  const invList = $('#inventory-list');
  invList.innerHTML = state.inventory.length ? state.inventory.map(w => `
    <div class="item-card">
      <div class="item-card-header">
        <span class="item-card-title">${esc(w.name)}</span>
        <span class="pill-badge" style="font-size: 11px;">${esc(w.type)}</span>
      </div>
      <div class="item-card-desc">${esc(w.effect)}</div>
      <div class="item-card-stats">
        <span>DMG <b>+${w.damage}</b></span>
        <span>QTY <b>×${w.quantity}</b></span>
      </div>
    </div>
  `).join('') : '<p class="muted">Your armory is empty.</p>';

  // Leaderboard Full Table
  const leadList = $('#leaderboard-list');
  leadList.innerHTML = boardSorted.length ? boardSorted.map((entry, idx) => `
    <tr class="${entry[0] === p.name ? 'highlight' : ''}">
      <td style="font-family: var(--font-mono); color: var(--text-muted);">#${idx + 1}</td>
      <td>${esc(entry[0])} ${entry[0] === p.name ? '<span class="pill-badge" style="font-size: 10px; margin-left: 6px;">YOU</span>' : ''}</td>
      <td style="text-align: right; font-family: var(--font-mono); font-weight: 600;">${entry[1].toLocaleString()}</td>
    </tr>
  `).join('') : '<tr><td colspan="3" class="muted">No scores logged yet.</td></tr>';

  // Achievements View
  const achCount = state.achievements.filter(a => a.unlocked).length;
  $('#achievement-count').textContent = `${achCount} / ${state.achievements.length} Unlocked`;
  const achList = $('#achievement-list');
  achList.innerHTML = state.achievements.length ? state.achievements.map(a => `
    <div class="item-card" style="${a.unlocked ? 'border-color: rgba(16, 185, 129, 0.4);' : 'opacity: 0.6;'}">
      <div class="item-card-header">
        <span class="item-card-title">${esc(a.title)}</span>
        <span class="pill-badge" style="font-size: 11px; ${a.unlocked ? 'color: var(--success); border-color: rgba(16, 185, 129, 0.3);' : ''}">
          ${a.unlocked ? '✓ UNLOCKED' : 'LOCKED'}
        </span>
      </div>
      <div class="item-card-desc">${esc(a.text)}</div>
    </div>
  `).join('') : '<p class="muted">No achievements registered.</p>';
}

// ==========================================================================
// COMBAT ENGINE (Frontend Controller)
// ==========================================================================

function logCombat(message, type = 'system') {
  const logEl = $('#combat-log');
  const line = document.createElement('div');
  line.className = `log-line ${type}`;
  line.textContent = `[T${combatState.turn}] ${message}`;
  logEl.prepend(line);
}

function updateCombatUI() {
  // Player bar
  const pPct = Math.max(0, Math.min(100, Math.round((combatState.playerHp / combatState.playerMaxHp) * 100)));
  $('#f-hp-bar').style.width = `${pPct}%`;
  $('#f-hp-bar').className = `bar-fill ${pPct < 30 ? 'danger' : pPct < 60 ? 'warning' : 'success'}`;
  $('#f-hp-text').textContent = `${combatState.playerHp} / ${combatState.playerMaxHp}`;
  $('#f-guard-status').style.display = combatState.isDefending ? 'inline' : 'none';
  $('#fighter-card').classList.toggle('guarding', combatState.isDefending);

  // Enemy bar
  const ePct = Math.max(0, Math.min(100, Math.round((combatState.enemyHp / combatState.enemyMaxHp) * 100)));
  $('#e-hp-bar').style.width = `${ePct}%`;
  $('#e-hp-text').textContent = `${combatState.enemyHp} / ${combatState.enemyMaxHp}`;
  $('#combat-turn-count').textContent = `Turn ${combatState.turn}`;
}

function startCombat() {
  const charId = parseInt($('#arena-character-select').value);
  if (!charId) {
    toast('Please select or create a character first.');
    return;
  }
  const char = state.characters.find(c => c.id === charId);
  if (!char) {
    toast('Character not found.');
    return;
  }

  const enemyName = $('#arena-enemy-select').value;
  const cfg = enemyConfigs[enemyName] || { hp: 60, threat: 'Enemy' };

  combatState = {
    active: true,
    characterId: char.id,
    character: char,
    enemyType: enemyName,
    playerHp: char.hp,
    playerMaxHp: char.hp,
    enemyHp: cfg.hp,
    enemyMaxHp: cfg.hp,
    isDefending: false,
    turn: 1
  };

  // Populate UI
  $('#f-name').textContent = char.name;
  $('#f-class').textContent = `${char.className} · Level ${char.level || 1}`;
  $('#f-atk').textContent = char.atk;
  $('#f-def').textContent = char.def;

  $('#e-name').textContent = enemyName;
  $('#e-threat').textContent = cfg.threat;
  $('#e-status').textContent = 'Engaged';
  $('#e-status').style.color = 'var(--text)';

  // Reset console
  $('#combat-log').innerHTML = '';
  logCombat(`Encounter started: ${char.name} engages ${enemyName}.`, 'system');

  // Show combat stage & action buttons
  $('#arena-stage').style.display = 'grid';
  $('#combat-actions').style.display = 'grid';
  $('#combat-log-panel').style.display = 'block';

  // Enable all actions
  $$('.btn-combat').forEach(b => { b.disabled = false; b.style.opacity = '1'; });

  updateCombatUI();
}

async function performCombatAction(action) {
  if (!combatState.active) return;

  // Disable buttons while processing turn
  $$('.btn-combat').forEach(b => { b.disabled = true; b.style.opacity = '0.6'; });

  try {
    const res = await api('/api/battle/action', {
      playerId,
      characterId: combatState.characterId,
      action,
      enemyType: combatState.enemyType,
      enemyHp: combatState.enemyHp,
      enemyMaxHp: combatState.enemyMaxHp,
      playerHp: combatState.playerHp,
      playerMaxHp: combatState.playerMaxHp,
      isDefending: String(combatState.isDefending)
    });

    combatState.playerHp = res.playerHp;
    combatState.enemyHp = res.enemyHp;
    combatState.isDefending = res.isDefending;

    if (res.playerLog) logCombat(res.playerLog, 'player');
    if (res.enemyLog) logCombat(res.enemyLog, res.victory ? 'victory' : 'enemy');

    updateCombatUI();

    if (res.victory) {
      combatState.active = false;
      $('#e-status').textContent = 'Defeated';
      $('#e-status').style.color = 'var(--success)';
      logCombat(`VICTORY! Rewards gained: +${res.reward.xp} XP · +${res.reward.coins} Coins · +${res.reward.score} Score.`, 'victory');
      toast(`Victory! +${res.reward.score} points recorded!`);

      // Update local state and re-render header/stats
      if (res.dashboard) {
        state = res.dashboard;
        render();
      }
    } else if (res.defeated) {
      combatState.active = false;
      $('#e-status').textContent = 'Victorious';
      $('#e-status').style.color = 'var(--danger)';
      logCombat('DEFEAT! Your hero was incapacitated in battle.', 'system');
      toast('Hero defeated in combat.');
    } else {
      combatState.turn++;
      $('#combat-turn-count').textContent = `Turn ${combatState.turn}`;
      // Re-enable actions for next turn
      $$('.btn-combat').forEach(b => { b.disabled = false; b.style.opacity = '1'; });
    }

  } catch (err) {
    toast(err.message);
    $$('.btn-combat').forEach(b => { b.disabled = false; b.style.opacity = '1'; });
  }
}

function resetCombat() {
  combatState.active = false;
  $('#arena-stage').style.display = 'none';
  $('#combat-actions').style.display = 'none';
  $('#combat-log-panel').style.display = 'none';
  $('#combat-log').innerHTML = '';
}

// Combat event bindings
$('#start-battle-btn').onclick = startCombat;
$('#act-attack').onclick = () => performCombatAction('attack');
$('#act-defend').onclick = () => performCombatAction('defend');
$('#act-special').onclick = () => performCombatAction('special');
$('#act-heal').onclick = () => performCombatAction('heal');
$('#act-reset').onclick = resetCombat;

// ==========================================================================
// MODALS & FORMS
// ==========================================================================

function openModal(kind) {
  const f = $('#form-fields');
  const m = $('#modal');
  m.dataset.kind = kind;

  if (kind === 'character') {
    $('#modal-title').textContent = 'Create Character';
    f.innerHTML = `
      <label>Character Name</label>
      <input required name="name" maxlength="50" placeholder="e.g. Roland" />
      <label>Class Specialization</label>
      <select name="className">
        <option value="Warrior">Warrior (High HP & Defense · Shield Mastery)</option>
        <option value="Mage">Mage (High Spell Damage · Mana Barrier)</option>
        <option value="Archer">Archer (High Critical Pierce · Evasion)</option>
      </select>
    `;
  } else if (kind === 'weapon') {
    $('#modal-title').textContent = 'Forge Weapon';
    f.innerHTML = `
      <label>Weapon Name</label>
      <input required name="name" maxlength="50" placeholder="e.g. Iron Longsword" />
      <label>Weapon Type</label>
      <select name="type">
        <option value="Sword">Sword (Melee Bleed Effect)</option>
        <option value="Bow">Bow (Ranged Piercing Shot)</option>
      </select>
      <label>Damage Value</label>
      <input required name="damage" type="number" min="1" max="500" value="25" />
      <label>Quantity</label>
      <input required name="quantity" type="number" min="1" max="99" value="1" />
    `;
  } else if (kind === 'score') {
    $('#modal-title').textContent = 'Submit High Score';
    f.innerHTML = `
      <label>Score Points</label>
      <input required name="score" type="number" min="0" max="1000000" value="1500" />
    `;
  }

  m.showModal();
}

$('#modal-close').onclick = () => $('#modal').close();
$('#modal-cancel').onclick = () => $('#modal').close();

$('#modal-form').addEventListener('submit', async e => {
  e.preventDefault();
  const v = Object.fromEntries(new FormData(e.currentTarget));
  const kind = $('#modal').dataset.kind;

  try {
    if (kind === 'character') {
      await api('/api/characters', { ...v, playerId });
      toast('Character created.');
    } else if (kind === 'weapon') {
      await api('/api/inventory', { ...v, playerId });
      toast('Weapon added to armory.');
    } else if (kind === 'score') {
      await api('/api/scores', { ...v, playerId });
      toast('Score submitted.');
    }
    $('#modal').close();
    await load(playerId);
  } catch (err) {
    toast(err.message);
  }
});

$('#add-character').onclick = () => openModal('character');
$('#add-item').onclick = () => openModal('weapon');
$('#submit-score').onclick = () => openModal('score');

// Log out bindings
$('#logout-btn').onclick = logout;
$('#sidebar-logout').onclick = logout;

// Auth form bindings
$('#auth-switch').onclick = () => {
  registering = !registering;
  $('#register-name').hidden = !registering;
  $('#auth-title').textContent = registering ? 'Create a Player Account' : 'Log in to your account';
  $('#auth-copy').textContent = registering ? 'Choose a handle and enter your email to enter Arcadia.' : 'Enter your credentials to access your characters and combat records.';
  $('#auth-submit').textContent = registering ? 'Create account' : 'Log in';
  $('#auth-switch').textContent = registering ? 'Already have an account? Log in' : 'New player? Create an account';
  $('#auth-error').textContent = '';
};

$('#auth-form').addEventListener('submit', async e => {
  e.preventDefault();
  const v = Object.fromEntries(new FormData(e.currentTarget));
  try {
    const d = await api(registering ? '/api/register' : '/api/login', v);
    await load(d.id);
    toast(registering ? 'Account created.' : 'Logged in.');
  } catch (err) {
    $('#auth-error').textContent = err.message;
  }
});

// Auto-login if session present
if (playerId) {
  load(playerId).catch(() => {
    localStorage.removeItem('arcadia-player-id');
    playerId = null;
  });
}
