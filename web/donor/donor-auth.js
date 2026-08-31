// donor-auth.js — مصادقة المتبرع لموقع تكافل
import { auth, db } from '../firebase-config.js';
import { uploadToSupabase } from '../supabase-config.js';
import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
  GoogleAuthProvider,
  signInWithPopup,
  sendPasswordResetEmail
} from 'https://www.gstatic.com/firebasejs/11.8.1/firebase-auth.js';
import {
  doc, getDoc, setDoc, serverTimestamp
} from 'https://www.gstatic.com/firebasejs/11.8.1/firebase-firestore.js';

const googleProvider = new GoogleAuthProvider();

// ── Roles allowed for donors ──
const DONOR_ROLES = ['donor', 'beneficiary', 'volunteer'];
const ADMIN_ROLES = ['admin'];

// ── Check if user is a donor ──
export async function checkDonorRole(uid) {
  try {
    const userDoc = await getDoc(doc(db, 'users', uid));
    if (!userDoc.exists()) return false;
    const data = userDoc.data();
    const role = data.role || data.accountType || 'donor';
    if (data.status === 'suspended') return false;
    // Return specific roles
    if (ADMIN_ROLES.includes(role)) return 'admin';
    if (role === 'beneficiary') return 'beneficiary';
    if (role === 'volunteer') return 'volunteer';
    // All other roles are considered donors
    return 'donor';
  } catch (e) {
    console.error('Error checking role:', e);
    return false;
  }
}

// ── Login ──
export async function loginDonor(email, password) {
  const cred = await signInWithEmailAndPassword(auth, email.trim(), password);
  const roleCheck = await checkDonorRole(cred.user.uid);
  if (roleCheck === 'admin') {
    await signOut(auth);
    throw new Error('ADMIN_REDIRECT');
  }
  if (!roleCheck) {
    await signOut(auth);
    throw new Error('هذا الحساب معلّق أو غير مسموح بالدخول.');
  }
  return cred.user;
}

// ── Register ──
export async function registerDonor(name, email, password, phone, role = 'donor', photoFile = null) {
  const cred = await createUserWithEmailAndPassword(auth, email.trim(), password);
  
  let photoURL = '';
  if (photoFile) {
    // ✅ Supabase Storage بدلاً من Firebase Storage
    const ext = photoFile.name.split('.').pop() || 'jpg';
    const path = `profile_pictures/${cred.user.uid}_${Date.now()}.${ext}`;
    photoURL = await uploadToSupabase(photoFile, path);
  }

  await setDoc(doc(db, 'users', cred.user.uid), {
    uid: cred.user.uid,
    name: name.trim(),
    email: email.trim(),
    phone: phone.trim(),
    role: role,
    photoURL: photoURL,
    paymentMethod: '',
    status: 'active',
    isAvailable: true,
    createdAt: serverTimestamp()
  });
  return cred.user;
}

// ── Register With Identity Verification (beneficiary / volunteer) ──
export async function registerWithVerification(name, email, password, phone, role, photoFile, docData) {
  const cred = await createUserWithEmailAndPassword(auth, email.trim(), password);
  const uid = cred.user.uid;

  // Upload profile photo
  let photoURL = '';
  if (photoFile) {
    const ext = photoFile.name.split('.').pop() || 'jpg';
    const path = `profile_pictures/${uid}_${Date.now()}.${ext}`;
    photoURL = await uploadToSupabase(photoFile, path);
  }

  // Upload identity document front
  let docFrontURL = '';
  if (docData.docFrontFile) {
    const ext = docData.docFrontFile.name.split('.').pop() || 'jpg';
    const path = `identity_docs/${uid}/front_${Date.now()}.${ext}`;
    docFrontURL = await uploadToSupabase(docData.docFrontFile, path);
  }

  // Upload identity document back (optional)
  let docBackURL = '';
  if (docData.docBackFile) {
    const ext = docData.docBackFile.name.split('.').pop() || 'jpg';
    const path = `identity_docs/${uid}/back_${Date.now()}.${ext}`;
    docBackURL = await uploadToSupabase(docData.docBackFile, path);
  }

  // Save user with pending_verification status
  await setDoc(doc(db, 'users', uid), {
    uid,
    name: name.trim(),
    email: email.trim(),
    phone: phone.trim(),
    role,
    photoURL,
    paymentMethod: '',
    status: 'pending_verification',   // Waiting admin approval
    isAvailable: false,
    createdAt: serverTimestamp()
  });

  // Save identity document record separately
  await setDoc(doc(db, 'identity_verifications', uid), {
    uid,
    name: name.trim(),
    email: email.trim(),
    role,
    docType: docData.docType,
    docNumber: docData.docNumber,
    docCountry: docData.docCountry || '',
    docExpiry: docData.docExpiry || '',
    reason: docData.reason || '',
    docFrontURL,
    docBackURL,
    verificationStatus: 'pending',     // 'pending' | 'approved' | 'rejected'
    adminNote: '',
    submittedAt: serverTimestamp()
  });

  return cred.user;
}

