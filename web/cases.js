// cases.js — إدارة الحالات الإنسانية

import { db } from './firebase-config.js';
import {
  collection, query, where, orderBy, onSnapshot,
  doc, updateDoc, serverTimestamp, getDoc, getDocs
} from 'https://www.gstatic.com/firebasejs/11.8.1/firebase-firestore.js';
import {
  showToast, showConfirm, formatCurrency, formatDate, timeAgo,
  getStatusLabel, getStatusBadgeClass, getCategoryLabel, escHtml,
  getUrgencyLabel, getUrgencyBadgeClass
} from './utils.js';

let allCases = [];
let currentFilter = 'all';
let searchQuery = '';
let caseListener = null;

// ── Start listening to all cases ──
export function startCasesListener(onUpdate) {
  if (caseListener) caseListener();

  const q = query(collection(db, 'cases'), orderBy('createdAt', 'desc'));
  caseListener = onSnapshot(q, (snap) => {
    allCases = snap.docs.map(d => ({ id: d.id, docId: d.id, ...d.data() }));
    onUpdate(allCases);
  }, (err) => {
    console.error('Cases listener error:', err);
    showToast('error', 'خطأ في الاتصال', 'فشل تحميل الحالات');
  });

  return () => { if (caseListener) caseListener(); };
}

// ── Approve a case ──
export async function approveCase(caseId, caseName) {
  const confirmed = await showConfirm(
    'تأكيد الموافقة',
    `هل تريد الموافقة على حالة "${caseName}"؟\nسيظهر للمتبرعين في التطبيق فوراً.`,
    '✅'
  );
  if (!confirmed) return false;

  try {
    await updateDoc(doc(db, 'cases', caseId), {
      status: 'approved',
      approvedAt: serverTimestamp()
    });
    showToast('success', 'تمت الموافقة', `تمت الموافقة على "${caseName}" وستظهر للمتبرعين الآن`);
    return true;
  } catch (e) {
    console.error(e);
    showToast('error', 'خطأ', 'فشل تحديث الحالة: ' + e.message);
    return false;
  }
}

// ── Reject a case ──
export async function rejectCase(caseId, caseName) {
  const confirmed = await showConfirm(
    'تأكيد الرفض',
    `هل تريد رفض حالة "${caseName}"؟`,
    '❌'
  );
  if (!confirmed) return false;

  try {
    await updateDoc(doc(db, 'cases', caseId), {
      status: 'rejected',
      rejectedAt: serverTimestamp()
    });
    showToast('warning', 'تم الرفض', `تم رفض حالة "${caseName}"`);
    return true;
  } catch (e) {
    console.error(e);
    showToast('error', 'خطأ', 'فشل رفض الحالة: ' + e.message);
    return false;
  }
}

// ── Mark case as completed ──
export async function completeCase(caseId, caseName) {
  const confirmed = await showConfirm(
    'إغلاق الحالة',
    `هل تريد إغلاق حالة "${caseName}" كمكتملة؟`,
    '🏁'
  );
  if (!confirmed) return false;

  try {
    await updateDoc(doc(db, 'cases', caseId), {
      status: 'completed',
      completedAt: serverTimestamp()
    });
    showToast('success', 'تم الإغلاق', `تم إغلاق حالة "${caseName}" كمكتملة`);
    return true;
  } catch (e) {
    showToast('error', 'خطأ', e.message);
    return false;
  }
}

// ── Filter cases ──
export function filterCases(status, search) {
  currentFilter = status || 'all';
  searchQuery = search || '';
  let result = [...allCases];

  if (currentFilter !== 'all') {
    result = result.filter(c => c.status === currentFilter);
  }
  if (searchQuery) {
    const q = searchQuery.toLowerCase();
    result = result.filter(c =>
      (c.title || '').toLowerCase().includes(q) ||
      (c.location || '').toLowerCase().includes(q) ||
      (c.story || '').toLowerCase().includes(q)
    );
  }
  return result;
}

// ── Get counts ──
export function getCaseCounts(cases) {
  return {
    total: cases.length,
    pending: cases.filter(c => c.status === 'pending').length,
    approved: cases.filter(c => c.status === 'approved').length,
    rejected: cases.filter(c => c.status === 'rejected').length,
    completed: cases.filter(c => c.status === 'completed').length,
  };
}

