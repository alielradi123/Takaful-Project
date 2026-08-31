// users.js — إدارة المستخدمين

import { db } from './firebase-config.js';
import {
  collection, query, orderBy, onSnapshot, doc, updateDoc
} from 'https://www.gstatic.com/firebasejs/11.8.1/firebase-firestore.js';
import {
  showToast, showConfirm, formatDate, timeAgo,
  getRoleLabel, getRoleBadgeClass, escHtml, getInitials
} from './utils.js';
import { uploadMultipleToSupabase } from './supabase-config.js';

const ROLES = [
  { value: 'donor',       label: 'متبرع',  icon: '❤️' },
  { value: 'beneficiary', label: 'مستفيد', icon: '🤲' },
  { value: 'volunteer',   label: 'متطوع',  icon: '🙋' },
  { value: 'admin',       label: 'مدير',   icon: '🛡️' },
];

let allUsers = [];
let usersListener = null;

// ── Start listener ──
export function startUsersListener(onUpdate) {
  if (usersListener) usersListener();

  const q = query(collection(db, 'users'), orderBy('createdAt', 'desc'));
  usersListener = onSnapshot(q, (snap) => {
    allUsers = snap.docs.map(d => ({ docId: d.id, ...d.data() }));
    onUpdate(allUsers);
  }, (err) => {
    console.error('Users listener:', err);
    showToast('error', 'خطأ', 'فشل تحميل المستخدمين');
  });

  return () => { if (usersListener) usersListener(); };
}

// ── Filter users ──
export function filterUsers(search, roleFilter, statusFilter) {
  let result = [...allUsers];
  if (roleFilter && roleFilter !== 'all') {
    result = result.filter(u => u.role === roleFilter);
  }
  if (statusFilter && statusFilter !== 'all') {
    result = result.filter(u => u.status === statusFilter);
  }
  if (search) {
    const q = search.toLowerCase();
    result = result.filter(u =>
      (u.name || '').toLowerCase().includes(q) ||
      (u.email || '').toLowerCase().includes(q) ||
      (u.phone || '').includes(q)
    );
  }
  return result;
}

// ── Toggle user status ──
export async function toggleUserStatus(docId, currentStatus, userName) {
  const newStatus = currentStatus === 'active' ? 'suspended' : 'active';
  const actionLabel = newStatus === 'suspended' ? 'تعليق' : 'تفعيل';
  const icon = newStatus === 'suspended' ? '🚫' : '✅';

  const confirmed = await showConfirm(
    `${actionLabel} الحساب`,
    `هل تريد ${actionLabel} حساب "${userName}"؟`,
    icon
  );
  if (!confirmed) return false;

  try {
    await updateDoc(doc(db, 'users', docId), { status: newStatus });
    showToast(
      newStatus === 'active' ? 'success' : 'warning',
      'تم التحديث',
      `تم ${actionLabel} حساب ${userName}`
    );
    return true;
  } catch (e) {
    showToast('error', 'خطأ', e.message);
    return false;
  }
}

