// firebase-config.js — إعداد Firebase لموقع تكافل الإداري

import { initializeApp } from 'https://www.gstatic.com/firebasejs/11.8.1/firebase-app.js';
import { getAuth } from 'https://www.gstatic.com/firebasejs/11.8.1/firebase-auth.js';
import { getFirestore } from 'https://www.gstatic.com/firebasejs/11.8.1/firebase-firestore.js';
import { getStorage } from 'https://www.gstatic.com/firebasejs/11.8.1/firebase-storage.js';

const firebaseConfig = {
  apiKey: "AIzaSyDuWfztq7byGdV72cLPdNr3hd7PE25SIFc",
  authDomain: "takaful-f662e.firebaseapp.com",
  projectId: "takaful-f662e",
  storageBucket: "takaful-f662e.firebasestorage.app",
  messagingSenderId: "822889698478",
  appId: "1:822889698478:web:25cae60d23544650b5c93f",
  measurementId: "G-3H64GC88L7"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
export default app;
