// donations.js — إدارة التبرعات

import { db } from './firebase-config.js';
import {
  collection, query, orderBy, onSnapshot, doc, updateDoc, serverTimestamp, addDoc
} from 'https://www.gstatic.com/firebasejs/11.8.1/firebase-firestore.js';
import {
  showToast, showConfirm, formatCurrency, timeAgo,
  getStatusLabel, getStatusBadgeClass, escHtml, getInitials
} from './utils.js';

let allDonations = [];
let donationListener = null;

// ── Start listener ──
export function startDonationsListener(onUpdate) {
  if (donationListener) donationListener();

  const q = query(collection(db, 'donations'), orderBy('createdAt', 'desc'));
  donationListener = onSnapshot(q, (snap) => {
    allDonations = snap.docs.map(d => ({ docId: d.id, ...d.data() }));
    onUpdate(allDonations);
  }, (err) => {
    console.error('Donations listener error:', err);
    showToast('error', 'خطأ', 'فشل تحميل التبرعات');
  });

  return () => { if (donationListener) donationListener(); };
}

// ── Filter donations ──
export function filterDonations(search, statusFilter) {
  let result = [...allDonations];
  if (statusFilter && statusFilter !== 'all') {
    result = result.filter(d => d.status === statusFilter);
  }
  if (search) {
    const q = search.toLowerCase();
    result = result.filter(d =>
      (d.caseTitle || '').toLowerCase().includes(q) ||
      (d.amountOrItem || '').toLowerCase().includes(q) ||
      (d.paymentMethod || '').toLowerCase().includes(q)
    );
  }
  return result;
}

// ── Update donation status ──
export async function updateDonationStatus(donation, newStatus) {
  const confirmed = await showConfirm(
    'تحديث حالة التبرع',
    `هل تريد تغيير حالة التبرع إلى "${getStatusLabel(newStatus)}"؟`,
    '🔄'
  );
  if (!confirmed) return false;

  try {
    const data = {
      status: newStatus,
      updatedAt: serverTimestamp()
    };
    if (newStatus === 'تم الاستلام') data.receivedAt = serverTimestamp();
    if (newStatus === 'تم التوزيع') data.distributedAt = serverTimestamp();

    await updateDoc(doc(db, 'donations', donation.docId), data);

    // Create Notification
    if (donation.userId || donation.donorId) {
      await addDoc(collection(db, 'notifications'), {
        userId: donation.userId || donation.donorId,
        title: 'تحديث حالة التبرع',
        message: `تم تحديث حالة تبرعك لـ "${donation.caseTitle || 'تبرع عام'}" إلى: ${newStatus}`,
        isRead: false,
        type: 'donation_update',
        timestamp: Date.now()
      });
    }

    showToast('success', 'تم التحديث', `تم تحديث حالة التبرع بنجاح وإشعار المتبرع`);
    return true;
  } catch (e) {
    showToast('error', 'خطأ', e.message);
    return false;
  }
}

// ── Get stats ──
export function getDonationStats(donations) {
  const total = donations.length;
  const totalAmount = donations.reduce((sum, d) => {
    const num = parseFloat(d.amountOrItem?.replace(/[^0-9.]/g, '')) || 0;
    return sum + num;
  }, 0);
  const distributed = donations.filter(d => d.status === 'تم التوزيع').length;
  const pending = donations.filter(d => d.status === 'قيد الجمع').length;
  return { total, totalAmount, distributed, pending };
}

// ── Render donations table ──
export function renderDonationsTable(donations, tbody) {
  if (!tbody) return;

  if (donations.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:40px;color:var(--text-muted)">لا توجد تبرعات</td></tr>`;
    return;
  }

  tbody.innerHTML = donations.map(d => {
    const initials = d.userId ? d.userId.slice(0, 2).toUpperCase() : '؟';
    const categoryIcon = d.category === 'مالي' ? '💰' : d.category === 'عيني' ? '📦' : '🏥';

    const statusOptions = [
      { val: 'قيد الجمع', label: 'قيد الجمع' },
      { val: 'تم التوزيع', label: 'تم التوزيع' },
      { val: 'تم الاستلام', label: 'تم الاستلام' },
      { val: 'قيد المراجعة', label: 'قيد المراجعة (بطاقة)' },
      { val: 'مقبول (خصم تلقائي)', label: 'مقبول (خصم تلقائي)' },
      { val: 'مرفوض', label: 'مرفوض' }
    ];

    const statusSelect = `
      <select class="filter-select" style="padding:5px 10px;font-size:12px" data-donation-id="${escHtml(d.docId)}" data-current="${escHtml(d.status)}">
        ${statusOptions.map(o => `<option value="${o.val}" ${d.status === o.val ? 'selected' : ''}>${o.label}</option>`).join('')}
      </select>`;

    return `
    <tr>
      <td>
        <div class="avatar" style="width:32px;height:32px;font-size:11px">${initials}</div>
      </td>
      <td>
        <div class="bold truncate" style="max-width:160px">${escHtml(d.caseTitle || 'تبرع عام')}</div>
      </td>
      <td>${categoryIcon} ${escHtml(d.category)}</td>
      <td class="bold" style="color:var(--gold)">${escHtml(d.amountOrItem)}</td>
      <td class="text-sm text-muted">
        ${escHtml(d.paymentMethod)}
        ${d.isRecurring ? '<br/><span class="badge" style="background:var(--brand-500);color:#fff;font-size:10px">خصم دوري</span>' : ''}
      </td>
      <td>${statusSelect}</td>
      <td class="text-sm text-muted">${timeAgo(d.createdAt)}</td>
    </tr>`;
  }).join('');

  // Attach status change events
  tbody.querySelectorAll('[data-donation-id]').forEach(sel => {
    sel.addEventListener('change', async () => {
      const docId = sel.dataset.donationId;
      const newStatus = sel.value;
      const donation = donations.find(d => d.docId === docId);
      
      const ok = await updateDonationStatus(donation, newStatus);
      if (!ok) {
        sel.value = sel.dataset.current; // revert
      } else {
        sel.dataset.current = newStatus;
      }
    });
  });
}