// ── Change user role ──
export function changeUserRole(docId, currentRole, userName) {
  return new Promise((resolve) => {
    // Remove any existing role modal
    document.getElementById('role-change-modal')?.remove();

    const overlay = document.createElement('div');
    overlay.id = 'role-change-modal';
    overlay.className = 'modal-overlay';

    const rolesHTML = ROLES.map(r => `
      <label class="role-option ${r.value === currentRole ? 'selected' : ''}" data-role="${r.value}">
        <input type="radio" name="new-role" value="${r.value}" ${r.value === currentRole ? 'checked' : ''} style="display:none">
        <span class="role-option-icon">${r.icon}</span>
        <span class="role-option-label">${r.label}</span>
        ${r.value === 'admin' ? '<span style="font-size:10px;color:var(--text-muted);display:block">صلاحيات كاملة</span>' : ''}
      </label>
    `).join('');

    overlay.innerHTML = `
      <div class="confirm-dialog" style="max-width:380px;width:90%;">
        <div class="confirm-icon">👤</div>
        <div class="confirm-title">تغيير دور المستخدم</div>
        <div class="confirm-msg" style="margin-bottom:16px;">اختر الدور الجديد لـ <strong>${escHtml(userName)}</strong>:</div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:20px;" id="role-options-grid">
          ${rolesHTML}
        </div>
        <div class="confirm-actions">
          <button class="btn btn-outline" id="role-cancel-btn">إلغاء</button>
          <button class="btn btn-approve" id="role-confirm-btn">تأكيد التغيير</button>
        </div>
      </div>
    `;

    document.body.appendChild(overlay);

    // Role option click → highlight
    overlay.querySelectorAll('.role-option').forEach(lbl => {
      lbl.addEventListener('click', () => {
        overlay.querySelectorAll('.role-option').forEach(l => l.classList.remove('selected'));
        lbl.classList.add('selected');
      });
    });

    overlay.addEventListener('click', e => { if (e.target === overlay) { overlay.remove(); resolve(false); } });
    document.getElementById('role-cancel-btn').onclick = () => { overlay.remove(); resolve(false); };

    document.getElementById('role-confirm-btn').onclick = async () => {
      const selected = overlay.querySelector('input[name="new-role"]:checked')?.value;
      if (!selected || selected === currentRole) { overlay.remove(); resolve(false); return; }

      const btn = document.getElementById('role-confirm-btn');
      btn.disabled = true;
      btn.textContent = 'جارٍ الحفظ...';

      try {
        await updateDoc(doc(db, 'users', docId), { role: selected });
        showToast('success', 'تم التحديث', `تم تغيير دور ${userName} إلى ${getRoleLabel(selected)}`);
        overlay.remove();
        resolve(true);
      } catch (e) {
        showToast('error', 'خطأ', e.message);
        btn.disabled = false;
        btn.textContent = 'تأكيد التغيير';
        resolve(false);
      }
    };
  });
}

// ── Edit user profile ──
export function editUserProfile(user) {
  return new Promise((resolve) => {
    document.getElementById('edit-user-modal')?.remove();

    const overlay = document.createElement('div');
    overlay.id = 'edit-user-modal';
    overlay.className = 'modal-overlay';

    overlay.innerHTML = `
      <div class="custom-modal">
        <div class="custom-modal-header">
          <h3 class="custom-modal-title">تعديل ملف ${escHtml(user.name || user.email)}</h3>
          <button type="button" class="custom-modal-close" id="edit-close-btn"><i class="bi bi-x-lg"></i></button>
        </div>
        <div class="modal-body py-3">
          <div class="form-group mb-3">
            <label class="form-label">الاسم</label>
            <input type="text" id="edit-name" class="form-control" value="${escHtml(user.name || '')}">
          </div>
          <div class="form-group mb-3">
            <label class="form-label">رقم الهاتف</label>
            <input type="text" id="edit-phone" class="form-control" value="${escHtml(user.phone || '')}">
          </div>
          <div class="form-group mb-3">
            <label class="form-label">تغيير الصورة الشخصية</label>
            <input type="file" id="edit-photo-file" class="form-control" accept="image/*">
            <div style="font-size:12px;color:var(--text-muted);margin-top:4px;">اتركه فارغاً للاحتفاظ بالصورة الحالية</div>
          </div>
        </div>
        <div class="custom-modal-footer">
          <button class="btn btn-outline" id="edit-cancel-btn">إلغاء</button>
          <button class="btn btn-primary" id="edit-save-btn">حفظ التعديلات</button>
        </div>
      </div>
    `;

    document.body.appendChild(overlay);

    const closeModal = () => { overlay.remove(); resolve(false); };
    document.getElementById('edit-close-btn').onclick = closeModal;
    document.getElementById('edit-cancel-btn').onclick = closeModal;
    overlay.addEventListener('click', e => { if (e.target === overlay) closeModal(); });

    document.getElementById('edit-save-btn').onclick = async () => {
      const btn = document.getElementById('edit-save-btn');
      btn.disabled = true;
      btn.textContent = 'جارٍ الحفظ...';

      const newName = document.getElementById('edit-name').value.trim();
      const newPhone = document.getElementById('edit-phone').value.trim();
      const fileInput = document.getElementById('edit-photo-file');

      try {
        const updateData = { name: newName, phone: newPhone };

        if (fileInput.files.length > 0) {
          btn.textContent = 'جارٍ الرفع...';
          const file = fileInput.files[0];
          const folder = 'profile_images';
          const urls = await uploadMultipleToSupabase([file], folder);
          if (urls && urls.length > 0) {
            updateData.photoURL = urls[0];
          }
        }

        await updateDoc(doc(db, 'users', user.docId), updateData);
        showToast('success', 'نجاح', 'تم تحديث بيانات المستخدم');
        overlay.remove();
        resolve(true);
      } catch (e) {
        showToast('error', 'خطأ', e.message);
        btn.disabled = false;
        btn.textContent = 'حفظ التعديلات';
        resolve(false);
      }
    };
  });
}