// ── Render case cards for pending view ──
export function renderCaseCards(cases, container) {
  if (!container) return;

  if (cases.length === 0) {
    container.innerHTML = `
      <div class="empty-state" style="grid-column:1/-1">
        <div class="empty-state-icon">📭</div>
        <h3>لا توجد حالات</h3>
        <p>لا توجد حالات تطابق الفلتر المحدد</p>
      </div>`;
    return;
  }

  container.innerHTML = cases.map(c => {
    const progress = c.progress || 0;
    const pct = Math.round(progress * 100);
    const statusBadge = `<span class="badge ${getStatusBadgeClass(c.status)}">${getStatusLabel(c.status)}</span>`;

    const actions = buildCaseActions(c);

    return `
    <div class="case-card ${c.status}" data-id="${escHtml(c.docId)}">
      <div class="case-card-header">
        <div class="case-title">${escHtml(c.title)}</div>
        ${statusBadge}
      </div>
      <div class="case-meta">
        <span class="case-meta-item">📍 ${escHtml(c.location)}</span>
        <span class="case-meta-item">${getCategoryLabel(c.category)}</span>
        <span class="badge ${getUrgencyBadgeClass(c.urgencyLevel)}">${getUrgencyLabel(c.urgencyLevel)}</span>
        <span class="case-meta-item">🕐 ${timeAgo(c.createdAt)}</span>
      </div>
      <div class="case-story">${escHtml(c.story)}</div>
      <div class="case-progress-section">
        <div class="case-progress-label">
          <span>${formatCurrency(c.amountRaised || 0)} تم جمعها</span>
          <span class="bold" style="color:var(--primary-light)">${pct}%</span>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" style="width:${pct}%"></div>
        </div>
        <div class="case-progress-label" style="margin-top:4px">
          <span class="text-muted text-sm">الهدف: ${formatCurrency(c.amountRequired || 0)}</span>
        </div>
      </div>
      <div class="case-actions">${actions}</div>
    </div>`;
  }).join('');

  // Attach event listeners
  container.querySelectorAll('[data-action]').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      const action = btn.dataset.action;
      const id = btn.dataset.id;
      const name = btn.dataset.name;
      if (action === 'approve') await approveCase(id, name);
      else if (action === 'reject') await rejectCase(id, name);
      else if (action === 'complete') await completeCase(id, name);
      else if (action === 'details') showCaseModal(allCases.find(c => c.docId === id));
    });
  });
}

function buildCaseActions(c) {
  let actions = `<button class="btn btn-sm btn-outline" data-action="details" data-id="${escHtml(c.docId)}" data-name="${escHtml(c.title)}">🔍 التفاصيل</button>`;

  if (c.status === 'pending') {
    actions += `
      <button class="btn btn-sm btn-approve" data-action="approve" data-id="${escHtml(c.docId)}" data-name="${escHtml(c.title)}">✅ موافقة</button>
      <button class="btn btn-sm btn-reject" data-action="reject" data-id="${escHtml(c.docId)}" data-name="${escHtml(c.title)}">❌ رفض</button>`;
  } else if (c.status === 'approved') {
    actions += `<button class="btn btn-sm btn-reject" data-action="complete" data-id="${escHtml(c.docId)}" data-name="${escHtml(c.title)}" style="background:linear-gradient(135deg,#1565C0,#1976D2)">🏁 إغلاق</button>`;
  }

  return actions;
}

// ── Render cases table ──
export function renderCasesTable(cases, tbody) {
  if (!tbody) return;
  if (cases.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:40px;color:var(--text-muted)">لا توجد حالات</td></tr>`;
    return;
  }

  tbody.innerHTML = cases.map(c => `
    <tr>
      <td>
        <div class="bold truncate" style="max-width:180px">${escHtml(c.title)}</div>
        <div class="text-sm text-muted">${escHtml(c.location)}</div>
      </td>
      <td>
        <div>${getCategoryLabel(c.category)}</div>
        <div style="margin-top: 4px;"><span class="badge ${getUrgencyBadgeClass(c.urgencyLevel)}" style="padding: 2px 8px; font-size: 10px;">${getUrgencyLabel(c.urgencyLevel)}</span></div>
      </td>
      <td>${formatCurrency(c.amountRequired)}</td>
      <td>
        <div>${formatCurrency(c.amountRaised || 0)}</div>
        <div class="progress-bar" style="margin-top:5px;width:80px">
          <div class="progress-fill" style="width:${Math.round((c.progress||0)*100)}%"></div>
        </div>
      </td>
      <td><span class="badge ${getStatusBadgeClass(c.status)}">${getStatusLabel(c.status)}</span></td>
      <td class="text-sm text-muted">${timeAgo(c.createdAt)}</td>
      <td>
        <div style="display:flex;gap:6px;flex-wrap:wrap">
          <button class="btn btn-sm btn-outline" data-action="details" data-id="${escHtml(c.docId)}" data-name="${escHtml(c.title)}">تفاصيل</button>
          ${c.status === 'pending' ? `
            <button class="btn btn-sm btn-approve" data-action="approve" data-id="${escHtml(c.docId)}" data-name="${escHtml(c.title)}">✅</button>
            <button class="btn btn-sm btn-reject" data-action="reject" data-id="${escHtml(c.docId)}" data-name="${escHtml(c.title)}">❌</button>` : ''}
        </div>
      </td>
    </tr>`).join('');

  // Attach events
  tbody.querySelectorAll('[data-action]').forEach(btn => {
    btn.addEventListener('click', async () => {
      const action = btn.dataset.action;
      const id = btn.dataset.id;
      const name = btn.dataset.name;
      if (action === 'approve') await approveCase(id, name);
      else if (action === 'reject') await rejectCase(id, name);
      else if (action === 'details') showCaseModal(allCases.find(c => c.docId === id));
    });
  });
}

