// auth.js — منطق المصادقة للموقع الإداري

import { auth, db } from './firebase-config.js';
import {
  signInWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
  signOut,
  onAuthStateChanged,
  sendPasswordResetEmail
} from 'https://www.gstatic.com/firebasejs/11.8.1/firebase-auth.js';
import { doc, getDoc, updateDoc, setDoc } from 'https://www.gstatic.com/firebasejs/11.8.1/firebase-firestore.js';
import { showToast } from './utils.js';

// ── Check if user is admin ──
export async function checkAdminRole(uid) {
  try {
    const userDoc = await getDoc(doc(db, 'users', uid));
    if (!userDoc.exists()) return false;
    const data = userDoc.data();
    // Allow 'admin', 'employee', or legacy 'supervisor'
    const role = data.role || data.accountType;
    const isAllowed = (role === 'admin' || role === 'employee' || role === 'supervisor') && data.status !== 'suspended';
    
    // Auto-migrate supervisor to admin role to ensure Security Rules compatibility
    if (isAllowed && (!data.role || data.accountType === 'supervisor')) {
      try {
        await updateDoc(doc(db, 'users', uid), {
          role: 'admin',
          name: data.fullName || data.name || 'NRC',
          updatedAt: Date.now()
        });
      } catch (err) {
        console.warn('Auto migration failed:', err);
      }
    }
    
    return isAllowed;
  } catch (e) {
    console.error('Error checking role:', e);
    return false;
  }
}

export async function checkSupervisorRole(uid) {
  return await checkAdminRole(uid);
}

// ── Login ──
export async function loginWithEmail(email, password) {
  try {
    const cred = await signInWithEmailAndPassword(auth, email.trim(), password);
    const isAdmin = await checkAdminRole(cred.user.uid);
    if (!isAdmin) {
      await signOut(auth);
      throw new Error('هذا الحساب ليس لديه صلاحية. يُسمح للإدارة فقط.');
    }
    return cred.user;
  } catch (err) {
    // If it is the default admin and the account doesn't exist, seed it automatically
    if (email.trim() === 'admin@takaful.org' && password === 'admin123' &&
        (err.code === 'auth/user-not-found' || err.code === 'auth/invalid-credential' || err.message.includes('not found') || err.message.includes('credentials'))) {
      try {
        const { createUserWithEmailAndPassword } = await import('https://www.gstatic.com/firebasejs/11.8.1/firebase-auth.js');
        const { doc, setDoc } = await import('https://www.gstatic.com/firebasejs/11.8.1/firebase-firestore.js');
        
        const cred = await createUserWithEmailAndPassword(auth, email.trim(), password);
        await setDoc(doc(db, 'users', cred.user.uid), {
          uid: cred.user.uid,
          name: 'NRC',
          email: email.trim(),
          phone: '0913065203',
          role: 'admin',
          photoURL: '',
          paymentMethod: '',
          status: 'active',
          createdAt: Date.now()
        });
        return cred.user;
      } catch (createErr) {
        console.error('Failed to auto-seed default admin:', createErr);
        throw err;
      }
    }
    throw err;
  }
}

// ── Login with Google ──
export async function loginWithGoogle() {
  try {
    const provider = new GoogleAuthProvider();
    provider.setCustomParameters({ prompt: 'select_account' });
    
    const cred = await signInWithPopup(auth, provider);
    const user = cred.user;
    
    let isAdmin = await checkAdminRole(user.uid);
    
    if (!isAdmin) {
      await signOut(auth);
      throw new Error('هذا الحساب ليس لديه صلاحية. يُسمح للإدارة فقط.');
    }
    
    return user;
  } catch (err) {
    console.error('Google Login Error:', err);
    throw err;
  }
}

// ── Reset Password ──
export async function resetPasswordEmail(email) {
  try {
    await sendPasswordResetEmail(auth, email.trim());
    return true;
  } catch (err) {
    console.error('Reset Password Error:', err);
    throw err;
  }
}

// ── Logout ──
export async function logout() {
  await signOut(auth);
  window.location.href = 'index.html';
}

// ── Auth guard: redirect to login if not authenticated ──
export function requireAuth(onUser) {
  return onAuthStateChanged(auth, async (user) => {
    if (!user) {
      window.location.href = 'index.html';
      return;
    }
    const isAdmin = await checkAdminRole(user.uid);
    if (!isAdmin) {
      await signOut(auth);
      window.location.href = 'index.html';
      return;
    }
    onUser(user);
  });
}

// ── Get current user display info ──
export async function getCurrentUserInfo(uid) {
  try {
    const userDoc = await getDoc(doc(db, 'users', uid));
    if (userDoc.exists()) {
      return { uid, ...userDoc.data() };
    }
  } catch (e) {
    console.error(e);
  }
  return { uid, name: auth.currentUser?.email, role: 'admin' };
}