// ── Get user stats ──
export function getUserStats(users) {
  return {
    total: users.length,
    donors: users.filter(u => u.role === 'donor').length,
    beneficiaries: users.filter(u => u.role === 'beneficiary').length,
    volunteers: users.filter(u => u.role === 'volunteer').length,
    admins: users.filter(u => u.role === 'admin' || u.role === 'employee').length,
    active: users.filter(u => u.status === 'active').length,
    suspended: users.filter(u => u.status === 'suspended').length,
  };
}

// ── Render users table ──
export function renderUsersTable(users, tbody) {
  if (!tbody) return;

  if (users.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:40px;color:var(--text-muted)">لا يوجد مستخدمون</td></tr>`;
    return;
  }

  tbody.innerHTML = users.map(u => {
    const initials = getInitials(u.name || u.email || 'U');
    const isActive = u.status !== 'suspended';
    const toggleLabel = isActive ? '🚫 تعليق' : '✅ تفعيل';
    const statusBadge = isActive
      ? '<span class="badge badge-active">نشط</span>'
      : '<span class="badge badge-suspended">موقوف</span>';

    // Image or initials
    const avatarContent = u.photoURL 
      ? `<img src="${escHtml(u.photoURL)}" alt="avatar" style="width:100%;height:100%;object-fit:cover;border-radius:50%;">`
      : initials;

    return `
    <tr>
      <td>
        <div style="display:flex;align-items:center;gap:10px">
          <div class="avatar" style="overflow:hidden;">${avatarContent}</div>
          <div>
            <div class="bold">${escHtml(u.name || '—')}</div>
            <div class="text-sm text-muted">${escHtml(u.email || '')}</div>
          </div>
        </div>
      </td>
      <td class="text-sm text-muted">${escHtml(u.phone || '—')}</td>
      <td><span class="badge ${getRoleBadgeClass(u.role || u.accountType)}">${getRoleLabel(u.role || u.accountType)}</span></td>
      <td>${statusBadge}</td>
      <td class="text-sm text-muted">${timeAgo(u.createdAt)}</td>
      <td>
        <div style="display:flex;gap:6px;flex-wrap:wrap;">
          <button class="btn btn-sm btn-outline toggle-status-btn"
            data-user-id="${escHtml(u.docId)}"
            data-status="${escHtml(u.status || 'active')}"
            data-name="${escHtml(u.name || u.email)}">
            ${toggleLabel}
          </button>
          <button class="btn btn-sm btn-outline change-role-btn"
            data-user-id="${escHtml(u.docId)}"
            data-role="${escHtml(u.role || 'donor')}"
            data-name="${escHtml(u.name || u.email)}"
            title="تغيير الدور"
            style="color:var(--brand-400);border-color:var(--brand-400);">
            🔑 الدور
          </button>
          <button class="btn btn-sm btn-outline edit-user-btn"
            data-user-index="${allUsers.indexOf(u)}"
            title="تعديل المستخدم"
            style="color:var(--text-secondary);border-color:var(--border);">
            ✏️ تعديل
          </button>
        </div>
      </td>
    </tr>`;
  }).join('');

  // Attach status toggle events
  tbody.querySelectorAll('.toggle-status-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      await toggleUserStatus(btn.dataset.userId, btn.dataset.status, btn.dataset.name);
    });
  });

  // Attach role change events
  tbody.querySelectorAll('.change-role-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      await changeUserRole(btn.dataset.userId, btn.dataset.role, btn.dataset.name);
    });
  });

  // Attach edit user events
  tbody.querySelectorAll('.edit-user-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const idx = parseInt(btn.dataset.userIndex, 10);
      const userObj = allUsers[idx];
      if (userObj) {
        await editUserProfile(userObj);
      }
    });
  });
}