// ── Case Detail Modal ──
export function showCaseModal(c) {
  if (!c) return;
  const pct = Math.round((c.progress || 0) * 100);

  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.innerHTML = `
    <div class="custom-modal">
      <div class="custom-modal-header">
        <div class="custom-modal-title">📋 ${escHtml(c.title)}</div>
        <button class="custom-modal-close" id="modal-close-btn">✕</button>
      </div>

      <div class="flex gap-12 mb-20" style="flex-wrap:wrap">
        <span class="badge ${getStatusBadgeClass(c.status)}">${getStatusLabel(c.status)}</span>
        <span class="badge badge-donor">${getCategoryLabel(c.category)}</span>
        <span class="badge ${getUrgencyBadgeClass(c.urgencyLevel)}">${getUrgencyLabel(c.urgencyLevel)}</span>
      </div>

      <div class="two-col" style="gap:16px;margin-bottom:20px">
        <div class="custom-modal-field">
          <div class="custom-modal-label">الموقع</div>
          <div class="custom-modal-value">📍 ${escHtml(c.location)}</div>
        </div>
        <div class="custom-modal-field">
          <div class="custom-modal-label">تاريخ التقديم</div>
          <div class="custom-modal-value">🗓 ${formatDate(c.createdAt)}</div>
        </div>
        <div class="custom-modal-field">
          <div class="custom-modal-label">المبلغ المطلوب</div>
          <div class="custom-modal-value bold" style="color:var(--gold)">${formatCurrency(c.amountRequired)}</div>
        </div>
        <div class="custom-modal-field">
          <div class="custom-modal-label">تم جمعه</div>
          <div class="custom-modal-value bold" style="color:var(--primary-light)">${formatCurrency(c.amountRaised || 0)}</div>
        </div>
      </div>

      <div class="custom-modal-field">
        <div class="custom-modal-label">التقدم</div>
        <div class="progress-bar" style="height:10px;margin-top:6px">
          <div class="progress-fill" style="width:${pct}%"></div>
        </div>
        <div style="text-align:left;font-size:13px;margin-top:4px;color:var(--primary-light)">${pct}%</div>
      </div>

      <div class="custom-modal-field">
        <div class="custom-modal-label">القصة / تفاصيل الحالة</div>
        <div class="custom-modal-value" style="background:var(--bg-glass);padding:14px;border-radius:10px;line-height:1.8;margin-top:6px">${escHtml(c.story)}</div>
      </div>


      ${(c.imageUrls && c.imageUrls.length > 0) ? `
      <div class="custom-modal-field">
        <div class="custom-modal-label">الصور المرفقة (${c.imageUrls.length})</div>
        <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(140px,1fr));gap:10px;margin-top:8px;">
          ${c.imageUrls.map(url => `
            <img src="${escHtml(url)}" alt="صورة الحالة" 
              style="width:100%;height:140px;object-fit:cover;border-radius:10px;border:1px solid var(--border);cursor:pointer;" 
              onclick="window.open('${escHtml(url)}','_blank')" />
          `).join('')}
        </div>
      </div>` : (c.documentUrl || c.imageUrl) ? `
      <div class="custom-modal-field">
        <div class="custom-modal-label">المستند الداعم</div>
        ${(c.documentUrl || c.imageUrl).match(/\.(jpeg|jpg|gif|png|webp|bmp)(?:\?.*)?$/i) || c.imageUrl ? `
           <img src="${escHtml(c.documentUrl || c.imageUrl)}" alt="مستند الحالة" style="max-width:100%; border-radius:12px; margin-top:8px; border:1px solid var(--border);" />
        ` : `
           <a href="${escHtml(c.documentUrl || c.imageUrl)}" target="_blank" class="btn btn-sm btn-outline" style="margin-top:6px;display:inline-flex">📄 عرض المستند</a>
        `}
      </div>` : ''}

      <div class="custom-modal-footer">
        <button class="btn btn-outline" id="modal-close-btn2">إغلاق</button>
        ${c.status === 'pending' ? `
          <button class="btn btn-reject" id="modal-reject">❌ رفض</button>
          <button class="btn btn-approve" id="modal-approve">✅ موافقة</button>` : ''}
      </div>
    </div>`;

  document.body.appendChild(overlay);

  const close = () => overlay.remove();
  document.getElementById('modal-close-btn').onclick = close;
  document.getElementById('modal-close-btn2').onclick = close;
  overlay.onclick = (e) => { if (e.target === overlay) close(); };

  if (c.status === 'pending') {
    document.getElementById('modal-approve').onclick = async () => {
      const ok = await approveCase(c.docId, c.title);
      if (ok) close();
    };
    document.getElementById('modal-reject').onclick = async () => {
      const ok = await rejectCase(c.docId, c.title);
      if (ok) close();
    };
  }
}

export { allCases };