// ── Google Sign In ──
export async function loginWithGoogle() {
  const cred = await signInWithPopup(auth, googleProvider);
  const userDoc = await getDoc(doc(db, 'users', cred.user.uid));
  
  if (!userDoc.exists()) {
    // User is new, needs to pick a role
    return { needsRole: true, user: cred.user };
  }
  
  // Existing user
  const roleCheck = await checkDonorRole(cred.user.uid);
  if (roleCheck === 'admin') {
    await signOut(auth);
    throw new Error('ADMIN_REDIRECT');
  }
  if (!roleCheck) {
    await signOut(auth);
    throw new Error('هذا الحساب معلّق أو غير مسموح بالدخول.');
  }
  return { needsRole: false, user: cred.user };
}

export async function completeGoogleRegistration(user, role) {
  await setDoc(doc(db, 'users', user.uid), {
    uid: user.uid,
    name: user.displayName || 'مستخدم',
    email: user.email,
    phone: user.phoneNumber || '',
    role: role,
    photoURL: user.photoURL || '',
    paymentMethod: '',
    status: 'active',
    isAvailable: true,
    createdAt: serverTimestamp()
  });
  return user;
}

// ── Google Sign In With Verification (beneficiary/volunteer) ──
export async function completeGoogleRegistrationWithVerification(user, role, docData) {
  const uid = user.uid;

  // Upload identity document front
  let docFrontURL = '';
  if (docData.docFrontFile) {
    const ext = docData.docFrontFile.name.split('.').pop() || 'jpg';
    const path = `identity_docs/${uid}/front_${Date.now()}.${ext}`;
    docFrontURL = await uploadToSupabase(docData.docFrontFile, path);
  }

  // Upload identity document back (optional)
  let docBackURL = '';
  if (docData.docBackFile) {
    const ext = docData.docBackFile.name.split('.').pop() || 'jpg';
    const path = `identity_docs/${uid}/back_${Date.now()}.${ext}`;
    docBackURL = await uploadToSupabase(docData.docBackFile, path);
  }

  // Save user with pending_verification status
  await setDoc(doc(db, 'users', uid), {
    uid,
    name: user.displayName || 'مستخدم',
    email: user.email,
    phone: docData.phone || user.phoneNumber || '',
    role,
    photoURL: user.photoURL || '',
    paymentMethod: '',
    status: 'pending_verification',
    isAvailable: false,
    createdAt: serverTimestamp()
  });

  // Save identity document record separately
  await setDoc(doc(db, 'identity_verifications', uid), {
    uid,
    name: user.displayName || 'مستخدم',
    email: user.email,
    role,
    docType: docData.docType,
    docNumber: docData.docNumber,
    docCountry: docData.docCountry || '',
    docExpiry: docData.docExpiry || '',
    reason: docData.reason || '',
    docFrontURL,
    docBackURL,
    verificationStatus: 'pending',
    adminNote: '',
    submittedAt: serverTimestamp()
  });

  return user;
}

// ── Auth guard: redirect to login if not authenticated ──
export function requireDonorAuth(onUser) {
  return onAuthStateChanged(auth, async (user) => {
    if (!user) {
      window.location.href = 'index.html';
      return;
    }
    const roleCheck = await checkDonorRole(user.uid);
    if (roleCheck === 'admin') {
      window.location.href = '../dashboard.html';
      return;
    }
    if (!roleCheck) {
      await signOut(auth);
      window.location.href = 'index.html';
      return;
    }
    
    const path = window.location.pathname;
    
    // Redirect logic based on roles
    if (roleCheck === 'beneficiary' && !path.includes('beneficiary-app.html')) {
      window.location.href = 'beneficiary-app.html';
      return;
    }
    if (roleCheck === 'volunteer' && !path.includes('volunteer-app.html')) {
      window.location.href = 'volunteer-app.html';
      return;
    }
    if (roleCheck === 'donor' && (path.includes('beneficiary-app.html') || path.includes('volunteer-app.html'))) {
      window.location.href = 'app.html';
      return;
    }

    onUser(user, roleCheck);
  });
}

// ── Get donor profile ──
export async function getDonorProfile(uid) {
  try {
    const userDoc = await getDoc(doc(db, 'users', uid));
    if (userDoc.exists()) {
      return { uid, ...userDoc.data() };
    }
  } catch (e) {
    console.error(e);
  }
  return { uid, name: auth.currentUser?.email || 'متبرع', role: 'donor' };
}

// ── Logout ──
export async function logoutDonor() {
  await signOut(auth);
  window.location.href = 'index.html';
}

// ── Reset Password ──
export async function resetPasswordDonorEmail(email) {
  try {
    await sendPasswordResetEmail(auth, email.trim());
    return true;
  } catch (err) {
    console.error('Reset Password Error:', err);
    throw err;
  }
}
