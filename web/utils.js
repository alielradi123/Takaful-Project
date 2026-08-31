// utils.js — دوال مساعدة مشتركة

// ── Format Numbers ──
export function formatNumber(n) {
  if (!n && n !== 0) return '0';
  return new Intl.NumberFormat('ar-YE').format(n);
}

export function formatCurrency(amount) {
  return `${formatNumber(amount)} ر.ي`;
}

// ── Format Dates ──
export function formatDate(timestamp) {
  if (!timestamp) return '—';
  let date;
  if (timestamp?.toDate) {
    date = timestamp.toDate();
  } else if (typeof timestamp === 'number') {
    date = new Date(timestamp);
  } else {
    date = new Date(timestamp);
  }
  return new Intl.DateTimeFormat('ar-SA', {
    year: 'numeric', month: 'long', day: 'numeric'
  }).format(date);
}

export function formatDateTime(timestamp) {
  if (!timestamp) return '—';
  let date;
  if (timestamp?.toDate) {
    date = timestamp.toDate();
  } else if (typeof timestamp === 'number') {
    date = new Date(timestamp);
  } else {
    date = new Date(timestamp);
  }
  return new Intl.DateTimeFormat('ar-SA', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit'
  }).format(date);
}

export function timeAgo(timestamp) {
  if (!timestamp) return '';
  let date;
  if (timestamp?.toDate) date = timestamp.toDate();
  else if (typeof timestamp === 'number') date = new Date(timestamp);
  else date = new Date(timestamp);

  const diff = Date.now() - date.getTime();
  const sec = Math.floor(diff / 1000);
  const min = Math.floor(sec / 60);
  const hr = Math.floor(min / 60);
  const day = Math.floor(hr / 24);

  if (sec < 60) return 'الآن';
  if (min < 60) return `منذ ${min} دقيقة`;
  if (hr < 24) return `منذ ${hr} ساعة`;
  if (day < 7) return `منذ ${day} يوم`;
  return formatDate(timestamp);
}

// ── Status Labels ──
export function getStatusLabel(status) {
  const map = {
    pending: 'معلق',
    approved: 'موافق عليه',
    rejected: 'مرفوض',
    completed: 'مكتمل',
    active: 'نشط',
    suspended: 'موقوف',
    'قيد الجمع': 'قيد الجمع',
    'تم التوزيع': 'تم التوزيع',
    'تم الاستلام': 'تم الاستلام',
  };
  return map[status] || status;
}

export function getStatusBadgeClass(status) {
  const map = {
    pending: 'badge-pending',
    approved: 'badge-approved',
    rejected: 'badge-rejected',
    completed: 'badge-completed',
    active: 'badge-active',
    suspended: 'badge-suspended',
    'قيد الجمع': 'badge-pending',
    'تم التوزيع': 'badge-approved',
    'تم الاستلام': 'badge-completed',
  };
  return map[status] || 'badge-pending';
}

export function getRoleLabel(role) {
  const map = {
    member: 'عضو',
    employee: 'موظف',
    admin: 'مدير',
    donor: 'متبرع',
    beneficiary: 'مستفيد',
    volunteer: 'متطوع',
    supervisor: 'مشرف',
  };
  return map[role] || role;
}

export function getRoleBadgeClass(role) {
  const map = {
    member: 'badge-donor',
    employee: 'badge-approved',
    admin: 'badge-supervisor',
    donor: 'badge-donor',
    beneficiary: 'badge-rejected', /* Using an existing color, or we can add badge-beneficiary in css later */
    volunteer: 'badge-pending',
    supervisor: 'badge-supervisor',
  };
  return map[role] || 'badge-donor';
}

export function getCategoryLabel(cat) {
  const map = {
    'مالي': '💰 مالي',
    'عيني': '📦 عيني',
    'طبي': '🏥 طبي',
  };
  return map[cat] || cat;
}

export function getUrgencyLabel(urgency) {
  if (!urgency) return 'عادي';
  return urgency; // 'عادي', 'متوسط', 'عاجل'
}

export function getUrgencyBadgeClass(urgency) {
  const map = {
    'عادي': 'badge-urgency-normal',
    'متوسط': 'badge-urgency-medium',
    'عاجل': 'badge-urgency-high',
  };
  return map[urgency] || 'badge-urgency-normal';
}

// ── Toast Notifications ──
let toastContainer;

function getToastContainer() {
  if (!toastContainer) {
    toastContainer = document.getElementById('toast-container');
    if (!toastContainer) {
      toastContainer = document.createElement('div');
      toastContainer.id = 'toast-container';
      toastContainer.className = 'toast-container';
      document.body.appendChild(toastContainer);
    }
  }
  return toastContainer;
}

export function showToast(type, title, msg = '', duration = 4000) {
  const icons = { success: '✅', error: '❌', info: 'ℹ️', warning: '⚠️' };
  const container = getToastContainer();

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span class="toast-icon">${icons[type] || '📢'}</span>
    <div class="toast-content">
      <div class="toast-title">${title}</div>
      ${msg ? `<div class="toast-msg">${msg}</div>` : ''}
    </div>
    <button class="toast-close" onclick="this.parentElement.remove()">✕</button>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.classList.add('toast-out');
    setTimeout(() => toast.remove(), 350);
  }, duration);
}

// ── Confirm Dialog ──
export function showConfirm(title, msg, icon = '❓') {
  return new Promise((resolve) => {
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `
      <div class="confirm-dialog">
        <div class="confirm-icon">${icon}</div>
        <div class="confirm-title">${title}</div>
        <div class="confirm-msg">${msg}</div>
        <div class="confirm-actions">
          <button class="btn btn-outline" id="confirm-cancel">إلغاء</button>
          <button class="btn btn-approve" id="confirm-ok">تأكيد</button>
        </div>
      </div>
    `;
    document.body.appendChild(overlay);

    document.getElementById('confirm-ok').onclick = () => { overlay.remove(); resolve(true); };
    document.getElementById('confirm-cancel').onclick = () => { overlay.remove(); resolve(false); };
    overlay.onclick = (e) => { if (e.target === overlay) { overlay.remove(); resolve(false); } };
  });
}

// ── Escape HTML ──
export function escHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// ── Avatar initials ──
export function getInitials(name) {
  if (!name) return '؟';
  const parts = name.trim().split(' ');
  return parts.length >= 2
    ? parts[0][0] + parts[parts.length - 1][0]
    : parts[0][0] || '؟';
}

// ── Debounce ──
export function debounce(fn, delay = 300) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}

// ── Live Clock ──
export function startClock(el) {
  if (!el) return;
  const update = () => {
    el.textContent = new Intl.DateTimeFormat('ar-SA', {
      hour: '2-digit', minute: '2-digit', second: '2-digit',
      hour12: false
    }).format(new Date());
  };
  update();
  setInterval(update, 1000);
}
